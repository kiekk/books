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

    public Node node(int index) {
        if (index < size / 2) {
            Node x = head;
            for (int i = 0; i < index; i++) {
                x = x.next;
            }
            return x;
        } else {
            Node x = tail;
            for (int i = size - 1; i > index; i--) {
                x = x.prev;
            }
            return x;
        }
    }

    public Object removeFirst() {
        Node temp = head;
        head = head.next;
        Object returnData = temp.data;

        if (head != null) {
            head.prev = null;
        }

        size--;
        return returnData;
    }

    public Object remove(int index) {
        if (index == 0) {
            return removeFirst();
        } else {
            Node temp = node(index - 1);
            Node todoDeleted = temp.next;
            temp.next = temp.next.next;

            if (temp.next != null) {
                temp.next.prev = temp;
            }

            Object returnData = todoDeleted.data;

            if (todoDeleted == tail) {
                tail = temp;
            }

            size--;
            return returnData;
        }
    }

    public ListIterator listIterator() {
        return new ListIterator();
    }

    public int size() {
        return size;
    }

    public class ListIterator {
        private Node next;
        private Node lastReturned;
        private int nextIndex;

        public ListIterator() {
            next = head;
            nextIndex = 0;
        }

        public Object next() {
            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.data;
        }

        public boolean hasNext() {
            return nextIndex < size();
        }

        public Object prev() {
            if (next == null) {
                lastReturned = next = tail;
            } else {
                lastReturned = next = next.prev;
            }
            nextIndex--;
            return lastReturned.data;
        }

        public boolean hasPrev() {
            return nextIndex > 0;
        }

        public void add(Object data) {
            Node newNode = new Node(data);

            if (lastReturned == null) {
                head = newNode;
                newNode.next = next;
            } else {
                lastReturned.next = newNode;
                newNode.prev = lastReturned;

                if (next != null) {
                    newNode.next = next;
                    next.prev = newNode;
                } else {
                    tail = newNode;
                }
            }

            lastReturned = newNode;
            nextIndex++;
            size++;
        }

        public void remove() {
//            if(lastReturned == null)
            if (nextIndex == 0) {
                throw new IllegalStateException();
            }

            Node n = lastReturned.next;
            Node p = lastReturned.prev;

            if (p == null) {
                head = n;
                head.prev = null;
            } else {
                p.next = n;
            }

            if (n == null) {
                tail = p;
                tail.next = null;
            } else {
                n.prev = p;
            }

            if(next == null) {
                lastReturned = tail;
            } else {
                lastReturned = next.prev;
            }

            size--;
            nextIndex--;
        }
    }
}
