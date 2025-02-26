package grafo.cdp.structure;

import grafo.cdp.util.FloatUtils;
import grafo.optilib.structure.Instance;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CDPInstance implements Instance {

    public class Edge {
        public int v1;
        public int v2;
        public float distance;

        public Edge(int v1, int v2, float distance) {
            this.v1 = v1;
            this.v2 = v2;
            this.distance = distance;
        }
    }

    private String name;
    private int n;
    private int b;
    private int[] capacity;
    private float[][] distance;

    // For the constructive
    private List<Edge> sortedDistances;

    public CDPInstance(String path) {
        readInstance(path);
    }

    public int getN() {
        return n;
    }

    public int getB() {
        return b;
    }

    public int getCapacity(int v) {
        return capacity[v];
    }

    public float getDistance(int v, int u) {
        return distance[v][u];
    }

    @Override
    public void readInstance(String s) {

        try (Scanner sc = new Scanner(new FileInputStream(s))) {
            name = s.substring(s.lastIndexOf('/')+1);
            n = sc.nextInt();
            b = sc.nextInt();
            capacity = new int[n+1];
            for (int i = 1; i <= n; i++) {
                // In the instance as float but it is actually an integer
                capacity[i] = (int) sc.nextFloat();
            }
            distance = new float[n+1][n+1];
            sortedDistances = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                for (int j = 1; j <= n; j++) {
                    float d = sc.nextFloat();
                    if (i == j) continue;
                    distance[i][j] = d;
                    sortedDistances.add(new Edge(i,j,d));
                }
            }
            sortedDistances.sort((e1, e2) -> FloatUtils.compareFloat(e2.distance, e1.distance));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getName() {
        return name;
    }

    public Edge getNthLongestDistance(int n) {
        return sortedDistances.get(n);
    }

    @Override
    public String toString() {
        StringBuilder stb = new StringBuilder();
        stb.append("N = ").append(n).append("\n");
        stb.append("B = ").append(b).append("\n");
        stb.append("CAPACITY =");
        for (int i = 1; i <= n; i++) {
            stb.append(" ").append(capacity[i]);
        }
        stb.append("\nDISTANCES:\n");
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                stb.append(distance[i][j]).append(" ");
            }
            stb.append("\n");
        }
        return stb.toString();
    }
}
