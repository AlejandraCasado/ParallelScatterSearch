package grafo.cdp.structure;

import grafo.cdp.util.FloatUtils;
import grafo.optilib.structure.Solution;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CDPSolution implements Solution {

    private CDPInstance instance;
    private Set<Integer> selected;
    // Objective function evaluation
    private float of;
    private int vMin1;
    private int vMin2;
    private int capacity;

    public CDPSolution(CDPInstance instance) {
        this.instance = instance;
        selected = new HashSet<>(instance.getN());
        vMin1 = -1;
        vMin2 = -1;
        of = instance.getNthLongestDistance(0).distance*10;
    }

    public CDPSolution(CDPSolution sol) {
        copy(sol);
    }

    public void copy(CDPSolution sol) {
        this.instance = sol.instance;
        this.of = sol.of;
        this.vMin1 = sol.vMin1;
        this.vMin2 = sol.vMin2;
        this.selected = new HashSet<>(sol.selected);
        this.capacity = sol.capacity;
    }

    public boolean contains(int v) {
        return selected.contains(v);
    }

    public float getOf() {
        return of;
    }

    public CDPInstance getInstance() {
        return instance;
    }

    public boolean isFeasible() {
        return capacity >= instance.getB();
    }

    public int getCapacity() {
        return capacity;
    }

    public Set<Integer> getSelected() {
        return selected;
    }

    public void add(int v) {
        selected.add(v);
        capacity += instance.getCapacity(v);
    }

    public void addAndUpdate(int v) {
        evalAdd(v);
        add(v);
    }

    private void evalAdd(int v) {
        for (int s : selected) {
            float d = instance.getDistance(v, s);
            if (FloatUtils.compareFloat(d, of) < 0) {
                of = d;
                vMin1 = s;
                vMin2 = v;
            }
        }
    }

    public void remove(int v) {
        selected.remove(v);
        capacity -= instance.getCapacity(v);
    }

    public void removeAndUpdate(int v) {
        remove(v);
        evalRemove(v);
    }

    private void evalRemove(int v) {
        if (v == vMin1 || v == vMin2) {
            // The vertex is involved in the objective function evaluation, update
            evalComplete();
        }
    }

    public boolean isBetterThan(CDPSolution sol) {
        return FloatUtils.compareFloat(of, sol.of) > 0;
    }

    public boolean isBetterThan(float solOF) {
        return FloatUtils.compareFloat(of, solOF) > 0;
    }

    public void evalComplete() {
        of = instance.getNthLongestDistance(0).distance * 10;
        for (int s1 : selected) {
            for (int s2 : selected) {
                if (s1 == s2) continue;
                float d = instance.getDistance(s1, s2);
                if (FloatUtils.compareFloat(d, of) < 0) {
                    of = d;
                    vMin1 = s1;
                    vMin2 = s2;
                }
            }
        }
    }

    public float getEvalComplete() {
        float of = instance.getNthLongestDistance(0).distance * 10;
        for (int s1 : selected) {
            for (int s2 : selected) {
                if (s1 == s2) continue;
                float d = instance.getDistance(s1, s2);
                if (FloatUtils.compareFloat(d, of) < 0) {
                    of = d;
                }
            }
        }
        return of;
    }

    public void updateOF(int vMin1, int vMin2, float of) {
        this.of = of;
        this.vMin1 = vMin1;
        this.vMin2 = vMin2;
    }

    public int getCritical(int number) {
        if (number == 1) {
            return vMin1;
        } else {
            return vMin2;
        }
    }

    public DistToSol distanceTo(int v) {
        float minDist = instance.getNthLongestDistance(0).distance*10;
        int vMin = -1;
        for (int s : selected) {
            float d = instance.getDistance(s, v);
            if (FloatUtils.compareFloat(d, minDist) < 0) {
                minDist = d;
                vMin = s;
            }
        }
        return new DistToSol(vMin, minDist);
    }

    public int distance(CDPSolution sol) {
        int diffElems = 0;
        for (int solSel : sol.selected) {
            if (!selected.contains(solSel)) {
                diffElems++;
            }
        }
        return diffElems + (selected.size() - (sol.selected.size() - diffElems));
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("OF = ").append(of);
        stb.append(" CAP = ").append(capacity).append(" -> ");
        stb.append(selected);
        return stb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CDPSolution that = (CDPSolution) o;
        return Objects.equals(selected, that.selected);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selected);
    }
}
