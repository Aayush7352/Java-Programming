package phase16.projects;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/**
 * TradingSystem.java
 *
 * Trading system: Order (BUY/SELL), OrderBook with price-time priority,
 * matching engine, trade execution, portfolio tracking.
 */
public class TradingSystem {

    // ═══════════════════════════════════════════════
    // Enums & Records
    // ═══════════════════════════════════════════════

    enum OrderSide { BUY, SELL }
    enum OrderType { MARKET, LIMIT }
    enum OrderStatus { NEW, PARTIALLY_FILLED, FILLED, CANCELLED }
    enum TimeInForce { DAY, GTC, IOC, FOK }

    record Order(String orderId, String symbol, OrderSide side, OrderType type, double price,
                 int quantity, int filledQuantity, OrderStatus status, TimeInForce tif,
                 String traderId, Instant createdAt) {
        public int remainingQuantity() { return quantity - filledQuantity; }
        public boolean isFilled() { return filledQuantity >= quantity; }
        public boolean isActive() { return status == OrderStatus.NEW || status == OrderStatus.PARTIALLY_FILLED; }

        Order withFilled(int additionalFill) {
            int newFilled = filledQuantity + additionalFill;
            OrderStatus newStatus = newFilled >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
            return new Order(orderId, symbol, side, type, price, quantity, newFilled, newStatus, tif, traderId, createdAt);
        }

        Order withStatus(OrderStatus s) {
            return new Order(orderId, symbol, side, type, price, quantity, filledQuantity, s, tif, traderId, createdAt);
        }
    }

    record Trade(String tradeId, String buyOrderId, String sellOrderId, String symbol,
                 double price, int quantity, Instant timestamp) {}

    record PortfolioSummary(String traderId, double cashBalance, Map<String, Integer> holdings,
                            double totalValue) {}

    record MarketData(String symbol, double bid, double ask, double last, int volume) {}

    // ═══════════════════════════════════════════════
    // Order Book
    // ═══════════════════════════════════════════════

    static final class OrderBook {
        private final String symbol;
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        // Buy orders: sorted by price descending (highest first), then time ascending
        private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(
            (a, b) -> {
                int cmp = Double.compare(b.price(), a.price());
                if (cmp != 0) return cmp;
                return a.createdAt().compareTo(b.createdAt());
            }
        );

        // Sell orders: sorted by price ascending (lowest first), then time ascending
        private final PriorityQueue<Order> sellOrders = new PriorityQueue<>(
            (a, b) -> {
                int cmp = Double.compare(a.price(), b.price());
                if (cmp != 0) return cmp;
                return a.createdAt().compareTo(b.createdAt());
            }
        );

        private final ConcurrentHashMap<String, Order> allOrders = new ConcurrentHashMap<>();
        private final List<Trade> trades = new CopyOnWriteArrayList<>();
        private final AtomicInteger tradeCounter = new AtomicInteger(0);

        OrderBook(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() { return symbol; }

        public Order addOrder(Order order) {
            allOrders.put(order.orderId(), order);
            rwLock.writeLock().lock();
            try {
                if (order.side() == OrderSide.BUY) {
                    buyOrders.offer(order);
                } else {
                    sellOrders.offer(order);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            return order;
        }

        public List<Trade> matchOrders() {
            var newTrades = new ArrayList<Trade>();
            rwLock.writeLock().lock();
            try {
                while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
                    Order bestBuy = buyOrders.peek();
                    Order bestSell = sellOrders.peek();

                    if (bestBuy.price() < bestSell.price()) break; // No match

                    int matchQuantity = Math.min(bestBuy.remainingQuantity(), bestSell.remainingQuantity());
                    double matchPrice = bestSell.price(); // Sell price wins

                    // Execute trade
                    String tradeId = "TRADE-" + tradeCounter.incrementAndGet();
                    var trade = new Trade(tradeId, bestBuy.orderId(), bestSell.orderId(),
                        symbol, matchPrice, matchQuantity, Instant.now());
                    newTrades.add(trade);
                    trades.add(trade);

                    // Update orders
                    Order filledBuy = bestBuy.withFilled(matchQuantity);
                    Order filledSell = bestSell.withFilled(matchQuantity);

                    allOrders.put(bestBuy.orderId(), filledBuy);
                    allOrders.put(bestSell.orderId(), filledSell);

                    pollAndUpdateQueue(buyOrders, filledBuy);
                    pollAndUpdateQueue(sellOrders, filledSell);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            return newTrades;
        }

        private void pollAndUpdateQueue(PriorityQueue<Order> queue, Order updated) {
            queue.poll(); // Remove current head
            if (updated.isActive()) {
                queue.offer(updated);
            }
        }

        public boolean cancelOrder(String orderId) {
            var order = allOrders.get(orderId);
            if (order == null || !order.isActive()) return false;
            var cancelled = order.withStatus(OrderStatus.CANCELLED);
            allOrders.put(orderId, cancelled);

            rwLock.writeLock().lock();
            try {
                buyOrders.removeIf(o -> o.orderId().equals(orderId));
                sellOrders.removeIf(o -> o.orderId().equals(orderId));
            } finally {
                rwLock.writeLock().unlock();
            }
            return true;
        }

        public Optional<Order> getOrder(String orderId) {
            return Optional.ofNullable(allOrders.get(orderId));
        }

        public List<Order> getBuyOrders() {
            rwLock.readLock().lock();
            try {
                return List.copyOf(buyOrders);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public List<Order> getSellOrders() {
            rwLock.readLock().lock();
            try {
                return List.copyOf(sellOrders);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public List<Trade> getTrades() { return List.copyOf(trades); }

        public MarketData getMarketData() {
            rwLock.readLock().lock();
            try {
                double bid = buyOrders.isEmpty() ? 0 : buyOrders.peek().price();
                double ask = sellOrders.isEmpty() ? 0 : sellOrders.peek().price();
                double last = trades.isEmpty() ? 0 : trades.getLast().price();
                int volume = trades.stream().mapToInt(Trade::quantity).sum();
                return new MarketData(symbol, bid, ask, last, volume);
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    // ═══════════════════════════════════════════════
    // Matching Engine
    // ═══════════════════════════════════════════════

    static final class MatchingEngine {
        private final ConcurrentHashMap<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
        private final AtomicInteger orderCounter = new AtomicInteger(0);
        private final List<Trade> allTrades = new CopyOnWriteArrayList<>();
        private final Consumer<List<Trade>> tradeCallback;

        MatchingEngine() { this(null); }

        MatchingEngine(Consumer<List<Trade>> tradeCallback) {
            this.tradeCallback = tradeCallback;
        }

        public OrderBook createOrderBook(String symbol) {
            return orderBooks.computeIfAbsent(symbol, OrderBook::new);
        }

        public Order placeOrder(String symbol, OrderSide side, OrderType type, double price,
                                 int quantity, TimeInForce tif, String traderId) {
            var book = orderBooks.computeIfAbsent(symbol, OrderBook::new);
            String orderId = "ORD-" + orderCounter.incrementAndGet();
            var order = new Order(orderId, symbol, side, type, price, quantity, 0,
                OrderStatus.NEW, tif, traderId, Instant.now());
            book.addOrder(order);

            var trades = book.matchOrders();
            if (!trades.isEmpty()) {
                allTrades.addAll(trades);
                if (tradeCallback != null) {
                    tradeCallback.accept(trades);
                }
            }
            return book.getOrder(orderId).orElse(order);
        }

        public Order placeMarketOrder(String symbol, OrderSide side, int quantity, String traderId) {
            double price = side == OrderSide.BUY ? Double.MAX_VALUE : 0;
            return placeOrder(symbol, side, OrderType.MARKET, price, quantity, TimeInForce.IOC, traderId);
        }

        public boolean cancelOrder(String symbol, String orderId) {
            var book = orderBooks.get(symbol);
            return book != null && book.cancelOrder(orderId);
        }

        public Optional<Order> getOrder(String symbol, String orderId) {
            var book = orderBooks.get(symbol);
            return book == null ? Optional.empty() : book.getOrder(orderId);
        }

        public MarketData getMarketData(String symbol) {
            var book = orderBooks.get(symbol);
            return book == null ? new MarketData(symbol, 0, 0, 0, 0) : book.getMarketData();
        }

        public List<Trade> getAllTrades(String symbol) {
            var book = orderBooks.get(symbol);
            return book == null ? List.of() : book.getTrades();
        }

        public List<Order> getBuyOrders(String symbol) {
            var book = orderBooks.get(symbol);
            return book == null ? List.of() : book.getBuyOrders();
        }

        public List<Order> getSellOrders(String symbol) {
            var book = orderBooks.get(symbol);
            return book == null ? List.of() : book.getSellOrders();
        }

        public List<Trade> getAllTrades() { return List.copyOf(allTrades); }
    }

    @FunctionalInterface
    interface Consumer<T> {
        void accept(T t);
    }

    // ═══════════════════════════════════════════════
    // Portfolio Tracker
    // ═══════════════════════════════════════════════

    static final class PortfolioTracker {
        private final ConcurrentHashMap<String, Double> cashBalances = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> holdings = new ConcurrentHashMap<>();

        public void deposit(String traderId, double amount) {
            cashBalances.merge(traderId, amount, Double::sum);
        }

        public boolean withdraw(String traderId, double amount) {
            return cashBalances.computeIfPresent(traderId, (k, v) -> v >= amount ? v - amount : v) != null
                && cashBalances.get(traderId) >= 0;
        }

        public void addHolding(String traderId, String symbol, int quantity) {
            holdings.computeIfAbsent(traderId, k -> new ConcurrentHashMap<>())
                .merge(symbol, quantity, Integer::sum);
        }

        public void removeHolding(String traderId, String symbol, int quantity) {
            holdings.computeIfPresent(traderId, (k, map) -> {
                map.computeIfPresent(symbol, (sym, qty) -> qty >= quantity ? qty - quantity : qty);
                return map;
            });
        }

        public double getCash(String traderId) {
            return cashBalances.getOrDefault(traderId, 0.0);
        }

        public Map<String, Integer> getHoldings(String traderId) {
            return new HashMap<>(holdings.getOrDefault(traderId, new ConcurrentHashMap<>()));
        }

        public PortfolioSummary getSummary(String traderId, Map<String, Double> marketPrices) {
            double cash = getCash(traderId);
            var h = getHoldings(traderId);
            double holdingsValue = h.entrySet().stream()
                .mapToDouble(e -> e.getValue() * marketPrices.getOrDefault(e.getKey(), 0.0))
                .sum();
            return new PortfolioSummary(traderId, cash, h, cash + holdingsValue);
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Trading System ===\n");

        MatchingEngine engine = new MatchingEngine();
        PortfolioTracker portfolio = new PortfolioTracker();

        // ─── Create Order Books ───
        System.out.println("--- Order Books ---");
        engine.createOrderBook("AAPL");
        engine.createOrderBook("GOOGL");
        System.out.println("  Created books for AAPL, GOOGL");

        // ─── Fund Traders ───
        System.out.println("\n--- Fund Traders ---");
        portfolio.deposit("TRADER-1", 100000);
        portfolio.deposit("TRADER-2", 50000);
        portfolio.deposit("TRADER-3", 200000);
        System.out.println("  TRADER-1: $100,000");
        System.out.println("  TRADER-2: $50,000");
        System.out.println("  TRADER-3: $200,000");

        // ─── Place Orders ───
        System.out.println("\n--- Placing Orders (AAPL) ---");
        var o1 = engine.placeOrder("AAPL", OrderSide.BUY, OrderType.LIMIT, 150.0, 100,
            TimeInForce.GTC, "TRADER-1");
        System.out.println("  " + o1.side() + " " + o1.quantity() + " AAPL @ $" + o1.price() + " [" + o1.status() + "]");

        var o2 = engine.placeOrder("AAPL", OrderSide.BUY, OrderType.LIMIT, 151.0, 200,
            TimeInForce.GTC, "TRADER-2");
        System.out.println("  " + o2.side() + " " + o2.quantity() + " AAPL @ $" + o2.price() + " [" + o2.status() + "]");

        var o3 = engine.placeOrder("AAPL", OrderSide.SELL, OrderType.LIMIT, 152.0, 150,
            TimeInForce.GTC, "TRADER-3");
        System.out.println("  " + o3.side() + " " + o3.quantity() + " AAPL @ $" + o3.price() + " [" + o3.status() + "]");

        // ─── Market Data ───
        System.out.println("\n--- Market Data (AAPL) ---");
        var md = engine.getMarketData("AAPL");
        System.out.printf("  Bid: $%.2f | Ask: $%.2f | Last: $%.2f | Vol: %d%n",
            md.bid(), md.ask(), md.last(), md.volume());

        // ─── Match with better buy price ───
        System.out.println("\n--- Matching: Better Buy Order ---");
        var o4 = engine.placeOrder("AAPL", OrderSide.BUY, OrderType.LIMIT, 153.0, 100,
            TimeInForce.GTC, "TRADER-1");
        System.out.println("  New buy @ $153 for 100 shares");

        // Check trades
        var trades = engine.getAllTrades("AAPL");
        System.out.println("\n  Trades executed: " + trades.size());
        for (var t : trades) {
            System.out.printf("    %s: %d AAPL @ $%.2f%n", t.tradeId(), t.quantity(), t.price());
        }

        // Update order status
        var updatedO3 = engine.getOrder("AAPL", o3.orderId()).orElseThrow();
        System.out.println("  Sell order status: " + updatedO3.status() + " (filled " + updatedO3.filledQuantity() + "/" + updatedO3.quantity() + ")");

        // ─── Market Order ───
        System.out.println("\n--- Market Order ---");
        var marketBuy = engine.placeMarketOrder("AAPL", OrderSide.BUY, 50, "TRADER-2");
        System.out.println("  Market buy: " + marketBuy.quantity() + " AAPL filled=" + marketBuy.filledQuantity() + " status=" + marketBuy.status());

        // ─── Cancel Order ───
        System.out.println("\n--- Cancel Order ---");
        boolean cancelled = engine.cancelOrder("AAPL", o1.orderId());
        System.out.println("  Cancelled order " + o1.orderId() + ": " + cancelled);

        // ─── Order Book State ───
        System.out.println("\n--- Order Book (AAPL) ---");
        System.out.println("  Buy orders (" + engine.getBuyOrders("AAPL").size() + "):");
        for (var o : engine.getBuyOrders("AAPL")) {
            System.out.printf("    %s: %d @ $%.2f (%s)%n", o.orderId(), o.remainingQuantity(), o.price(), o.status());
        }
        System.out.println("  Sell orders (" + engine.getSellOrders("AAPL").size() + "):");
        for (var o : engine.getSellOrders("AAPL")) {
            System.out.printf("    %s: %d @ $%.2f (%s)%n", o.orderId(), o.remainingQuantity(), o.price(), o.status());
        }

        // ─── GOOGL Trading ───
        System.out.println("\n--- GOOGL Trading ---");
        engine.placeOrder("GOOGL", OrderSide.SELL, OrderType.LIMIT, 2800.0, 10,
            TimeInForce.GTC, "TRADER-3");
        engine.placeOrder("GOOGL", OrderSide.BUY, OrderType.LIMIT, 2795.0, 5,
            TimeInForce.GTC, "TRADER-1");
        engine.placeOrder("GOOGL", OrderSide.BUY, OrderType.LIMIT, 2805.0, 8,
            TimeInForce.GTC, "TRADER-2");

        var googlTrades = engine.getAllTrades("GOOGL");
        System.out.println("  GOOGL trades: " + googlTrades.size());
        var googlMd = engine.getMarketData("GOOGL");
        System.out.printf("  Bid: $%.2f | Ask: $%.2f%n", googlMd.bid(), googlMd.ask());

        // ─── Virtual Thread Concurrent Trading ───
        System.out.println("\n--- Concurrent Trading (Virtual Threads) ---");
        var concurrentEngine = new MatchingEngine();
        concurrentEngine.createOrderBook("BTC");
        var tradeCount = new AtomicInteger(0);

        var traders = new String[]{"ALICE", "BOB", "CHARLIE", "DAVE", "EVE"};
        for (var t : traders) portfolio.deposit(t, 100000);

        var threads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            int idx = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                var side = idx % 2 == 0 ? OrderSide.BUY : OrderSide.SELL;
                double price = 50000 + idx * 10;
                int qty = ThreadLocalRandom.current().nextInt(1, 10);
                var order = concurrentEngine.placeOrder("BTC", side, OrderType.LIMIT, price, qty,
                    TimeInForce.GTC, traders[idx % traders.length]);
                if (order.isFilled()) tradeCount.incrementAndGet();
            });
        }
        for (var t : threads) t.join();

        System.out.println("  BTC trades executed: " + concurrentEngine.getAllTrades("BTC").size());
        System.out.println("  Filled orders: " + tradeCount.get());

        var btcMd = concurrentEngine.getMarketData("BTC");
        System.out.printf("  BTC Bid: $%.2f | Ask: $%.2f | Last: $%.2f | Vol: %d%n",
            btcMd.bid(), btcMd.ask(), btcMd.last(), btcMd.volume());

        System.out.println("\n=== Trading System Complete ===");
    }
}
