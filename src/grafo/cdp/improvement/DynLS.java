package grafo.cdp.improvement;


import grafo.cdp.experiment.Execution;
import grafo.cdp.structure.CDPInstance;
import grafo.cdp.structure.CDPSolution;
import grafo.cdp.util.FloatUtils;
import grafo.optilib.metaheuristics.Improvement;
import grafo.optilib.tools.RandomManager;
import grafo.optilib.tools.Timer;

import java.util.ArrayList;
import java.util.List;

public class DynLS implements Improvement<CDPSolution> {

    private class Candidate {
        int v;
        int capacity;

        Candidate(int v, int capacity) {
            this.v = v;
            this.capacity = capacity;
        }
    }

    private float beta;

    public static long nChanges;
    public static long maxChanges;
    public static long iters = 0;

    public DynLS(float beta) {
        this.beta = beta;
    }


    @Override
    public void improve(CDPSolution sol) {
        CDPInstance instance = sol.getInstance();
        int n = instance.getN();
        float currBeta = (beta > 0) ? beta : (float) (1 + RandomManager.getRandom().nextFloat() * (0.5));
        while (FloatUtils.compareFloat(currBeta, 1) >= 0) {
            boolean improve = true;
            while (improve) {
                if (Timer.getTime() > Execution.TIME_LIMIT) {
                    return;
                }
                improve = false;
                List<Integer> clRemove = new ArrayList<>(n);
                clRemove.add(sol.getCritical(1));
                clRemove.add(sol.getCritical(2));
                float dStar = sol.getOf();
                for (int cRem : clRemove) {
                    if (Timer.getTime() > Execution.TIME_LIMIT) {
                        return;
                    }
                    List<Candidate> clEnter = createCandidateToEnter(sol, cRem, currBeta);
                    int idx = 0;
                    int currentCapacity = 0;
                    for (int sel : sol.getSelected()) {
                        if (sel != cRem) {
                            currentCapacity += instance.getCapacity(sel);
                        }
                    }
                    List<Integer> toEnter = new ArrayList<>();
                    while (idx < clEnter.size() && currentCapacity < instance.getB()) {
                        if (Timer.getTime() > Execution.TIME_LIMIT) {
                            return;
                        }
                        Candidate nextEnter = clEnter.get(idx);
                        // Check if distance to the new selected is larger than the new of
                        boolean enter = true;
                        for (int alreadyEnter : toEnter) {
                            float d = instance.getDistance(alreadyEnter, nextEnter.v);
                            if (FloatUtils.compareFloat(d, dStar) <= 0) {
                                enter = false;
                                break;
                            }
                        }
                        if (enter) {
                            toEnter.add(nextEnter.v);
                            currentCapacity += instance.getCapacity(nextEnter.v);
                        }
                        idx++;
                    }
                    // Vertices in toEnter are those that can enter in solution when removing cRem
                    if (currentCapacity >= instance.getB()) {
                        sol.remove(cRem);
                        for (int enter : toEnter) {
                            sol.add(enter);
                        }
                        sol.evalComplete();
                        improve = true;
                        break;
                    }
                }
            }
            currBeta -= 0.1f;
        }
    }

    private List<Candidate> createCandidateToEnter(CDPSolution sol, int cRem, float currBeta) {
        CDPInstance instance = sol.getInstance();
        int n = instance.getN();
        List<Integer> selected = new ArrayList<>(sol.getSelected());
        List<Candidate> cl = new ArrayList<>(n);
        for (int v = 1; v <= n; v++) {
            if (!sol.contains(v)) {
                // It is a candidate to enter in the solution
                float minDist = findMinDistWithout(instance, selected, v, cRem);
                // Selection criterion if minDist > beta * ofVal
                if (Float.compare(minDist, currBeta * sol.getOf()) > 0) {
                    cl.add(new Candidate(v, instance.getCapacity(v)));
                }
            }
        }
        // Sort candidates by capacity
        cl.sort((c1, c2) -> c2.capacity - c1.capacity);
        return cl;
    }

    private float findMinDistWithout(CDPInstance instance, List<Integer> selected, int v, int without) {
        float minDist = 0x3f3f3f3f/1000;
        for (int s : selected) {
            if (s != without) {
                float dist = instance.getDistance(s, v);
                minDist = (FloatUtils.compareFloat(dist, minDist) < 0) ? dist : minDist;
            }
        }
        return minDist;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+beta+")";
    }
}
