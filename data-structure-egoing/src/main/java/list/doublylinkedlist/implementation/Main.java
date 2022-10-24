package list.doublylinkedlist.implementation;

public class Main {
    public static void main(String[] args) {
        DoublyLinkedList doublyLinkedList = new DoublyLinkedList();

        doublyLinkedList.addFirst(1);
        doublyLinkedList.addFirst(2);
        doublyLinkedList.addFirst(3);

        doublyLinkedList.addLast(5);
        doublyLinkedList.addLast(6);

        doublyLinkedList.add(0, 10);
        doublyLinkedList.add(3, 50);

        System.out.println("remove data: " + doublyLinkedList.removeFirst());
        System.out.println("remove data(0) " + doublyLinkedList.remove(0));
        System.out.println("remove data(1) " + doublyLinkedList.remove(1));

        DoublyLinkedList.ListIterator listIterator = doublyLinkedList.listIterator();

        while (listIterator.hasNext()) {
            int next = (int) listIterator.next();

            if (next == 2) {
                listIterator.add(100);
            }

            System.out.println("next : " + next);
        }

        while (listIterator.hasPrev()) {
            System.out.println("prev : " + listIterator.prev());
        }
    }
}
