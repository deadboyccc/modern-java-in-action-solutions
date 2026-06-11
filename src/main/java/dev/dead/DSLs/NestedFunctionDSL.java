package dev.dead.DSLs;

import java.util.stream.Stream;

// =========================================================================
// MAIN RUNNABLE FILE & TEST DRIVE
// =========================================================================
public class NestedFunctionDSL {

    public static void main(String[] args) {
        // Using static methods to recreate Listing 10.7
        // (Simulating a clean environment where NestedFunctionOrderBuilder's methods are statically imported)
        Order order = NestedFunctionOrderBuilder.order("BigBank",
                NestedFunctionOrderBuilder.buy(80,
                        NestedFunctionOrderBuilder.stock("IBM", NestedFunctionOrderBuilder.on("NYSE")),
                        NestedFunctionOrderBuilder.at(125.00)),
                NestedFunctionOrderBuilder.sell(50,
                        NestedFunctionOrderBuilder.stock("GOOGLE", NestedFunctionOrderBuilder.on("NASDAQ")),
                        NestedFunctionOrderBuilder.at(375.00))
        );

        // Verification Output
        System.out.println("--- Nested Function DSL Executed Successfully ---");
        System.out.println("Customer: " + order.getCustomer());
        System.out.println("Total Trades: " + order.getTrades().size());
        System.out.println("Total Order Value: $" + order.getValue());
    }
}

// =========================================================================
// NESTED FUNCTION DSL BUILDER IMPLEMENTATION
// =========================================================================
class NestedFunctionOrderBuilder {

    // Creates an order for a given customer and populates its trades array
    public static Order order(String customer, Trade... trades) {
        Order order = new Order();
        order.setCustomer(customer);
        Stream.of(trades).forEach(order::addTrade);
        return order;
    }

    // Creates a trade to buy a stock
    public static Trade buy(int quantity, Stock stock, double price) {
        return buildTrade(quantity, stock, price, Trade.Type.BUY);
    }

    // Creates a trade to sell a stock
    public static Trade sell(int quantity, Stock stock, double price) {
        return buildTrade(quantity, stock, price, Trade.Type.SELL);
    }

    // Helper method to structurally unify trade creation
    private static Trade buildTrade(int quantity, Stock stock, double price, Trade.Type type) {
        Trade trade = new Trade();
        trade.setQuantity(quantity);
        trade.setType(type);
        trade.setStock(stock);
        trade.setPrice(price);
        return trade;
    }

    // Dummy method to visually define the unit price of the traded stock
    public static double at(double price) {
        return price;
    }

    // Instantiates the traded Stock domain bean
    public static Stock stock(String symbol, String market) {
        Stock stock = new Stock();
        stock.setSymbol(symbol);
        stock.setMarket(market);
        return stock;
    }

    // Dummy method to visually define the market where the stock is traded
    public static String on(String market) {
        return market;
    }
}