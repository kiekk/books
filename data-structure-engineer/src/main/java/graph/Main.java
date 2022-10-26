package graph;

public class Main {
    public static void main(String[] args) {
        Graph g = new Graph(9);

        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(1, 3);
        g.addEdge(2, 4);
        g.addEdge(2, 3);
        g.addEdge(3, 4);
        g.addEdge(3, 5);
        g.addEdge(5, 6);
        g.addEdge(5, 7);
        g.addEdge(6, 8);

        Graph g2 = new Graph(9);

        g2.addEdge(0, 1);
        g2.addEdge(1, 2);
        g2.addEdge(1, 3);
        g2.addEdge(2, 4);
        g2.addEdge(2, 3);
        g2.addEdge(3, 4);
        g2.addEdge(3, 5);
        g2.addEdge(5, 6);
        g2.addEdge(5, 7);
        g2.addEdge(6, 8);

        Graph g3 = new Graph(9);

        g3.addEdge(0, 1);
        g3.addEdge(1, 2);
        g3.addEdge(1, 3);
        g3.addEdge(2, 4);
        g3.addEdge(2, 3);
        g3.addEdge(3, 4);
        g3.addEdge(3, 5);
        g3.addEdge(5, 6);
        g3.addEdge(5, 7);
        g3.addEdge(6, 8);

        System.out.println("---DFS---");
        g.dfs();
        System.out.println("\n---BFS---");
        g2.bfs();
        System.out.println("\n---DFS Recursive---");
        g3.dfsR();
    }
}
