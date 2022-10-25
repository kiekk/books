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
    }
}
