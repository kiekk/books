package chapter03;

public class Mocha extends CondimentDecorator {

    public Mocha(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public double cost() {
        double cost = beverage.cost();

        switch (beverage.getSize()) {
            case TALL:
                cost += .15;
                break;
            case GRANDE:
                cost += .20;
                break;
            case BENTI:
                cost += .25;
                break;
        }

        return cost;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", 모카";
    }
}
