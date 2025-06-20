package graph;

import java.util.LinkedList;
import java.util.Stack;

public class Graph {

    class Node {
        int data;
        LinkedList<Node> adjacent;
        boolean marked;

        public Node(int data) {
            this.data = data;
            this.marked = false;
            adjacent = new LinkedList<>();
        }
    }

    Node[] nodes;

    public Graph(int size) {
        nodes = new Node[size];
        for (int i = 0; i < size; i++) {
            nodes[i] = new Node(i);
        }
    }

    void addEdge(int i1, int i2) {
        Node n1 = nodes[i1];
        Node n2 = nodes[i2];

        if (!n1.adjacent.contains(n2)) {
            n1.adjacent.add(n2);
        }
        if (!n2.adjacent.contains(n1)) {
            n2.adjacent.add(n1);
        }
    }

    void dfs() {
        dfs(0);
    }

    void dfs(int index) {
        Node root = nodes[index];
        Stack<Node> stack = new Stack<>();
        stack.push(root);
        root.marked = true;
        while (!stack.isEmpty()) {
            Node r = stack.pop();
            for (Node n : r.adjacent) {
                if (!n.marked) {
                    n.marked = true;
                    stack.push(n);
                }
            }
            visit(r);
        }
    }

    void visit(Node n) {
        System.out.printf(n.data + " ");
    }

    void bfs() {
        bfs(0);
    }

    void bfs(int index) {
        Node root = nodes[index];
        Queue<Node> queue = new Queue<>();
        queue.add(root);
        root.marked = true;
        while (!queue.isEmpty()) {
            Node r = queue.remove();
            for (Node n : r.adjacent) {
                if (!n.marked) {
                    n.marked = true;
                    queue.add(n);
                }
            }
            visit(r);
        }
    }

    void dfsR(Node r) {
        if (r == null) {
            return;
        }
        r.marked = true;
        visit(r);
        for (Node n : r.adjacent) {
            if (!n.marked) {
                dfsR(n);
            }
        }
    }

    void dfsR() {
        dfsR(0);
    }

    void dfsR(int index) {
        Node r = nodes[index];
        dfsR(r);
    }

    void initMarks() {
        for (Node n : nodes) {
            n.marked = false;
        }
    }

    boolean search(int i1, int i2) {
        return search(nodes[i1], nodes[i2]);
    }

    boolean search(Node start, Node end) {
        initMarks();
        LinkedList<Node> q = new LinkedList<>();
        q.add(start);

        while (!q.isEmpty()) {
            Node root = q.removeFirst();

            if (root == end) {
                return true;
            }

            for (Node n : root.adjacent) {
                if (!n.marked) {
                    n.marked = true;
                    q.add(n);
                }
            }
        }
        return false;
    }

}
