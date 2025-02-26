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

public class SSS implements Algorithm<PInstance, PSolution> {
    
    private PSolution best;
    private Constructive<PInstance, PSolution> cons;
    private Improvement<PSolution> ls;
    private Combiner<PSolution> combiner;
    private int iters;
    private int globalIters;
    private int refSetSize;
    private double dtresh;
    private boolean updatedRefSet;

    private class Pair{
        private PSolution sol1;
        private PSolution sol2;
        public Pair(PSolution sol1, PSolution sol2){
            this.sol1 = sol1;
            this.sol2 = sol2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return (Objects.equals(sol1, pair.sol1) && Objects.equals(sol2, pair.sol2)) || (Objects.equals(sol1, pair.sol2) && Objects.equals(sol2, pair.sol1));
        }

        @Override
        public int hashCode() {
            return Objects.hash(sol1, sol2);
        }
    }
    public SSS(Constructive<PInstance, PSolution> cons, Improvement<PSolution> ls, Combiner<PSolution> combiner, int iters, int globalIters, int refSetSize, double dtresh){
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
        Result result = new Result(instance.getName());
        for (int it = 0; it < globalIters; it++) {
            this.best = null;
            RandomManager.setSeed(System.currentTimeMillis());
            System.out.print(instance.getName() + "\t");
            Timer.initTimer(1800 * 1000);
            List<PSolution> initialPopulation = createInitialPopulation(instance);
            List<PSolution> refSet = createRefSet(initialPopulation);
            this.updatedRefSet = true;
            Set<PSolution> combinedSols = new HashSet<>();
            Set<Pair> yetCombined = new HashSet<>();
            while (updatedRefSet && !Timer.timeReached()){
                this.updatedRefSet = false;
                List<PSolution> refSetAsList = new ArrayList<>(refSet);
                for (int i = 0; i < refSet.size(); i++) { //0 con 1, 0 con 2... 0 con n; 1 con 2, 1 con 3...
                    for (int j = i + 1; j < refSet.size(); j++) {
                        Pair p = new Pair(refSetAsList.get(i), refSetAsList.get(j));
                        if (yetCombined.contains(p)){
                            continue;
                        }else{
                            yetCombined.add(p);
                        }
                        PSolution combinedSol = this.combiner.combine(new PSolution(refSetAsList.get(i)), new PSolution(refSetAsList.get(j)));
                        ls.improve(combinedSol);
                        combinedSols.add(combinedSol);
                    }
                }
                updateRefSet(refSet, combinedSols);
            }
            double secs = Timer.getTime() / 1000.0;

            List<PSolution> auxRefset = new ArrayList<>(refSet);
            auxRefset.sort(Comparator.comparingInt(PSolution::getOfValue));
            this.best = auxRefset.getFirst();

            result.add("O.F Value it "+it, best.getOfValue());
            result.add("Time (s) it "+it, secs);
            System.out.println("Best it " + it +":" + best);
            System.out.println("O.F  it " + it +":" +best.getOfValue());
            System.out.println("Time (s) " + secs);
        }

        return result;
    }

    private List<PSolution> createRefSet(List<PSolution> initialPopulation) {
        List<PSolution> refSet = new ArrayList<>(this.refSetSize);
        int initSize = initialPopulation.size();
        for (int i = 0; i < Math.min(initSize / 2, this.refSetSize / 2); i++) {
            refSet.add(initialPopulation.removeFirst());
        }

        while (refSet.size() < Math.min(initSize, this.refSetSize)) {
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

    private List<PSolution> createInitialPopulation(PInstance instance) {
        List<PSolution> solutions = new ArrayList<>(iters);
        Set<PSolution> yetGenerated = new HashSet<>(256);

        for (int i = 0; i < iters; i++) {
            PSolution sol = cons.constructSolution(instance);
            ls.improve(sol);
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
        solutions.sort(Comparator.comparingInt(PSolution::getOfValue));
        return solutions;
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

    private List<PSolution> updateRefSet(List<PSolution> refSet) {
        List<PSolution> diverseAndIntenseRefset = new ArrayList<>();
        List<PSolution> auxRefset = new ArrayList<>(refSet);
        auxRefset.sort(Comparator.comparingInt(PSolution::getOfValue));
        for (int i = 0; i < this.refSetSize / 2 && !auxRefset.isEmpty(); i++) {
            diverseAndIntenseRefset.add(auxRefset.removeFirst());
        }

        while (diverseAndIntenseRefset.size() < this.refSetSize && diverseAndIntenseRefset.size() != refSet.size()) {
            int maxDist = -0x3f3f3f;
            PSolution solutionToAdd = null;
            for (PSolution pSolution : auxRefset) {
                int minDist = getMinDistanceFromSolToRefset(pSolution, diverseAndIntenseRefset);
                if (solutionToAdd == null || minDist > maxDist) {
                    solutionToAdd = new PSolution(pSolution);
                    maxDist = minDist;
                }
            }
            diverseAndIntenseRefset.add(solutionToAdd);
            auxRefset.remove(solutionToAdd);
        }
        return diverseAndIntenseRefset;
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
    public String toString(){
        return this.getClass().getSimpleName()+"("+cons+", "+ls+", "+combiner+")";
    }
}
