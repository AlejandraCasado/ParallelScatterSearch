package grafo.cdp.constructives;

import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.optilib.metaheuristics.Constructive;

public class DGM implements Constructive<CDPInstance, CDPSolution> {

    private C1 c1;
    private float alpha;
    private int firstEdge;
    private String lastInstanceName;

    public DGM(float alpha) {
        this.c1 = new C1(alpha);
        this.alpha = alpha;
        this.firstEdge = 0;
        lastInstanceName = null;
    }

    @Override
    public CDPSolution constructSolution(CDPInstance instance) {
        if (lastInstanceName == null || !lastInstanceName.equals(instance.getName())) {
            firstEdge = 0;
            lastInstanceName = instance.getName();
        }
        CDPSolution sol = c1.constructSolution(instance);
        firstEdge++;
        c1.setFirstEdge(firstEdge);
        return sol;
    }

    @Override
    public String toString() {
        return String.format("%s(%.2f)", this.getClass().getSimpleName(), this.alpha);
    }
}
