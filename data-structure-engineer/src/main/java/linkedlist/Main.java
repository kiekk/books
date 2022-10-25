package linkedlist;

public class Main {
    public static void main(String[] args) {
        Node head = new Node(1);

        head.append(2);
        head.append(3);
        head.append(4);
        head.append(5);
        head.retrieve();
        head.delete(3);
        head.retrieve();

        LinkedList linkedList = new LinkedList();

        linkedList.append(1);
        linkedList.append(2);
        linkedList.append(3);
        linkedList.append(4);
        linkedList.retrieve();
        linkedList.delete(4);
        linkedList.retrieve();

        LinkedList linkedList1 = new LinkedList();

        linkedList1.append(1);
        linkedList1.append(1);
        linkedList1.append(2);
        linkedList1.append(3);
        linkedList1.append(4);
        linkedList1.append(5);
        linkedList1.append(5);
        linkedList1.retrieve();
        linkedList1.removeDups();
        linkedList1.retrieve();

        int k = 2;
        LinkedList.Node kth = linkedList1.kthToLast(linkedList1.header, k);
        System.out.println("Last k(" + k + ")th data is " + kth.data);
        LinkedList.Reference r = new LinkedList.Reference();
        System.out.println(linkedList1.kthToLast2(linkedList1.header, 3, r).data);

        System.out.println(deleteNode(linkedList1.get(4)));
        linkedList1.retrieve();


        LinkedList linkedList2 = new LinkedList();
        linkedList2.append(1);
        linkedList2.append(9);
        linkedList2.append(3);
        linkedList2.append(8);
        linkedList2.append(5);
        linkedList2.append(6);
        linkedList2.append(2);
        LinkedList.Node n = partition(linkedList2.get(1), 5);

        while (n.next != null) {
            System.out.printf(n.data + " -> ");
            n = n.next;
        }
        System.out.println(n.data);

    }

    static boolean deleteNode(LinkedList.Node n) {
        if (n == null || n.next == null) {
            return false;
        }

        LinkedList.Node next = n.next;
        n.data = next.data;
        n.next = next.next;
        return true;
    }

    static LinkedList.Node partition(LinkedList.Node n, int x) {
        LinkedList.Node s1 = null;
        LinkedList.Node e1 = null;
        LinkedList.Node s2 = null;
        LinkedList.Node e2 = null;

        while (n != null) {
            LinkedList.Node next = n.next;
            n.next = null;

            if (n.data < x) {
                if (s1 == null) {
                    s1 = n;
                    e1 = s1;
                } else {
                    e1.next = n;
                    e1 = n;
                }
            } else {
                if (s2 == null) {
                    s2 = n;
                    e2 = s2;
                } else {
                    e2.next = n;
                    e2 = n;
                }
            }
            n = next;
        }
        if (s1 == null) {
            return s2;
        }
        e1.next = s2;
        return s1;
    }
}
