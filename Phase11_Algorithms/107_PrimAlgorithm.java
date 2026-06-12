package phase11.algorithms;

import java.util.*;

record PrimEdge(int source, int destination, int weight) {}

record PrimEdgeDest(int destination, int weight) {}

class PrimGraph {
    private final List<List<PrimEdgeDest>> adjacencyList;
    private final int vertices;

    public PrimGraph(int vertices) {
        this.vertices = vertices;
        adjacencyList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination, int weight) {
        adjacencyList.get(source).add(new PrimEdgeDest(destination, weight));
        adjacencyList.get(destination).add(new PrimEdgeDest(source, weight));
    }

    public List<PrimEdgeDest> getNeighbors(int vertex) {
        return adjacencyList.get(vertex);
    }

    public int getVertices() {
        return vertices;
    }
}

class PrimNode {
    int vertex;
    int key;
    int parent;

    PrimNode(int vertex, int key, int parent) {
        this.vertex = vertex;
        this.key = key;
        this.parent = parent;
    }
}

class PrimAlgorithm {

    public static List<PrimEdge> minimumSpanningTree(PrimGraph graph) {
        int vertices = graph.getVertices();
        boolean[] inMST = new boolean[vertices];
        int[] key = new int[vertices];
        int[] parent = new int[vertices];
        Arrays.fill(key, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        key[0] = 0;

        PriorityQueue<PrimNode> pq = new PriorityQueue<>(
                Comparator.comparingInt(a -> a.key)
        );
        pq.offer(new PrimNode(0, 0, -1));

        while (!pq.isEmpty()) {
            PrimNode node = pq.poll();
            int u = node.vertex;

            if (inMST[u]) continue;
            inMST[u] = true;

            for (PrimEdgeDest edge : graph.getNeighbors(u)) {
                int v = edge.destination();
                int weight = edge.weight();
                if (!inMST[v] && weight < key[v]) {
                    key[v] = weight;
                    parent[v] = u;
                    pq.offer(new PrimNode(v, key[v], u));
                }
            }
        }

        List<PrimEdge> mst = new ArrayList<>();
        for (int i = 1; i < vertices; i++) {
            if (parent[i] != -1) {
                int weight = key[i];
                mst.add(new PrimEdge(parent[i], i, weight));
            }
        }
        return mst;
    }

    public static int mstWeight(PrimGraph graph) {
        return minimumSpanningTree(graph).stream()
                .mapToInt(PrimEdge::weight)
                .sum();
    }

    public static void main(String[] args) {
        PrimGraph graph = new PrimGraph(7);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 3, 5);
        graph.addEdge(1, 2, 8);
        graph.addEdge(1, 3, 9);
        graph.addEdge(1, 4, 7);
        graph.addEdge(2, 4, 5);
        graph.addEdge(3, 4, 15);
        graph.addEdge(3, 5, 6);
        graph.addEdge(4, 5, 8);
        graph.addEdge(4, 6, 9);
        graph.addEdge(5, 6, 11);

        System.out.println("=== Prim's Minimum Spanning Tree ===");
        List<PrimEdge> mst = minimumSpanningTree(graph);

        int totalWeight = 0;
        for (PrimEdge edge : mst) {
            System.out.println(edge.destination() + " -- (weight " + edge.weight() + ")");
            totalWeight += edge.weight();
        }
        System.out.println("Total MST weight: " + totalWeight);
        System.out.println("Number of edges in MST: " + mst.size());
    }
}
