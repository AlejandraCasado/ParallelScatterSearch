package grafo.maxcut.combiner;

import grafo.maxcut.structure.MCSolution;

import java.util.*;

public class CB3 implements Combiner {

    @Override
    public List<MCSolution> combine(MCSolution s1, MCSolution s2) {
        BitSet diff=new BitSet(s1.getNodesBelonging().size());
        diff.or(s1.getNodesBelonging());
        diff.xor(s2.getNodesBelonging());

        MCSolution newSol1=new MCSolution(s1);
        pathRelinking(diff,newSol1,s1,s2);
        MCSolution newSol2=new MCSolution(s2);
        pathRelinking(diff,newSol2,s1,s2);

        List<MCSolution> solutions=new ArrayList<>();
        newSol1.setCombiner("cb3");
        newSol2.setCombiner("cb3");
        solutions.add(newSol1);
        solutions.add(newSol2);

        return solutions;
    }

    private void pathRelinking(BitSet differentNodes, MCSolution newSol, MCSolution sol1, MCSolution sol2){
        for(int i=0;i<differentNodes.cardinality()/2;i++){
            int bestValue=-0x3F3F3F3F;
            int bestNode=-1;
            for(int node=differentNodes.nextSetBit(0);node>=0;node=differentNodes.nextSetBit(node+1)){
                int value=newSol.calculateAmountEdgesExchange(node);
                if(value>bestValue){
                    bestNode=node;
                    bestValue=value;
                }
            }
            differentNodes.flip(bestNode);
            newSol.moveNode(bestNode,bestValue);
        }
    }
}
