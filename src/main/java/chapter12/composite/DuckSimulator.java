package chapter12.composite;

public class DuckSimulator {

    public static void main(String[] args) {
        DuckSimulator simulator = new DuckSimulator();
        simulator.simulate();
    }

    void simulate() {
        Quackable redheadDuck = new QuackCounter(new RedheadDuck());
        Quackable duckCall = new QuackCounter(new DuckCall());
        Quackable rubberDuck = new QuackCounter(new RubberDuck());
        Quackable decoyDuck = new QuackCounter(new DecoyDuck());
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

