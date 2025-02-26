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

import java.sql.Time;
import java.util.*;

public class SSS implements Algorithm<MCInstance, MCSolution> {

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
            return Objects.equals(solution, candidate.solution);
        }

        @Override
        public int hashCode() {
            return Objects.hash(solution);
        }

        @Override
        public String toString() {
            return solution.toString();
        }
    }

    private final Constructive<MCInstance, MCSolution> constructive;
    private final Improvement<MCSolution> improvement;
    private final int numSol;
    private List<Candidate> solutions;
    private Candidate [] refSet;
    private final int refSetSize=10;
    private MCSolution best;
    private final Combiner cb3;
    private boolean updated;

    public SSS(int numSol, Constructive<MCInstance, MCSolution> constructive, Improvement<MCSolution> improvement){
        this.constructive=constructive;
        this.improvement=improvement;
        this.numSol=numSol;
        solutions= new ArrayList<>();
        refSet=new Candidate [refSetSize];
        cb3=new CB3();
        updated=true;
    }
    @Override
    public Result execute(MCInstance instance) {
        Timer.initTimer(1800 * 1000);
        best=new MCSolution(instance);
        solutions = new ArrayList<>(grasp(instance));
        refSet = new Candidate [refSetSize];
        initializeRefSet();
        int bestOF = 0;
        for(int i=0; i<50; i++){
            if(!updated){
                int cutsActualBest=refSet[0].getSolution().getOF();
                if (bestOF <cutsActualBest){
                    bestOF =cutsActualBest;
                    best= new MCSolution(refSet[0].getSolution());
                }
                solutions.clear();
                solutions.addAll(grasp(instance));
                if (Timer.timeReached()) break;
                initializeRefSet();
                updated=true;
            }else{
                List<MCSolution> pool=combination();
                updateRefSet(pool);
            }
            if (Timer.timeReached()) break;
        }
        if (refSet.length > 0) {
            int cutsActualBest = refSet[0].getSolution().getOF();
            if (bestOF < cutsActualBest) {
                bestOF = cutsActualBest;
                best = refSet[0].getSolution();
            }
        }
        Result result= new Result(instance.getName());
        result.add("cuts", bestOF);
        result.add("time",Timer.getTime()/1000.0);
        return result;
    }

    private List<MCSolution> combination(){
        List<MCSolution> pool=new ArrayList<>();
        for (int i=0; i<refSet.length-1;i++) {
            MCSolution sol1=refSet[i].getSolution();
            if(refSet[i].isCombined()){
                continue;
            }
            refSet[i].setCombined(true);
            for (int j=i+1; j<refSet.length;j++) {
                MCSolution sol2=refSet[j].getSolution();
                List<MCSolution> localSols;
                localSols=cb3.combine(sol1,sol2);
                for(MCSolution localSol:localSols){
                    int k=Math.floorDiv((sol1.getOF()*sol1.getK())+(sol2.getOF()*sol2.getK()),(sol1.getOF()+sol2.getOF()));
                    localSol.setK(k);
                    improvement.improve(localSol);
                    pool.add(localSol);
                    if (Timer.timeReached()) {
                        return pool;
                    }
                }
            }
        }
        return pool;
    }

    private void initializeRefSet(){
        solutions.sort((o1,o2)-> -Integer.compare(o1.getOF(),o2.getOF()));
        for(int i=0; i<refSetSize/2;i++){
            Candidate candidateAux=solutions.remove(0);
            refSet[i]= new Candidate(candidateAux.getSolution(), candidateAux.getSolution().getOF());
        }
        ArrayList<MCSolution> differentSolutions=new ArrayList<>(divSolutionsTraditional());
        differentSolutions.sort((o1, o2) -> -Integer.compare(o1.getOF(),o2.getOF()));
        for(int i=0; i<refSetSize/2;i++){
            if (differentSolutions.size() <= i) break;
            refSet[i+(refSetSize/2)]= new Candidate(differentSolutions.get(i), differentSolutions.get(i).getOF());
        }
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

    private void updateRefSet(List<MCSolution> pool){
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
                if(refSet[i].getOF()< newSol.getOF() && !found){
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
        Set<Candidate> solutionsSet=new HashSet<>(numSol);
        for(int i=0; i<numSol+margin;i++) {
            MCSolution sol = constructive.constructSolution(instance);
            improvement.improve(sol);
            solutionsSet.add(new Candidate(sol, sol.getOF()));
            if (solutionsSet.size() == numSol) break;
            if (Timer.timeReached()) break;
        }
        return solutionsSet;
    }

    private List<MCSolution> divSolutionsTraditional(){
        List<MCSolution> divSols=new ArrayList<>(refSetSize/2);
        for(int j=0; j<refSetSize/2;j++){
            int maxDiff=0;
            int bestSolIdx=-1;
            for(int i=0; i<solutions.size();i++){
                MCSolution sol=solutions.get(i).getSolution();
                for(int k=0; k<refSetSize/2;k++){
                    MCSolution refSetSol=refSet[k].getSolution();
                    int diff=sol.calculateDifferences(refSetSol);
                    if(diff>maxDiff){
                        bestSolIdx=i;
                        maxDiff=diff;
                    }
                }
            }
            if (bestSolIdx >= 0) {
                divSols.add(solutions.get(bestSolIdx).getSolution());
                solutions.remove(bestSolIdx);
            }
        }
        return divSols;
    }

    @Override
    public MCSolution getBestSolution() {
        return best;
    }

    public String toString(){
        return this.getClass().getSimpleName()+"("+constructive.toString()+(improvement!=null?"+"+improvement.toString():"")+")";
    }
}
