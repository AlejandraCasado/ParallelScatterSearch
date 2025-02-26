package grafo.profile.constructive;

import grafo.optilib.metaheuristics.Constructive;
import grafo.optilib.tools.RandomManager;
import grafo.profile.structure.PInstance;
import grafo.profile.structure.PSolution;

import java.util.*;

public class C4 implements Constructive<PInstance, PSolution> {

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
        public int compareTo(Candidate o) {
            return Integer.compare(this.degree, o.degree);
        }
    }

    public C4(double alpha) {
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
        Collections.shuffle(cl);
        double realAlpha = (alpha >= 0) ? alpha : rnd.nextDouble();
        int limit = 0;
        while (limit <= (int) (realAlpha * cl.size())) {
            limit++;
        }
        List<Candidate> rcl = new ArrayList<>(cl.subList(0, limit));
        while (!rcl.isEmpty()) {
            labelAct++;
            rcl.sort((a, b) -> Integer.compare(b.greedyFunct, a.greedyFunct));
            int node = rcl.removeFirst().vertex;
            sol.assignLabelToNode(labelAct, node);
            updateCandidateList(rcl, node, instance, sol);
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
        cl.add(candidate);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + alpha + ")";
    }
}
