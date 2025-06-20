package chapter12;

public interface QuackObservable {
    void registerObserver(Observer observer);

    void notifyObservers();
}
