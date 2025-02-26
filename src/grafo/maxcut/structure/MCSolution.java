package grafo.maxcut.structure;

import grafo.optilib.structure.Solution;
import grafo.optilib.tools.RandomManager;

import java.util.*;

public class MCSolution implements Solution {

    private BitSet nodesBelonging;
    private int cuts=0;
    private MCInstance instance;
    private int k=0;
    private String combiner;
    private Set<Integer> located;

    public MCSolution(MCInstance mcInstance) {
        this.instance = mcInstance;
        located=new HashSet<>();
        nodesBelonging=new BitSet();
        k= RandomManager.getRandom().nextInt(6)+1;
    }

    public MCSolution(MCSolution mcSolution) {
        located=new HashSet<>();
        nodesBelonging=new BitSet();
        copy(mcSolution);
    }

    public void setNodesRight(int startId, int endId){
        nodesBelonging.set(0,instance.getNumNodes());
        for(int i=startId;i<endId;i++){
            located.add(i);
        }
    }

    public void addNode(int node, boolean right){
        located.add(node);
        cuts+=calculateAmountEdges(node,!right);
        nodesBelonging.set(node,right);
    }

    public void addNode(int node, boolean right, int amount){
        located.add(node);
        nodesBelonging.set(node,right);
        cuts+=amount;
    }

    public void moveNode(int node, int amount){
        if(amount!=0){
            cuts+=amount;
        }
        else{
            cuts+=calculateAmountEdgesExchange(node);
        }
        nodesBelonging.flip(node);
    }

    public int calculateAmountEdgesExchange(int node){
        int amountR=calculateAmountEdges(node,true);
        int amountL=calculateAmountEdges(node,false);
        return !nodesBelonging.get(node)?amountL-amountR: amountR-amountL;
    }

    public int calculateAmountEdges(int node, boolean right){
        int amount=0;
        if(right){
            for (int n: instance.getAdjacencyList()[node]) {
                if(located.contains(n) && nodesBelonging.get(n)){
                    amount+= instance.getAdjacencyMatrix()[node][n];
                }
            }
        }else{
            for (int n: instance.getAdjacencyList()[node]) {
                if(located.contains(n) && !nodesBelonging.get(n)){
                    amount+= instance.getAdjacencyMatrix()[node][n];
                }
            }
        }
        return amount;
    }

    public int calculateDifferences(MCSolution s2){
        Set<Integer> edgesS1=new HashSet<>();
        Set<Integer> edgesS2=new HashSet<>();
        for(int node=nodesBelonging.nextSetBit(0);node>=0;node=nodesBelonging.nextSetBit(node+1)){
            for (int neighbor:instance.getAdjacencyList()[node]) {
                if(!nodesBelonging.get(neighbor)){
                    edgesS1.add((Math.min(node,neighbor)*instance.getNumNodes())+Math.max(node,neighbor));
                }
            }
        }
        for(int node=s2.getNodesBelonging().nextSetBit(0);node>=0;node=s2.getNodesBelonging().nextSetBit(node+1)){
            for (int neighbor:instance.getAdjacencyList()[node]) {
                if(!s2.getNodesBelonging().get(neighbor)){
                    edgesS2.add((Math.min(node,neighbor)*instance.getNumNodes())+Math.max(node,neighbor));
                }
            }
        }
        Set<Integer> edgesDiffS1=new HashSet<>(edgesS1);
        Set<Integer> edgesDiffS2=new HashSet<>(edgesS2);
        edgesDiffS1.removeAll(edgesS2);
        edgesDiffS2.removeAll(edgesS1);
        return edgesDiffS1.size()+edgesDiffS2.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCSolution that = (MCSolution) o;
        return cuts == that.cuts && calculateDifferences(that)==0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodesBelonging, cuts);
    }

    public void copy(MCSolution sol){
        this.located.addAll(sol.located);
        this.nodesBelonging.clear();
        this.k= sol.getK();
        this.instance =sol.getInstance();
        this.nodesBelonging.or(sol.nodesBelonging);
        this.cuts=sol.getOF();
        this.combiner=sol.getCombiner();
    }
    public int getOF(){
        return cuts;
    }
    public BitSet getNodesBelonging() {
        return nodesBelonging;
    }
    public MCInstance getInstance() {
        return instance;
    }
    public int getK() {
        return k;
    }
    public void setK(int k) {
        this.k = k;
    }
    public String getCombiner() {
        return combiner;
    }
    public void setCombiner(String combiner) {
        this.combiner = combiner;
    }
    @Override
    public String toString() {
        return getOF()+" -> "+nodesBelonging.toString();
    }
}

