package dev.dead.DSLs;

import java.util.function.Consumer;

// =========================================================================
// MAIN RUNNABLE FILE & TEST DRIVE
// =========================================================================
public class LambdaSequenceDSL {

    public static void main(String[] args) {

        // Recreating Listing 10.9 via deep lambda functional sequencing blocks
        Order order = LambdaOrderBuilder.order(o -> {
            o.forCustomer("BigBank");

            o.buy(t -> {
                t.quantity(80);
                t.price(125.00);
                t.stock(s -> {
                    s.symbol("IBM");
                    s.market("NYSE");
                });
            });

            o.sell(t -> {
                t.quantity(50);
                t.price(375.00);
                t.stock(s -> {
                    s.symbol("GOOGLE");
                    s.market("NASDAQ");
                });
            });
        });

        // Verification Output
        System.out.println("--- Lambda Sequence DSL Executed Successfully ---");
        System.out.println("Customer: " + order.getCustomer());
        System.out.println("Total Trades: " + order.getTrades().size());
        System.out.println("Total Order Value: $" + order.getValue());
    }
}

// =========================================================================
// FUNCTION SEQUENCING LAMBDA BUILDERS
// =========================================================================

// Top-level builder creating and configuring the core Order
class LambdaOrderBuilder {
    private final Order order = new Order();

    public static Order order(Consumer<LambdaOrderBuilder> consumer) {
        LambdaOrderBuilder builder = new LambdaOrderBuilder();
        consumer.accept(builder);
        return builder.order;
    }

    public void forCustomer(String customer) {
        order.setCustomer(customer);
    }

    public void buy(Consumer<LambdaTradeBuilder> consumer) {
        trade(consumer, Trade.Type.BUY);
    }

    public void sell(Consumer<LambdaTradeBuilder> consumer) {
        trade(consumer, Trade.Type.SELL);
    }

    private void trade(Consumer<LambdaTradeBuilder> consumer, Trade.Type type) {
        LambdaTradeBuilder builder = new LambdaTradeBuilder();
        builder.setTradeType(type);
        consumer.accept(builder);
        order.addTrade(builder.getTrade());
    }
}

// Mid-level builder configuring properties inside a Trade sequence block
class LambdaTradeBuilder {
    private final Trade trade = new Trade();

    void setTradeType(Trade.Type type) {
        trade.setType(type);
    }

    public void quantity(int quantity) {
        trade.setQuantity(quantity);
    }

    public void price(double price) {
        trade.setPrice(price);
    }

    public void stock(Consumer<LambdaStockBuilder> consumer) {
        LambdaStockBuilder builder = new LambdaStockBuilder();
        consumer.accept(builder);
        trade.setStock(builder.getStock());
    }

    Trade getTrade() {
        return trade;
    }
}

// Base builder managing Stock parameters inside the innermost lambda block
class LambdaStockBuilder {
    private final Stock stock = new Stock();

    public void symbol(String symbol) {
        stock.setSymbol(symbol);
    }

    public void market(String market) {
        stock.setMarket(market);
    }

    Stock getStock() {
        return stock;
    }
}