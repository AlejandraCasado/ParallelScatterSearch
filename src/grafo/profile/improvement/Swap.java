package grafo.profile.improvement;

import grafo.optilib.metaheuristics.Improvement;
import grafo.profile.structure.PSolution;

public class Swap implements Improvement<PSolution> {
    @Override
    public void improve(PSolution sol) {
        boolean improved = true;
        while (improved) {
            improved = false;
            int prevOfValue = sol.getOfValue();
            for (int i = 1; i < sol.getNumberOfNodes(); i++) {
                for (int j = i + 1; j < sol.getNumberOfNodes(); j++) {
                    sol.swapNodes(i, j);
                    int actVal = sol.getOfValue();
                    if (actVal >= prevOfValue) {
                        sol.swapNodes(i, j);
                    }else{
                        improved = true;
                        prevOfValue = actVal;
                    }
                }
            }
        }
    }

    @Override
    public String toString(){
        return this.getClass().getSimpleName();
    }
}
