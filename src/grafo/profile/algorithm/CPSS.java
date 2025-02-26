package grafo.profile.algorithm;

import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Combiner;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.results.Result;
import grafo.optilib.tools.RandomManager;
import grafo.optilib.tools.Timer;
import grafo.profile.structure.PInstance;
import grafo.profile.structure.PSolution;

import java.util.*;
import java.util.concurrent.*;

public class CPSS implements Algorithm<PInstance, PSolution> {

    private PSolution best;
    private Constructive<PInstance, PSolution>[] cons;
    private Improvement<PSolution>[] ls;
    private Combiner<PSolution>[] combiner;
    private int iters;
    private int globalIters;
    private int refSetSize;
    private final double dtresh;
    private boolean updatedRefSet;

    private ExecutorService pool;

    public CPSS(Constructive<PInstance, PSolution>[] cons, Improvement<PSolution>[] ls, Combiner<PSolution>[] combiner, int iters, int globalIters, int refSetSize, double dtresh) {
        this.cons = cons;
        this.ls = ls;
        this.combiner = combiner;
        this.iters = iters;
        this.globalIters = globalIters;
        this.refSetSize = refSetSize;
        this.dtresh = dtresh;
    }

    @Override
    public Result execute(PInstance instance) {
        int nThreads = Runtime.getRuntime().availableProcessors();
        pool = Executors.newFixedThreadPool(nThreads);
        Result result = new Result(instance.getName());
        for (int it = 0; it < globalIters; it++) {
            RandomManager.setSeed(System.currentTimeMillis());
            this.best = null;
            System.out.print(instance.getName() + "\t");
            Timer.initTimer(1800 * 1000);
            List<PSolution> initialPopulation = createInitialPopulation(instance);
            List<PSolution> refSet = createRefSet(initialPopulation);
            this.updatedRefSet = true;
            Set<PSolution> combinedSols = new HashSet<>();
            while (this.updatedRefSet && !Timer.timeReached()) {
                this.updatedRefSet = false;
                for (int i = 0; i < refSet.size(); i++) {
                    for (int j = i + 1; j < refSet.size(); j++) {
                        PSolution[] combinedSolutions = combineSolutions(refSet.get(i), refSet.get(j), combinedSols);
                        for (PSolution combinedSol : combinedSolutions) {
                            PSolution[] bestAndDiverse = improveSolution(combinedSol);
                            for (PSolution sol : bestAndDiverse) {
                                if (sol.isBetterThan(best)) {
                                    best.copy(sol);
                                }
                            }
                        }
                    }
                }
                updateRefSet(refSet, combinedSols);
            }
            double secs = Timer.getTime() / 1000.0;
            result.add("O.F Value it " + it, best.getOfValue());
            result.add("Time (s) it " + it, secs);
            System.out.println("Best it " + it + ":" + best);
            System.out.println("O.F  it " + it + ":" + best.getOfValue());
            System.out.println("Time (s) " + secs);
        }

        pool.shutdown();
        try {
            if (pool.awaitTermination(1, TimeUnit.DAYS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private void updateRefSet(List<PSolution> refSet, Set<PSolution> combinedSols) {
        refSet.sort(Comparator.comparingInt(PSolution::getOfValue));
        for (PSolution combinedSol : combinedSols) {
            if (combinedSol.isBetterThan(refSet.getFirst())
                    || (combinedSol.isBetterThan(refSet.getLast())
                    && Double.compare(1.*getMinDistanceFromSolToRefset(combinedSol, refSet),
                    dtresh * maxDistanceRefSet(refSet)) > 0)){
                replaceSolution(combinedSol, refSet);
                this.updatedRefSet = true;
            }
        }
    }

    private void replaceSolution(PSolution combinedSol, List<PSolution> auxRefset) {
        int minDistance = 0x3f3f3f;
        int profileOfCombined = combinedSol.getOfValue();
        int index = auxRefset.size() - 1;
        int indexToBeReplazed = index;

        while (index >= 0 && profileOfCombined < auxRefset.get(index).getOfValue()){
            int actDist = combinedSol.getDistanceToSol(auxRefset.get(index));
            if (actDist < minDistance){
                minDistance = actDist;
                indexToBeReplazed = index;
            }
            index--;
        }

        auxRefset.set(indexToBeReplazed, combinedSol);

    }

    private int maxDistanceRefSet(List<PSolution> auxRefset) {
        int n = auxRefset.size();
        int maxDistance = -0x3f3f3f;
        for (int i = 0; i < n; i++) {
            int actDistance = auxRefset.get(i).getDistanceToSol(auxRefset.get(n-(i+1)));
            if (actDistance > maxDistance){
                maxDistance = actDistance;
            }
        }
        return maxDistance;
    }

    private PSolution[] combineSolutions(PSolution origin, PSolution guide, Set<PSolution> combinedSols) {
        List<Future<PSolution>> futures = new ArrayList<>(cons.length * ls.length);
        for (Combiner<PSolution> comb : combiner) {
            PSolution localOrigin = new PSolution(origin);
            PSolution localGuide = new PSolution(guide);
            futures.add(pool.submit(() -> comb.combine(localOrigin, localGuide)));
        }
        List<PSolution> sols = new ArrayList<>(futures.size());
        PSolution bestLocalSol = null;
        for (Future<PSolution> future : futures) {
            PSolution sol;
            try {
                sol = future.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (bestLocalSol == null || sol.isBetterThan(bestLocalSol)) {
                bestLocalSol = new PSolution(sol);
            }
            sols.add(sol);
            combinedSols.add(sol);
        }
        PSolution diverse = bestLocalSol;
        int maxDistance = 0;
        for (PSolution sol : sols) {
            int d = sol.getDistanceToSol(bestLocalSol);
            if (d > maxDistance) {
                maxDistance = d;
                diverse = new PSolution(sol);
            }
        }
        return new PSolution[]{bestLocalSol, diverse};
    }

    private List<PSolution> createInitialPopulation(PInstance instance) {
        List<PSolution> solutions = new ArrayList<>(iters);
        Set<PSolution> yetGenerated = new HashSet<>(256);

        for (int i = 0; i < iters; i++) {
            PSolution[] bestAndDiverse = constructBestAndDiverse(instance);
            PSolution[] bestImproved = improveSolution(bestAndDiverse[0]);
            PSolution[] diverseImproved = improveSolution(bestAndDiverse[1]);

            checkNewSols(bestImproved, yetGenerated, solutions);
            checkNewSols(diverseImproved, yetGenerated, solutions);
        }

        solutions.sort(Comparator.comparingInt(PSolution::getOfValue));
        return solutions;
    }

    private void checkNewSols(PSolution[] bestImproved, Set<PSolution> yetGenerated, List<PSolution> solutions) {
        for (PSolution sol : bestImproved) {
            if (!yetGenerated.contains(sol)) {
                yetGenerated.add(sol);
                solutions.add(sol);
                if (best == null) {
                    best = new PSolution(sol);
                } else if (sol.isBetterThan(best)) {
                    best.copy(sol);
                }
            }
        }
    }

    private PSolution[] improveSolution(PSolution solution) {
        List<Future<PSolution>> futures = new ArrayList<>(cons.length * ls.length);
        for (Improvement<PSolution> ls : ls) {
            PSolution localSol = new PSolution(solution);
            futures.add(pool.submit(() -> {
                ls.improve(localSol);
                return localSol;
            }));
        }
        List<PSolution> sols = new ArrayList<>(futures.size());
        PSolution bestLocalSol = null;
        for (Future<PSolution> future : futures) {
            PSolution sol;
            try {
                sol = future.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (bestLocalSol == null || sol.isBetterThan(bestLocalSol)) {
                bestLocalSol = new PSolution(sol);
            }
            sols.add(sol);
        }
        PSolution diverse = bestLocalSol;
        int maxDistance = 0;
        for (PSolution sol : sols) {
            int d = sol.getDistanceToSol(bestLocalSol);
            if (d > maxDistance) {
                maxDistance = d;
                diverse = new PSolution(sol);
            }
        }
        return new PSolution[]{bestLocalSol, diverse};
    }

    private PSolution[] constructBestAndDiverse(PInstance instance) {
        List<Future<PSolution>> futures = new ArrayList<>(cons.length * ls.length);
        for (Constructive<PInstance, PSolution> c : cons) {
            futures.add(pool.submit(() -> c.constructSolution(instance)));
        }
        List<PSolution> sols = new ArrayList<>(futures.size());
        PSolution bestLocalSol = null;
        for (Future<PSolution> future : futures) {
            PSolution sol;
            try {
                sol = future.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (bestLocalSol == null || sol.isBetterThan(bestLocalSol)) {
                bestLocalSol = new PSolution(sol);
            }
            sols.add(sol);
        }
        PSolution diverse = bestLocalSol;
        int maxDistance = 0;
        for (PSolution sol : sols) {
            int d = sol.getDistanceToSol(bestLocalSol);
            if (d > maxDistance) {
                maxDistance = d;
                diverse = new PSolution(sol);
            }
        }
        return new PSolution[]{bestLocalSol, diverse};
    }

    private List<PSolution> createRefSet(List<PSolution> initialPopulation) {
        List<PSolution> refSet = new ArrayList<>(this.refSetSize);
        int initialSize = initialPopulation.size();
        for (int i = 0; i < Math.min(initialSize / 2, this.refSetSize / 2); i++) {
//        for (int i = 0; i < this.refSetSize / 2; i++) {
            refSet.add(initialPopulation.removeFirst());
        }

        while (refSet.size() < Math.min(initialSize, this.refSetSize)) {
//        while (refSet.size() < this.refSetSize) {
            int maxDist = -0x3f3f3f;
            PSolution solutionToAdd = null;
            for (PSolution pSolution : initialPopulation) {
                int minDist = getMinDistanceFromSolToRefset(pSolution, refSet);
                if (solutionToAdd == null || minDist > maxDist) {
                    solutionToAdd = new PSolution(pSolution);
                    maxDist = minDist;
                }
            }
            refSet.add(solutionToAdd);
            initialPopulation.remove(solutionToAdd);
        }
        return refSet;
    }

    private int getMinDistanceFromSolToRefset(PSolution refSol, List<PSolution> diverseAndIntenseRefset) {
        int minDistance = 0x3f3f3f;
        for (PSolution solInRefset : diverseAndIntenseRefset) {
            int actDistance = refSol.getDistanceToSol(solInRefset);
            if (actDistance < minDistance) {
                minDistance = actDistance;
            }
        }
        return minDistance;
    }

    @Override
    public PSolution getBestSolution() {
        return best;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + Arrays.toString(cons) + ", " + Arrays.toString(ls) + ", " + Arrays.toString(combiner) + ")";
    }
}
