package chapter12.composite;

public class DuckSimulator {

    public static void main(String[] args) {
        DuckSimulator simulator = new DuckSimulator();
        System.out.println("오리 소리 카운팅 X");
        AbstractDuckFactory duckFactory = new DuckFactory();
        simulator.simulate(duckFactory);

        System.out.println("오리 소리 카운팅 O");
        AbstractDuckFactory countingDuckFactory = new CountingDuckFactory();
        simulator.simulate(countingDuckFactory);
    }

    void simulate(AbstractDuckFactory duckFactory) {
        Quackable redheadDuck = duckFactory.createRedheadDuck();
        Quackable duckCall = duckFactory.createDuckCall();
        Quackable rubberDuck = duckFactory.createRubberDuck();
        Quackable decoyDuck = duckFactory.createDecoyDuck();
        Quackable gooseDuck = new GooseAdapter(new Goose());

        System.out.println("오리 시뮬레이션 게임");

        simulate(redheadDuck);
        simulate(duckCall);
        simulate(rubberDuck);
        simulate(decoyDuck);
        simulate(gooseDuck);

        // 거위가 소리 낸 횟수는 포함 X
        System.out.println("오리가 소리 낸 횟수 : " + QuackCounter.getQuacks() + " 번");
    }

    void simulate(Quackable duck) {
        duck.quack();
    }
}

