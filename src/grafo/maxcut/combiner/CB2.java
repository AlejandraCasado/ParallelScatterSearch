package grafo.maxcut.combiner;

import grafo.maxcut.structure.MCSolution;
import grafo.optilib.tools.RandomManager;

import java.util.*;

public class CB2 implements Combiner {

    @Override
    public List<MCSolution> combine(MCSolution s1, MCSolution s2) {
        MCSolution newSol1=new MCSolution(s1.getInstance());
        MCSolution newSol2=new MCSolution(s2.getInstance());
        BitSet intersection=new BitSet(s1.getNodesBelonging().size());
        intersection.or(s1.getNodesBelonging());
        intersection.and(s2.getNodesBelonging());
        initializeSols(intersection,newSol1,newSol2,s1);

        for (int i=0; i<s1.getInstance().getNumNodes();i++){
            if(!intersection.get(i)){
                int edgesWithR=newSol1.calculateAmountEdges(i, true);
                int edgesWithL=newSol1.calculateAmountEdges(i, false);
                if(edgesWithL>edgesWithR){
                    newSol1.addNode(i,true, edgesWithL);
                }else if(edgesWithL<edgesWithR){
                    newSol1.addNode(i,false,edgesWithR);
                }else{
                    boolean rnd=RandomManager.getRandom().nextBoolean();
                    newSol1.addNode(i,rnd,rnd?edgesWithL:edgesWithR);
                }
                boolean random= RandomManager.getRandom().nextBoolean();
                newSol2.addNode(i,random);
            }
        }
        List<MCSolution> solutions=new ArrayList<>();
        newSol1.setCombiner("cb2");
        newSol2.setCombiner("cb2");
        solutions.add(newSol1);
        solutions.add(newSol2);
        return solutions;
    }

    private void initializeSols(BitSet intersection, MCSolution newSol1, MCSolution newSol2, MCSolution sol1){
        for(int i=intersection.nextSetBit(0);i>=0;i=intersection.nextSetBit(i+1)){
            newSol1.addNode(i,sol1.getNodesBelonging().get(i));
            newSol2.addNode(i,sol1.getNodesBelonging().get(i));
        }
    }
}
