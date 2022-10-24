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
}
