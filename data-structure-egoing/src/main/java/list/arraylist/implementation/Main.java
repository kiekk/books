package list.arraylist.implementation;

public class Main {
    public static void main(String[] args) {
        ArrayList numbers = new ArrayList();

        numbers.addLast(10);
        numbers.addLast(20);
        numbers.addLast(30);
        numbers.addLast(40);

        numbers.add(1, 15);
        numbers.addFirst(100);

        System.out.println(numbers);
        System.out.println("size : " + numbers.size());

        System.out.println("remove element : " + numbers.remove(1));
        System.out.println(numbers);
        System.out.println("size : " + numbers.size());

        System.out.println("remove first element : " + numbers.removeFirst());
        System.out.println(numbers);
        System.out.println("size : " + numbers.size());

        System.out.println("remove last element : " + numbers.removeLast());
        System.out.println(numbers);
        System.out.println("size : " + numbers.size());

        System.out.println("get element : " + numbers.get(0));
        System.out.println("get element : " + numbers.get(1));
        System.out.println("get element : " + numbers.get(2));
    }
}
