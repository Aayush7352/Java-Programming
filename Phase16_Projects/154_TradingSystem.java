package phase16.projects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class TradingSystem {

    public static enum OrderSide { BUY, SELL }
    public static enum OrderType { MARKET, LIMIT, STOP, STOP_LIMIT }
    public static enum OrderStatus { PENDING, PARTIAL, FILLED, CANCELLED, EXPIRED }

    public static sealed interface Order permits LimitOrder, MarketOrder, StopOrder, StopLimitOrder {
        String orderId();
        String symbol();
        OrderSide side();
        OrderType type();
        BigDecimal price();
        int quantity();
        int filledQuantity();
        OrderStatus status();
        Instant createdAt();
        Order withFilled(int additionalFill);
        Order withStatus(OrderStatus newStatus);
    }

    public static record LimitOrder(String orderId, String symbol, OrderSide side,
                                     BigDecimal price, int quantity, int filledQuantity,
                                     OrderStatus status, Instant createdAt)
            implements Order, Comparable<LimitOrder> {

        public LimitOrder {
            Objects.requireNonNull(orderId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(side);
            Objects.requireNonNull(price);
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        public LimitOrder(String orderId, String symbol, OrderSide side, BigDecimal price, int quantity) {
            this(orderId, symbol, side, price, quantity, 0, OrderStatus.PENDING, Instant.now());
        }

        @Override
        public Order withFilled(int additionalFill) {
            var newFilled = filledQuantity + additionalFill;
            var newStatus = newFilled >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            return new LimitOrder(orderId, symbol, side, price, quantity, newFilled, newStatus, createdAt);
        }

        @Override
        public Order withStatus(OrderStatus newStatus) {
            return new LimitOrder(orderId, symbol, side, price, quantity, filledQuantity, newStatus, createdAt);
        }

        @Override public OrderType type() { return OrderType.LIMIT; }

        public int remainingQuantity() { return quantity - filledQuantity; }

        @Override
        public int compareTo(LimitOrder other) {
            if (this.side != other.side) return 0;
            int priceCmp;
            if (this.side == OrderSide.BUY) {
                priceCmp = other.price.compareTo(this.price);
            } else {
                priceCmp = this.price.compareTo(other.price);
            }
            if (priceCmp != 0) return priceCmp;
            return this.createdAt.compareTo(other.createdAt);
        }

        @Override
        public String toString() {
            return "Limit[%s %s %s @ $%s qty=%d filled=%d %s]".formatted(
                    orderId, side, symbol, price, quantity, filledQuantity, status);
        }
    }

    public static record MarketOrder(String orderId, String symbol, OrderSide side,
                                      int quantity, int filledQuantity,
                                      OrderStatus status, Instant createdAt)
            implements Order {

        public MarketOrder {
            Objects.requireNonNull(orderId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(side);
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        public MarketOrder(String orderId, String symbol, OrderSide side, int quantity) {
            this(orderId, symbol, side, quantity, 0, OrderStatus.PENDING, Instant.now());
        }

        @Override public BigDecimal price() { return BigDecimal.ZERO; }
        @Override public OrderType type() { return OrderType.MARKET; }

        @Override
        public Order withFilled(int additionalFill) {
            var newFilled = filledQuantity + additionalFill;
            var newStatus = newFilled >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            return new MarketOrder(orderId, symbol, side, quantity, newFilled, newStatus, createdAt);
        }

        @Override
        public Order withStatus(OrderStatus newStatus) {
            return new MarketOrder(orderId, symbol, side, quantity, filledQuantity, newStatus, createdAt);
        }

        public int remainingQuantity() { return quantity - filledQuantity; }

        @Override public String toString() {
            return "Market[%s %s %s qty=%d filled=%d %s]".formatted(
                    orderId, side, symbol, quantity, filledQuantity, status);
        }
    }

    public static record StopOrder(String orderId, String symbol, OrderSide side,
                                    BigDecimal stopPrice, int quantity, int filledQuantity,
                                    OrderStatus status, Instant createdAt)
            implements Order {

        public StopOrder {
            Objects.requireNonNull(orderId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(side);
            Objects.requireNonNull(stopPrice);
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        public StopOrder(String orderId, String symbol, OrderSide side, BigDecimal stopPrice, int quantity) {
            this(orderId, symbol, side, stopPrice, quantity, 0, OrderStatus.PENDING, Instant.now());
        }

        @Override public BigDecimal price() { return stopPrice; }
        @Override public OrderType type() { return OrderType.STOP; }

        @Override
        public Order withFilled(int additionalFill) {
            var newFilled = filledQuantity + additionalFill;
            var newStatus = newFilled >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            return new StopOrder(orderId, symbol, side, stopPrice, quantity, newFilled, newStatus, createdAt);
        }

        @Override
        public Order withStatus(OrderStatus newStatus) {
            return new StopOrder(orderId, symbol, side, stopPrice, quantity, filledQuantity, newStatus, createdAt);
        }

        public int remainingQuantity() { return quantity - filledQuantity; }

        @Override public String toString() {
            return "Stop[%s %s %s @ $%s qty=%d %s]".formatted(
                    orderId, side, symbol, stopPrice, quantity, status);
        }
    }

    public static record StopLimitOrder(String orderId, String symbol, OrderSide side,
                                         BigDecimal stopPrice, BigDecimal limitPrice,
                                         int quantity, int filledQuantity,
                                         OrderStatus status, Instant createdAt)
            implements Order {

        public StopLimitOrder {
            Objects.requireNonNull(orderId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(side);
            Objects.requireNonNull(stopPrice);
            Objects.requireNonNull(limitPrice);
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        @Override public BigDecimal price() { return limitPrice; }
        @Override public OrderType type() { return OrderType.STOP_LIMIT; }

        @Override
        public Order withFilled(int additionalFill) {
            var newFilled = filledQuantity + additionalFill;
            var newStatus = newFilled >= quantity ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            return new StopLimitOrder(orderId, symbol, side, stopPrice, limitPrice,
                    quantity, newFilled, newStatus, createdAt);
        }

        @Override
        public Order withStatus(OrderStatus newStatus) {
            return new StopLimitOrder(orderId, symbol, side, stopPrice, limitPrice,
                    quantity, filledQuantity, newStatus, createdAt);
        }

        public int remainingQuantity() { return quantity - filledQuantity; }

        @Override public String toString() {
            return "StopLimit[%s %s %s stop=$%s limit=$%s qty=%d %s]".formatted(
                    orderId, side, symbol, stopPrice, limitPrice, quantity, status);
        }
    }

    public static record Trade(String tradeId, String symbol, BigDecimal price, int quantity,
                                String buyOrderId, String sellOrderId, Instant timestamp) {
        public Trade {
            Objects.requireNonNull(tradeId);
            Objects.requireNonNull(symbol);
            Objects.requireNonNull(price);
            Objects.requireNonNull(buyOrderId);
            Objects.requireNonNull(sellOrderId);
            Objects.requireNonNull(timestamp);
        }

        public BigDecimal notional() {
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public static final class OrderBook {
        private final String symbol;
        private final PriorityQueue<LimitOrder> buyOrders = new PriorityQueue<>();
        private final PriorityQueue<LimitOrder> sellOrders = new PriorityQueue<>();
        private final List<Order> allOrders = new CopyOnWriteArrayList<>();
        private final List<Trade> trades = new CopyOnWriteArrayList<>();
        private final Lock lock = new ReentrantLock();
        private final AtomicLong orderCounter = new AtomicLong(0);
        private final AtomicLong tradeCounter = new AtomicLong(0);
        private volatile BigDecimal lastPrice = BigDecimal.ZERO;
        private volatile BigDecimal lastChange = BigDecimal.ZERO;

        public OrderBook(String symbol) {
            this.symbol = Objects.requireNonNull(symbol);
        }

        public Order placeOrder(Order order) {
            lock.lock();
            try {
                var processed = switch (order) {
                    case LimitOrder lo -> processLimitOrder(lo);
                    case MarketOrder mo -> processMarketOrder(mo);
                    case StopOrder so -> {
                        allOrders.add(so);
                        yield so;
                    }
                    case StopLimitOrder slo -> {
                        allOrders.add(slo);
                        yield slo;
                    }
                };
                return processed;
            } finally {
                lock.unlock();
            }
        }

        private Order processLimitOrder(LimitOrder order) {
            var remaining = order.remainingQuantity();
            Order currentOrder = order;

            if (order.side() == OrderSide.BUY) {
                while (remaining > 0 && !sellOrders.isEmpty()) {
                    var bestSell = sellOrders.peek();
                    if (bestSell.price().compareTo(order.price()) > 0) break;
                    sellOrders.poll();
                    var fillQty = Math.min(remaining, bestSell.remainingQuantity());
                    executeTrade(order.orderId(), bestSell.orderId(), bestSell.price(), fillQty);
                    currentOrder = currentOrder.withFilled(fillQty);
                    remaining -= fillQty;
                    if (bestSell.remainingQuantity() > fillQty) {
                        sellOrders.offer((LimitOrder) bestSell.withFilled(fillQty));
                    }
                }
                if (remaining > 0) {
                    var updated = (LimitOrder) currentOrder;
                    buyOrders.offer(new LimitOrder(updated.orderId(), updated.symbol(), updated.side(),
                            updated.price(), updated.quantity(), updated.filledQuantity(),
                            OrderStatus.PENDING, updated.createdAt()));
                }
            } else {
                while (remaining > 0 && !buyOrders.isEmpty()) {
                    var bestBuy = buyOrders.peek();
                    if (bestBuy.price().compareTo(order.price()) < 0) break;
                    buyOrders.poll();
                    var fillQty = Math.min(remaining, bestBuy.remainingQuantity());
                    executeTrade(bestBuy.orderId(), order.orderId(), bestBuy.price(), fillQty);
                    currentOrder = currentOrder.withFilled(fillQty);
                    remaining -= fillQty;
                    if (bestBuy.remainingQuantity() > fillQty) {
                        buyOrders.offer((LimitOrder) bestBuy.withFilled(fillQty));
                    }
                }
                if (remaining > 0) {
                    var updated = (LimitOrder) currentOrder;
                    sellOrders.offer(new LimitOrder(updated.orderId(), updated.symbol(), updated.side(),
                            updated.price(), updated.quantity(), updated.filledQuantity(),
                            OrderStatus.PENDING, updated.createdAt()));
                }
            }

            allOrders.add(currentOrder);
            return currentOrder;
        }

        private Order processMarketOrder(MarketOrder order) {
            var remaining = order.remainingQuantity();
            Order currentOrder = order;

            if (order.side() == OrderSide.BUY) {
                while (remaining > 0 && !sellOrders.isEmpty()) {
                    var bestSell = sellOrders.poll();
                    var fillQty = Math.min(remaining, bestSell.remainingQuantity());
                    executeTrade(order.orderId(), bestSell.orderId(), bestSell.price(), fillQty);
                    currentOrder = currentOrder.withFilled(fillQty);
                    remaining -= fillQty;
                    if (bestSell.remainingQuantity() > fillQty) {
                        sellOrders.offer((LimitOrder) bestSell.withFilled(fillQty));
                    }
                }
            } else {
                while (remaining > 0 && !buyOrders.isEmpty()) {
                    var bestBuy = buyOrders.poll();
                    var fillQty = Math.min(remaining, bestBuy.remainingQuantity());
                    executeTrade(bestBuy.orderId(), order.orderId(), bestBuy.price(), fillQty);
                    currentOrder = currentOrder.withFilled(fillQty);
                    remaining -= fillQty;
                    if (bestBuy.remainingQuantity() > fillQty) {
                        buyOrders.offer((LimitOrder) bestBuy.withFilled(fillQty));
                    }
                }
            }

            if (remaining > 0) {
                currentOrder = currentOrder.withFilled(0);
            }
            allOrders.add(currentOrder);
            return currentOrder;
        }

        private void executeTrade(String buyOrderId, String sellOrderId, BigDecimal price, int quantity) {
            var trade = new Trade(
                    "TRADE-" + tradeCounter.incrementAndGet(),
                    symbol, price, quantity, buyOrderId, sellOrderId, Instant.now());
            trades.add(trade);
            lastChange = price.subtract(lastPrice);
            lastPrice = price;
        }

        public void checkStopOrders(BigDecimal currentPrice) {
            lock.lock();
            try {
                var toProcess = new ArrayList<Order>();
                var iterator = allOrders.iterator();
                while (iterator.hasNext()) {
                    var order = iterator.next();
                    if (order.status() != OrderStatus.PENDING) continue;
                    switch (order) {
                        case StopOrder so when shouldTriggerStop(so, currentPrice) -> {
                            toProcess.add(so);
                            var marketOrder = new MarketOrder(
                                    so.orderId() + "-MKT", so.symbol(), so.side(),
                                    so.remainingQuantity());
                            toProcess.add(processMarketOrder(marketOrder));
                        }
                        case StopLimitOrder slo when shouldTriggerStop(slo, currentPrice) -> {
                            toProcess.add(slo);
                            var limitOrder = new LimitOrder(
                                    slo.orderId() + "-LMT", slo.symbol(), slo.side(),
                                    slo.limitPrice(), slo.remainingQuantity());
                            toProcess.add(processLimitOrder(limitOrder));
                        }
                        default -> {}
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        private boolean shouldTriggerStop(Order order, BigDecimal currentPrice) {
            return switch (order) {
                case StopOrder so ->
                    (so.side() == OrderSide.SELL && currentPrice.compareTo(so.stopPrice()) <= 0) ||
                            (so.side() == OrderSide.BUY && currentPrice.compareTo(so.stopPrice()) >= 0);
                case StopLimitOrder slo ->
                    (slo.side() == OrderSide.SELL && currentPrice.compareTo(slo.stopPrice()) <= 0) ||
                            (slo.side() == OrderSide.BUY && currentPrice.compareTo(slo.stopPrice()) >= 0);
                default -> false;
            };
        }

        public boolean cancelOrder(String orderId) {
            lock.lock();
            try {
                return allOrders.stream()
                        .filter(o -> o.orderId().equals(orderId) && o.status() == OrderStatus.PENDING)
                        .findFirst()
                        .map(o -> {
                            allOrders.remove(o);
                            allOrders.add(o.withStatus(OrderStatus.CANCELLED));
                            return true;
                        })
                        .orElse(false);
            } finally {
                lock.unlock();
            }
        }

        public OrderBookSnapshot getSnapshot() {
            lock.lock();
            try {
                var topBid = buyOrders.isEmpty() ? null : buyOrders.peek();
                var topAsk = sellOrders.isEmpty() ? null : sellOrders.peek();
                return new OrderBookSnapshot(symbol, lastPrice, topBid, topAsk,
                        List.copyOf(buyOrders), List.copyOf(sellOrders),
                        List.copyOf(trades), List.copyOf(allOrders));
            } finally {
                lock.unlock();
            }
        }

        public String getSymbol() { return symbol; }
        public int tradeCount() { return trades.size(); }
        public int pendingBuyOrders() { return buyOrders.size(); }
        public int pendingSellOrders() { return sellOrders.size(); }
        public BigDecimal getLastPrice() { return lastPrice; }
    }

    public static record OrderBookSnapshot(String symbol, BigDecimal lastPrice,
                                            LimitOrder topBid, LimitOrder topAsk,
                                            List<LimitOrder> bids, List<LimitOrder> asks,
                                            List<Trade> trades, List<Order> allOrders) {
    }

    public static final class MatchingEngine {
        private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

        public OrderBook createOrderBook(String symbol) {
            var ob = new OrderBook(symbol);
            orderBooks.put(symbol, ob);
            return ob;
        }

        public OrderBook getOrderBook(String symbol) {
            var ob = orderBooks.get(symbol);
            if (ob == null) throw new IllegalArgumentException("Unknown symbol: " + symbol);
            return ob;
        }

        public Order placeOrder(String symbol, Order order) {
            return getOrderBook(symbol).placeOrder(order);
        }

        public List<Trade> getRecentTrades(String symbol, int count) {
            var ob = getOrderBook(symbol);
            var snap = ob.getSnapshot();
            var trades = snap.trades();
            return trades.subList(Math.max(0, trades.size() - count), trades.size());
        }

        public Map<String, Object> getMarketSummary() {
            var summary = new HashMap<String, Object>();
            for (var entry : orderBooks.entrySet()) {
                var snap = entry.getValue().getSnapshot();
                summary.put(entry.getKey(), Map.of(
                        "last", snap.lastPrice(),
                        "bid", snap.topBid() != null ? snap.topBid().price() : "N/A",
                        "ask", snap.topAsk() != null ? snap.topAsk().price() : "N/A",
                        "trades", snap.trades().size(),
                        "bids", snap.bids().size(),
                        "asks", snap.asks().size()
                ));
            }
            return summary;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Trading System ===%n".formatted());

        var engine = new MatchingEngine();
        var aapl = engine.createOrderBook("AAPL");
        var googl = engine.createOrderBook("GOOGL");

        System.out.println("--- Placing Limit Orders ---");
        var buy1 = aapl.placeOrder(new LimitOrder("O-001", "AAPL", OrderSide.BUY, new BigDecimal("150.00"), 100));
        System.out.println("  " + buy1);
        var buy2 = aapl.placeOrder(new LimitOrder("O-002", "AAPL", OrderSide.BUY, new BigDecimal("149.50"), 200));
        System.out.println("  " + buy2);
        var buy3 = aapl.placeOrder(new LimitOrder("O-003", "AAPL", OrderSide.BUY, new BigDecimal("151.00"), 50));
        System.out.println("  " + buy3);

        var sell1 = aapl.placeOrder(new LimitOrder("O-004", "AAPL", OrderSide.SELL, new BigDecimal("151.00"), 75));
        System.out.println("  " + sell1);
        var sell2 = aapl.placeOrder(new LimitOrder("O-005", "AAPL", OrderSide.SELL, new BigDecimal("152.00"), 150));
        System.out.println("  " + sell2);

        var snap = aapl.getSnapshot();
        System.out.println("%n  Order Book: AAPL".formatted());
        System.out.println("  Best Bid: " + (snap.topBid() != null ? "$" + snap.topBid().price() + " (" + snap.topBid().remainingQuantity() + ")" : "none"));
        System.out.println("  Best Ask: " + (snap.topAsk() != null ? "$" + snap.topAsk().price() + " (" + snap.topAsk().remainingQuantity() + ")" : "none"));

        System.out.println("%n--- Market Order (Buy 100) ---%n".formatted());
        var mktBuy = aapl.placeOrder(new MarketOrder("O-006", "AAPL", OrderSide.BUY, 100));
        System.out.println("  " + mktBuy);

        System.out.println("%n--- Trades Executed ---%n".formatted());
        var trades = engine.getRecentTrades("AAPL", 5);
        for (var trade : trades) {
            System.out.println("  %s: %d @ $%s (notional: $%s)".formatted(
                    trade.tradeId(), trade.quantity(), trade.price(),
                    NumberFormat.getNumberInstance(Locale.US).format(trade.notional())));
        }

        System.out.println("%n--- Stop Order Example ---%n".formatted());
        var stopSell = aapl.placeOrder(new StopOrder("O-007", "AAPL", OrderSide.SELL,
                new BigDecimal("148.00"), 50));
        System.out.println("  Placed stop sell at $148.00: " + stopSell);
        aapl.checkStopOrders(new BigDecimal("147.50"));
        System.out.println("  After price drops to $147.50, stop order triggered");
        var afterStop = aapl.getSnapshot();
        System.out.println("  Trades: " + afterStop.trades().size());
        System.out.println("  Last price: $" + aapl.getLastPrice());

        System.out.println("%n--- Cancel Order ---%n".formatted());
        var cancelled = aapl.cancelOrder("O-002");
        System.out.println("  Cancelled O-002: " + cancelled);

        System.out.println("%n--- GOOGL Orders ---%n".formatted());
        googl.placeOrder(new LimitOrder("G-001", "GOOGL", OrderSide.BUY, new BigDecimal("140.00"), 100));
        googl.placeOrder(new LimitOrder("G-002", "GOOGL", OrderSide.SELL, new BigDecimal("141.00"), 50));
        googl.placeOrder(new MarketOrder("G-003", "GOOGL", OrderSide.BUY, 50));
        var googlTrades = engine.getRecentTrades("GOOGL", 5);
        System.out.println("  GOOGL trades: " + googlTrades.size());

        System.out.println("%n--- Market Summary ---%n".formatted());
        var summary = engine.getMarketSummary();
        summary.forEach((symbol, data) ->
            System.out.println("  %s: %s".formatted(symbol, data)));

        System.out.println("%n--- Pattern Matching on Orders ---%n".formatted());
        for (var order : aapl.getSnapshot().allOrders().stream().limit(8).toList()) {
            switch (order) {
                case LimitOrder lo when lo.status() == OrderStatus.FILLED ->
                    System.out.println("  Filled Limit: %s %d @ $%s".formatted(lo.side(), lo.quantity(), lo.price()));
                case LimitOrder lo ->
                    System.out.println("  Open Limit: %s %d @ $%s (%d remaining)".formatted(lo.side(), lo.quantity(), lo.price(), lo.remainingQuantity()));
                case MarketOrder mo when mo.status() == OrderStatus.FILLED ->
                    System.out.println("  Filled Market: %s %d".formatted(mo.side(), mo.quantity()));
                case StopOrder so ->
                    System.out.println("  Stop: %s %d @ $%s".formatted(so.side(), so.quantity(), so.stopPrice()));
                case StopLimitOrder slo ->
                    System.out.println("  StopLimit: %s %d stop=$%s limit=$%s".formatted(slo.side(), slo.quantity(), slo.stopPrice(), slo.limitPrice()));
                default ->
                    System.out.println("  Other: %s %s".formatted(order.type(), order.orderId()));
            }
        }

        System.out.println("%n--- Virtual Threads: Concurrent Order Placement ---%n".formatted());
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var side = idx % 2 == 0 ? OrderSide.BUY : OrderSide.SELL;
                    var price = new BigDecimal("150.00").add(BigDecimal.valueOf(Math.random() * 10 - 5));
                    var order = aapl.placeOrder(new LimitOrder(
                            "VT-" + idx, "AAPL", side,
                            price.setScale(2, RoundingMode.HALF_UP), 10));
                    System.out.println("  [VT-%d] %s".formatted(idx, order));
                });
            }
        }

        System.out.println("%nFinal Stats: %d total trades on AAPL, %d pending bids, %d pending asks"
                .formatted(aapl.tradeCount(), aapl.pendingBuyOrders(), aapl.pendingSellOrders()));
        System.out.println("=== Done ===");
    }
}
