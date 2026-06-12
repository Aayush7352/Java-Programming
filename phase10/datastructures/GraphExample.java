package phase10.datastructures;

import java.util.*;

public class GraphExample {
    private final Map<Integer, List<Integer>> adj;

    public GraphExample() {
        adj = new HashMap<>();
    }

    public void addVertex(int v) {
        adj.putIfAbsent(v, new ArrayList<>());
    }

    public void addEdge(int u, int v) {
        adj.putIfAbsent(u, new ArrayList<>());
        adj.putIfAbsent(v, new ArrayList<>());
        adj.get(u).add(v);
        adj.get(v).add(u); // undirected
    }

    public boolean hasPath(int src, int dest) {
        Set<Integer> visited = new HashSet<>();
        return hasPathDFS(src, dest, visited);
    }

    private boolean hasPathDFS(int src, int dest, Set<Integer> visited) {
        if (src == dest) return true;
        visited.add(src);
        for (int neighbor : adj.getOrDefault(src, List.of())) {
            if (!visited.contains(neighbor) && hasPathDFS(neighbor, dest, visited)) return true;
        }
        return false;
    }

    public List<Integer> bfs(int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            int v = queue.poll();
            result.add(v);
            for (int neighbor : adj.getOrDefault(v, List.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    public List<Integer> dfs(int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        dfsRecursive(start, visited, result);
        return result;
    }

    private void dfsRecursive(int v, Set<Integer> visited, List<Integer> result) {
        visited.add(v);
        result.add(v);
        for (int neighbor : adj.getOrDefault(v, List.of())) {
            if (!visited.contains(neighbor)) dfsRecursive(neighbor, visited, result);
        }
    }

    public void printGraph() {
        for (var entry : adj.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    public static void main(String[] args) {
        GraphExample graph = new GraphExample();
        graph.addEdge(0, 1);
        graph.addEdge(0, 4);
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);

        System.out.println("Graph adjacency list:");
        graph.printGraph();

        System.out.println("\nBFS from 0: " + graph.bfs(0));
        System.out.println("DFS from 0: " + graph.dfs(0));
        System.out.println("hasPath(0, 3): " + graph.hasPath(0, 3));
        System.out.println("hasPath(0, 5): " + graph.hasPath(0, 5));
    }
}
