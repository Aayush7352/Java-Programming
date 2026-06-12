package phase14.designpatterns;

import java.util.function.Function;

// Decorator Pattern: Component interface, concrete component, decorator abstract class, concrete decorators

// Component interface
interface Coffee {
    String getDescription();
    double getCost();
}

// Concrete component
class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "Simple coffee";
    }

    @Override
    public double getCost() {
        return 2.0;
    }
}

// Decorator abstract class
abstract class CoffeeDecorator implements Coffee {
    protected final Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }
}

// Concrete decorators
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + milk";
    }

    @Override
    public double getCost() {
        return super.getCost() + 0.5;
    }
}

class SugarDecorator extends CoffeeDecorator {
    private final int teaspoons;

    public SugarDecorator(Coffee coffee, int teaspoons) {
        super(coffee);
        this.teaspoons = teaspoons;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + " + teaspoons + " sugar";
    }

    @Override
    public double getCost() {
        return super.getCost() + teaspoons * 0.2;
    }
}

class WhippedCreamDecorator extends CoffeeDecorator {
    public WhippedCreamDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + whipped cream";
    }

    @Override
    public double getCost() {
        return super.getCost() + 0.7;
    }
}

class CaramelDecorator extends CoffeeDecorator {
    public CaramelDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + caramel";
    }

    @Override
    public double getCost() {
        return super.getCost() + 0.8;
    }
}

class ExtraShotDecorator extends CoffeeDecorator {
    public ExtraShotDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + extra shot";
    }

    @Override
    public double getCost() {
        return super.getCost() + 1.0;
    }
}

// Another component hierarchy: Pizza
interface Pizza {
    String getDescription();
    double getCost();
}

class MargheritaPizza implements Pizza {
    @Override
    public String getDescription() { return "Margherita Pizza"; }
    @Override
    public double getCost() { return 8.0; }
}

class PepperoniPizza implements Pizza {
    @Override
    public String getDescription() { return "Pepperoni Pizza"; }
    @Override
    public double getCost() { return 10.0; }
}

abstract class PizzaDecorator implements Pizza {
    protected final Pizza pizza;
    public PizzaDecorator(Pizza pizza) { this.pizza = pizza; }
    @Override
    public String getDescription() { return pizza.getDescription(); }
    @Override
    public double getCost() { return pizza.getCost(); }
}

class ExtraCheeseDecorator extends PizzaDecorator {
    public ExtraCheeseDecorator(Pizza pizza) { super(pizza); }
    @Override
    public String getDescription() { return super.getDescription() + " + extra cheese"; }
    @Override
    public double getCost() { return super.getCost() + 1.5; }
}

class MushroomDecorator extends PizzaDecorator {
    public MushroomDecorator(Pizza pizza) { super(pizza); }
    @Override
    public String getDescription() { return super.getDescription() + " + mushrooms"; }
    @Override
    public double getCost() { return super.getCost() + 1.0; }
}

class OlivesDecorator extends PizzaDecorator {
    public OlivesDecorator(Pizza pizza) { super(pizza); }
    @Override
    public String getDescription() { return super.getDescription() + " + olives"; }
    @Override
    public double getCost() { return super.getCost() + 0.8; }
}

// Functional decorator approach using Function composition
record FunctionalCoffee(String description, double cost) {
    static FunctionalCoffee of(String description, double cost) {
        return new FunctionalCoffee(description, cost);
    }

    // Decorator as function
    public FunctionalCoffee decorate(Function<FunctionalCoffee, FunctionalCoffee> decorator) {
        return decorator.apply(this);
    }
}

public class DecoratorPattern {
    public static void main(String[] args) {
        System.out.println("=== Decorator Pattern Demo ===\n");

        // 1. Basic coffee
        System.out.println("1. Simple Coffee:");
        Coffee coffee = new SimpleCoffee();
        System.out.println("  " + coffee.getDescription() + " = $" + coffee.getCost());

        // 2. Coffee with milk
        System.out.println("\n2. Adding Decorators (one at a time):");
        Coffee withMilk = new MilkDecorator(new SimpleCoffee());
        System.out.println("  " + withMilk.getDescription() + " = $" + withMilk.getCost());

        // 3. Multiple decorators stacked
        System.out.println("\n3. Stacked Decorators:");
        Coffee fancyCoffee = new CaramelDecorator(
                new WhippedCreamDecorator(
                        new ExtraShotDecorator(
                                new MilkDecorator(
                                        new SugarDecorator(new SimpleCoffee(), 2)))));
        System.out.println("  " + fancyCoffee.getDescription() + " = $" + fancyCoffee.getCost());

        // 4. Different combinations
        System.out.println("\n4. Various Coffee Combinations:");
        printCoffee(new MilkDecorator(new SimpleCoffee()));
        printCoffee(new SugarDecorator(new MilkDecorator(new SimpleCoffee()), 1));
        printCoffee(new ExtraShotDecorator(new CaramelDecorator(new SimpleCoffee())));
        printCoffee(new WhippedCreamDecorator(
                new CaramelDecorator(
                        new ExtraShotDecorator(new SimpleCoffee()))));

        // 5. Decorator with different component (Pizza)
        System.out.println("\n5. Pizza Decorator (another component):");
        Pizza pizza = new ExtraCheeseDecorator(
                new MushroomDecorator(
                        new OlivesDecorator(new MargheritaPizza())));
        System.out.println("  " + pizza.getDescription() + " = $" + pizza.getCost());

        // 6. Functional decorator (using Function composition)
        System.out.println("\n6. Functional Decorator (Java 8+):");
        Function<FunctionalCoffee, FunctionalCoffee> addMilk =
                c -> FunctionalCoffee.of(c.description() + " + milk", c.cost() + 0.5);
        Function<FunctionalCoffee, FunctionalCoffee> addSugar =
                c -> FunctionalCoffee.of(c.description() + " + sugar", c.cost() + 0.2);
        Function<FunctionalCoffee, FunctionalCoffee> addWhippedCream =
                c -> FunctionalCoffee.of(c.description() + " + whipped cream", c.cost() + 0.7);

        var base = FunctionalCoffee.of("Black coffee", 2.0);
        var functionalCoffee = base
                .decorate(addMilk)
                .decorate(addSugar)
                .decorate(addWhippedCream);
        System.out.println("  " + functionalCoffee.description()
                + " = $" + functionalCoffee.cost());

        // Composing functions
        Function<FunctionalCoffee, FunctionalCoffee> combo =
                addMilk.andThen(addSugar).andThen(addWhippedCream);
        var composed = base.decorate(combo);
        System.out.println("  (composed) " + composed.description()
                + " = $" + composed.cost());

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Component interface - defines the core object interface");
        System.out.println("Concrete component - the base object being decorated");
        System.out.println("Decorator abstract class - wraps a component, delegates to it");
        System.out.println("Concrete decorators - add specific behaviors/responsibilities");
        System.out.println("Stacked decorators - multiple decorators combined (composition)");
        System.out.println("Functional decorator - using Function<T, T> composition");
        System.out.println("Open/Closed Principle - extend behavior without modifying existing code");
    }

    private static void printCoffee(Coffee coffee) {
        System.out.println("  " + coffee.getDescription() + " = $" + coffee.getCost());
    }
}
