# Bootiful Kotlin — Study Guide

> **📦 Repository:** [joshlong-attic/2026-05-21-kotlin-conf-bootiful-kotlin](https://github.com/joshlong-attic/2026-05-21-kotlin-conf-bootiful-kotlin)
> **Stack:** Kotlin · Spring Boot 4 · Spring MVC + virtual threads · Spring Data JDBC · PostgreSQL · JTE · Spring AI + Ollama · Spring Security (WebAuthn, one-time tokens) · OpenTelemetry

This guide explains the project from a senior backend and system-design angle. It covers not only *what* the code does, but also:

- **Why** each piece exists and what Spring does behind the scenes
- How requests move through the system, down to the JVM and database level
- How authentication works
- How AI inference changes the concurrency model
- What the architecture means under production load — and what must change before deploying

> ⚠️ **Important:** The repo is primarily a **conference demo**. Some architecture from the README is shown conceptually and is **not** fully implemented in the supplied archive (see [Part 10](#part-10-readme-vs-reality)).

**Legend:** 💡 key idea · ⚠️ watch out · 🏭 production note

---

## TL;DR

- One Spring Boot 4 app with **three very different request paths**: `/dogs` (DB-bound), `/ask` (inference-bound), `/` (auth-bound).
- **Servlet MVC + virtual threads** fits because JDBC and Ollama calls are blocking.
- **Concurrency ≠ capacity.** Virtual threads make *waiting* cheap; they don't make PostgreSQL or the GPU faster.
- Startup calls the model (`ApplicationRunner`), which couples startup and tests to Ollama.
- Demo conveniences (printed one-time token, hard-coded credentials, `GET /ask`) are **not** production-ready.
- Production path: keep the monolith, add ports/adapters, pagination, timeouts, bounded AI concurrency, migrations, observability, and real tests.

---

## Contents

1. [The Big Picture](#part-1-the-big-picture)
2. [Build and Tooling](#part-2-build-and-tooling)
3. [Spring Boot Core](#part-3-spring-boot-core)
4. [The Data Layer](#part-4-the-data-layer)
5. [The Web Layer](#part-5-the-web-layer)
6. [The AI Layer](#part-6-the-ai-layer)
7. [Security](#part-7-security)
8. [Routing and Concurrency](#part-8-routing-and-concurrency)
9. [Observability and Testing](#part-9-observability-and-testing)
10. [README vs Reality](#part-10-readme-vs-reality)
11. [Production Gaps](#part-11-production-gaps)
12. [Target Architecture](#part-12-target-architecture)
13. [Resilience and Scaling](#part-13-resilience-and-scaling)
14. [Request Walkthroughs](#part-14-request-walkthroughs)
15. [Checklist and Principles](#part-15-checklist-and-principles)
16. [Conclusion: Purpose of This Guide](#conclusion-purpose-of-this-guide)

---

## Part 1: The Big Picture

### 1.1 What the app does

One Spring Boot application combines five responsibilities:

1. A **database-backed app** that retrieves dogs from PostgreSQL.
2. A **server-side HTML app** that renders those dogs with JTE.
3. An **AI integration** that sends prompts to a locally hosted Ollama model.
4. **Authentication infrastructure**: database-backed users, passkeys, one-time tokens.
5. A **servlet-based Spring MVC** app running on virtual threads.

### 1.2 Current architecture

```text
                         ┌──────────────────────┐
                         │       Clients        │
                         │ Browser / HTTP / API │
                         └──────────┬───────────┘
                                    │
                                    │ HTTP
                                    ▼
                    ┌───────────────────────────────┐
                    │       Spring Boot 4           │
                    │     Embedded Servlet Server   │
                    └──────────────┬────────────────┘
                                   │
                         ┌─────────▼─────────┐
                         │ Spring Security   │
                         │ Filter Chain      │
                         │ Authentication    │
                         │ Authorization     │
                         └─────────┬─────────┘
                                   │
                  ┌────────────────┼────────────────┐
                  │                │                │
                  ▼                ▼                ▼
             MVC Controller   Functional Router   Other
                  │                │
          ┌───────┴───────┐        │
          │               │        │
          ▼               ▼        ▼
    DogRepository    ChatClient  SecurityContext
          │               │
          ▼               ▼
   Spring Data JDBC   Spring AI
          │               │
          ▼               ▼
      PostgreSQL        Ollama
                           │
                           ▼
                      Gemma Model
```

It is effectively a **modular monolith in potential**, but most of the supplied code sits in **one Kotlin source file**. There is no message broker, distributed cache, API gateway, or set of microservices.

> 💡 Understand the *actual* architecture before introducing distributed-system complexity.

### 1.3 The three request paths

```text
GET /dogs
  → Security filters → AssistantController → DogRepository.findAll()
  → Spring Data JDBC → JDBC driver → PostgreSQL
  → List<Dog> → JTE template → HTML response

GET /ask?question=...
  → Security filters → AssistantController → ChatClient → Spring AI
  → Ollama HTTP API → Gemma model → generated text → HTTP response

GET /
  → Security filters → SecurityContext → authenticated principal → JSON response
```

| Path | Primarily bound by |
|---|---|
| `/dogs` | Database |
| `/ask` | Model / inference |
| `/` | Authentication / application |

Their latency and resource profiles are very different.

### 1.4 The README's talk outline → where it's covered

The repo's README is a **talk outline** (build the app step by step), not documentation.

| README step | Covered in |
|---|---|
| Basics of Kotlin | Parts 2, 4 |
| Generate project on start.spring.io (deps: Kotlin, DevTools, Spring Data JDBC, JTE, PostgreSQL, Ollama, Spring Web, OpenTelemetry, Docker Compose Support, Spring Security, WebAuthn) | Part 2 |
| Build setup: Kotlin Maven plugin, "Prancer" (as listed), virtual threads | Parts 2, 8 |
| Configure AI: `spring.ai.ollama.chat.model=gemma4:26b` | Part 6 |
| Spring Data JDBC repository for `Dog` | Part 4 |
| `BeanRegistrarDsl`: `ChatClient`, `ApplicationRunner` | Parts 3, 6 |
| Using Exposed | Part 12 |
| `/ask` endpoint | Part 6 |
| Skills support for `ChatClient` | Parts 6, 10 |
| JTE views + `/dogs` (`gg.jte.template-suffix=.kte`, `dogs.kte` in `src/main/jte`) | Part 5 |
| Security: JDBC users, WebAuthn, OTT | Part 7 |
| Extract `Dogs` interface, switch to Exposed (`exposed-spring-boot4-starter` 1.3.0) | Part 12 |
| `SecurityContextHolder` + JSpecify, `/` endpoint | Part 7 |
| Run the app at `http://localhost:3000` | Part 7 (origins) |

> ⚠️ **Two small mismatches to be aware of:**
> - README says to open `http://localhost:3000`, but the WebAuthn config in the code allows origin `http://localhost:8080`. Origins must match exactly, so check your `server.port`.
> - README sketches `CrudRepository<Dog, Long>`; the code uses `ListCrudRepository<Dog, Int>`.

---

## Part 2: Build and Tooling

File: `agent/pom.xml`

### 2.1 Build time vs runtime

```text
BUILD TIME   resolve dependencies → compile Kotlin → generate templates → run tests → package JAR

RUNTIME      JVM starts → Spring builds ApplicationContext → beans created
             → embedded server starts → requests processed
```

> 💡 Build-time behavior and runtime behavior are separate concerns.

### 2.2 Spring Boot parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.6</version>
    <relativePath/>
</parent>
```

The parent provides coordinated build configuration and dependency management. That's why the PostgreSQL driver needs no version:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Without coordinated versions you can accidentally mix incompatible ones — everything compiles independently, then fails at runtime:

```text
Spring Framework A + Spring Data B + Spring Security C + Jackson D
```

### 2.3 Java and Kotlin versions

```xml
<properties>
    <java.version>24</java.version>
    <kotlin.version>2.2.21</kotlin.version>
    <spring-ai.version>2.0.0-M6</spring-ai.version>
</properties>
```

| Property | Meaning |
|---|---|
| `java.version` | Java target/toolchain configuration |
| `kotlin.version` | Kotlin compiler/library version |
| `spring-ai.version` | Spring AI dependency family |

```text
Kotlin source → Kotlin compiler → JVM bytecode → Spring Boot → JVM execution
```

> 💡 Deployment architecture is determined by the runtime, framework, libraries, and infrastructure — not by whether the source is Java or Kotlin.

### 2.4 Spring AI BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**BOM = Bill of Materials.** It coordinates versions across Spring AI modules.

```text
ChatClient → Spring AI abstraction → Ollama integration → HTTP transport → Ollama
```

> 🏭 The project uses a **milestone** Spring AI version. Test upgrades against the exact APIs and runtime behavior the app uses.

### 2.5 Kotlin Maven plugin and Spring

```xml
<args>
    <arg>-Xjsr305=strict</arg>
    <arg>-Xannotation-default-target=param-property</arg>
</args>

<compilerPlugins>
    <plugin>spring</plugin>
</compilerPlugins>
```

Two concepts are at play: **Kotlin's default finality** and **Java/Kotlin nullability interop** (see 2.6).

#### Why the Kotlin Spring compiler plugin exists

Kotlin classes are `final` by default:

```kotlin
class DogService   // ≈ public final class DogService {}
```

Spring sometimes uses **subclass-based proxies** for transactions, caching, method security, and other AOP-style interception. The plugin automatically opens appropriate Spring-managed classes, so you don't write `open` everywhere.

#### Spring proxying

```kotlin
@Service
class DogService(
    private val repository: DogRepository
) {
    @Transactional
    fun renameDog(id: Int, name: String) {
        // Database operation
    }
}
```

```text
Controller → Spring Proxy → Transaction Interceptor → DogService.renameDog() → Repository
```

The proxy can (1) begin a transaction, (2) invoke the method, (3) commit on success, (4) roll back when appropriate.

> Not every bean is proxied, and Spring can also use interface-based proxies.

#### Self-invocation trap

```kotlin
@Service
class DogService {

    fun outer() {
        inner()          // same object → bypasses the proxy
    }

    @Transactional
    fun inner() {
        // Database operation
    }
}
```

When `outer()` calls `inner()` on the same object, the call can bypass the Spring proxy, so the transaction advice on `inner()` may not run.

> 💡 Annotation-driven framework behavior often depends on calls **crossing a proxy boundary**.

### 2.6 Kotlin nullability and Java interop

```xml
<arg>-Xjsr305=strict</arg>
```

Java expresses nullability more weakly than Kotlin. Without annotations, Kotlin sees **platform types**, written `String!`. Strict handling makes supported Java nullability annotations meaningful to Kotlin, moving null errors from runtime toward compile time.

> ⚠️ It is **not** a guarantee that every Java API becomes null-safe.

### 2.7 JTE template generation

The Maven build compiles JTE templates during the source-generation phase:

```text
dogs.kte → template compilation → generated code → JVM application → runtime rendering
```

| Benefit | Trade-off |
|---|---|
| Earlier template errors | Template compilation becomes part of the build |
| Less runtime template-processing overhead | |
| More compile-time integration | |

---

## Part 3: Spring Boot Core

### 3.1 Startup

```kotlin
@Import(MyBeanRegistrar::class)
@SpringBootApplication
class AgentApplication

fun main(args: Array<String>) {
    runApplication<AgentApplication>(*args)
}
```

```text
JVM → main() → SpringApplication → Environment → ApplicationContext
  → bean definitions → auto-configuration → bean instantiation
  → post-processors / proxies → embedded server → ApplicationRunner → Ready
```

The real internal lifecycle is more detailed, but this model is enough for system design.

### 3.2 What `@SpringBootApplication` means

It combines three annotations:

| Annotation | Role |
|---|---|
| `@SpringBootConfiguration` | Marks the class as a Spring Boot configuration source |
| `@EnableAutoConfiguration` | Configures infrastructure from classpath dependencies, existing beans, properties, conditional config, and available runtime infrastructure |
| `@ComponentScan` | Discovers `@Controller`, `@Service`, `@Repository`, `@Component`, `@Configuration` |

Because the application class is in the base package, Spring scans that package and its descendants.

### 3.3 Dependency injection = graph construction

```kotlin
class AssistantController(
    private val cc: ChatClient,
    private val dogs: DogRepository
)
```

Spring must find objects that satisfy both dependencies:

```text
ApplicationContext
│
├── ChatClient
│      └── Ollama integration
│
├── DogRepository
│      └── JDBC infrastructure
│             └── DataSource
│
└── AssistantController
       ├── ChatClient
       └── DogRepository
```

The controller does **not** construct its own dependencies.

#### Why constructor injection matters

**Avoid:**

```kotlin
class AssistantController {
    private val client = SomeConcreteAiClient()
}
```

Here the controller decides which implementation to use, how to construct and configure it, and possibly how to manage its lifecycle.

**Prefer:**

```kotlin
class AssistantController(
    private val client: ChatClient
)
```

Those decisions move to the application **composition layer**. Benefits: testability, replaceability, visible dependencies, better separation of concerns.

### 3.4 `BeanRegistrarDsl`

```kotlin
class MyBeanRegistrar : BeanRegistrarDsl({

    registerBean {
        JdbcUserDetailsManager(bean<DataSource>())
    }
})
```

A **Spring bean** is an object managed by the application context.

```kotlin
val service = DogService()      // your code owns creation
```

```text
Spring bean: ApplicationContext creates object → resolves dependencies
  → applies lifecycle behavior → applies post-processing → manages bean
```

#### `bean<DataSource>()`

This does **not** mean `DataSource()`. `DataSource` is an abstraction; Spring Boot creates/configures an implementation from available dependencies and properties. The DSL simply asks the context for the appropriate bean.

#### Registration ≠ immediate execution

```text
Bean definition registration → context initialization → dependency resolution
  → bean creation → application startup → ApplicationRunner execution
```

Spring manages the lifecycle.

#### The registrar as a composition root

It decides infrastructure-composition matters: authentication implementation, AI client construction, functional routes, startup actions. A larger app could split them:

```text
config/
├── AiConfiguration.kt
├── SecurityConfiguration.kt
├── RoutingConfiguration.kt
└── PersistenceConfiguration.kt
```

The principle: **separation of responsibility**.

---

## Part 4: The Data Layer

### 4.1 The `Dog` model

```kotlin
data class Dog(
    @Id val id: Int,
    val name: String,
    val description: String
)
```

It plays two roles: a **Kotlin data class** and a **Spring Data JDBC persistence model**. Larger systems can separate those roles even though they're combined here.

#### What Kotlin generates for a data class

`equals()` · `hashCode()` · `toString()` · `componentN()` · `copy()`

```kotlin
val dog = Dog(id = 1, name = "Rex", description = "German Shepherd")
val renamed = dog.copy(name = "Max")   // original unchanged
```

#### ⚠️ `copy()` is shallow

```kotlin
data class DogProfile(
    val name: String,
    val tags: MutableList<String>
)

val original = DogProfile("Rex", mutableListOf("friendly"))
val copied = original.copy()
copied.tags.add("trained")   // affects both objects
```

```text
original.tags ─────┐
                   ▼
             MutableList
                   ▲
                   │
copied.tags ───────┘
```

### 4.2 `@Id` and identity vs equality

```kotlin
@Id
val id: Int
```

`@Id` marks persistent identity. For database-generated IDs, a **nullable ID** can better represent lifecycle:

```kotlin
data class Dog(
    @Id
    val id: Long? = null,
    val name: String,
    val description: String
)
```

```text
id == null  → new / unpersisted
id != null  → persistent identity
```

(This depends on the actual persistence design.)

#### Persistence identity vs value equality

```kotlin
Dog(1, "Rex", "Friendly")
Dog(1, "Rex", "Aggressive")
```

These are **unequal** (descriptions differ) yet can represent the **same** database entity (ID `1`).

| Concept | Meaning |
|---|---|
| Value equality | Relevant values are equal |
| Entity identity | Same persistent entity |

This matters for sets, maps, caches, collections, and concurrent operations.

### 4.3 The Spring Data JDBC repository

```kotlin
interface DogRepository :
    ListCrudRepository<Dog, Int>
```

Spring Data generates the implementation:

```text
Spring startup → repository scanning → discover DogRepository
  → inspect generics (Dog = aggregate type, Int = ID type)
  → repository factory → runtime implementation/proxy → Spring bean
```

#### `ListCrudRepository`

Common operations: `save()` · `saveAll()` · `findById()` · `findAll()` · `findAllById()` · `existsById()` · `count()` · `deleteById()` · `delete()` · `deleteAll()`

It's convenient in Kotlin because collection-returning operations use `List`.

#### What happens during `findAll()`

```text
Controller → Repository proxy → Spring Data JDBC → JDBC → connection acquisition
  → SQL → ResultSet → row mapping → List<Dog>
```

Conceptual SQL (exact SQL depends on mapping metadata and dialect/naming rules):

```sql
SELECT id, name, description
FROM dog;
```

### 4.4 Spring Data JDBC vs JPA/Hibernate

| Concern | Spring Data JDBC | JPA/Hibernate |
|---|---|---|
| Persistence model | Aggregate-oriented | ORM / persistence context |
| Lazy loading | Not the normal model | Supported |
| Dirty checking | No Hibernate-style dirty checking | Supported |
| Persistence context | No JPA persistence context | Yes |
| SQL behavior | More explicit | ORM lifecycle influences SQL |
| Relationships | Aggregate-oriented | Rich ORM relationships |

**JPA** — mutation is detected and flushed automatically:

```kotlin
@Transactional
fun rename(id: Long) {
    val dog = entityManager.find(DogEntity::class.java, id)
    dog.name = "Max"
}
```

**Spring Data JDBC** — you save explicitly:

```kotlin
fun rename(id: Long) {
    val dog = repository.findById(id).orElseThrow()
    repository.save(dog.copy(name = "Max"))
}
```

### 4.5 Aggregate boundaries

Spring Data JDBC is strongly **aggregate-oriented**. Suppose a dog has vaccination records:

```text
Option A: one aggregate        Option B: two aggregates
Dog Aggregate                  Dog Aggregate
├── Dog                        Vaccination Aggregate
└── Vaccinations
```

- Independent lifecycle → separate aggregates may make sense.
- Must always change consistently with the dog → aggregate ownership may be appropriate.

This affects transactions, concurrency, deletes, updates, invariants, and database mapping.

### 4.6 Connection pooling

```properties
spring.datasource.url=jdbc:postgresql://localhost/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
```

Spring Boot commonly uses a pool such as **HikariCP**. Creating a DB connection can involve:

```text
TCP connection → TLS negotiation → authentication → session initialization → ready
```

Pooling avoids repeating that per request.

#### The pool is a concurrency limit

```text
HTTP requests = 200        DB connections = 10

200 requests → Connection Pool → 10 active connections → PostgreSQL
```

Only ~10 operations can hold connections at once; the rest wait.

> 💡 Application concurrency and database concurrency are **different things**. The pool is also a **bulkhead** protecting PostgreSQL.

---

## Part 5: The Web Layer

### 5.1 The `/dogs` endpoint

```kotlin
@Controller
@ResponseBody
class AssistantController(
    private val cc: ChatClient,
    private val dogs: DogRepository
) {

    @GetMapping("/dogs")
    fun dogs() = ModelAndView(
        "dogs",
        mapOf("dogs" to dogs.findAll())
    )
}
```

Spring MVC + server-side rendering.

#### `@Controller` vs `@RestController`

- `@Controller` identifies an MVC controller.
- `@ResponseBody` enables response-body semantics.
- `@RestController` is a convenience combination.
- `ModelAndView` explicitly represents **view rendering**.

> 🏭 For readability, production code generally separates **HTML controllers** from **REST API controllers**.

### 5.2 `/dogs` request lifecycle

```text
Servlet Container → Spring Security Filter Chain → DispatcherServlet
  → HandlerMapping → HandlerAdapter → Controller → Repository → PostgreSQL
  → ModelAndView → View Resolution → JTE → HTML Response
```

The controller supplies view info and data; it doesn't hand-build the whole response.

### 5.3 `ModelAndView`

```kotlin
ModelAndView("dogs", mapOf("dogs" to dogs.findAll()))
```

```text
View = "dogs"
Model:
    dogs
      ├── Dog
      ├── Dog
      └── Dog
```

The template consumes the model.

### 5.4 The JTE template

```kotlin
@import com.example.agent.Dog
@param dogs : List<Dog>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>Hello World</title>
</head>
<body>
<h1> dogs </h1>
<ol>
    @for (dog in dogs)
        <li>${dog.name}</li>
    @endfor
</ol>
</body>
</html>
```

The template explicitly expects `List<Dog>`, and the controller provides a matching model value.

### 5.5 Template escaping and XSS

A malicious dog name could contain:

```html
<script>alert('XSS')</script>
```

Inserted as raw HTML, it could execute. Normal **escaped** template output protects against this class of issue.

> 💡 Encode data for the context in which it is rendered. HTML, JavaScript, SQL, and URLs each need different handling.

### 5.6 The unbounded `findAll()` problem

`dogs.findAll()` is fine for a tiny dataset. At millions of rows:

```text
Database scan → huge DB result → network transfer → millions of JVM objects
  → heap pressure → template rendering → huge response
```

> 🏭 Production APIs normally paginate:
>
> ```http
> GET /dogs?page=0&size=20
> ```
>
> For very large mutable datasets, **keyset/cursor pagination** may beat deep offset pagination.

> 💡 Result cardinality is part of an API's resource contract.

---

## Part 6: The AI Layer

### 6.1 Spring AI + Ollama

```kotlin
registerBean {
    bean<ChatClient.Builder>().build()
}
```

```kotlin
cc
    .prompt()
    .user(question)
    .call()
    .content()
```

```text
Controller → ChatClient → Spring AI → Ollama Adapter → Ollama HTTP API → Gemma
```

#### `ChatClient.Builder`

Lets you centralize configuration of default prompts, advisors, tools, model options, observability, and provider settings — so the controller doesn't construct this infrastructure itself.

#### The fluent API

| Call | What it does |
|---|---|
| `prompt()` | Begins building a model request |
| `user(question)` | Adds the user message |
| `call()` | Executes the request (synchronous from the caller's view) |
| `content()` | Extracts generated text (the underlying response can carry richer info) |

### 6.2 Ollama configuration

```properties
spring.ai.ollama.chat.model=gemma4:26b
```

The model is **not** inside the Spring Boot process:

```text
Spring Boot ──HTTP──▶ Ollama ──▶ Model weights ──▶ CPU/GPU
```

Ollama and Spring Boot are separate runtime components.

### 6.3 Model inference (simplified)

```text
Prompt → tokenization → input tokens → model computation
  → next-token probabilities → token selection → repeat
  → output tokens → decoded text
```

Latency depends on: model size, hardware, input token count, output token count, concurrency, model loading, runtime configuration.

### 6.4 Is this an AI agent?

**Not in the richer, tool-using sense.** Current path:

```text
User Prompt → Model → Text Response
```

A tool-using agent looks more like:

```text
User Request → Model → Tool Decision → Application validates tool request
  → Tool executes → Tool result → Model → Final response
```

The project does **not** implement this full agent loop. Tool use brings significant authorization and security concerns.

### 6.5 `ApplicationRunner` — AI at startup

```kotlin
registerBean {
    ApplicationRunner {
        val client = bean<ChatClient>()

        println(
            "Hello from Spring Boot! ${
                client
                    .prompt()
                    .user("tell me a joke")
                    .call()
                    .content()
            }"
        )
    }
}
```

#### Startup coupling

```text
Spring startup → ChatClient → Ollama → Model → Inference
```

- Ollama unavailable or inference fails → startup may fail.
- Inference slow → startup delayed.

#### 🏭 Why this matters in Kubernetes

With 20 replicas, every replica sends an AI request at startup:

```text
Replica 1 → AI request
Replica 2 → AI request
...
Replica 20 → AI request
```

Deployment itself generates model traffic and can overload a constrained model service. Better separation:

| Objective | Mechanism |
|---|---|
| Required initialization | Explicit startup initialization |
| Process health | Liveness |
| Ability to serve traffic | Readiness |
| Dependency availability | Health / metrics |
| AI integration test | Integration test |
| Synthetic AI verification | Controlled monitoring |

---

## Part 7: Security

### 7.1 Overview

Dependencies: `spring-boot-starter-security` and `spring-security-webauthn`, plus:

```kotlin
JdbcUserDetailsManager(bean<DataSource>())
```

Keep three concerns separate:

1. **Authentication**
2. **Authorization**
3. **Session / security-context management**

### 7.2 The Spring Security filter chain

```text
HTTP Request → Servlet Container → DelegatingFilterProxy → FilterChainProxy
  → SecurityFilterChain → Authentication Filters → Authorization Filter
  → DispatcherServlet → Controller
```

Filters can inspect requests, establish authentication, reject requests, populate context, and continue processing.

### 7.3 Authentication vs authorization

| | Question |
|---|---|
| **Authentication** | Who are you? |
| **Authorization** | What can you do? |

A user can be authenticated yet not authorized. Illustrative configuration:

```kotlin
.authorizeHttpRequests {
    it
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/ask").authenticated()
        .anyRequest().authenticated()
}
```

### 7.4 JDBC user details

```text
Authentication → Authentication Provider → UserDetailsService
  → JdbcUserDetailsManager → DataSource → PostgreSQL
```

A conventional schema:

```sql
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    FOREIGN KEY (username)
        REFERENCES users(username)
);
```

> ⚠️ The supplied archive does **not** include these migrations.

### 7.5 Password storage

Never store plaintext passwords.

```text
Submitted password → PasswordEncoder.matches() → stored password hash → authentication result
```

> 💡 A password hash is **not** encryption.

### 7.6 WebAuthn and passkeys

```kotlin
.webAuthn { x ->
    x.rpId("localhost")
        .rpName("localhost")
        .allowedOrigins("http://localhost:8080")
}
```

WebAuthn uses **asymmetric cryptography**:

```text
Authenticator
    ├── Private key  (stays protected by the authenticator)
    └── Public key   (stored by the server, with credential metadata)
```

#### Registration

```text
User → server generates challenge → browser WebAuthn API → authenticator
  → credential created → public key returned → server validates → credential persisted
```

The private key never needs to be sent to the server.

#### Authentication

```text
User → server challenge → browser → authenticator → private key signs
  → assertion returned → server verifies → authenticated
```

The challenge helps prevent **replay**.

#### `rpId` — Relying Party ID

`.rpId("localhost")` — RP means **Relying Party**. The RP ID defines the web-domain scope of the credential and contributes to WebAuthn's **phishing resistance**.

#### `allowedOrigins`

`.allowedOrigins("http://localhost:8080")` — an origin is **scheme + host + port**, so these are different origins:

```text
http://localhost:8080
http://localhost:3000
```

> 🏭 Production config must match the real **HTTPS** deployment.

#### Credential storage

Production storage can include: credential ID, public key, user association, signature counter/state, credential metadata.

> ⚠️ The project enables WebAuthn but does **not** provide a complete production credential-storage/migration design.

### 7.7 One-time-token (OTT) authentication

```text
Login request → generate random token → associate with user → deliver token
  → user presents token → validate → consume → authenticated session
```

A production token should be: **unpredictable · short-lived · purpose-bound · single-use · protected from disclosure · rate-limited**.

#### Why the demo prints the token

The demo prints the auth URL to the console — convenient for a conference demo, **inappropriate in production**, because logs may be visible to developers, operators, centralized logging, monitoring systems, and third parties with log access.

> 💡 An authentication token is effectively a credential.

#### Token-in-URL risks

The demo uses `/login/ott?token=...`. URLs can appear in browser history, access logs, monitoring, proxy logs, and referrer-related contexts. Production systems should control: token lifetime, logging, referrer behavior, token consumption, post-login redirects.

### 7.8 `SecurityContextHolder`

```kotlin
val name = SecurityContextHolder
    .getContext()
    .authentication!!
    .name
```

```text
SecurityContext
    └── Authentication
          ├── Principal
          ├── Authorities
          ├── Authentication state
          └── Details
```

#### Security context and concurrency

The servlet security model ties the security context to the **current execution/thread context**. Don't assume async tasks inherit it:

```kotlin
executor.submit {
    // Do not blindly assume original authentication exists.
}
```

This matters with executors, coroutines, background jobs, message consumers, and reactive pipelines.

> 💡 Security-context propagation must be **deliberate**.

#### Kotlin `!!`

`.authentication!!` means: *treat this as non-null and throw at runtime if it is null.* A more explicit form:

```kotlin
val authentication =
    requireNotNull(
        SecurityContextHolder
            .getContext()
            .authentication
    ) {
        "Authentication is required"
    }
```

In MVC controllers, injecting the authentication/principal as a controller argument also reduces coupling to `SecurityContextHolder`.

---

## Part 8: Routing and Concurrency

### 8.1 Functional routing vs annotation MVC

```kotlin
// Annotation style
@GetMapping("/ask")
fun ask(...)

// Functional style
router {
    GET("/") {
        ServerResponse.ok().body(...)
    }
}
```

Both can run on Spring MVC. **Functional routing does not automatically mean reactive execution.**

| Concern | Annotation MVC | Functional MVC |
|---|---|---|
| Route declaration | Annotations | Kotlin DSL |
| Handler | Controller method | Function |
| Runtime | Servlet MVC | Servlet MVC |
| Blocking operations | Supported | Supported |
| Virtual threads | Compatible | Compatible |
| Reactive by default | No | No |

### 8.2 Virtual threads

```properties
spring.threads.virtual.enabled=true
```

Virtual threads are lightweight, JVM-managed threads that make thread-per-task programming scalable for **blocking I/O**.

**Traditional blocking model:**

```text
HTTP Request → Platform Thread → Database → thread waits → Response
```

**Virtual-thread model:**

```text
Request A → Virtual Thread ┐
Request B → Virtual Thread ├─▶ JVM scheduling ─▶ Carrier threads
Request C → Virtual Thread │
Request D → Virtual Thread ┘
```

Many waiting tasks can be represented efficiently.

#### Why they help `/ask`

If inference takes 8 seconds, the request spends most of that time waiting on an external service. Virtual threads make that waiting cheaper from the application-thread perspective.

> ⚠️ Virtual threads ≠ faster inference. They don't add GPU capacity.

#### Virtual threads don't remove bottlenecks

```text
HTTP concurrency → DB pool → PostgreSQL

HTTP concurrency → AI concurrency → Ollama → CPU/GPU
```

They improve the **application concurrency layer**, not downstream capacity.

#### Little's Law

**`L = λ × W`**

- `L` = average in-flight work
- `λ` = throughput
- `W` = average time in system

Example: 10 requests/second × 8 seconds average latency → **`L = 10 × 8 = 80`** requests in flight on average (computing or queued).

> 💡 Slow dependencies increase in-flight concurrency.

#### Virtual threads vs coroutines vs WebFlux

| Model | Abstraction | Style |
|---|---|---|
| Virtual threads | JVM threads | Synchronous / blocking-looking |
| Coroutines | Suspendable computations | `suspend` |
| WebFlux | Reactive streams | `Mono`, `Flux` |

For this app, **servlet MVC + virtual threads is coherent** because JDBC and AI calls are blocking.

---

## Part 9: Observability and Testing

### 9.1 OpenTelemetry

The app includes OpenTelemetry support. Three signals: **Traces · Metrics · Logs**.

**`GET /dogs` trace**

```text
Trace: GET /dogs
├── HTTP Server Span
├── Controller
├── JDBC
│   └── PostgreSQL
└── Template rendering
```

**`GET /ask` trace**

```text
Trace: GET /ask
├── HTTP Server Span
├── ChatClient
└── Ollama HTTP Request
    └── Model inference
```

This helps locate where latency originates.

#### Trace IDs

```text
API Gateway  (trace_id=abc123)
    ▼
Spring Boot  (trace_id=abc123)
    ▼
Model Service (trace_id=abc123)
    ▼
Ollama
```

Correlation makes cross-service debugging practical.

#### Four golden signals

| Signal | Example |
|---|---|
| Latency | HTTP p95 |
| Traffic | Requests/second |
| Errors | Error rate |
| Saturation | DB pool utilization |

Also measure for this app: AI inference duration · AI queue time · AI failure rate · DB query duration · DB connection acquisition · authentication failures · request concurrency · model throughput · token usage (where available).

#### Why averages aren't enough

```text
100 ms, 110 ms, 120 ms, 130 ms, 10,000 ms
```

The average hides the severe tail. Track **p50 / p95 / p99** — tail latency matters most when requests depend on multiple downstream systems.

### 9.2 Testing

The supplied test:

```kotlin
@SpringBootTest
class AgentApplicationTests {

    @Test
    fun contextLoads() {
    }
}
```

The empty test means **successful context startup is the only assertion**.

#### What `@SpringBootTest` does *not* prove

`/dogs` correctness · SQL correctness · AI output correctness · authorization · WebAuthn · token expiry · error handling · concurrency safety · load behavior.

#### The startup-test problem

Because startup runs a real AI request:

```text
@SpringBootTest → application startup → ApplicationRunner → Ollama → model request
```

the test can fail just because Ollama is unavailable — **environmental coupling**.

#### Testing pyramid

```text
             ┌────────────────────┐
             │  End-to-End Tests  │
             └──────────┬─────────┘
                        │
               ┌────────▼────────┐
               │ Integration     │
               │ Tests           │
               └────────┬────────┘
                        │
               ┌────────▼────────┐
               │ Slice Tests     │
               └────────┬────────┘
                        │
               ┌────────▼────────┐
               │ Unit Tests      │
               └─────────────────┘
```

| Level | Use for |
|---|---|
| Unit | Business rules |
| Slice | MVC / JDBC / security pieces |
| Integration | Real PostgreSQL and controlled dependencies |
| End-to-end | Full HTTP behavior |

> 🏭 **Testcontainers** is useful for PostgreSQL integration tests.

---

## Part 10: README vs Reality

Based on the supplied archive:

| Feature | Status |
|---|---|
| Kotlin / Spring Boot | ✅ Implemented |
| Spring Data JDBC | ✅ Implemented |
| PostgreSQL datasource | ✅ Configured |
| JTE dog template | ✅ Implemented |
| `/dogs` | ✅ Implemented |
| `/ask` | ✅ Implemented |
| Ollama configuration | ✅ Present |
| `BeanRegistrarDsl` | ✅ Implemented |
| Virtual threads | ✅ Enabled |
| JDBC user manager | ✅ Registered |
| WebAuthn configuration | ✅ Present |
| One-time-token configuration | ✅ Present |
| Authenticated `/` | ✅ Implemented |
| Exposed persistence implementation | ⚠️ Commented dependency only |
| `Dogs` abstraction | ❌ Not implemented |
| Rich AI tools/skills | ❌ Not implemented |
| Database migrations | ❌ Not supplied |
| Docker Compose | ❌ Not supplied |
| Production observability deployment | ❌ Not supplied |
| Comprehensive tests | ❌ Not supplied |

> ⚠️ The GraalVM native-image plugin being present does **not** prove that a production native executable has been built and verified.

---

## Part 11: Production Gaps

These are not necessarily mistakes for a conference demo. They matter in production.

### 11.1 Everything in one file

Bootstrap, persistence, security, AI configuration, startup logic, routing, and the controller are all together. Convenient for a talk; increases coupling.

### 11.2 Controller depends directly on infrastructure

```text
Current:                      More scalable:
Controller                    Controller
 ├── ChatClient                  ↓
 └── DogRepository            Application Service
                                 ↓
                              Domain / Ports
                                 ↓
                              Infrastructure
```

This pays off with validation, authorization, transactions, caching, retries, and business rules.

### 11.3 `GET` for AI generation

```kotlin
@GetMapping("/ask")
fun ask(@RequestParam question: String)
```

Prefer:

```http
POST /api/assistant/messages
Content-Type: application/json
```

```json
{
  "message": "Explain distributed transactions."
}
```

Query parameters can appear in browser history and infrastructure logs. POST doesn't make data private by itself, but it keeps the prompt out of the URL.

### 11.4 No prompt limits

Large prompts increase tokenization cost, context usage, latency, memory, and concurrent workload. Enforce limits. Character count ≠ token count, so model-specific **token budgets** may also matter.

### 11.5 No explicit AI concurrency control

Virtual threads don't protect Ollama from overload.

```kotlin
private val inferencePermits =
    Semaphore(4)
```

(Illustrative only — the real limit should reflect actual inference capacity.) Without a bound you get queues, long tail latency, timeouts, memory pressure, and cascading failures.

### 11.6 No explicit failure handling

Failure modes to handle: PostgreSQL unavailable · Ollama unavailable · DB timeout · model timeout · pool exhausted · invalid input · authentication failure · authorization failure.

> 🏭 Distinguish client errors from server/dependency failures, and don't expose internal details.

### 11.7 Hard-coded credentials

```properties
spring.datasource.username=myuser
spring.datasource.password=secret
```

Basic externalization:

```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Production should use a proper **secret-management** mechanism.

### 11.8 No database migrations

```text
Application v1 → Schema v1
Application v2 → Schema v2
Database       → maybe still Schema v1
```

Use **Flyway** or **Liquibase** to version schema changes.

---

## Part 12: Target Architecture

### 12.1 A clean production version can stay a monolith

**Outside the app:**

```text
                         ┌────────────────────┐
                         │     Browser/API    │
                         └─────────┬──────────┘
                                   │
                                   ▼
                         ┌────────────────────┐
                         │   Load Balancer    │
                         └─────────┬──────────┘
                                   │
                 ┌─────────────────┼─────────────────┐
                 │                 │                 │
                 ▼                 ▼                 ▼
              App 1             App 2             App 3
                 │                 │                 │
                 └─────────────────┼─────────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                    ▼                             ▼
               PostgreSQL                      AI Service
                                                  │
                                                  ▼
                                               Ollama
                                                  │
                                                  ▼
                                               Model
```

**Inside each instance:**

```text
                     HTTP
                       │
                       ▼
              ┌─────────────────┐
              │ Security Layer  │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Web Controllers │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Application     │
              │ Services        │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Domain / Ports  │
              └───────┬─────────┘
                     / \
                    /   \
                   ▼     ▼
            JDBC Adapter  AI Adapter
                │             │
                ▼             ▼
           PostgreSQL       Ollama
```

This gives **boundaries without prematurely introducing microservices**.

### 12.2 Proposed package structure

```text
com.example.agent
│
├── AgentApplication.kt
│
├── dog/
│   ├── domain/
│   │   ├── Dog.kt
│   │   └── Dogs.kt
│   │
│   ├── application/
│   │   └── DogService.kt
│   │
│   ├── infrastructure/
│   │   ├── JdbcDogRepository.kt
│   │   └── SpringDataDogRepository.kt
│   │
│   └── web/
│       ├── DogController.kt
│       └── DogResponse.kt
│
├── assistant/
│   ├── application/
│   │   └── AssistantService.kt
│   │
│   ├── infrastructure/
│   │   └── SpringAiAssistant.kt
│   │
│   └── web/
│       ├── AssistantController.kt
│       └── AskRequest.kt
│
├── security/
│   └── SecurityConfiguration.kt
│
└── config/
    ├── AiConfiguration.kt
    └── PersistenceConfiguration.kt
```

Dependency direction:

```text
Web → Application → Domain / Ports ← Infrastructure
```

### 12.3 The `Dogs` port

```kotlin
interface Dogs {

    fun findAll(): List<Dog>

    fun findById(id: Long): Dog?

    fun save(dog: Dog): Dog
}
```

Application service:

```kotlin
@Service
class DogService(
    private val dogs: Dogs
) {

    fun findAll(): List<Dog> =
        dogs.findAll()
}
```

Infrastructure adapter:

```kotlin
@Repository
class JdbcDogs(
    private val repository: SpringDataDogRepository
) : Dogs {

    override fun findAll(): List<Dog> =
        repository.findAll()

    override fun findById(id: Long): Dog? =
        repository.findById(id).orElse(null)

    override fun save(dog: Dog): Dog =
        repository.save(dog)
}
```

> Illustrative — actual ID and persistence types must stay consistent.

#### Why the abstraction helps

```text
Without:  Controller → Spring Data JDBC

With:     Controller → DogService → Dogs ← JDBC implementation
```

You can replace the adapter without making the application layer depend on the persistence framework.

> ⚠️ An abstraction doesn't eliminate migration work. The replacement must preserve **transactions, query semantics, aggregate behavior, concurrency semantics, and mapping behavior**.

### 12.4 Switching to Exposed

The POM includes a **commented** Exposed dependency. Exposed is a Kotlin-oriented SQL framework.

```kotlin
DogsTable.selectAll()      // Exposed
repository.findAll()       // Spring Data JDBC
```

The architectural difference is mainly **how explicitly persistence behavior and SQL are represented**. The `Dogs` interface shields the application layer from knowing which implementation is used.

### 12.5 Designing the AI API

**DTOs** — define the HTTP contract independently of Spring AI's internal representation:

```kotlin
data class AskRequest(
    val message: String
)

data class AskResponse(
    val answer: String
)
```

**AI application port:**

```kotlin
interface AiAssistant {
    fun answer(message: String): String
}
```

Spring AI is now just an infrastructure implementation.

**Spring AI adapter:**

```kotlin
@Component
class SpringAiAssistant(
    private val chatClient: ChatClient
) : AiAssistant {

    override fun answer(message: String): String =
        requireNotNull(
            chatClient
                .prompt()
                .user(message)
                .call()
                .content()
        ) {
            "AI provider returned no content"
        }
}
```

The application depends on `AiAssistant`, not directly on `ChatClient`.

**Application service:**

```kotlin
@Service
class AssistantService(
    private val assistant: AiAssistant
) {

    fun ask(message: String): String {
        require(message.isNotBlank()) {
            "Message must not be blank"
        }

        require(message.length <= 4_000) {
            "Message is too long"
        }

        return assistant.answer(message)
    }
}
```

The numeric limit is illustrative — production limits should consider model-specific token budgets.

**HTTP controller:**

```kotlin
@RestController
@RequestMapping("/api/assistant")
class AssistantController(
    private val service: AssistantService
) {

    @PostMapping("/ask")
    fun ask(
        @RequestBody request: AskRequest
    ): AskResponse =
        AskResponse(
            answer = service.ask(request.message)
        )
}
```

Resulting dependency graph:

```text
AssistantController → AssistantService → AiAssistant ← SpringAiAssistant → ChatClient → Ollama
```

> 💡 Testing gets easier: `AiAssistant` can be replaced with a fake.

---

## Part 13: Resilience and Scaling

### 13.1 Designing for failure

External AI dependencies can fail through: connection errors · network interruption · timeouts · model unavailability · overload · process crashes · unexpected responses.

#### Timeouts

| Timeout | Meaning |
|---|---|
| Connection timeout | Time to establish a connection |
| Response / read timeout | Time waiting for a response |
| Application deadline | Maximum operation duration |

A model request can connect immediately yet take a long time to generate output.

#### Retries

Retries help with transient failures but can **amplify overload**:

```text
100 failures × 3 retries each = 300 additional requests
```

A retry policy should consider: failure type · idempotency / repeat safety · retry count · backoff · jitter · overall deadline. **Don't retry every exception.**

#### Circuit breakers

```text
CLOSED  (calls allowed)
  │  failure threshold reached
  ▼
OPEN    (calls rejected quickly)
  │  recovery interval
  ▼
HALF-OPEN
  ├── success → CLOSED
  └── failure → OPEN
```

This limits the **blast radius** of a failing dependency.

#### Bulkheads

Separate resource pools:

```text
Database        → DB connection pool
AI              → AI concurrency limiter
Authentication  → Security infrastructure
```

AI overload should not consume every resource that normal CRUD requests need.

### 13.2 Scaling the application

**Simple deployment:**

```text
Browser → Spring Boot ─┬─▶ PostgreSQL
                       └─▶ Ollama
```

**Scaled deployment:**

```text
              Load Balancer
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
        App 1     App 2     App 3
          │         │         │
          └─────────┼─────────┘
                    │
             ┌──────┴──────┐
             ▼             ▼
        PostgreSQL       Ollama
```

#### DB connections multiply

```text
20 replicas × 10 DB connections each = 200 potential connections
```

Pool sizing must account for the **total replica count**.

#### AI capacity doesn't automatically scale

Adding App 1/2/3 doesn't add GPU capacity if every replica calls one Ollama instance. **Application scaling and inference scaling are separate problems.**

#### Session management

```text
Request 1 → App 1
Request 2 → App 2
```

Auth/session state must be available across replicas. Options: shared session storage · an appropriate session architecture · carefully configured affinity where suitable. **WebAuthn credential storage** must also work across replicas.

#### Database migrations during deployment (expand-and-contract)

```text
1. Add backward-compatible schema
2. Deploy new application
3. Migrate / backfill data
4. Stop using old schema
5. Remove obsolete schema later
```

Old and new app versions can coexist during rolling deployments.

---

## Part 14: Request Walkthroughs

### 14.1 `GET /dogs`

```http
GET /dogs HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=...
```

| Step | Component | Responsibility |
|---|---|---|
| 1 | Servlet server | Accept HTTP connection |
| 2 | Security filters | Process authentication |
| 3 | Authorization | Decide access |
| 4 | DispatcherServlet | Coordinate MVC |
| 5 | HandlerMapping | Locate `/dogs` |
| 6 | Controller | Invoke repository |
| 7 | Repository proxy | Dispatch persistence |
| 8 | JDBC | Acquire connection |
| 9 | PostgreSQL | Execute query |
| 10 | JDBC mapping | Build `Dog` objects |
| 11 | Controller | Build `ModelAndView` |
| 12 | JTE | Render HTML |
| 13 | Servlet server | Write response |

### 14.2 `GET /ask`

```text
HTTP Request → Security → MVC argument resolution → question → AssistantController
  → ChatClient → Ollama HTTP request → model inference → generated response
  → Spring AI → Controller → HTTP response
```

Two concurrency domains exist: **application concurrency** and **inference concurrency**. Measure and control them independently.

---

## Part 15: Checklist and Principles

### 15.1 Production improvement checklist

| Area | Improvement |
|---|---|
| Architecture | Extract application services and infrastructure adapters |
| Database | Add versioned migrations |
| Persistence | Add pagination |
| Transactions | Define explicit transaction boundaries |
| Authentication | Configure durable credential/session storage |
| Secrets | Externalize credentials |
| AI API | Prefer validated POST contract |
| AI reliability | Add timeouts |
| AI reliability | Add bounded concurrency |
| AI reliability | Add controlled retries |
| AI reliability | Consider circuit breakers |
| Startup | Remove unconditional AI inference from startup |
| Observability | Configure exporters, metrics, traces |
| Testing | Add unit tests |
| Testing | Add MVC tests |
| Testing | Add repository tests |
| Testing | Add security tests |
| Testing | Add integration tests |
| Deployment | Add health/readiness checks |
| Deployment | Configure resource limits |
| Scaling | Account for DB connection multiplication |

### 15.2 The deeper engineering principles

| # | Principle | Essence |
|---|---|---|
| 1 | **Dependency inversion** | Application → interface ← infrastructure. Behavior depends on contracts, not specific infrastructure. |
| 2 | **Security at the boundary** | Enforce authn/authz coherently at the security boundary: Request → Security Boundary → Application. |
| 3 | **Concurrency ≠ capacity** | Virtual threads make waiting more efficient; they don't increase PostgreSQL capacity, GPU capacity, model throughput, or network bandwidth. |
| 4 | **Persistence models shape domain models** | Spring Data JDBC's aggregate orientation encourages explicit consistency boundaries. |
| 5 | **External dependencies define reliability** | Assume PostgreSQL, Ollama, networks, and auth infrastructure can fail or slow down. |
| 6 | **Abstractions have runtime consequences** | `@Transactional`, `@GetMapping`, `@Service` hide real mechanisms: proxying, DI, request dispatch, transactions, security filters, repository generation, context propagation. |

Understanding those mechanisms is what turns **framework familiarity** into **system-design understanding**.

### 15.3 The most important mental model

When reading a Spring application, don't stop at *"this annotation does X."* Ask:

```text
Who creates this object?
        ↓
Who owns its lifecycle?
        ↓
Who intercepts the call?
        ↓
What thread executes it?
        ↓
What resource does it consume?
        ↓
What external dependency does it call?
        ↓
What happens when that dependency is slow?
        ↓
What happens when it fails?
        ↓
What happens under 1 request?
        ↓
What happens under 1,000 requests?
        ↓
What happens when there are 20 replicas?
        ↓
What state must be shared?
        ↓
What can become a bottleneck?
        ↓
How do we observe it?
        ↓
How do we test it?
```

That is the difference between learning **Spring APIs** and learning **system design**.

### 15.4 The architectural chain of this project

```text
Kotlin
   │
   ▼
Spring Boot
   │
   ├───────────────┬───────────────────┐
   ▼               ▼                   ▼
Spring MVC    Spring Security      Spring Data JDBC
   │               │                   │
   ▼               ▼                   ▼
HTTP          Authentication       PostgreSQL
                   │
                   ▼
              SecurityContext

Spring MVC
   │
   ▼
Spring AI
   │
   ▼
Ollama
   │
   ▼
Gemma

JVM
   │
   ▼
Virtual Threads
   │
   ▼
Efficient blocking I/O concurrency

OpenTelemetry
   │
   ▼
Observability across the system
```

Once you can mentally trace a request through **all of those layers** — including the resources consumed at each layer and the failure modes between them — you are no longer merely reading the code; you are **reasoning about the system**.

---

## Conclusion: Purpose of This Guide

This document exists to **summarize and study** the [Bootiful Kotlin repository](https://github.com/joshlong-attic/2026-05-21-kotlin-conf-bootiful-kotlin) — its **concepts, code, and architecture**.

It condenses a deep code walkthrough (Spring Boot 4, Kotlin, Spring Data JDBC, JTE, Spring AI + Ollama, Spring Security, virtual threads, observability) into a structured reference, and ties each piece to system-design thinking. Use it to:

- Understand **what** the demo code does and **why** it is written that way
- Trace a request through every layer, with the resources consumed and failure modes at each step
- Separate what the repo **implements** from what it only **sketches** (Part 10)
- Know what to change to move from **conference demo** to **production system** (Parts 11–15)

> 📦 **Source repo:** <https://github.com/joshlong-attic/2026-05-21-kotlin-conf-bootiful-kotlin>
