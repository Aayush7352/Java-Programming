package phase14.designpatterns;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Observer Pattern: Observable/Observer, Subject interface, push vs pull, PropertyChangeListener

// 1. Classic Observer pattern
interface Observer {
    void update(String event, Object data);
}

// Subject interface
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers(String event, Object data);
}

// Concrete Subject (Observable)
class NewsAgency implements Subject {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private String latestNews;

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
        System.out.println("  [NewsAgency] Observer attached: " + observer.getClass().getSimpleName());
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
        System.out.println("  [NewsAgency] Observer detached: " + observer.getClass().getSimpleName());
    }

    @Override
    public void notifyObservers(String event, Object data) {
        System.out.println("  [NewsAgency] Notifying " + observers.size() + " observers of '" + event + "'");
        for (var observer : observers) {
            observer.update(event, data);
        }
    }

    // Business method that triggers notification
    public void publishNews(String news) {
        this.latestNews = news;
        System.out.println("\n  [NewsAgency] Breaking News: " + news);
        notifyObservers("news", news);
    }
}

// Concrete Observer (push model - receives data)
class NewsChannel implements Observer {
    private final String channelName;

    public NewsChannel(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void update(String event, Object data) {
        if ("news".equals(event)) {
            System.out.println("    [" + channelName + "] Received: " + data);
        }
    }
}

// Concrete Observer (pull model - asks subject for specific data)
class StockTrader implements Observer {
    private final String name;
    private final double sellThreshold;

    public StockTrader(String name, double sellThreshold) {
        this.name = name;
        this.sellThreshold = sellThreshold;
    }

    @Override
    public void update(String event, Object data) {
        if ("stockPrice".equals(event) && data instanceof Double price) {
            String action = price >= sellThreshold ? "SELL" : "HOLD";
            System.out.println("    [Trader " + name + "] Stock at $" + price + " -> " + action);
        }
    }
}

// 2. PropertyChangeListener (java.beans) approach
class TemperatureSensor {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private double temperature;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void setTemperature(double newTemperature) {
        double old = this.temperature;
        this.temperature = newTemperature;
        System.out.println("\n  [TemperatureSensor] Temperature changed: " + old + " -> " + newTemperature);
        pcs.firePropertyChange("temperature", old, newTemperature);
    }
}

class ThermostatDisplay {
    private final String name;

    public ThermostatDisplay(String name) {
        this.name = name;
    }

    public PropertyChangeListener createListener() {
        return evt -> {
            if ("temperature".equals(evt.getPropertyName())) {
                System.out.println("    [" + name + "] Temperature: "
                        + evt.getNewValue() + "°C (was " + evt.getOldValue() + "°C)");
            }
        };
    }
}

// 3. Event-driven stock market (push vs pull demo)
class StockMarket implements Subject {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    private final java.util.Map<String, Double> stocks = new java.util.HashMap<>();

    public StockMarket() {
        stocks.put("AAPL", 150.0);
        stocks.put("GOOGL", 2800.0);
        stocks.put("TSLA", 700.0);
    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event, Object data) {
        for (var observer : observers) {
            observer.update(event, data);
        }
    }

    public void updateStockPrice(String symbol, double newPrice) {
        double oldPrice = stocks.getOrDefault(symbol, 0.0);
        stocks.put(symbol, newPrice);
        System.out.println("\n  [StockMarket] " + symbol + ": $" + oldPrice + " -> $" + newPrice);

        // Push: send the new price directly
        notifyObservers("stockPrice", newPrice);

        // Push: send full context
        notifyObservers("stockUpdate", new StockUpdate(symbol, oldPrice, newPrice));
    }
}

record StockUpdate(String symbol, double oldPrice, double newPrice) {}

// Pull-model observer
class PullModelObserver implements Observer {
    private final StockMarket market;

    public PullModelObserver(StockMarket market) {
        this.market = market;
    }

    @Override
    public void update(String event, Object data) {
        if ("stockUpdate".equals(event) && data instanceof StockUpdate update) {
            // Observer pulls data from the event object
            double change = update.newPrice() - update.oldPrice();
            String direction = change >= 0 ? "↑" : "↓";
            System.out.println("    [Pull Observer] " + update.symbol()
                    + " " + direction + " $" + String.format("%.2f", Math.abs(change)));
        }
    }
}

public class ObserverPattern {
    public static void main(String[] args) {
        System.out.println("=== Observer Pattern Demo ===\n");

        // 1. Classic Observable/Observer (push model)
        System.out.println("1. Classic Observable/Observer (push model):");
        var newsAgency = new NewsAgency();
        var cnn = new NewsChannel("CNN");
        var bbc = new NewsChannel("BBC");
        var sky = new NewsChannel("Sky News");

        newsAgency.attach(cnn);
        newsAgency.attach(bbc);
        newsAgency.attach(sky);

        newsAgency.publishNews("Java 21 Released with Virtual Threads!");
        newsAgency.detach(sky);
        newsAgency.publishNews("Record Patterns Now Available in Java");

        // 2. Pull model (observer requests specific data)
        System.out.println("\n2. Push vs Pull Model:");
        var stockMarket = new StockMarket();
        var trader1 = new StockTrader("Alice", 750.0);
        var trader2 = new StockTrader("Bob", 160.0);
        var pullObserver = new PullModelObserver(stockMarket);

        stockMarket.attach(trader1);
        stockMarket.attach(trader2);
        stockMarket.attach(pullObserver);

        stockMarket.updateStockPrice("TSLA", 750.0);
        stockMarket.updateStockPrice("AAPL", 155.0);

        // 3. PropertyChangeListener (java.beans)
        System.out.println("\n3. PropertyChangeListener (java.beans):");
        var sensor = new TemperatureSensor();
        var display1 = new ThermostatDisplay("Living Room");
        var display2 = new ThermostatDisplay("Bedroom");

        sensor.addPropertyChangeListener(display1.createListener());
        sensor.addPropertyChangeListener(display2.createListener());

        sensor.setTemperature(22.5);
        sensor.setTemperature(24.0);
        sensor.setTemperature(19.8);

        // 4. Multiple event types
        System.out.println("\n4. Multiple Event Types:");
        var multiSubject = new NewsAgency();
        var multiObserver = new Observer() {
            @Override
            public void update(String event, Object data) {
                System.out.println("    [MultiObserver] Event: " + event + " | Data: " + data);
            }
        };
        multiSubject.attach(multiObserver);
        multiSubject.publishNews("Weather Alert: Storm coming");
        multiSubject.notifyObservers("weather", "Sunny with clouds");
        multiSubject.notifyObservers("sports", "Team wins championship!");

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("Subject/Observable - maintains list of observers, notifies on state change");
        System.out.println("Observer - receives updates from subject (update method)");
        System.out.println("Push model - subject sends data to observers (data included in notification)");
        System.out.println("Pull model - observer retrieves specific data from subject/event");
        System.out.println("PropertyChangeListener - java.beans built-in observer support");
        System.out.println("Multiple event types - single observer handling different event categories");
    }
}
