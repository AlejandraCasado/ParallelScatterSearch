package grafo.cdp.algorithms;

import grafo.cdp.combiners.PathRelinking;
import grafo.cdp.experiment.Execution;
import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.cdp.util.FloatUtils;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.results.Result;
import grafo.optilib.tools.Timer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class APSS implements Algorithm<CDPInstance, CDPSolution> {

    private Constructive<CDPInstance, CDPSolution> c[];
    private Improvement<CDPSolution> ls[];
    private PathRelinking pr;
    private float beta;
    private int refSetSize;
    private CDPSolution bestSol;
    private int option;

    // Parallel execution
    private ExecutorService pool;

    public APSS(Constructive<CDPInstance, CDPSolution> c[], Improvement<CDPSolution> ls[], float beta, int refSetSize, int option) {
        this.c = c;
        this.ls = ls;
        this.beta = beta;
        this.refSetSize = refSetSize;
        this.option = option;
        this.pr = new PathRelinking(option);
    }

    @Override
    public Result execute(CDPInstance instance) {
        pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        bestSol = null;
        System.out.print(instance.getName()+"\t");
        Result r = new Result(instance.getName());
        Timer.initTimer();
        List<CDPSolution> initialPopulation = createInitialPopulation(instance, beta, r);
        List<CDPSolution> refSet = createRefSet(initialPopulation, refSetSize);
        System.out.print(bestSol.getOf()+"\t");

        int size = refSet.size();
        int improvesPR = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i+1; j < size; j++) {
                List<Future<CDPSolution>> futSols = new ArrayList<>(size*size);
                CDPSolution[] prSols = pr.combine(refSet.get(i), refSet.get(j));
                for (Improvement<CDPSolution> improvement : ls) {
                    for (CDPSolution prSol : prSols) {
                        if (prSol != null) {
                            CDPSolution prLocalSol = new CDPSolution(prSol);
                            futSols.add(pool.submit(() -> {
                                improvement.improve(prLocalSol);
                                return prLocalSol;
                            }));
                        }
                    }
                }
                for (Future<CDPSolution> futSol : futSols) {
                    CDPSolution interSol = null;
                    try {
                        interSol = futSol.get();
                        if (interSol.isBetterThan(bestSol)) {
                            bestSol.copy(interSol);
                            improvesPR++;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }



        double secs = Timer.getTime() / 1000.0;
        System.out.println(bestSol.getOf()+"\t"+improvesPR+"\t"+bestSol.getEvalComplete()+"\t"+secs);
        r.add("OF", bestSol.getOf());
        r.add("Time (s)", secs);

        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return r;
    }

    @Override
    public CDPSolution getBestSolution() {
        return bestSol;
    }

    private List<CDPSolution> createInitialPopulation(CDPInstance instance, float beta, Result r) {
        int constructions = (int) Math.ceil(instance.getN() * beta);
        List<CDPSolution> sols = new ArrayList<>(constructions);
        Set<CDPSolution> usedSols = new HashSet<>(constructions);
        float bestConst = -1;
        float bestLS = -1;
        for (int i = 0; i < constructions; i++) {
            List<Future<CDPSolution>> futSols = new ArrayList<>(c.length*ls.length);
            for (Constructive<CDPInstance, CDPSolution> constructive : c) {
                for (Improvement<CDPSolution> improvement : ls) {
                    futSols.add(pool.submit(() -> {
                        CDPSolution sol = constructive.constructSolution(instance);
                        if (Timer.getTime() > Execution.TIME_LIMIT) {
                            return sol;
                        }
                        improvement.improve(sol);
                        return sol;
                    }));
                }
            }
            for (Future<CDPSolution> futSol : futSols) {
                CDPSolution sol = null;
                try {
                    sol = futSol.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                if (!usedSols.contains(sol)) {
                    usedSols.add(sol);
                    sols.add(sol);
                    if (bestSol == null) {
                        bestSol = new CDPSolution(sol);
                    } else if (sol.isBetterThan(bestSol)) {
                        bestSol.copy(sol);
                    }
                }
            }
        }

        r.add("BestConst", bestConst);
        r.add("BestLS", bestLS);
        sols.sort((s1, s2) -> FloatUtils.compareFloat(s1.getOf(), s2.getOf()));
        return sols;
    }

    private List<CDPSolution> createRefSet(List<CDPSolution> initialPopulation, int refSetSize) {
        int promising = refSetSize / 2;
        int diverse = refSetSize-promising;
        List<CDPSolution> refSet = new ArrayList<>(refSetSize);
        for (int i = 0; i < promising; i++) {
            refSet.add(initialPopulation.remove(initialPopulation.size()-1));
        }
        int limit = Math.min(diverse, initialPopulation.size());
        for (int i = 0; i < limit; i++) {
            int diverseIdx = mostDiverse(initialPopulation, refSet);

            refSet.add(initialPopulation.remove(diverseIdx));
        }
        return refSet;
    }

    private int mostDiverse(List<CDPSolution> initialPopulation, List<CDPSolution> refSet) {
        int bestIndex = -1;
        int maxDistance = 0;
        for (int i = 0; i < initialPopulation.size(); i++) {
            CDPSolution sol = initialPopulation.get(i);
            int distToRefSet = Integer.MAX_VALUE;
            for (CDPSolution refSetSol : refSet) {
                int dist = sol.distance(refSetSol);
                if (dist < distToRefSet) {
                    distToRefSet = dist;
                }
            }
            if (distToRefSet > maxDistance) {
                maxDistance = distToRefSet;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @Override
    public String toString() {
        StringBuilder constString = new StringBuilder();
        for (Constructive<CDPInstance, CDPSolution> constructive : c) {
            constString.append(constructive).append(",");
        }
        StringBuilder lsString = new StringBuilder();
        for (Improvement<CDPSolution> improvement : ls) {
            lsString.append(improvement).append(",");
        }
        return this.getClass().getSimpleName()+"("+constString+lsString+beta+","+refSetSize+","+option+")";
    }
}
