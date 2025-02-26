package grafo.cdp.constructives;

import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.cdp.structure.DistToSol;
import grafo.cdp.util.FloatUtils;
import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.tools.RandomManager;

import java.util.ArrayList;
import java.util.List;

public class C1 implements Constructive<CDPInstance, CDPSolution> {

    private class Candidate {
        int v;
        float cost;
        int closestV;

        Candidate(int v, int closestV, float cost) {
            this.v = v;
            this.closestV = closestV;
            this.cost = cost;
        }
    }

    private float alpha;
    private int firstEdge;

    public C1(float alpha) {
        this.alpha = alpha;
        firstEdge = 0;
    }

    public void setFirstEdge(int firstEdge) {
        this.firstEdge = firstEdge;
    }

    @Override
    public CDPSolution constructSolution(CDPInstance instance) {
        CDPSolution sol = new CDPSolution(instance);
        // Add the endpoints of the largest distance
        CDPInstance.Edge edge = instance.getNthLongestDistance(firstEdge);
        sol.add(edge.v1);
        sol.add(edge.v2);
        sol.updateOF(edge.v1, edge.v2, edge.distance);
        // Create CL sorted by distance to set of selected nodes
        List<Candidate> cl = createCL(sol);
        float realAlpha = (alpha >= 0) ? alpha : RandomManager.getRandom().nextFloat();
        while (!sol.isFeasible()) {
            // Select the vertex with largest capacity among the most promising ones
            float distanceLimit = cl.get(0).cost - realAlpha * cl.get(cl.size()-1).cost;
            int i = 0;
            int maxCap = 0;
            int vWithMaxCap = -1;
            while (i < cl.size() && FloatUtils.compareFloat(cl.get(i).cost, distanceLimit) >= 0) {
                int v = cl.get(i).v;
                int vCap = instance.getCapacity(v);
                if (vCap > maxCap) {
                    maxCap = vCap;
                    vWithMaxCap = i;
                }
                i++;
            }
            // Add it to the solution using the precached information about objective function value
            //TODO Revisar aqui que algo falla con la actualizacion de la funcion objetivo
            Candidate c = cl.remove(vWithMaxCap);
            sol.add(c.v);
            if (FloatUtils.compareFloat(c.cost, sol.getOf()) < 0) {
                sol.updateOF(c.v, c.closestV, c.cost);
            }
            if (sol.getEvalComplete() != sol.getOf()) {
                System.out.println("MAL: "+sol.getEvalComplete()+" vs "+sol.getOf());
            }
            updateCL(sol, cl, c.v);
            // ============================================
            // DEBUG
//            float ofVal = sol.getOf();
//            sol.getEvalComplete();
//            if (FloatUtils.compareFloat(ofVal, sol.getOf()) != 0) {
//                System.out.println("ERROR ADDING "+c.v);
//                System.out.println(sol);
//                System.out.println(ofVal);
//            }
            // ============================================
        }
        return sol;
    }

    private List<Candidate> createCL(CDPSolution sol) {
        CDPInstance instance = sol.getInstance();
        int n = instance.getN();
        List<Candidate> cl = new ArrayList<>(n);
        for (int v = 1; v <= n; v++) {
            if (sol.contains(v)) continue;
            DistToSol distToSol = sol.distanceTo(v);
            Candidate c = new Candidate(v, distToSol.getV(), distToSol.getD());
            cl.add(c);
        }
        cl.sort((c1, c2) -> FloatUtils.compareFloat(c2.cost, c1.cost));
        return cl;
    }

    private void updateCL(CDPSolution sol, List<Candidate> cl, int lastAdded) {
        CDPInstance instance = sol.getInstance();
        for (Candidate c : cl) {
            float dToLast = instance.getDistance(lastAdded, c.v);
            if (FloatUtils.compareFloat(dToLast, c.cost) < 0) {
                c.cost = dToLast;
                c.closestV = lastAdded;
            }
        }
        cl.sort((c1, c2) -> FloatUtils.compareFloat(c2.cost, c1.cost));
    }

    @Override
    public String toString() {
        return String.format("%s(%.2f)", this.getClass().getSimpleName(), this.alpha);
    }
}
