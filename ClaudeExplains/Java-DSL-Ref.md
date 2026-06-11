# Domain-Specific Languages in Java — Complete Reference Guide

> **Goal:** Understand the three core DSL patterns in Java, when to use each, how lambdas and method references transform API design, and how major frameworks (jOOQ, Cucumber, Spring Integration) apply these patterns in production.

---

## Table of Contents

1. [What Is a DSL and Why Does It Matter](#1-what-is-a-dsl-and-why-does-it-matter)
2. [Internal vs. External DSLs](#2-internal-vs-external-dsls)
3. [Small DSLs Already in the Java API](#3-small-dsls-already-in-the-java-api)
4. [The Domain Model We'll Use](#4-the-domain-model-well-use)
5. [Pattern 1 — Method Chaining](#5-pattern-1--method-chaining)
6. [Pattern 2 — Nested Functions](#6-pattern-2--nested-functions)
7. [Pattern 3 — Function Sequencing with Lambdas](#7-pattern-3--function-sequencing-with-lambdas)
8. [Combining All Three Patterns](#8-combining-all-three-patterns)
9. [Method References in DSLs](#9-method-references-in-dsls)
10. [Pattern Comparison & Decision Guide](#10-pattern-comparison--decision-guide)
11. [Real-World DSLs in Java Libraries](#11-real-world-dsls-in-java-libraries)
12. [Quick Reference Cheat Sheet](#12-quick-reference-cheat-sheet)

---

## 1. What Is a DSL and Why Does It Matter

A **Domain-Specific Language (DSL)** is a small, focused API or language designed to express operations in one particular domain clearly and concisely. It bridges the gap between how developers write code and how domain experts think about problems.

**The core problem DSLs solve:**

```java
// Without a DSL — valid code, but only a developer can validate it
Order order = new Order();
order.setCustomer("BigBank");
Trade trade = new Trade();
trade.setType(Trade.Type.BUY);
Stock stock = new Stock();
stock.setSymbol("IBM");
stock.setMarket("NYSE");
trade.setStock(stock);
trade.setPrice(125.00);
trade.setQuantity(80);
order.addTrade(trade);

// With a DSL — a domain expert can read and validate this
Order order = forCustomer("BigBank")
    .buy(80).stock("IBM").on("NYSE").at(125.00)
    .end();
```

Both do the same thing. Only the second is auditable by a non-developer.

**DSL goals:**
- Readability for domain experts, not just developers
- Fewer opportunities to misuse the API (enforced sequencing)
- Self-documenting business logic
- Reduced boilerplate at the call site

---

## 2. Internal vs. External DSLs

| Type | Definition | Pros | Cons |
|---|---|---|---|
| **Internal** | Built in the same language as the app (Java) | No new tooling, IDE support, type safety | Constrained by Java syntax |
| **External** | A separate language (SQL, Gherkin/Cucumber) | Maximum flexibility, non-dev readable | Requires parsing, tooling, build integration |
| **Polyglot** | JVM language (Groovy, Kotlin, Scala) DSL embedded in Java app | More expressive syntax, concise | Complex build, interop friction |

**Java's position:** Java is verbose and rigid compared to Groovy or Scala for DSL writing. However, **lambdas and method references in Java 8+ significantly closed the gap**, making internal Java DSLs practical.

---

## 3. Small DSLs Already in the Java API

Before building your own, recognize that Java's standard library already uses DSL patterns extensively. These are worth studying because they show the patterns applied by API experts.

### `Comparator` — A Sorting DSL

Evolution from inner class → lambda → method reference → composed comparator:

```java
// Java 7: inner class — noisy, hard to read
Collections.sort(persons, new Comparator<Person>() {
    public int compare(Person p1, Person p2) {
        return p1.getAge() - p2.getAge();
    }
});

// Java 8: lambda — better
Collections.sort(persons, (p1, p2) -> p1.getAge() - p2.getAge());

// Java 8 + static import: reading like a sentence
Collections.sort(persons, comparing(Person::getAge));

// Composed: sort by age, then alphabetically within same age
persons.sort(comparing(Person::getAge)
             .thenComparing(Person::getName));

// Reversed
persons.sort(comparing(Person::getAge).reverse());
```

`comparing().thenComparing().reverse()` is a mini-DSL. Each call returns a `Comparator`, enabling fluent chaining.

### `Stream` API — A Collection Manipulation DSL

```java
// Imperative: scattered logic, hard to reason about
List<String> errors = new ArrayList<>();
int errorCount = 0;
String line = reader.readLine();
while (errorCount < 40 && line != null) {
    if (line.startsWith("ERROR")) {
        errors.add(line);
        errorCount++;
    }
    line = reader.readLine();
}

// Stream DSL: reads like a specification
List<String> errors = Files.lines(Paths.get(fileName))
    .filter(line -> line.startsWith("ERROR"))
    .limit(40)
    .collect(toList());
```

The Stream API demonstrates the **fluent pipeline style**: intermediate operations are lazy and return `Stream`, terminal operations are eager and trigger computation.

### `Collectors` — A Data Aggregation DSL

```java
// Nested grouping — Collectors are composed via nesting
Map<String, Map<Color, List<Car>>> carsByBrandAndColor =
    cars.stream().collect(
        groupingBy(Car::getBrand,
            groupingBy(Car::getColor))
    );

// Note: Collectors nest (innermost evaluates first)
// vs. Comparator which chains (left-to-right)
// This is a deliberate design choice — the nesting reflects
// the hierarchy of grouping levels
```

---

## 4. The Domain Model We'll Use

All three DSL patterns below are demonstrated on the same domain: a stock trading order system.

```java
public class Stock {
    private String symbol;  // e.g. "IBM"
    private String market;  // e.g. "NYSE"
    // getters/setters
}

public class Trade {
    public enum Type { BUY, SELL }
    private Type type;
    private Stock stock;
    private int quantity;
    private double price;

    public double getValue() { return quantity * price; }
    // getters/setters
}

public class Order {
    private String customer;
    private List<Trade> trades = new ArrayList<>();

    public void addTrade(Trade trade) { trades.add(trade); }
    public double getValue() {
        return trades.stream().mapToDouble(Trade::getValue).sum();
    }
    // getters/setters
}
```

**Target:** express this two-trade order cleanly:
- Customer: BigBank
- Buy 80 IBM @ NYSE @ $125
- Buy 50 GOOGLE @ NASDAQ @ $375

The raw setter-based code for this is 20+ lines of boilerplate (see §1). Each DSL pattern below solves the same problem with a different tradeoff.

---

## 5. Pattern 1 — Method Chaining

### What It Looks Like

```java
Order order = forCustomer("BigBank")
    .buy(80)
        .stock("IBM")
        .on("NYSE")
        .at(125.00)
    .sell(50)
        .stock("GOOGLE")
        .on("NASDAQ")
        .at(375.00)
    .end();
```

Reads almost like plain English. Method names act as named arguments. The user cannot call `.at()` before `.on()` — the type system enforces the correct sequence.

### How It Works — Builder Chain

The trick: each builder method returns a **different builder type** that only exposes the next valid method. This enforces ordering at compile time.

```
forCustomer(String)                  → MethodChainingOrderBuilder
    .buy(int) / .sell(int)           → TradeBuilder
        .stock(String)               → StockBuilder
            .on(String)              → TradeBuilderWithStock
                .at(double)          → MethodChainingOrderBuilder  (back to top)
    .end()                           → Order
```

### The Implementation

```java
// Top-level builder — wraps the Order being built
public class MethodChainingOrderBuilder {
    public final Order order = new Order();

    private MethodChainingOrderBuilder(String customer) {
        order.setCustomer(customer);
    }

    // Static factory — entry point
    public static MethodChainingOrderBuilder forCustomer(String customer) {
        return new MethodChainingOrderBuilder(customer);
    }

    public TradeBuilder buy(int quantity) {
        return new TradeBuilder(this, Trade.Type.BUY, quantity);
    }

    public TradeBuilder sell(int quantity) {
        return new TradeBuilder(this, Trade.Type.SELL, quantity);
    }

    // Called by TradeBuilderWithStock.at() after a trade is fully configured
    public MethodChainingOrderBuilder addTrade(Trade trade) {
        order.addTrade(trade);
        return this;  // return self — allows chaining the next buy()/sell()
    }

    public Order end() {
        return order;
    }
}
```

```java
// Second builder — collects quantity and type, forces .stock() next
public class TradeBuilder {
    private final MethodChainingOrderBuilder builder;
    public final Trade trade = new Trade();

    TradeBuilder(MethodChainingOrderBuilder builder, Trade.Type type, int quantity) {
        this.builder = builder;
        trade.setType(type);
        trade.setQuantity(quantity);
    }

    // Only method available — forces user to specify a stock
    public StockBuilder stock(String symbol) {
        return new StockBuilder(builder, trade, symbol);
    }
}
```

```java
// Third builder — forces .on() (market) next
public class StockBuilder {
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
```

```java
// Fourth builder — forces .at() (price) last, then returns to top-level builder
public class TradeBuilderWithStock {
    private final MethodChainingOrderBuilder builder;
    private final Trade trade;

    TradeBuilderWithStock(MethodChainingOrderBuilder builder, Trade trade) {
        this.builder = builder;
        this.trade = trade;
    }

    public MethodChainingOrderBuilder at(double price) {
        trade.setPrice(price);
        return builder.addTrade(trade);  // finishes trade, returns to order builder
    }
}
```

### Key Design Principles

**Return `this` to continue the same level:**
```java
public MethodChainingOrderBuilder addTrade(Trade trade) {
    order.addTrade(trade);
    return this;  // allows .buy().sell() chaining
}
```

**Return a new builder to change level:**
```java
public TradeBuilder buy(int quantity) {
    return new TradeBuilder(this, Trade.Type.BUY, quantity);
    // user is now "inside" TradeBuilder — only .stock() is available
}
```

**Return a plain object to terminate:**
```java
public Order end() {
    return order;  // DSL over, real object returned
}
```

### Pros & Cons

✅ Lowest syntactic noise at the call site  
✅ Method names act as documentation / named arguments  
✅ Compile-time enforcement of call order  
✅ Works naturally with optional parameters (just add more methods that return `this`)  
✅ Minimal static method usage  

❌ Verbose to implement — multiple builder classes required  
❌ Indentation is convention only, not enforced  
❌ Nesting hierarchy is not structurally visible in the code  

---

## 6. Pattern 2 — Nested Functions

### What It Looks Like

```java
Order order = order("BigBank",
    buy(80,
        stock("IBM", on("NYSE")),
        at(125.00)),
    sell(50,
        stock("GOOGLE", on("NASDAQ")),
        at(375.00))
);
```

The domain hierarchy (order → trades → stocks) is **structurally mirrored** by function nesting. More compact implementation than method chaining.

### How It Works

Each function creates and returns a domain object. Functions are nested so that inner results become arguments to outer functions. Static imports make call sites clean.

```java
public class NestedFunctionOrderBuilder {

    // Entry point — varargs accepts any number of trades
    public static Order order(String customer, Trade... trades) {
        Order order = new Order();
        order.setCustomer(customer);
        Stream.of(trades).forEach(order::addTrade);
        return order;
    }

    public static Trade buy(int quantity, Stock stock, double price) {
        return buildTrade(quantity, stock, price, Trade.Type.BUY);
    }

    public static Trade sell(int quantity, Stock stock, double price) {
        return buildTrade(quantity, stock, price, Trade.Type.SELL);
    }

    private static Trade buildTrade(int quantity, Stock stock, double price, Trade.Type type) {
        Trade trade = new Trade();
        trade.setQuantity(quantity);
        trade.setType(type);
        trade.setStock(stock);
        trade.setPrice(price);
        return trade;
    }

    // Dummy wrapper — purely for readability, clarifies argument role
    public static double at(double price) {
        return price;
    }

    public static Stock stock(String symbol, String market) {
        Stock stock = new Stock();
        stock.setSymbol(symbol);
        stock.setMarket(market);
        return stock;
    }

    // Dummy wrapper — clarifies "on which market"
    public static String on(String market) {
        return market;
    }
}
```

**Dummy methods like `at()` and `on()`** are a key technique: they have no runtime effect but add semantic clarity. Without them:

```java
// Confusing — which double is price, which is something else?
buy(80, stock("IBM", "NYSE"), 125.00)

// Clear — at() labels the price argument
buy(80, stock("IBM", on("NYSE")), at(125.00))
```

### Pros & Cons

✅ Domain hierarchy is **visually encoded** by nesting  
✅ Compact implementation — no builder classes needed  
✅ Straightforward to reason about  

❌ Heavy use of static methods (requires static imports)  
❌ Many parentheses — syntax noise increases with nesting depth  
❌ Arguments are position-based, not name-based  
❌ Optional fields require method overloading for each combination  

---

## 7. Pattern 3 — Function Sequencing with Lambdas

### What It Looks Like

```java
Order order = order(o -> {
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
```

Each level of the domain hierarchy gets its own lambda and its own builder. Fields are set by name rather than position. The nesting of lambdas mirrors the nesting of domain objects.

### How It Works

Builders accept `Consumer<Builder>` — the user provides a lambda that configures the builder. The builder executes the lambda, then extracts the populated object.

```java
public class LambdaOrderBuilder {
    private Order order = new Order();

    // Static entry point — accepts a lambda that configures this builder
    public static Order order(Consumer<LambdaOrderBuilder> consumer) {
        LambdaOrderBuilder builder = new LambdaOrderBuilder();
        consumer.accept(builder);   // execute the user's lambda
        return builder.order;       // return the populated order
    }

    public void forCustomer(String customer) {
        order.setCustomer(customer);
    }

    public void buy(Consumer<TradeBuilder> consumer) {
        trade(consumer, Trade.Type.BUY);
    }

    public void sell(Consumer<TradeBuilder> consumer) {
        trade(consumer, Trade.Type.SELL);
    }

    private void trade(Consumer<TradeBuilder> consumer, Trade.Type type) {
        TradeBuilder builder = new TradeBuilder();
        builder.trade.setType(type);
        consumer.accept(builder);       // execute the user's lambda for this trade
        order.addTrade(builder.trade);  // add the populated trade to the order
    }
}
```

```java
public class TradeBuilder {
    Trade trade = new Trade();  // package-visible for the parent builder

    public void quantity(int quantity) { trade.setQuantity(quantity); }
    public void price(double price)    { trade.setPrice(price); }

    public void stock(Consumer<StockBuilder> consumer) {
        StockBuilder builder = new StockBuilder();
        consumer.accept(builder);     // execute user's lambda for this stock
        trade.setStock(builder.stock);
    }
}
```

```java
public class StockBuilder {
    Stock stock = new Stock();

    public void symbol(String symbol) { stock.setSymbol(symbol); }
    public void market(String market) { stock.setMarket(market); }
}
```

### The Core Mechanic — `Consumer<Builder>`

```java
// This pattern is the heart of function sequencing:
public static Order order(Consumer<LambdaOrderBuilder> consumer) {
    LambdaOrderBuilder builder = new LambdaOrderBuilder();
    consumer.accept(builder);  // ← user's lambda runs here, configures builder
    return builder.order;      // ← extract the result
}

// Usage: the lambda receives the builder as its parameter
Order order = order(o -> {
    o.forCustomer("BigBank");  // o is the LambdaOrderBuilder instance
    o.buy(t -> { ... });       // t is a TradeBuilder instance
});
```

`Consumer<T>` is `void T -> void`. The builder is mutated by the lambda in place.

### Pros & Cons

✅ Lambda nesting **structurally mirrors** domain object hierarchy  
✅ Fields set by name, not position — no dummy methods needed  
✅ Works naturally with optional fields (just omit the setter call)  
✅ No glue code between builders (no reference passing between builder classes)  

❌ Most verbose implementation of the three  
❌ Lambdas add syntactic noise at the call site (`->`, braces, semicolons)  
❌ Requires familiarity with `Consumer` and functional interfaces  

---

## 8. Combining All Three Patterns

Nothing stops you from mixing patterns within a single DSL. The goal is to use the best fit for each level of the domain hierarchy.

```java
// Outer structure: nested functions
// Trade creation: lambda (Consumer<TradeBuilder>)
// Trade configuration: method chaining inside the lambda

Order order = forCustomer("BigBank",
    buy(t -> t.quantity(80)
              .stock("IBM")
              .on("NYSE")
              .at(125.00)),
    sell(t -> t.quantity(50)
               .stock("GOOGLE")
               .on("NASDAQ")
               .at(375.00))
);
```

### Mixed Builder Implementation

```java
public class MixedBuilder {

    // Nested function at the top level
    public static Order forCustomer(String customer, TradeBuilder... builders) {
        Order order = new Order();
        order.setCustomer(customer);
        Stream.of(builders).forEach(b -> order.addTrade(b.trade));
        return order;
    }

    // Lambda for trade creation
    public static TradeBuilder buy(Consumer<TradeBuilder> consumer) {
        return buildTrade(consumer, Trade.Type.BUY);
    }

    public static TradeBuilder sell(Consumer<TradeBuilder> consumer) {
        return buildTrade(consumer, Trade.Type.SELL);
    }

    private static TradeBuilder buildTrade(Consumer<TradeBuilder> consumer, Trade.Type type) {
        TradeBuilder builder = new TradeBuilder();
        builder.trade.setType(type);
        consumer.accept(builder);
        return builder;
    }
}
```

```java
// Method chaining inside the lambda body
public class TradeBuilder {
    Trade trade = new Trade();

    public TradeBuilder quantity(int quantity) {
        trade.setQuantity(quantity);
        return this;  // enables chaining
    }

    public TradeBuilder at(double price) {
        trade.setPrice(price);
        return this;
    }

    public StockBuilder stock(String symbol) {
        return new StockBuilder(this, trade, symbol);
    }
}

public class StockBuilder {
    private final TradeBuilder builder;
    private final Trade trade;
    private final Stock stock = new Stock();

    StockBuilder(TradeBuilder builder, Trade trade, String symbol) {
        this.builder = builder;
        this.trade = trade;
        stock.setSymbol(symbol);
    }

    public TradeBuilder on(String market) {
        stock.setMarket(market);
        trade.setStock(stock);
        return builder;  // return TradeBuilder to allow .at() after .on()
    }
}
```

**When to mix:** Use the combined approach when different parts of your domain have naturally different shapes — flat lists at the top, sequential configuration in the middle, optional fields at the leaves.

---

## 9. Method References in DSLs

Method references can replace both lambdas and flags, producing the most concise and expressive DSL code.

### The Problem: Boolean Flags

```java
// Unreadable — which bool is which?
double value = calculate(order, true, false, true);

// Better with named methods, but one method/field per tax — doesn't scale
double value = new TaxCalculator()
    .withTaxRegional()
    .withTaxSurcharge()
    .calculate(order);
```

### The Solution: `DoubleUnaryOperator` Composition

```java
// Tax functions — each transforms a value
public class Tax {
    public static double regional(double value)  { return value * 1.1; }
    public static double general(double value)   { return value * 1.3; }
    public static double surcharge(double value) { return value * 1.05; }
}
```

```java
public class TaxCalculator {

    // Starts as the identity function — no taxes yet
    private DoubleUnaryOperator taxFunction = d -> d;

    // Compose the new tax onto the existing function chain
    public TaxCalculator with(DoubleUnaryOperator f) {
        taxFunction = taxFunction.andThen(f);
        return this;
    }

    public double calculate(Order order) {
        return taxFunction.applyAsDouble(order.getValue());
    }
}
```

```java
// Method references make the call site read like a business rule
double value = new TaxCalculator()
    .with(Tax::regional)
    .with(Tax::surcharge)
    .calculate(order);
```

### How `andThen` Composes Functions

```
Initial:     taxFunction = d -> d                          (identity)
After .with(Tax::regional):
             taxFunction = d -> Tax.regional(d)
After .with(Tax::surcharge):
             taxFunction = d -> Tax.surcharge(Tax.regional(d))

calculate(): taxFunction.applyAsDouble(order.getValue())
           = Tax.surcharge(Tax.regional(order.getValue()))
```

**Benefits of this approach:**
- Adding a new tax requires only adding a new static method in `Tax` — no changes to `TaxCalculator`
- Call site is readable and self-documenting
- Any `DoubleUnaryOperator` (including lambdas) can be passed — fully open for extension
- Function composition is explicit and traceable

---

## 10. Pattern Comparison & Decision Guide

### Side-by-Side Summary

| | Method Chaining | Nested Functions | Function Sequencing |
|---|---|---|---|
| **Call site noise** | Lowest | Medium | Highest (lambda syntax) |
| **Implementation effort** | Highest (multiple builders) | Lowest | Medium |
| **Hierarchy visibility** | No (indentation convention only) | Yes (structural nesting) | Yes (lambda nesting) |
| **Optional fields** | Easy (extra methods returning `this`) | Hard (overloads) | Easy (just omit the setter) |
| **Argument naming** | Method name = argument name | Position-based (use dummy methods) | Field name = method name |
| **Order enforcement** | Yes (type system) | No (argument order only) | No |
| **Static methods** | Minimal | Heavy | Minimal |

### Decision Guide

```
Does the domain have a natural sequential workflow
(like a query: SELECT → WHERE → ORDER BY)?
    → Method Chaining (jOOQ, Stream API model)

Does the domain have a nested tree structure
(order → trades → stocks)?
    → Nested Functions (mirrors the hierarchy structurally)

Does the domain have many optional fields
with no fixed ordering?
    → Function Sequencing (lambdas, each field set by name)

Do you need all three in one DSL?
    → Combine: nested functions at top, method chaining inside lambdas
```

### Enforcing Call Order — Method Chaining Type Trick

This is a critical technique. Use distinct return types to enforce sequence:

```java
// User CANNOT call .at() before .on() — .at() doesn't exist on StockBuilder
// They CANNOT call .on() before .stock() — .on() doesn't exist on TradeBuilder
// The compiler enforces the entire sequence

.buy(80)          // → TradeBuilder      only exposes .stock()
.stock("IBM")     // → StockBuilder      only exposes .on()
.on("NYSE")       // → TradeBuilderWithStock  only exposes .at()
.at(125.00)       // → MethodChainingOrderBuilder  back to .buy()/.sell()/.end()
```

---

## 11. Real-World DSLs in Java Libraries

### jOOQ — SQL as a Type-Safe Internal DSL

jOOQ generates Java classes from your DB schema and wraps SQL in a fluent method-chaining DSL. The Java compiler type-checks your SQL.

```java
// SQL:
// SELECT * FROM BOOK WHERE BOOK.PUBLISHED_IN = 2016 ORDER BY BOOK.TITLE

// jOOQ DSL:
create.selectFrom(BOOK)
      .where(BOOK.PUBLISHED_IN.eq(2016))
      .orderBy(BOOK.TITLE);
```

**Why method chaining fits SQL:** SQL has a rigid, ordered clause structure (SELECT before WHERE before ORDER BY). Method chaining enforces this — you can't call `.orderBy()` on a raw `SelectFromStep`, only on a `SelectWhereStep`. The type system mirrors SQL grammar.

**Combined with Stream API:**

```java
DSL.using(connection)
   .select(BOOK.AUTHOR, BOOK.TITLE)
   .where(BOOK.PUBLISHED_IN.eq(2016))
   .orderBy(BOOK.TITLE)
   .fetch()          // ← jOOQ DSL ends here, returns jOOQ Result
   .stream()         // ← Java Stream API begins here
   .collect(groupingBy(
       r -> r.getValue(BOOK.AUTHOR),
       mapping(r -> r.getValue(BOOK.TITLE), toList())
   ));
```

This is a clean composition of two internal DSLs: jOOQ for DB access, Stream/Collectors for in-memory aggregation.

---

### Cucumber — External DSL + Internal DSL Combination

Cucumber uses an **external DSL** (Gherkin) for the human-readable scenario definition, then maps it to Java code via annotations or lambdas.

**External DSL (Gherkin) — written by domain experts:**

```gherkin
Feature: Buy stock
  Scenario: Buy 10 IBM stocks
    Given the price of a "IBM" stock is 125$
    When I buy 10 "IBM"
    Then the order value should be 1250$
```

**Internal DSL — annotation style (Java 7+):**

```java
public class BuyStocksSteps {
    private Map<String, Integer> stockUnitPrices = new HashMap<>();
    private Order order = new Order();

    @Given("^the price of a \"(.*?)\" stock is (\\d+)\\$$")
    public void setUnitPrice(String stockName, int unitPrice) {
        stockUnitPrices.put(stockName, unitPrice);
    }

    @When("^I buy (\\d+) \"(.*?)\"$")
    public void buyStocks(int quantity, String stockName) {
        Trade trade = new Trade();
        trade.setType(Trade.Type.BUY);
        Stock stock = new Stock();
        stock.setSymbol(stockName);
        trade.setStock(stock);
        trade.setPrice(stockUnitPrices.get(stockName));
        trade.setQuantity(quantity);
        order.addTrade(trade);
    }

    @Then("^the order value should be (\\d+)\\$$")
    public void checkOrderValue(int expectedValue) {
        assertEquals(expectedValue, order.getValue());
    }
}
```

**Internal DSL — lambda style (Java 8+):**

```java
public class BuyStocksSteps implements cucumber.api.java8.En {
    private Map<String, Integer> stockUnitPrices = new HashMap<>();
    private Order order = new Order();

    public BuyStocksSteps() {
        Given("^the price of a \"(.*?)\" stock is (\\d+)\\$$",
            (String stockName, Integer unitPrice) -> {
                stockUnitPrices.put(stockName, unitPrice);
            });

        When("^I buy (\\d+) \"(.*?)\"$",
            (Integer quantity, String stockName) -> {
                // populate trade...
            });

        Then("^the order value should be (\\d+)\\$$",
            (Integer expectedValue) -> {
                assertEquals(expectedValue, order.getValue());
            });
    }
}
```

Lambda style eliminates the need for `@Given`/`@When`/`@Then` annotations and removes the burden of naming test methods that rarely carry useful semantic meaning.

**Pattern used:** External DSL (Gherkin) + Internal DSL (function sequencing with lambdas inside the constructor).

---

### Spring Integration — Message Flow DSL

Spring Integration implements Enterprise Integration Patterns. Its DSL configures message channels, filters, transformers, and output channels as a fluent pipeline.

```java
@Configuration
@EnableIntegration
public class MyConfiguration {

    @Bean
    public MessageSource<?> integerMessageSource() {
        MethodInvokingMessageSource source = new MethodInvokingMessageSource();
        source.setObject(new AtomicInteger());
        source.setMethodName("getAndIncrement");
        return source;
    }

    @Bean
    public DirectChannel inputChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow myFlow() {
        return IntegrationFlows
            .from(this.integerMessageSource(),              // source of messages
                  c -> c.poller(Pollers.fixedRate(10)))     // poll every 10ms
            .channel(this.inputChannel())                   // route to channel
            .filter((Integer p) -> p % 2 == 0)             // keep even numbers
            .transform(Object::toString)                    // Integer → String
            .channel(MessageChannels.queue("queueChannel")) // output channel
            .get();                                         // build the flow
    }
}
```

**Lambda-based shorthand** (when starting from a direct channel, not a MessageSource):

```java
@Bean
public IntegrationFlow myFlow() {
    return flow -> flow
        .filter((Integer p) -> p % 2 == 0)
        .transform(Object::toString)
        .handle(System.out::println);
}
```

**Patterns used:**
- Primary: **method chaining** (the `IntegrationFlows` fluent builder)
- Secondary: **function sequencing with lambdas** (the top-level `IntegrationFlow` lambda, and individual step arguments)

**Why method chaining fits integration flows:** Like SQL clauses, integration steps have a natural sequence: source → filter → transform → output. Method chaining enforces this linearity and reads like a dataflow diagram.

---

## 12. Quick Reference Cheat Sheet

### The Three Patterns — At a Glance

```java
// ── METHOD CHAINING ──────────────────────────────────────────────
// Fluent, sequential, enforced by return types
Order order = forCustomer("BigBank")
    .buy(80).stock("IBM").on("NYSE").at(125.00)
    .end();

// ── NESTED FUNCTIONS ─────────────────────────────────────────────
// Structural hierarchy mirrored by function nesting, uses static imports
Order order = order("BigBank",
    buy(80, stock("IBM", on("NYSE")), at(125.00)),
    sell(50, stock("GOOGLE", on("NASDAQ")), at(375.00))
);

// ── FUNCTION SEQUENCING ───────────────────────────────────────────
// Named setters in lambdas, mirrors nesting via lambda nesting
Order order = order(o -> {
    o.forCustomer("BigBank");
    o.buy(t -> { t.quantity(80); t.price(125.00);
                 t.stock(s -> { s.symbol("IBM"); s.market("NYSE"); }); });
});
```

### Method References for Function Composition

```java
// Chain tax functions via andThen — open/closed principle in action
double value = new TaxCalculator()
    .with(Tax::regional)    // method reference to static method
    .with(Tax::surcharge)
    .calculate(order);

// The engine:
public TaxCalculator with(DoubleUnaryOperator f) {
    taxFunction = taxFunction.andThen(f);  // compose, don't replace
    return this;
}
```

### Functional Interfaces Used in DSL Patterns

| Interface | Signature | Used For |
|---|---|---|
| `Consumer<T>` | `T → void` | Function sequencing — builder receives, configures, returns nothing |
| `Function<T,R>` | `T → R` | Mapping/transformation inside a pipeline |
| `DoubleUnaryOperator` | `double → double` | Composable single-value transforms (taxes, discounts) |
| `Comparator<T>` | `(T,T) → int` | Composable sort criteria |
| `Predicate<T>` | `T → boolean` | Composable filter conditions |

### Returning `this` vs. Returning a New Builder

```java
// Return `this` — stay at the same level
public TradeBuilder quantity(int q) {
    trade.setQuantity(q);
    return this;  // .quantity(80).price(125.0) chaining stays in TradeBuilder
}

// Return a new builder — move to the next level
public StockBuilder stock(String symbol) {
    return new StockBuilder(this, trade, symbol);
    // user is now in StockBuilder — only .on() is available
}

// Return parent builder — move UP a level
public TradeBuilder on(String market) {
    stock.setMarket(market);
    trade.setStock(stock);
    return builder;  // back to TradeBuilder, can call .at() now
}
```

### DSL Pattern → Real-World Library Mapping

| Library | Pattern | Why It Fits |
|---|---|---|
| `Stream` / `jOOQ` | Method chaining | Sequential pipeline with ordered steps |
| `Collectors` | Nested functions | Hierarchical structure (group within group) |
| `Comparator` | Method chaining | Linear composition (compare by X, then Y) |
| Spring Integration | Method chaining + lambdas | Message flow is a sequence; steps have optional config |
| Cucumber | External DSL + function sequencing | Business scenarios need non-Java readability |

---

*Reference compiled from Modern Java in Action, Chapter 10. Covers the core DSL patterns you'll encounter and implement in production Java API design.*
