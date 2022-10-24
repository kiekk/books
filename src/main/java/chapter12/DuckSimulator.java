package chapter12;

public class DuckSimulator {

    public static void main(String[] args) {
        DuckSimulator simulator = new DuckSimulator();
        AbstractDuckFactory countingDuckFactory = new CountingDuckFactory();
        simulator.simulate(countingDuckFactory);
    }

    void simulate(AbstractDuckFactory duckFactory) {
        Quackable redheadDuck = duckFactory.createRedheadDuck();
        Quackable duckCall = duckFactory.createDuckCall();
        Quackable rubberDuck = duckFactory.createRubberDuck();
        Quackable decoyDuck = duckFactory.createDecoyDuck();
        Quackable gooseDuck = new GooseAdapter(new Goose());

        System.out.println("오리 시뮬레이션 게임: 무리 (+옵저버)");

        Flock flockOfDucks = new Flock();

        flockOfDucks.add(redheadDuck);
        flockOfDucks.add(duckCall);
        flockOfDucks.add(rubberDuck);
        flockOfDucks.add(decoyDuck);
        flockOfDucks.add(gooseDuck);

        Flock flockOfMallards = new Flock();

        Quackable mallardOne = duckFactory.createMallardDuck();
        Quackable mallardTwo = duckFactory.createMallardDuck();
        Quackable mallardThree = duckFactory.createMallardDuck();
        Quackable mallardFour = duckFactory.createMallardDuck();

        flockOfMallards.add(mallardOne);
        flockOfMallards.add(mallardTwo);
        flockOfMallards.add(mallardThree);
        flockOfMallards.add(mallardFour);

        flockOfDucks.add(flockOfMallards);

        Quacklogist quacklogist = new Quacklogist();
        flockOfMallards.registerObserver(quacklogist);
        flockOfDucks.registerObserver(quacklogist);

        System.out.println("오리 시뮬레이션 게임: 전체 무리");
        simulate(flockOfDucks);

        System.out.println("오리 시뮬레이션 게임: 물오리 무리");
        simulate(flockOfMallards);

        System.out.println("오리가 소리 낸 횟수 : " + QuackCounter.getQuacks() + " 번");
    }

    void simulate(Quackable duck) {
        duck.quack();
    }
}

