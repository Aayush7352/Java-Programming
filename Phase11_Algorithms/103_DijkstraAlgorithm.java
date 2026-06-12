package phase11.algorithms;

import java.util.*;

record DijkstraEdge(int destination, int weight) {}

class WeightedGraph {
    private final List<List<DijkstraEdge>> adjacencyList;
    private final int vertices;

    public WeightedGraph(int vertices) {
        this.vertices = vertices;
        adjacencyList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination, int weight) {
        adjacencyList.get(source).add(new DijkstraEdge(destination, weight));
    }

    public List<DijkstraEdge> getNeighbors(int vertex) {
        return adjacencyList.get(vertex);
    }

    public int getVertices() {
        return vertices;
    }
}

class DijkstraAlgorithm {

    public static int[] shortestPath(WeightedGraph graph, int start) {
        int vertices = graph.getVertices();
        int[] distance = new int[vertices];
        Arrays.fill(distance, Integer.MAX_VALUE);
        distance[start] = 0;

        PriorityQueue<DijkstraEdge> pq = new PriorityQueue<>(
                Comparator.comparingInt(DijkstraEdge::weight)
        );
        pq.offer(new DijkstraEdge(start, 0));

        while (!pq.isEmpty()) {
            DijkstraEdge current = pq.poll();
            int currentVertex = current.destination();
            int currentDist = current.weight();

            if (currentDist > distance[currentVertex]) continue;

            for (DijkstraEdge edge : graph.getNeighbors(currentVertex)) {
                int newDist = distance[currentVertex] + edge.weight();
                if (newDist < distance[edge.destination()]) {
                    distance[edge.destination()] = newDist;
                    pq.offer(new DijkstraEdge(edge.destination(), newDist));
                }
            }
        }
        return distance;
    }

    public static List<Integer> shortestPathWithRoute(WeightedGraph graph, int start, int end) {
        int vertices = graph.getVertices();
        int[] distance = new int[vertices];
        int[] parent = new int[vertices];
        Arrays.fill(distance, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        distance[start] = 0;

        PriorityQueue<DijkstraEdge> pq = new PriorityQueue<>(
                Comparator.comparingInt(DijkstraEdge::weight)
        );
        pq.offer(new DijkstraEdge(start, 0));

        while (!pq.isEmpty()) {
            DijkstraEdge current = pq.poll();
            int currentVertex = current.destination();
            int currentDist = current.weight();

            if (currentDist > distance[currentVertex]) continue;

            for (DijkstraEdge edge : graph.getNeighbors(currentVertex)) {
                int newDist = distance[currentVertex] + edge.weight();
                if (newDist < distance[edge.destination()]) {
                    distance[edge.destination()] = newDist;
                    parent[edge.destination()] = currentVertex;
                    pq.offer(new DijkstraEdge(edge.destination(), newDist));
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
        WeightedGraph graph = new WeightedGraph(6);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 2, 1);
        graph.addEdge(1, 3, 5);
        graph.addEdge(2, 3, 8);
        graph.addEdge(2, 4, 10);
        graph.addEdge(3, 4, 2);
        graph.addEdge(3, 5, 6);
        graph.addEdge(4, 5, 3);

        System.out.println("=== Dijkstra's Shortest Path from 0 ===");
        int[] distances = shortestPath(graph, 0);
        for (int i = 0; i < distances.length; i++) {
            System.out.println("Distance to " + i + ": " +
                    (distances[i] == Integer.MAX_VALUE ? "Infinity" : distances[i]));
        }

        System.out.println("\n=== Shortest Path from 0 to 5 ===");
        var path = shortestPathWithRoute(graph, 0, 5);
        System.out.println("Path: " + path);
        System.out.println("Total distance: " + distances[5]);
    }
}
