package grafo.maxcut.algorithm;

import grafo.maxcut.combiner.CB1;
import grafo.maxcut.combiner.CB2;
import grafo.maxcut.combiner.CB3;
import grafo.maxcut.combiner.Combiner;
import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Algorithm;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.results.Result;
import grafo.optilib.tools.RandomManager;
import grafo.optilib.tools.Timer;

import java.util.*;
import java.util.concurrent.*;

public class APSS implements Algorithm<MCInstance, MCSolution> {

    public static class Candidate {
        private MCSolution solution;
        private final int of;
        private boolean combined=false;
        public Candidate (MCSolution solution, int of) {
            this.of=of;
            this.solution=solution;
        }
        public int getOF () {
            return of;
        }
        public MCSolution getSolution () {
            return solution;
        }
        public void setCombined(Boolean value){combined=value;}
        public Boolean isCombined(){return combined;}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Candidate candidate = (Candidate) o;
            return of == candidate.of && solution.equals(candidate.getSolution());
        }

        @Override
        public int hashCode() {
            return Objects.hash(solution, of);
        }

        @Override
        public String toString() {
            return solution.toString();
        }
    }

    private final Constructive<MCInstance, MCSolution> constructives [];
    private final Improvement<MCSolution> improvements [];
    private Combiner combiners [];
    private final int refSetSize=10;
    private MCSolution best;
    private final int numSol;
    private boolean updated;
    private ExecutorService pool;

    public APSS(int numSol, Constructive<MCInstance, MCSolution>[] constructives, Improvement<MCSolution> [] improvements, Combiner [] combiners){
        this.constructives=constructives;
        this.improvements=improvements;
        this.numSol=numSol;
        this.combiners=combiners;
        updated=true;
    }

    @Override
    public Result execute(MCInstance instance) {
        Timer.initTimer(1800 * 1000);
        pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Candidate> initialPopulation=new ArrayList<>(grasp(instance));
        best=new MCSolution(instance);
        Candidate[] refSet=initializeRefSet(initialPopulation);
        int bestOF = 0;
        for(int i=0; i<50; i++){
            if(!updated){
                if (bestOF <refSet[0].getOF()){
                    bestOF =refSet[0].getOF();
                    best.copy(refSet[0].getSolution());
                }
                initialPopulation.clear();
                initialPopulation.addAll(grasp(instance));
                if (Timer.timeReached()) break;
                initializeRefSet(initialPopulation);
                updated=true;
            }else{
                List<MCSolution> pool=combination(refSet);
                updateRefSet(pool,refSet);
            }
            if (Timer.timeReached()) break;
        }
        Result result= new Result(instance.getName());
        result.add("cuts", refSet[0].getOF());
        result.add("time",Timer.getTime()/1000.0);
        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.DAYS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private List<MCSolution> combination(Candidate [] refSet){
        List<MCSolution> prSols=new ArrayList<>();
        List<Future<MCSolution>> futSols = new ArrayList<>(refSetSize*refSetSize);
        for (int i=0; i<refSet.length-1;i++) {
            MCSolution sol1=refSet[i].getSolution();
            if(refSet[i].isCombined()){
                continue;
            }
            refSet[i].setCombined(true);
            List<Future<List<MCSolution>>> localFutSols = new ArrayList<>(refSetSize*refSetSize);
            for (int j=i+1; j<refSet.length;j++) {
                MCSolution sol2=refSet[j].getSolution();
                for (Combiner combiner : combiners) {
                    localFutSols.add(pool.submit(() -> combiner.combine(sol1, sol2)));
                }
                for (Future<List<MCSolution>> futSol : localFutSols) {
                    try {
                        for (Improvement<MCSolution> improvement : improvements) {
                            for (MCSolution prSol : futSol.get()) {
                                if (prSol != null) {
                                    futSols.add(pool.submit(() -> {
                                        MCSolution prLocalSol = new MCSolution(prSol);
                                        if (Timer.timeReached()) return prLocalSol;
                                        improvement.improve(prLocalSol);
                                        return prLocalSol;
                                    }));
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        for (Future<MCSolution> futSol : futSols) {
            try {
                prSols.add(futSol.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return prSols;
    }

    private Candidate[] initializeRefSet(List<Candidate> initialPopulation){
        Candidate[] refSet=new Candidate [refSetSize];
        initialPopulation.sort((o1,o2)-> -Integer.compare(o1.getOF(),o2.getOF()));
        for(int i=0; i<refSetSize/2;i++){
            Candidate candidateAux=initialPopulation.remove(0);
            refSet[i]= new Candidate(candidateAux.getSolution(), candidateAux.getSolution().getOF());
        }
        ArrayList<MCSolution> differentSolutions=new ArrayList<>(divSolutionsTraditional(initialPopulation,refSet));
        differentSolutions.sort((o1, o2) -> -Integer.compare(o1.getOF(),o2.getOF()));
        for(int i=0; i<refSetSize/2;i++){
            refSet[i+(refSetSize/2)]= new Candidate(differentSolutions.get(i), differentSolutions.get(i).getOF());
        }
        return refSet;
    }

    public boolean containedIn(MCSolution newSol, Candidate[] solutionsSet){
        for (Candidate candidate : solutionsSet) {
            boolean equals = newSol.equals(candidate.getSolution());
            if (equals) {
                return true;
            }
        }
        return false;
    }

    private void updateRefSet(List<MCSolution> pool, Candidate[] refSet){
        updated=false;
        for(MCSolution newSol: pool){
            if(containedIn(newSol, refSet) || newSol.getOF()<refSet[refSetSize-1].getSolution().getOF()){
                continue;
            }
            int newSolPos=-1;
            int mostSimilarValue=0x3F3F3F3F;
            int mostSimilar=-1;
            boolean found=false;
            for (int i=0; i< refSetSize;i++){
                if(refSet[i].getOF()< newSol.getOF() && !found && !refSet[i].solution.equals(newSol)){
                    newSolPos=i;
                    found=true;
                }
                int diff=newSol.calculateDifferences(refSet[i].getSolution());
                if(diff<mostSimilarValue && found){
                    mostSimilarValue=diff;
                    mostSimilar=i;
                }
            }
            if(newSolPos!=-1){
                updated=true;
                Candidate previousSol=null;
                for(int i=0;i<refSetSize;i++){
                    previousSol=refSet[i];
                    if(newSolPos==i){
                        refSet[i]= new Candidate(newSol, newSol.getOF());
                    }else if(i>newSolPos && i<=mostSimilar){
                        refSet[i]=previousSol;
                    }
                }
            }
        }
    }
    private Set<Candidate> grasp(MCInstance instance){
        int margin=50;
        int constructions=numSol+margin;
        Set<Candidate> sols=new HashSet<>(constructions);
        Set<MCSolution> usedSols = new HashSet<>(constructions);
        List<Future<MCSolution>> futSols = new ArrayList<>(constructives.length*improvements.length);
        for(int i=0; i<constructions;i++) {
            for (Constructive<MCInstance, MCSolution> constructive : constructives) {
                for (Improvement<MCSolution> improvement : improvements) {
                    futSols.add(pool.submit(() -> {
                        if (Timer.timeReached()) return null;
                        MCSolution sol = constructive.constructSolution(instance);
                        if (Timer.timeReached()) return sol;
                        improvement.improve(sol);
                        return sol;
                    }));
                }
            }
        }
        for (Future<MCSolution> futSol : futSols) {
            MCSolution sol = null;
            try {
                sol = futSol.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (sol != null && !usedSols.contains(sol)) {
                usedSols.add(sol);
                Candidate c=new Candidate(sol, sol.getOF());
                sols.add(c);
            }
        }
        return sols;
    }

    private List<MCSolution> divSolutionsTraditional(List<Candidate> initialPopulation, Candidate [] refSet){
        List<MCSolution> divSols=new ArrayList<>(refSetSize/2);
        for(int j=0; j<refSetSize/2;j++){
            int maxDiff=0;
            int bestSolIdx=-1;
            for(int i=0; i<initialPopulation.size();i++){
                MCSolution sol=initialPopulation.get(i).getSolution();
                for(int k=0; k<refSetSize/2;k++){
                    MCSolution refSetSol=refSet[k].getSolution();
                    int diff=sol.calculateDifferences(refSetSol);
                    if(diff>maxDiff){
                        bestSolIdx=i;
                        maxDiff=diff;
                    }
                }
            }
            divSols.add(initialPopulation.get(bestSolIdx).getSolution());
            initialPopulation.remove(bestSolIdx);
        }
        return divSols;
    }
    @Override
    public MCSolution getBestSolution() {
        return best;
    }
    public String toString(){
        return this.getClass().getSimpleName();
    }
}
