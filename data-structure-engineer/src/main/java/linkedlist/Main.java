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
    }
}
