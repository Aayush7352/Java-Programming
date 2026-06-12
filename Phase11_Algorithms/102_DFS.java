package phase11.algorithms;

import java.util.*;

record EdgeDFS(int destination) {}

class GraphDFS {
    private final List<List<Integer>> adjacencyList;
    private final int vertices;

    public GraphDFS(int vertices) {
        this.vertices = vertices;
        adjacencyList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination) {
        adjacencyList.get(source).add(destination);
        adjacencyList.get(destination).add(source); // undirected
    }

    public List<Integer> getNeighbors(int vertex) {
        return adjacencyList.get(vertex);
    }

    public int getVertices() {
        return vertices;
    }
}

class DFS {

    public static List<Integer> dfsRecursive(GraphDFS graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        dfsHelper(graph, start, visited, result);
        return result;
    }

    private static void dfsHelper(GraphDFS graph, int vertex, boolean[] visited, List<Integer> result) {
        visited[vertex] = true;
        result.add(vertex);
        for (int neighbor : graph.getNeighbors(vertex)) {
            if (!visited[neighbor]) {
                dfsHelper(graph, neighbor, visited, result);
            }
        }
    }

    public static List<Integer> dfsIterative(GraphDFS graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(start);
        while (!stack.isEmpty()) {
            int vertex = stack.pop();
            if (!visited[vertex]) {
                visited[vertex] = true;
                result.add(vertex);
                for (int neighbor : graph.getNeighbors(vertex).reversed()) {
                    if (!visited[neighbor]) {
                        stack.push(neighbor);
                    }
                }
            }
        }
        return result;
    }

    public static List<List<Integer>> connectedComponents(GraphDFS graph) {
        List<List<Integer>> components = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];

        for (int i = 0; i < graph.getVertices(); i++) {
            if (!visited[i]) {
                List<Integer> component = new ArrayList<>();
                dfsHelper(graph, i, visited, component);
                components.add(component);
            }
        }
        return components;
    }

    public static boolean hasPath(GraphDFS graph, int source, int destination) {
        boolean[] visited = new boolean[graph.getVertices()];
        return hasPathHelper(graph, source, destination, visited);
    }

    private static boolean hasPathHelper(GraphDFS graph, int current, int target, boolean[] visited) {
        if (current == target) return true;
        visited[current] = true;
        for (int neighbor : graph.getNeighbors(current)) {
            if (!visited[neighbor] && hasPathHelper(graph, neighbor, target, visited)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        GraphDFS graph = new GraphDFS(7);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 5);
        graph.addEdge(2, 6);

        System.out.println("=== DFS Recursive (starting from 0) ===");
        System.out.println(dfsRecursive(graph, 0));

        System.out.println("\n=== DFS Iterative (starting from 0) ===");
        System.out.println(dfsIterative(graph, 0));

        GraphDFS disconnectedGraph = new GraphDFS(6);
        disconnectedGraph.addEdge(0, 1);
        disconnectedGraph.addEdge(0, 2);
        disconnectedGraph.addEdge(3, 4);

        System.out.println("\n=== Connected Components ===");
        var components = connectedComponents(disconnectedGraph);
        for (int i = 0; i < components.size(); i++) {
            System.out.println("Component " + i + ": " + components.get(i));
        }

        System.out.println("\n=== Has Path? ===");
        System.out.println("Path from 0 to 5: " + hasPath(graph, 0, 5));
        System.out.println("Path from 0 to 7 (doesn't exist): " + hasPath(graph, 0, 7));
    }
}
