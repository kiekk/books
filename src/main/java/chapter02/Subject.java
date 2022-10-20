package chapter02;

public interface Subject {
    // Subject 는 Observer 를 등록/삭제 할 수 있습니다.
    void registerObserver(Observer o);

    void removeObserver(Observer o);

    // Subject 상태가 바뀔 때 등록된 모든 Observer 들에게 변경 내용을 알릴 수 있습니다.
    void notifyObservers();

}
