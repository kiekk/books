package linkedlist;

public class LinkedList {
    Node header;

    static class Node {
        int data;
        Node next = null;

        public Node(int data) {
            this.data = data;
        }
    }

    public LinkedList() {
        this.header = new Node(0);
    }

    void append(int data) {
        Node end = new Node(data);
        Node n = header;

        while (n.next != null) {
            n = n.next;
        }
        n.next = end;
    }

    void delete(int data) {
        Node n = header;

        while (n.next != null) {
            if (n.next.data == data) {
                n.next = n.next.next;
            } else {
                n = n.next;
            }
        }
    }

    void retrieve() {
        Node n = header.next;

        while (n.next != null) {
            System.out.printf(n.data + "-> ");
            n = n.next;
        }

        System.out.println(n.data);
    }

    void removeDups() {
        Node n = header;

        while (n != null && n.next != null) {
            Node r = n;
            while (r.next != null) {
                if (n.data == r.next.data) {
                    r.next = r.next.next;
                } else {
                    r = r.next;
                }
            }
            n = n.next;
        }
    }

    Node kthToLast(Node first, int k) {
        Node n = first;
        int total = 1;

        while (n.next != null) {
            total++;
            n = n.next;
        }
        n = first;
        for (int i = 1; i < total - k + 1; i++) {
            n = n.next;
        }
        return n;
    }

    Node kthToLast2(Node n, int k, Reference r) {
        if (n == null) {
            return null;
        }

        Node found = kthToLast2(n.next, k, r);
        r.count++;

        if (r.count == k) {
            return n;
        }
        return found;
    }

    Node get(int k) {
        Node n = header;
        for (int i = 0; i < k; i++) {
            n = n.next;
        }
        return n;
    }

    static class Reference {
        public int count;
    }
}
