package dev.dead.DSLs;

import java.util.ArrayList;
import java.util.List;

// 1. Represents a stock quoted on a given market
public class Stock {
    private String symbol;
    private String market;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }
}

// 2. Represents a trade to buy or sell a given quantity of a stock at a given price
class Trade {
    private Type type;
    private Stock stock;
    private int quantity;
    private double price;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getValue() {
        return quantity * price;
    }

    public enum Type {BUY, SELL}
}

// 3. Represents an order placed by a customer containing one or more trades
class Order {
    private final List<Trade> trades = new ArrayList<>();
    private String customer;

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
    }

    public double getValue() {
        return trades.stream()
                .mapToDouble(Trade::getValue)
                .sum();
    }
}