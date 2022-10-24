package chapter07;

public class DuckTestDrive {
    public static void main(String[] args) {
        Duck duck = new MallardDuck();
        Turkey turkey = new WildTurkey();

        System.out.println("칠면조가 말하길");
        turkey.gobble();
        turkey.fly();

        System.out.println("오리가 말하길");
        testDuck(duck);

        // 만약 칠면조 객체를 오리 객체 대신 사용해야 한다면?
        // Duck duck = new WildTurkey(); // Error!

        // Adapter 적용
        Duck turkeyAdapter = new TurkeyAdapter(turkey);
        System.out.println("칠면조 어댑터가 말하길");
        testDuck(turkeyAdapter);
    }

    static void testDuck(Duck duck) {
        duck.quack();
        duck.fly();
    }
}
