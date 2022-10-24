package chapter12;

public class DecoyDuck implements Quackable {

    Observable observable;

    public DecoyDuck() {
        this.observable = new Observable(this);
    }

    @Override
    public void quack() {
        // do nothing
        System.out.println("침묵....");
        notifyObservers();
    }

    @Override
    public void registerObserver(Observer observer) {
        observable.registerObserver(observer);
    }

    @Override
    public void notifyObservers() {
        observable.notifyObservers();
    }

    @Override
    public String toString() {
        return "가짜 오리";
    }
}
