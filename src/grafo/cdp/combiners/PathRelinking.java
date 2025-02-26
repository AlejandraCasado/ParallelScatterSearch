package grafo.cdp.combiners;

import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.cdp.util.FloatUtils;
import grafo.optilib.tools.RandomManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PathRelinking {

    private int option;

    public PathRelinking(int option) {
        this.option = option;
    }

    public CDPSolution[] combine(CDPSolution src, CDPSolution dst) {
        if (option == 0) {
            CDPSolution prDistance = combineDistance(src, dst);
            return new CDPSolution[]{prDistance};
        } else if (option == 1) {
            CDPSolution prCapacity = combineCapacity(src, dst);
            return new CDPSolution[]{prCapacity};
        } else {
            CDPSolution prDistance = combineDistance(src, dst);
            CDPSolution prCapacity = combineCapacity(src, dst);
            return new CDPSolution[]{prDistance, prCapacity};
        }
    }

    private float getValue(CDPSolution solEval, int candidate, boolean option) {
        return option ? solEval.distanceTo(candidate).getD() : solEval.getInstance().getCapacity(candidate);
    }

    public CDPSolution combineCapacity(CDPSolution src, CDPSolution dst) {
        CDPInstance instance = src.getInstance();
        CDPSolution interSol = new CDPSolution(src);
        CDPSolution bestLocal = null;
        List<Integer> clEnter = createCandidatesToEnter(src, dst);
        List<Integer> clExit = createCandidatesToExit(src, dst);
        clExit.sort((c1, c2) -> -Integer.compare(instance.getCapacity(c1), instance.getCapacity(c2)));
        clEnter.sort((c1, c2) -> -Integer.compare(instance.getCapacity(c1), instance.getCapacity(c2)));
        int idxExit = 0;
        int idEnter = 0;
        while (idEnter < clEnter.size() && idxExit < clExit.size()) {
            interSol.removeAndUpdate(clExit.get(idxExit));
            idxExit++;
            while (!interSol.isFeasible() && idEnter < clEnter.size()) {
                interSol.addAndUpdate(clEnter.get(idEnter));
                idEnter++;
            }
            if (interSol.isFeasible()) {
                if (bestLocal == null) {
                    bestLocal = new CDPSolution(interSol);
                } else if (FloatUtils.compareFloat(bestLocal.getOf(), interSol.getOf()) < 0) {
                    bestLocal.copy(interSol);
                }
            }
        }
        return interSol;
    }

    public CDPSolution combineDistance(CDPSolution src, CDPSolution dst) {
        Random rnd = RandomManager.getRandom();
        CDPSolution interSol = new CDPSolution(src);
        CDPSolution bestLocal = null;
        List<Integer> clEnter = createCandidatesToEnter(src, dst);
        while (!clEnter.isEmpty()) {
            int rem = interSol.getCritical(rnd.nextInt(2));
            float ofPrev = interSol.getOf();
            interSol.removeAndUpdate(rem);
            while (!interSol.isFeasible()) {
                // Adds elements from dst \ src while not satisfying capacity
                int vEnter = -1;
                float maxDist = ofPrev;
                for (int c : clEnter) {
                    float d = interSol.distanceTo(c).getD();
                    if (FloatUtils.compareFloat(d, maxDist) > 0) {
                        maxDist = d;
                        vEnter = c;
                    }
                }
                if (vEnter < 0) {
                    clEnter.clear();
                    break;
                }
                interSol.addAndUpdate(vEnter);
            }
            if (!interSol.isFeasible()) {
                break;
            } else {
                if (bestLocal == null) {
                    bestLocal = new CDPSolution(interSol);
                } else if (FloatUtils.compareFloat(bestLocal.getOf(), interSol.getOf()) < 0) {
                    bestLocal.copy(interSol);
                }
            }
        }
        return bestLocal;
    }

    private List<Integer> createCandidatesToEnter(CDPSolution src, CDPSolution dst) {
        List<Integer> cl = new ArrayList<>(src.getInstance().getN());
        for (int dstNode : dst.getSelected()) {
            if (!src.contains(dstNode)) {
                cl.add(dstNode);
            }
        }
        return cl;
    }

    private List<Integer> createCandidatesToExit(CDPSolution src, CDPSolution dst) {
        List<Integer> cl = new ArrayList<>(src.getInstance().getN());
        for (int srcNode : src.getSelected()) {
            if (dst.contains(srcNode)) {
                cl.add(srcNode);
            }
        }
        return cl;
    }
}
