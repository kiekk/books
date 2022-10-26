package tree;

public class Main {
    public static void main(String[] args) {
        Tree t = new Tree();
        Node n4 = t.makeNode(null, 4, null);
        Node n5 = t.makeNode(null, 5, null);
        Node n2 = t.makeNode(n4, 2, n5);
        Node n3 = t.makeNode(null, 3, null);
        Node n1 = t.makeNode(n2, 1, n3);

        t.setRoot(n1);

        System.out.println("---in order---");
        t.inorder(t.getRoot());

        System.out.println("---pre order---");
        t.preorder(t.getRoot());

        System.out.println("---post order---");
        t.postorder(t.getRoot());

        int[] a = new int[10];

        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        Tree t2 = new Tree();
        t2.makeTree(a);

        t2.searchBTree(t2.getRoot(), 3);
    }
}
