package grafo.maxcut.constructive;

import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.tools.RandomManager;

import java.util.*;

public class C1 implements Constructive<MCInstance, MCSolution> {

    private class Tuple {
        int amountEdgesRight;
        int amountEdgesLeft;

        public Tuple(int amountEdgesLeft, int amountEdgesRight) {
            this.amountEdgesLeft = amountEdgesLeft;
            this.amountEdgesRight = amountEdgesRight;
        }

        public int getAmountEdgesRight() {
            return amountEdgesRight;
        }

        public int getAmountEdgesLeft() {
            return amountEdgesLeft;
        }
    }

    @Override
    public MCSolution constructSolution(MCInstance instance) {
        List<Integer> candidates;
        int gmin;
        int gmax;
        Map<Integer,Tuple> amountEdgesCandidates;
        candidates=new ArrayList<>(instance.getNumNodes());
        amountEdgesCandidates =new HashMap<>();

        MCSolution sol=new MCSolution(instance);
        candidates.addAll(instance.getNodes());
        float alpha= RandomManager.getRandom().nextFloat();
        int randomNodeR=RandomManager.getRandom().nextInt(candidates.size());
        sol.addNode(candidates.get(randomNodeR),true);
        candidates.remove(randomNodeR);

        int randomNodeL=RandomManager.getRandom().nextInt(candidates.size());
        sol.addNode(candidates.get(randomNodeL),false);
        candidates.remove(randomNodeL);

        while(!candidates.isEmpty()){
            gmin=0x3f3f3f;
            gmax=-0x3f3f3f;
            for (int node: candidates) {
                int amountEdgesRight=sol.calculateAmountEdges(node,true);
                int amountEdgesLeft=sol.calculateAmountEdges(node,false);
                amountEdgesCandidates.put(node,new Tuple(amountEdgesLeft,amountEdgesRight));
                if(amountEdgesLeft<gmin){
                    gmin=amountEdgesLeft;
                }
                if (amountEdgesRight<gmin){
                    gmin=amountEdgesRight;
                }
                if(amountEdgesLeft>gmax){
                    gmax=amountEdgesLeft;
                }
                if (amountEdgesRight>gmax){
                    gmax=amountEdgesRight;
                }
            }
            float threshold=gmin+alpha*(gmax-gmin);
            int nodeIdx=selectRandomFromRCL(threshold,amountEdgesCandidates,candidates);
            int node=candidates.get(nodeIdx);
            if(amountEdgesCandidates.get(node).getAmountEdgesRight()< amountEdgesCandidates.get(node).getAmountEdgesLeft()){
                sol.addNode(node,true,amountEdgesCandidates.get(node).getAmountEdgesLeft());
            }else{
                sol.addNode(node,false, amountEdgesCandidates.get(node).getAmountEdgesRight());
            }
            candidates.remove(nodeIdx);
            amountEdgesCandidates.remove(node);
        }
        return sol;
    }
    private int selectRandomFromRCL(float threshold,Map<Integer,Tuple> amountEdgesCandidates, List<Integer> candidates){
        for (int i=0;i<candidates.size();i++) {
            int candidate=candidates.get(i);
            if(threshold<= amountEdgesCandidates.get(candidate).getAmountEdgesLeft() || threshold<= amountEdgesCandidates.get(candidate).getAmountEdgesRight()){
                return i;
            }
        }
        return -1;
    }
    public String toString(){
        return getClass().getSimpleName();
    }
}
