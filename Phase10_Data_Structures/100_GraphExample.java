package phase10.datastructures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class GraphExample {

    private final Map<String, List<String>> adjacencyList = new HashMap<>();

    public void addVertex(String vertex) {
        adjacencyList.putIfAbsent(vertex, new ArrayList<>());
    }

    public void addEdge(String source, String destination) {
        addVertex(source);
        addVertex(destination);
        adjacencyList.get(source).add(destination);
        adjacencyList.get(destination).add(source);
    }

    public void bfs(String start) {
        if (!adjacencyList.containsKey(start)) {
            System.out.println("Start vertex not found: " + start);
            return;
        }
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        System.out.print("BFS from " + start + ": ");
        while (!queue.isEmpty()) {
            var vertex = queue.poll();
            System.out.print(vertex + " ");
            for (var neighbor : adjacencyList.get(vertex)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        System.out.println();
    }

    public void dfs(String start) {
        if (!adjacencyList.containsKey(start)) {
            System.out.println("Start vertex not found: " + start);
            return;
        }
        Set<String> visited = new HashSet<>();
        System.out.print("DFS from " + start + ": ");
        dfsRec(start, visited);
        System.out.println();
    }

    private void dfsRec(String vertex, Set<String> visited) {
        visited.add(vertex);
        System.out.print(vertex + " ");
        for (var neighbor : adjacencyList.get(vertex)) {
            if (!visited.contains(neighbor)) {
                dfsRec(neighbor, visited);
            }
        }
    }

    public boolean hasPath(String source, String destination) {
        if (!adjacencyList.containsKey(source) || !adjacencyList.containsKey(destination)) {
            return false;
        }
        Set<String> visited = new HashSet<>();
        return hasPathRec(source, destination, visited);
    }

    private boolean hasPathRec(String current, String target, Set<String> visited) {
        if (current.equals(target)) return true;
        visited.add(current);
        for (var neighbor : adjacencyList.get(current)) {
            if (!visited.contains(neighbor)) {
                if (hasPathRec(neighbor, target, visited)) return true;
            }
        }
        return false;
    }

    public void printGraph() {
        System.out.println("Adjacency List:");
        for (var entry : adjacencyList.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
    }

    public static void main(String[] args) {
        var graph = new GraphExample();

        graph.addEdge("A", "B");
        graph.addEdge("A", "C");
        graph.addEdge("B", "D");
        graph.addEdge("B", "E");
        graph.addEdge("C", "F");
        graph.addEdge("E", "F");

        graph.printGraph();
        System.out.println();

        graph.bfs("A");
        graph.dfs("A");

        System.out.println("\nPath A -> D: " + graph.hasPath("A", "D"));
        System.out.println("Path A -> F: " + graph.hasPath("A", "F"));
        System.out.println("Path A -> X: " + graph.hasPath("A", "X"));

        System.out.println("\n=== Disconnected Graph ===");
        graph.addEdge("X", "Y");
        graph.bfs("X");
        System.out.println("Path A -> Y? " + graph.hasPath("A", "Y"));
    }
}
