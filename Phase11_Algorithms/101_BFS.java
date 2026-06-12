package phase11.algorithms;

import java.util.*;

record Edge(int destination, int weight) {}

class Graph {
    private final List<List<Edge>> adjacencyList;
    private final int vertices;

    public Graph(int vertices) {
        this.vertices = vertices;
        adjacencyList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination) {
        addEdge(source, destination, 1);
    }

    public void addEdge(int source, int destination, int weight) {
        adjacencyList.get(source).add(new Edge(destination, weight));
    }

    public List<Edge> getNeighbors(int vertex) {
        return adjacencyList.get(vertex);
    }

    public int getVertices() {
        return vertices;
    }
}

class BFS {

    public static List<Integer> bfsTraversal(Graph graph, int start) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.getVertices()];
        Queue<Integer> queue = new LinkedList<>();

        visited[start] = true;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(current);

            for (Edge edge : graph.getNeighbors(current)) {
                int neighbor = edge.destination();
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }
        return result;
    }

    public static int[] shortestPath(Graph graph, int start) {
        int vertices = graph.getVertices();
        int[] distance = new int[vertices];
        Arrays.fill(distance, -1);
        Queue<Integer> queue = new LinkedList<>();

        distance[start] = 0;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int current = queue.poll();

            for (Edge edge : graph.getNeighbors(current)) {
                int neighbor = edge.destination();
                if (distance[neighbor] == -1) {
                    distance[neighbor] = distance[current] + 1;
                    queue.offer(neighbor);
                }
            }
        }
        return distance;
    }

    public static List<Integer> shortestPathToTarget(Graph graph, int start, int target) {
        int vertices = graph.getVertices();
        int[] parent = new int[vertices];
        Arrays.fill(parent, -1);
        boolean[] visited = new boolean[vertices];
        Queue<Integer> queue = new LinkedList<>();

        visited[start] = true;
        queue.offer(start);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == target) break;

            for (Edge edge : graph.getNeighbors(current)) {
                int neighbor = edge.destination();
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    parent[neighbor] = current;
                    queue.offer(neighbor);
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (!visited[target]) return path;

        for (int at = target; at != -1; at = parent[at]) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    public static void main(String[] args) {
        Graph graph = new Graph(6);
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 4);
        graph.addEdge(3, 5);
        graph.addEdge(4, 5);

        System.out.println("=== BFS Traversal (starting from 0) ===");
        var traversal = bfsTraversal(graph, 0);
        System.out.println(traversal);

        System.out.println("\n=== Shortest Distances from 0 ===");
        int[] distances = shortestPath(graph, 0);
        for (int i = 0; i < distances.length; i++) {
            System.out.println("Distance to " + i + ": " + distances[i]);
        }

        System.out.println("\n=== Shortest Path from 0 to 5 ===");
        var path = shortestPathToTarget(graph, 0, 5);
        System.out.println(path);
    }
}
