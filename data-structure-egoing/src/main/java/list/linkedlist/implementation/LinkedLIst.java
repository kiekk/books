package list.linkedlist.implementation;

public class LinkedLIst {
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
}
