package list.linkedlist.implementation;

public class Main {
    public static void main(String[] args) {
        LinkedList numbers = new LinkedList();

        numbers.addFirst(30);
        numbers.addFirst(20);
        numbers.addFirst(10);

        numbers.addLast(10);
        numbers.addLast(20);
        numbers.addLast(30);

        numbers.add(1, 15);
        numbers.add(0, 100);

        System.out.println(numbers);

        System.out.println("removed: " + numbers.removeFirst());
        System.out.println(numbers);

        System.out.println("removed: " + numbers.remove(3));
        System.out.println(numbers);

        System.out.println("size: " + numbers.size());
        System.out.println("get(0): " + numbers.get(0));
        System.out.println("get(1): " + numbers.get(1));

        System.out.println("indexOf(20): " + numbers.indexOf(20));
        System.out.println("indexOf(30): " + numbers.indexOf(30));
        System.out.println("indexOf(40): " + numbers.indexOf(40));

        LinkedList.ListIterator listIterator = numbers.listIterator();

        while(listIterator.hasNext()) {
            System.out.println(listIterator.next());
        }

        System.out.println(numbers);
        LinkedList.ListIterator listIterator2 = numbers.listIterator();
        listIterator2.add(5);
        listIterator2.next();
        listIterator2.add(40);
        System.out.println(numbers);
    }
}
