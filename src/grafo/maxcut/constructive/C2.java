package grafo.maxcut.constructive;

import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.tools.RandomManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class C2 implements Constructive<MCInstance, MCSolution> {

    @Override
    public MCSolution constructSolution(MCInstance instance) {
        List<Integer> candidates;
        int gmin;
        int gmax;
        Map<Integer,Integer> increaseValue;
        candidates=new ArrayList<>(instance.getNumNodes());
        increaseValue=new HashMap<>();
        float alpha=0.75f;
        boolean stop=false;
        MCSolution sol=new MCSolution(instance);
        candidates.addAll(instance.getNodes());
        sol.setNodesRight(0,instance.getNumNodes());
        while(!stop){
            gmin=0x3f3f3f;
            gmax=-0x3f3f3f;
            int increase;
            int count=0;
            for (int node: candidates) {
                increase=cutValueIncrease(node,sol,increaseValue);
                if(increase<0){
                    count++;
                }
                if(increase<gmin){
                    gmin=increase;
                }
                if(increase>gmax){
                    gmax=increase;
                }
            }
            if(gmin<0){
                gmin=0;
            }
            if(count==candidates.size()){
                stop=true;
            }else{
                float threshold=gmin+alpha*(gmax-gmin);
                int idxNode=selectRandomFromRCL(threshold,candidates,increaseValue);
                int node=candidates.get(idxNode);
                sol.moveNode(node, increaseValue.get(node));
                candidates.remove(idxNode);
                increaseValue.remove(node);
            }
        }
        return sol;
    }

    private int cutValueIncrease(int node,MCSolution sol, Map<Integer,Integer> increaseValue){
        int increase= sol.calculateAmountEdgesExchange(node);
        increaseValue.put(node,increase);
        return increase;
    }

    private int selectRandomFromRCL(float threshold,List<Integer> candidates, Map<Integer,Integer> increaseValue){
        int rnd=RandomManager.getRandom().nextInt(candidates.size());
        for (int i=rnd;i<candidates.size()+rnd;i++) {
            int candidate=candidates.get(i % candidates.size());
            if(threshold<=increaseValue.get(candidate)){
                return i % candidates.size();
            }
        }
        return -1;
    }
    public String toString(){
        return getClass().getSimpleName();
    }

}
