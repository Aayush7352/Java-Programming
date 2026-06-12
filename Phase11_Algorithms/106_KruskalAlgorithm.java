package phase11.algorithms;

import java.util.*;

record KruskalEdge(int source, int destination, int weight) implements Comparable<KruskalEdge> {
    @Override
    public int compareTo(KruskalEdge other) {
        return Integer.compare(this.weight, other.weight);
    }
}

class UnionFind {
    private final int[] parent;
    private final int[] rank;

    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX == rootY) return false;

        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            parent[rootY] = rootX;
            rank[rootX]++;
        }
        return true;
    }
}

class KruskalAlgorithm {

    public static List<KruskalEdge> minimumSpanningTree(List<KruskalEdge> edges, int vertices) {
        List<KruskalEdge> mst = new ArrayList<>();
        Collections.sort(edges);
        UnionFind uf = new UnionFind(vertices);

        for (KruskalEdge edge : edges) {
            if (uf.union(edge.source(), edge.destination())) {
                mst.add(edge);
                if (mst.size() == vertices - 1) break;
            }
        }
        return mst;
    }

    public static int mstWeight(List<KruskalEdge> edges, int vertices) {
        return minimumSpanningTree(edges, vertices).stream()
                .mapToInt(KruskalEdge::weight)
                .sum();
    }

    public static void main(String[] args) {
        int vertices = 7;
        List<KruskalEdge> edges = new ArrayList<>();
        edges.add(new KruskalEdge(0, 1, 7));
        edges.add(new KruskalEdge(0, 3, 5));
        edges.add(new KruskalEdge(1, 2, 8));
        edges.add(new KruskalEdge(1, 3, 9));
        edges.add(new KruskalEdge(1, 4, 7));
        edges.add(new KruskalEdge(2, 4, 5));
        edges.add(new KruskalEdge(3, 4, 15));
        edges.add(new KruskalEdge(3, 5, 6));
        edges.add(new KruskalEdge(4, 5, 8));
        edges.add(new KruskalEdge(4, 6, 9));
        edges.add(new KruskalEdge(5, 6, 11));

        System.out.println("=== Kruskal's Minimum Spanning Tree ===");
        List<KruskalEdge> mst = minimumSpanningTree(edges, vertices);

        int totalWeight = 0;
        for (KruskalEdge edge : mst) {
            System.out.println(edge.source() + " -- " + edge.destination() + " : " + edge.weight());
            totalWeight += edge.weight();
        }
        System.out.println("Total MST weight: " + totalWeight);
        System.out.println("Number of edges in MST: " + mst.size());

        System.out.println("\n=== Verification ===");
        boolean isConnected = mst.size() == vertices - 1;
        System.out.println("Graph is connected: " + isConnected);
    }
}
