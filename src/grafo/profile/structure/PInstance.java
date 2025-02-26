package grafo.profile.structure;

import grafo.optilib.structure.Instance;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PInstance implements Instance {

    private int nodes;
    private int edges;
    private Set<Integer>[] graph;

    private boolean[][] adjacenceMatrix;
    private String name;
    public PInstance(String path){
        this.readInstance(path);
    }

    @Override
    public void readInstance(String path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            this.name = path.substring(path.lastIndexOf("/"));
            br.readLine();
            String[] infoInstance = br.readLine().split(" ");
            this.nodes = Integer.parseInt(infoInstance[1]);
            this.edges = Integer.parseInt(infoInstance[2]);
            this.graph = new Set[nodes + 1];
            this.graph[0] = null;
            this.adjacenceMatrix = new boolean[nodes + 1][nodes + 1];
            for (int i = 1; i < nodes + 1; i++) {
                graph[i] = new HashSet<>();
            }
            for (int i = 0; i < edges; i++) {
                String[] edge = br.readLine().split(" ");
                int or = Integer.parseInt(edge[0]);
                int dest = Integer.parseInt(edge[1]);
                graph[or].add(dest);
                graph[dest].add(or);
                adjacenceMatrix[or][dest] = true;
                adjacenceMatrix[dest][or] = true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getNodes(){
        return nodes;
    }

    public String getName() {
        return this.name;
    }

    public boolean areAdjacents(int node, int labeledNode) {
        return adjacenceMatrix[node][labeledNode];
    }

    public int getDegreeOfNode(int node) {
        return graph[node].size();
    }

    public Set<Integer> getAdjacents(int node) {
        return graph[node];
    }
}
