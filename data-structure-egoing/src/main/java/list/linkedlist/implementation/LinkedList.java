package list.linkedlist.implementation;

public class LinkedList {
    private Node head;
    private Node tail;
    private int size = 0;

    private class Node {
        private Object data;
        private Node next;

        public Node(Object data) {
            this.data = data;
            this.next = null;
        }

        @Override
        public String toString() {
            return String.valueOf(this.data);
        }
    }

    public void addFirst(Object data) {
        Node newNode = new Node(data);

        newNode.next = head;
        head = newNode;
        size++;

        if (head.next == null) {
            tail = head;
        }
    }

    public void addLast(Object data) {
        Node newNode = new Node(data);

        if (size == 0) {
            addFirst(data);
        } else {
            tail.next = newNode;
            tail = newNode;
            size++;
        }
    }

    Node node(int index) {
        Node node = head;

        for (int i = 0; i < index; i++) {
            node = node.next;
        }

        return node;
    }

    public void add(int index, Object data) {
        if (index == 0) {
            addFirst(data);
        } else {
            Node temp1 = node(index - 1);
            Node temp2 = temp1.next;
            Node newNode = new Node(data);

            temp1.next = newNode;
            newNode.next = temp2;
            size++;

            if (newNode.next == null) {
                tail = newNode;
            }
        }
    }

    public Object removeFirst() {
        Node temp = head;
        head = head.next;

        Object returnData = temp.data;
        temp = null;
        size--;

        return returnData;
    }

    @Override
    public String toString() {
        if (head == null) {
            return "[]";
        }
        Node temp = head;
        StringBuilder str = new StringBuilder("[");

        while (temp.next != null) {
            str.append(temp).append(", ");
            temp = temp.next;
        }
        str.append(temp);
        return str + "]";
    }
}
