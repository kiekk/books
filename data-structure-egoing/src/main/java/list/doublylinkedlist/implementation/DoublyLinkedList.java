package list.doublylinkedlist.implementation;

public class DoublyLinkedList {
    private Node head;
    private Node tail;
    private int size = 0;

    private class Node {
        private Object data;

        private Node prev;
        private Node next;

        public Node(Object data) {
            this.data = data;
            this.prev = null;
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

        if (head != null) {
            head.prev = newNode;
        }

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
            newNode.prev = tail;
            tail = newNode;
            size++;
        }
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

            if (temp2 != null) {
                temp2.prev = newNode;
            }

            newNode.prev = temp1;
            size++;

            if (newNode.next == null) {
                tail = newNode;
            }
        }
    }
}
