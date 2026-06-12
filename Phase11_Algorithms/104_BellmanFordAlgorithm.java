package phase11.algorithms;

import java.util.*;

record BellmanFordEdge(int source, int destination, int weight) {}

class BellmanFordAlgorithm {

    public static int[] shortestPath(List<BellmanFordEdge> edges, int vertices, int start) {
        int[] distance = new int[vertices];
        Arrays.fill(distance, Integer.MAX_VALUE);
        distance[start] = 0;

        for (int i = 0; i < vertices - 1; i++) {
            boolean relaxed = false;
            for (BellmanFordEdge edge : edges) {
                if (distance[edge.source()] != Integer.MAX_VALUE) {
                    long newDist = (long) distance[edge.source()] + edge.weight();
                    if (newDist < distance[edge.destination()]) {
                        distance[edge.destination()] = (int) newDist;
                        relaxed = true;
                    }
                }
            }
            if (!relaxed) break;
        }
        return distance;
    }

    public static boolean hasNegativeCycle(List<BellmanFordEdge> edges, int vertices, int start) {
        int[] distance = shortestPath(edges, vertices, start);

        for (BellmanFordEdge edge : edges) {
            if (distance[edge.source()] != Integer.MAX_VALUE) {
                long newDist = (long) distance[edge.source()] + edge.weight();
                if (newDist < distance[edge.destination()]) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Integer> shortestPathWithRoute(List<BellmanFordEdge> edges, int vertices,
                                                       int start, int end) {
        int[] distance = new int[vertices];
        int[] parent = new int[vertices];
        Arrays.fill(distance, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        distance[start] = 0;

        for (int i = 0; i < vertices - 1; i++) {
            for (BellmanFordEdge edge : edges) {
                if (distance[edge.source()] != Integer.MAX_VALUE) {
                    long newDist = (long) distance[edge.source()] + edge.weight();
                    if (newDist < distance[edge.destination()]) {
                        distance[edge.destination()] = (int) newDist;
                        parent[edge.destination()] = edge.source();
                    }
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (distance[end] == Integer.MAX_VALUE) return path;

        for (int at = end; at != -1; at = parent[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public static void main(String[] args) {
        int vertices = 5;
        List<BellmanFordEdge> edges = new ArrayList<>();
        edges.add(new BellmanFordEdge(0, 1, 6));
        edges.add(new BellmanFordEdge(0, 2, 7));
        edges.add(new BellmanFordEdge(1, 2, 8));
        edges.add(new BellmanFordEdge(1, 3, 5));
        edges.add(new BellmanFordEdge(1, 4, -4));
        edges.add(new BellmanFordEdge(2, 3, -3));
        edges.add(new BellmanFordEdge(2, 4, 9));
        edges.add(new BellmanFordEdge(3, 1, -2));
        edges.add(new BellmanFordEdge(4, 0, 2));
        edges.add(new BellmanFordEdge(4, 3, 7));

        System.out.println("=== Bellman-Ford: Shortest Path from 0 ===");
        int[] distances = shortestPath(edges, vertices, 0);
        for (int i = 0; i < vertices; i++) {
            System.out.println("Distance to " + i + ": " +
                    (distances[i] == Integer.MAX_VALUE ? "Infinity" : distances[i]));
        }

        boolean hasCycle = hasNegativeCycle(edges, vertices, 0);
        System.out.println("\nHas negative cycle: " + hasCycle);

        System.out.println("\n=== Shortest Path from 0 to 4 ===");
        var path = shortestPathWithRoute(edges, vertices, 0, 4);
        System.out.println("Path: " + path);

        List<BellmanFordEdge> edgesWithNegativeCycle = new ArrayList<>();
        edgesWithNegativeCycle.add(new BellmanFordEdge(0, 1, 1));
        edgesWithNegativeCycle.add(new BellmanFordEdge(1, 2, -1));
        edgesWithNegativeCycle.add(new BellmanFordEdge(2, 0, -1));

        System.out.println("\n=== Graph with Negative Cycle ===");
        boolean negCycle = hasNegativeCycle(edgesWithNegativeCycle, 3, 0);
        System.out.println("Has negative cycle: " + negCycle);
    }
}
