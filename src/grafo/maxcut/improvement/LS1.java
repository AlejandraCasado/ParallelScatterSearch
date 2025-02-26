package grafo.maxcut.improvement;

import grafo.maxcut.structure.MCInstance;
import grafo.maxcut.structure.MCSolution;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.tools.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LS1 implements Improvement<MCSolution> {
    @Override
    public void improve(MCSolution sol) {
        boolean improve=true;
        while(improve)
        {
            improve=loop(sol,sol.getInstance());
            if (Timer.timeReached()) break;
        }
    }

    private boolean loop(MCSolution sol, MCInstance instance) {
        int maxAmount=-0x3f3f3f;
        int maxAmountNode=-1;
        int amount;

        for(int node: instance.getNodes()){
            amount=sol.calculateAmountEdgesExchange(node);

            if(amount>maxAmount){
                maxAmount=amount;
                maxAmountNode=node;
            }
            if (Timer.timeReached()) return false;
        }

        if(maxAmount<=0){
            return false;
        }
        sol.moveNode(maxAmountNode,maxAmount);

        return true;
    }

    public String toString(){
        return this.getClass().getSimpleName();
    }
}
