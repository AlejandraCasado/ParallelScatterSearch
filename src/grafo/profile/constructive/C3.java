package grafo.profile.constructive;

import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.tools.RandomManager;
import grafo.profile.structure.PInstance;
import grafo.profile.structure.PSolution;

import java.util.*;

public class C3 implements Constructive<PInstance, PSolution> {

    private double alpha;

    private class Candidate implements Comparable<Candidate> {
        private int vertex;
        private int degree;
        private int greedyFunct;

        public Candidate(int vertex, int degree) {
            this.vertex = vertex;
            this.degree = degree;
        }

        public Candidate(int vertex, int degree, int greedyFunct) {
            this.vertex = vertex;
            this.degree = degree;
            this.greedyFunct = greedyFunct;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Candidate candidate = (Candidate) o;
            return vertex == candidate.vertex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex);
        }

        @Override
        public int compareTo(Candidate o) {
            return Integer.compare(this.degree, o.degree);
        }
    }

    public C3(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public PSolution constructSolution(PInstance instance) {
        PSolution sol = new PSolution(instance);
        int nNodes = instance.getNodes();
        int nodeWithMinDegree = -1;
        int labelAct = 1;
        int minDegree = 0x3f3f3f3f;
        for (int i = 1; i < nNodes + 1; i++) {
            int actDegree = instance.getDegreeOfNode(i);
            if (actDegree < minDegree) {
                nodeWithMinDegree = i;
                minDegree = actDegree;
            }
        }

        sol.assignLabelToNode(labelAct, nodeWithMinDegree);
        List<Candidate> cl = new ArrayList<>();
        createCandidateList(cl, nodeWithMinDegree, instance, sol);
        Random rnd = RandomManager.getRandom();
//        Random rnd = new Random();
        double realAlpha = (alpha >= 0) ? alpha : rnd.nextDouble();
        while (!cl.isEmpty()) {
            labelAct++;
            cl.sort((a, b) -> Integer.compare(b.greedyFunct, a.greedyFunct));
            double gmin = cl.get(cl.size() - 1).greedyFunct;
            double gmax = cl.get(0).greedyFunct;
            double th = gmax - realAlpha * (gmax - gmin);
            int limit = 0;
            while (limit < cl.size() && Double.compare(cl.get(limit).greedyFunct, th) >= 0) {
                limit++;
            }
            int selected = rnd.nextInt(limit);
            int node = cl.remove(selected).vertex;
            sol.assignLabelToNode(labelAct, node);
            updateCandidateList(cl, node, instance, sol);
        }
        return sol;
    }

    private void createCandidateList(List<Candidate> cl, int node, PInstance instance, PSolution sol) {
        for (int adj : instance.getAdjacents(node)) {
            calculateGreedyFunction(cl, instance, sol, adj);
        }
    }

    private void updateCandidateList(List<Candidate> cl, int node, PInstance instance, PSolution sol) {
        Set<Candidate> candidatesToRemove = new HashSet<>(cl.size());
        for (Candidate candidate : cl) {
            if (sol.isLabeledNode(candidate.vertex)) candidatesToRemove.add(candidate);
        }
        cl.removeAll(candidatesToRemove);
        for (int adj : instance.getAdjacents(node)) {
            if (!sol.isLabeledNode(adj)) {
                calculateGreedyFunction(cl, instance, sol, adj);
            }
        }
    }

    private void calculateGreedyFunction(List<Candidate> cl, PInstance instance, PSolution sol, int adj) {
        int labeledAdj = 0;
        int nonLabeledAdj = 0;
        int labeledSum = 0;
        for (int v : instance.getAdjacents(adj)) {
            if (sol.isLabeledNode(v)) {
                labeledAdj++;
                labeledSum += Math.abs(sol.getLabelOfNode(v) - sol.getLabelOfNode(adj));
            } else {
                nonLabeledAdj++;
            }
        }
        Candidate candidate = new Candidate(adj, instance.getDegreeOfNode(adj), (labeledAdj - nonLabeledAdj) * labeledSum);
        if (!cl.contains(candidate)) cl.add(candidate);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + alpha + ")";
    }
}
