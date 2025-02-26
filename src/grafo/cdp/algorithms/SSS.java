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

public class SSS implements Algorithm<CDPInstance, CDPSolution> {

    private Constructive<CDPInstance, CDPSolution> c;
    private Improvement<CDPSolution> ls;
    private PathRelinking pr;
    private float beta;
    private int refSetSize;
    private CDPSolution bestSol;
    private int option;

    public SSS(Constructive<CDPInstance, CDPSolution> c, Improvement<CDPSolution> ls, float beta, int refSetSize, int option) {
        this.c = c;
        this.ls = ls;
        this.beta = beta;
        this.refSetSize = refSetSize;
        this.option = option;
        this.pr = new PathRelinking(option);
    }

    @Override
    public Result execute(CDPInstance instance) {
        bestSol = null;
        System.out.print(instance.getName()+"\t");
        Result r = new Result(instance.getName());
        Timer.initTimer();
        List<CDPSolution> initialPopulation = createInitialPopulation(instance, beta, r);
        List<CDPSolution> refSet = createRefSet(initialPopulation, refSetSize);
        System.out.print(bestSol.getOf()+"\t");

        int size = refSet.size();
        int improvesPR = 0;
        LIMITTIME:
        for (int i = 0; i < size; i++) {
            for (int j = i+1; j < size; j++) {
                CDPSolution[] interSol = pr.combine(refSet.get(i), refSet.get(j));
                for (CDPSolution prSol : interSol) {
                    if (prSol != null) {
                        ls.improve(prSol);
                        if (prSol.isBetterThan(bestSol)) {
                            bestSol.copy(prSol);
                            improvesPR++;
                        }
                        if (Timer.getTime() > Execution.TIME_LIMIT) {
                            break LIMITTIME;
                        }
                    }
                }
            }
        }

        double secs = Timer.getTime() / 1000.0;
        System.out.println(bestSol.getOf()+"\t"+improvesPR+"\t"+bestSol.getEvalComplete()+"\t"+secs);
        r.add("OF", bestSol.getOf());
        r.add("Time (s)", secs);
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
            CDPSolution sol = c.constructSolution(instance);
            if (Float.compare(sol.getOf(), bestConst) > 0) {
                bestConst = sol.getOf();
            }
            ls.improve(sol);
            if (Float.compare(sol.getOf(), bestLS) > 0) {
                bestLS = sol.getOf();
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
            if (Timer.getTime() > Execution.TIME_LIMIT) {
                break;
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
        return this.getClass().getSimpleName()+"("+c+","+ls+","+beta+","+refSetSize+","+option+")";
    }
}
