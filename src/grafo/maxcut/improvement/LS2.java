package grafo.maxcut.improvement;

import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.tools.Timer;

public class LS2 implements Improvement<MCSolution> {
    @Override
    public void improve(MCSolution sol) {
        boolean improve=true;
        while(improve)
        {
            improve=loop(sol,sol.getInstance());
        }
    }

    private boolean loop(MCSolution sol, MCInstance instance){
        int maxDepth=3;
        boolean improvement=false;
        int originalOF=sol.getOF();
        MCSolution originalSolution=new MCSolution(sol);
        MCSolution bestSolution=new MCSolution(instance);
        int bestOF=originalOF;
        for(int i=0; i<instance.getNumNodes();i++){
            int depth=0;
            int node=i;
            while(depth<maxDepth){
                sol.moveNode(node,0);
                if(sol.getOF()>bestOF){
                    break;
                }
                int j=0;
                boolean found=false;
                while(j<instance.getAdjacencyList()[node].size() && !found){
                    int neighbor=instance.getAdjacencyList()[node].get(j);
                    if(sol.getNodesBelonging().get(neighbor) != sol.getNodesBelonging().get(node)){
                        depth++;
                        node=neighbor;
                        found=true;
                    }
                    j++;
                }
                if(!found){
                    break;
                }
                if (Timer.timeReached()) break;
            }
            if(sol.getOF()>bestOF){
                bestOF=sol.getOF();
                bestSolution.copy(sol);

                improvement=true;
            }
            sol.copy(originalSolution);
            if (Timer.timeReached()) break;
        }
        if(improvement){
            sol.copy(bestSolution);
        }
        return improvement;
    }

    public String toString(){
        return this.getClass().getSimpleName();
    }
}
