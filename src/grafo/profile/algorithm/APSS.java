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

public class APSS implements Algorithm<PInstance, PSolution> {

    private PSolution best;
    private final Constructive<PInstance, PSolution>[] cons;
    private final Improvement<PSolution>[] ls;
    private final Combiner<PSolution>[] combiner;
    private final int iters;
    private final int globalIters;
    private final int refSetSize;
    private boolean updatedRefSet;
    private ExecutorService pool;
    private double dtresh;
    public APSS(Constructive<PInstance, PSolution>[] cons, Improvement<PSolution>[] ls, Combiner<PSolution>[] combiner, int iters, int globalIters, int refSetSize, double dtresh) {
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
                        List<Future<PSolution>> futuresComb = new ArrayList<>(refSet.size() * refSet.size());
                        for (Combiner<PSolution> com : combiner) {
                            int finalI = i;
                            int finalJ = j;
                            futuresComb.add(pool.submit(() -> com.combine(new PSolution(refSet.get(finalI)), new PSolution(refSet.get(finalJ)))));
                        }
                        List<Future<PSolution>> futures = new ArrayList<>(refSet.size() * refSet.size());
                        for (Future<PSolution> future : futuresComb) {
                            PSolution combinedSol;
                            try {
                                combinedSol = future.get();
                                for (Improvement<PSolution> improvement : ls) {
                                    PSolution localSol = new PSolution(combinedSol);
                                    futures.add(pool.submit(() -> {
                                        improvement.improve(localSol);
                                        return localSol;
                                    }));
                                }
                                combinedSols.add(combinedSol);
                                if (combinedSol.isBetterThan(best)){
                                    best.copy(combinedSol);
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        for (Future<PSolution> future : futures) {
                            PSolution intSol;
                            try {
                                intSol = future.get();
                                if (intSol.isBetterThan(best)){
                                    best.copy(intSol);
                                }
                            } catch (ExecutionException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                updateRefSet(refSet, combinedSols);
            }


            double secs = Timer.getTime() / 1000.0;
            result.add("O.F Value it "+it, best.getOfValue());
            result.add("Time (s) it "+it, secs);
            System.out.println("Best it " + it +":" + best);
            System.out.println("O.F  it " + it +":" +best.getOfValue());
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

    private List<PSolution> createInitialPopulation(PInstance instance) {
        List<PSolution> solutions = new ArrayList<>(iters);
        Set<PSolution> yetGenerated = new HashSet<>(256);

        for (int i = 0; i < iters; i++) {
            List<Future<PSolution>> futures = new ArrayList<>(cons.length * ls.length);
            for (Constructive<PInstance, PSolution> constructive : cons) {
                for (Improvement<PSolution> improvement : ls) {
                    futures.add(pool.submit(() -> {
                        PSolution sol = constructive.constructSolution(instance);
                        improvement.improve(sol);
                        return sol;
                    }));
                }
            }
            for (Future<PSolution> futSol : futures) {
                PSolution sol;
                try {
                    sol = futSol.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
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


        solutions.sort(Comparator.comparingInt(PSolution::getOfValue));
        return solutions;
    }

    private List<PSolution> createRefSet(List<PSolution> initialPopulation) {
        List<PSolution> refSet = new ArrayList<>(this.refSetSize);
//        for (int i = 0; i < Math.min(initialPopulation.size() / 2, this.refSetSize / 2); i++) {
        for (int i = 0; i < this.refSetSize / 2; i++) {
            refSet.add(initialPopulation.removeFirst());
        }

//        while (refSet.size() < Math.min(initialPopulation.size(), this.refSetSize)) {
        while (refSet.size() < this.refSetSize) {
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

    @Override
    public PSolution getBestSolution() {
        return best;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + Arrays.toString(cons) + ", " + Arrays.toString(ls) + ", " + Arrays.toString(combiner) + ")";
    }
}
