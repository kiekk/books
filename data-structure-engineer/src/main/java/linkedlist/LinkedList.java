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
}
