package dev.dead.DSLs;

// =========================================================================
// THE METHOD CHAINING BUILDERS
// =========================================================================

public class MethodChainingDSL {

    // Test Drive Execution (psvm)
    public static void main(String[] args) {

        // Construct the nested domain objects cleanly via Method Chaining DSL
        Order order = MethodChainingOrderBuilder.forCustomer("BigBank")
                .buy(80)
                .stock("IBM")
                .on("NYSE")
                .at(125.00)
                .sell(50)
                .stock("GOOGLE")
                .on("NASDAQ")
                .at(375.00)
                .end();

        // Verify the results
        printOrderDeatails(order);
        Order order1 = MethodChainingOrderBuilder.forCustomer("SmallBank")
                .buy(20)
                .stock("Joe")
                .on("NASDAQ")
                .at(120.33)
                .end();

        Order order2 = MethodChainingOrderBuilder.forCustomer("MeBank")
                .buy(200)
                .stock("Joe")
                .on("Nas")
                .at(120.33)
                .end();
        printOrderDeatails(order2);

    }

    private static void printOrderDeatails(Order order) {
        System.out.println("--- DSL Execution Successful ---");
        System.out.println("Customer: " + order.getCustomer());
        System.out.println("Total Trades Processed: " + order.getTrades().size());
        System.out.println("Total Order Value: $" + order.getValue());
        System.out.println("-".repeat(15));
    }
}

// Top-level builder managing Order scope
class MethodChainingOrderBuilder {
    public final Order order = new Order();

    private MethodChainingOrderBuilder(String customer) {
        order.setCustomer(customer);
    }

    public static MethodChainingOrderBuilder forCustomer(String customer) {
        return new MethodChainingOrderBuilder(customer);
    }

    public TradeBuilder buy(int quantity) {
        return new TradeBuilder(this, Trade.Type.BUY, quantity);
    }

    public TradeBuilder sell(int quantity) {
        return new TradeBuilder(this, Trade.Type.SELL, quantity);
    }

    public MethodChainingOrderBuilder addTrade(Trade trade) {
        order.addTrade(trade);
        return this;
    }

    public Order end() {
        return order;
    }
}

// Intermediate builder setting Trade attributes
class TradeBuilder {
    public final Trade trade = new Trade();
    private final MethodChainingOrderBuilder builder;

    TradeBuilder(MethodChainingOrderBuilder builder, Trade.Type type, int quantity) {
        this.builder = builder;
        trade.setType(type);
        trade.setQuantity(quantity);
    }

    public StockBuilder stock(String symbol) {
        return new StockBuilder(builder, trade, symbol);
    }
}

// Intermediate builder setting Stock details
class StockBuilder {
    private final MethodChainingOrderBuilder builder;
    private final Trade trade;
    private final Stock stock = new Stock();

    StockBuilder(MethodChainingOrderBuilder builder, Trade trade, String symbol) {
        this.builder = builder;
        this.trade = trade;
        stock.setSymbol(symbol);
    }

    public TradeBuilderWithStock on(String market) {
        stock.setMarket(market);
        trade.setStock(stock);
        return new TradeBuilderWithStock(builder, trade);
    }
}

// Final builder step forcing price specification before looping back
class TradeBuilderWithStock {
    private final MethodChainingOrderBuilder builder;
    private final Trade trade;

    TradeBuilderWithStock(MethodChainingOrderBuilder builder, Trade trade) {
        this.builder = builder;
        this.trade = trade;
    }

    public MethodChainingOrderBuilder at(double price) {
        trade.setPrice(price);
        return builder.addTrade(trade);
    }
}