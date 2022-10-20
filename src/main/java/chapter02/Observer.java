package chapter02;

// Observer 가 되기 위해서는 Observer 인터페이스를 구현해야 합니다.
// 그리고 update 메소드에 변경될 때마다 실행될 내용을 작성합니다.
public interface Observer {
    void update(float temp, float humidity, float pressure);
}
