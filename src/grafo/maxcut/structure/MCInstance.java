package grafo.maxcut.structure;

import grafo.maxcut.Main;
import grafo.optilib.structure.Instance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MCInstance implements Instance {

    Set<Integer> nodes;
    private String name;
    private int numNodes;
    private List<Integer>[] adjacencyList;
    private int[][] adjacencyMatrix;

    public MCInstance(String path){
        this.name = path.substring(Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"))+1).replace(".txt", "");
        readInstance(path);
    }

    @Override
    public void readInstance(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))){
            String line;
            String[] lineContent;
            line= br.readLine();
            lineContent = line.split("\\s");
            numNodes = Integer.parseInt(lineContent[0]);
            nodes=new HashSet<>(numNodes);
            int numEdges = Integer.parseInt(lineContent[1]);
            adjacencyMatrix = new int [numNodes][numNodes];
            adjacencyList = new ArrayList[numNodes];
            for(int i=0; i<numNodes;i++){
                adjacencyList[i]=new ArrayList<>();
                nodes.add(i);
            }
            for (int i = 0; i< numEdges; i++){
                line = br.readLine();
                lineContent=line.split("\\s");
                int node1=Integer.parseInt(lineContent[0]);
                int node2=Integer.parseInt(lineContent[1]);
                int weight=Integer.parseInt(lineContent[2]);
                adjacencyList[node1-1].add(node2-1);
                adjacencyList[node2-1].add(node1-1);
                adjacencyMatrix[node1-1][node2-1]=weight;
                adjacencyMatrix[node2-1][node1-1]=weight;
            }
        } catch (FileNotFoundException e){
            System.out.println(("File not found " + path));
        } catch (IOException e){
            System.out.println("Error reading line");
        }
    }
    public String getName(){
        return name;
    }
    public int getNumNodes() {
        return numNodes;
    }
    public List<Integer>[] getAdjacencyList() {
        return adjacencyList;
    }
    public int[][] getAdjacencyMatrix() {
        return adjacencyMatrix;
    }
    public Set<Integer> getNodes() {
        return nodes;
    }
}