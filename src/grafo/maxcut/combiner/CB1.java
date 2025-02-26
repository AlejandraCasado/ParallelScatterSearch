package grafo.maxcut.combiner;

import grafo.maxcut.structure.MCSolution;
import grafo.optilib.tools.RandomManager;
import grafo.optilib.tools.Timer;

import java.util.ArrayList;
import java.util.List;

public class CB1 implements Combiner {
    float r=0.5f;

    @Override
    public List<MCSolution> combine(MCSolution s1, MCSolution s2) {
        int cut1=s1.getOF();
        int cut2=s2.getOF();
        List<MCSolution> solutions=new ArrayList<>();
        MCSolution newSol1=new MCSolution(s1.getInstance());
        generateNewSol(s1,s2,cut1,cut2, newSol1);
        solutions.add(newSol1);
        if (Timer.timeReached()) return solutions;
        MCSolution newSol2=new MCSolution(s1.getInstance());
        generateNewSol(s1,s2,cut1,cut2, newSol2);
        solutions.add(newSol2);
        return solutions;
    }

    private void generateNewSol(MCSolution s1, MCSolution s2, int cut1, int cut2, MCSolution newSol){
        for(int i=0; i<s1.getInstance().getNumNodes();i++){
            int inRightSetS1= s1.getNodesBelonging().get(i)? 1:0;
            int inRightSetS2= s2.getNodesBelonging().get(i)? 1:0;
            float score=((cut1*inRightSetS1)+(cut2*inRightSetS2))/((cut1+cut2)*1.0f);
            r= RandomManager.getRandom().nextFloat();
            newSol.addNode(i, !(score <= r));
        }
        newSol.setCombiner("cb1");
    }
}
