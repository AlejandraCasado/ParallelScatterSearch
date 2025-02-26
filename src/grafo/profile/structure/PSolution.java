package grafo.profile.structure;

import grafo.optilib.structure.Solution;

import java.util.*;

public class PSolution implements Solution, Comparable<PSolution> {

    private PInstance instance;
    private Set<Integer> labeledNodes;
    private int[] labelOfNode;
    private int[] profileOfNode;
    private int[] orderOfNodes;
    private int[] fvOfNode;
    private int ofValue;

    public PSolution(PInstance instance) {
        int nNodes = instance.getNodes() + 1;
        this.instance = instance;
        this.labeledNodes = new HashSet<>(nNodes);
        this.labelOfNode = new int[nNodes];
        this.profileOfNode = new int[nNodes];
        this.orderOfNodes = new int[nNodes];
        this.fvOfNode = new int[nNodes];
        this.ofValue = 0;
    }

    public PSolution(PSolution sol) {
        this.copy(sol);
    }

    public void copy(PSolution sol) {
        this.instance = sol.instance;
        int nNodes = instance.getNodes() + 1;
        this.labeledNodes = new HashSet<>(sol.labeledNodes);
        this.labelOfNode = Arrays.copyOf(sol.labelOfNode, nNodes);
        this.profileOfNode = Arrays.copyOf(sol.profileOfNode, nNodes);
        this.orderOfNodes = Arrays.copyOf(sol.orderOfNodes, nNodes);
        this.fvOfNode = Arrays.copyOf(sol.fvOfNode, nNodes);
        this.ofValue = sol.ofValue;
    }

    public int getOfValue() {
        return ofValue;
    }

    public int getNumberOfNodes() {
        return instance.getNodes();
    }

    public void assignLabelToNode(int label, int node) {
        int minPos = getMinPosLabeled(node, label);
        this.labeledNodes.add(node);
        this.labelOfNode[node] = label;
        this.orderOfNodes[label] = node;
        profileOfNode[node] = label - minPos;
        this.fvOfNode[node] = minPos;
        this.ofValue += profileOfNode[node];
    }

    private int getMinPosLabeled(int node, int label) {
        int minPos = label;
        for (int adj : instance.getAdjacents(node)) {
            if (labelOfNode[adj] < minPos && labelOfNode[adj] != 0) {
                minPos = labelOfNode[adj];
            }
        }
        return minPos;
    }

    public void swapNodes(int nodeOriginLabel, int nodeDestLabel) {

        int originNode = orderOfNodes[nodeOriginLabel];
        int destNode = orderOfNodes[nodeDestLabel];

        this.ofValue -= (profileOfNode[originNode] + profileOfNode[destNode]);

        this.labelOfNode[originNode] = nodeDestLabel;
        this.orderOfNodes[nodeDestLabel] = originNode;
        this.labelOfNode[destNode] = nodeOriginLabel;
        this.orderOfNodes[nodeOriginLabel] = destNode;

        int minPosOrigin = getMinPosLabeled(originNode, nodeDestLabel);
        int minPosDest = getMinPosLabeled(destNode, nodeOriginLabel);

        this.fvOfNode[originNode] = minPosOrigin;
        this.fvOfNode[destNode] = minPosDest;
        profileOfNode[originNode] = nodeDestLabel - minPosOrigin;
        profileOfNode[destNode] = nodeOriginLabel - minPosDest;

        this.ofValue += (profileOfNode[originNode] + profileOfNode[destNode]);

        int nodeOrigin = orderOfNodes[nodeOriginLabel];
        int nodeDest = orderOfNodes[nodeDestLabel];
        updateOFValueForNode(nodeOriginLabel, nodeDest, nodeOrigin);
        updateOFValueForNode(nodeDestLabel, nodeOrigin, nodeDest);
    }


    private void updateOFValueForNode(int nodeDestLabel, int nodeOrigin, int nodeDest) {
        for (int adjacent : instance.getAdjacents(nodeDest)) {
            int adjLabel = getLabelOfNode(adjacent);
            if ((adjLabel < nodeDestLabel) || instance.areAdjacents(adjacent, nodeOrigin)) continue;
            this.ofValue -= profileOfNode[adjacent];
            int minPosAdj = getMinPosLabeled(adjacent, adjLabel);
            profileOfNode[adjacent] = adjLabel - minPosAdj;
            this.fvOfNode[adjacent] = minPosAdj;
            this.ofValue += profileOfNode[adjacent];
        }
    }

    public boolean isLabeledNode(int actVertex) {
        return labeledNodes.contains(actVertex);
    }

    public boolean isBetterThan(PSolution sol) {
        return this.ofValue < sol.getOfValue();
    }

    public int getLabelOfNode(int node) {
        return labelOfNode[node];
    }

    public Set<Integer> getNodesWithDifferentLabelIn(PSolution s2) {
        Set<Integer> differentLabels = new HashSet<>();
        for (int i = 1; i < labelOfNode.length; i++) {
            if (labelOfNode[i] != s2.labelOfNode[i]) {
                differentLabels.add(i);
            }
        }
        return differentLabels;
    }

    public int getDistanceToSol(PSolution solInRefset) {
        int distance = 0;
        for (int i = 1; i < labelOfNode.length; i++) {
            distance += Math.abs(labelOfNode[i] - solInRefset.labelOfNode[i]);
        }
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSolution pSolution = (PSolution) o;
        return ofValue == pSolution.ofValue && Arrays.equals(labelOfNode, pSolution.labelOfNode) && Arrays.equals(profileOfNode, pSolution.profileOfNode) && Arrays.equals(orderOfNodes, pSolution.orderOfNodes) && Arrays.equals(fvOfNode, pSolution.fvOfNode);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(ofValue);
        result = 31 * result + Arrays.hashCode(labelOfNode);
        result = 31 * result + Arrays.hashCode(profileOfNode);
        result = 31 * result + Arrays.hashCode(orderOfNodes);
        result = 31 * result + Arrays.hashCode(fvOfNode);
        return result;
    }

    @Override
    public String toString() {
        return Arrays.toString(labelOfNode) + " - " + Arrays.toString(orderOfNodes);
    }

    @Override
    public int compareTo(PSolution o) {
        return Integer.compare(this.ofValue, o.ofValue);
    }
}
