package chapter12;

public class DecoyDuck implements Quackable {
    @Override
    public void quack() {
        // do nothing
        System.out.println("침묵....");
    }
}
