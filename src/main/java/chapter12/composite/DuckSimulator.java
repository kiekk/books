package chapter12.composite;

public class DuckSimulator {

    public static void main(String[] args) {
        DuckSimulator simulator = new DuckSimulator();
        simulator.simulate();
    }

    void simulate() {
        Quackable redheadDuck = new RedheadDuck();
        Quackable duckCall = new DuckCall();
        Quackable rubberDuck = new RubberDuck();
        Quackable decoyDuck = new DecoyDuck();

        System.out.println("오리 시뮬레이션 게임");

        simulate(redheadDuck);
        simulate(duckCall);
        simulate(rubberDuck);
        simulate(decoyDuck);
    }

    void simulate(Quackable duck) {
        duck.quack();
    }
}

