package chapter03;

public class Soy extends CondimentDecorator {

    public Soy(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public double cost() {
        double cost = beverage.cost();

        switch (beverage.getSize()) {
            case TALL:
                cost += .10;
                break;
            case GRANDE:
                cost += .15;
                break;
            case BENTI:
                cost += .20;
                break;
        }

        return cost;
    }

    @Override
    public String getDescription() {
        return beverage.getDescription() + ", 두유";
    }
}
