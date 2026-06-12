package phase11.algorithms;

import java.util.*;

class FloydWarshallAlgorithm {

    public static final int INF = Integer.MAX_VALUE / 2;

    public static int[][] allPairsShortestPath(int[][] graph) {
        int vertices = graph.length;
        int[][] distance = new int[vertices][vertices];
        int[][] next = new int[vertices][vertices];

        for (int i = 0; i < vertices; i++) {
            System.arraycopy(graph[i], 0, distance[i], 0, vertices);
            for (int j = 0; j < vertices; j++) {
                next[i][j] = (graph[i][j] != INF && i != j) ? j : -1;
            }
        }

        for (int k = 0; k < vertices; k++) {
            for (int i = 0; i < vertices; i++) {
                if (distance[i][k] == INF) continue;
                for (int j = 0; j < vertices; j++) {
                    if (distance[k][j] == INF) continue;
                    int newDist = distance[i][k] + distance[k][j];
                    if (newDist < distance[i][j]) {
                        distance[i][j] = newDist;
                        next[i][j] = next[i][k];
                    }
                }
            }
        }
        return distance;
    }

    public static boolean hasNegativeCycle(int[][] distance) {
        int vertices = distance.length;
        for (int i = 0; i < vertices; i++) {
            if (distance[i][i] < 0) return true;
        }
        return false;
    }

    public static List<Integer> reconstructPath(int[][] next, int start, int end) {
        List<Integer> path = new ArrayList<>();
        if (next[start][end] == -1) return path;
        path.add(start);
        while (start != end) {
            start = next[start][end];
            path.add(start);
        }
        return path;
    }

    public static void printMatrix(int[][] matrix) {
        int vertices = matrix.length;
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                if (matrix[i][j] == INF) {
                    System.out.printf("%-5s", "INF");
                } else {
                    System.out.printf("%-5d", matrix[i][j]);
                }
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int vertices = 4;
        int[][] graph = new int[vertices][vertices];
        for (int i = 0; i < vertices; i++) {
            Arrays.fill(graph[i], INF);
            graph[i][i] = 0;
        }
        graph[0][1] = 3;
        graph[0][2] = 8;
        graph[0][3] = INF;
        graph[1][3] = 1;
        graph[2][1] = 4;
        graph[3][2] = 2;
        graph[3][0] = INF;

        System.out.println("=== Original Graph ===");
        printMatrix(graph);

        int[][] shortestDistances = allPairsShortestPath(graph);

        System.out.println("\n=== All-Pairs Shortest Paths (Floyd-Warshall) ===");
        printMatrix(shortestDistances);

        System.out.println("\nHas negative cycle: " + hasNegativeCycle(shortestDistances));

        System.out.println("\nShortest distance from 0 to 3: " + shortestDistances[0][3]);

        int[][] disconnectedGraph = new int[4][4];
        for (int i = 0; i < 4; i++) {
            Arrays.fill(disconnectedGraph[i], INF);
            disconnectedGraph[i][i] = 0;
        }
        disconnectedGraph[0][1] = 2;
        disconnectedGraph[1][2] = 3;
        disconnectedGraph[2][0] = -1;
        disconnectedGraph[2][3] = 4;

        System.out.println("\n=== Graph with Potential Negative Cycle ===");
        int[][] distances2 = allPairsShortestPath(disconnectedGraph);
        printMatrix(distances2);
        System.out.println("Has negative cycle: " + hasNegativeCycle(distances2));
    }
}
