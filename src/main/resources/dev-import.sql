-- src/main/resources/dev-import.sql
-- Create test environment for blog posts about Java and Distributed Systems
-- Includes published posts and draft posts

-- ============================================
-- 1. Limpar dados existentes (opcional - para desenvolvimento)
-- ============================================
TRUNCATE TABLE tb_posts CASCADE;
TRUNCATE TABLE tb_images CASCADE;

-- ============================================
-- 2. Inserir imagens de exemplo
-- ============================================

INSERT INTO tb_images(uuid, filename, content_type, size, file_path, url, active, created_at) 
VALUES 
(
    'img-distributed-systems-001',
    'distributed-systems-cover.jpg',
    'image/jpeg',
    245678,
    '/tmp/contraponto-images/distributed-systems-cover.jpg',
    'https://picsum.photos/id/1/800/600',
    TRUE,
    '2023-01-15 08:00:00'
),
(
    'img-microservices-002',
    'microservices-patterns-cover.jpg',
    'image/jpeg',
    189432,
    '/tmp/contraponto-images/microservices-patterns-cover.jpg',
    'https://picsum.photos/id/2/800/600',
    TRUE,
    '2023-02-20 08:00:00'
),
(
    'img-kafka-003',
    'apache-kafka-cover.jpg',
    'image/jpeg',
    312567,
    '/tmp/contraponto-images/apache-kafka-cover.jpg',
    'https://picsum.photos/id/3/800/600',
    TRUE,
    '2023-04-05 08:00:00'
),
(
    'img-saga-004',
    'saga-pattern-cover.jpg',
    'image/jpeg',
    278943,
    '/tmp/contraponto-images/saga-pattern-cover.jpg',
    'https://picsum.photos/id/4/800/600',
    TRUE,
    '2023-06-12 08:00:00'
),
(
    'img-kubernetes-005',
    'kubernetes-cover.jpg',
    'image/jpeg',
    423156,
    '/tmp/contraponto-images/kubernetes-cover.jpg',
    'https://picsum.photos/id/5/800/600',
    TRUE,
    '2023-08-18 08:00:00'
),
(
    'img-grpc-006',
    'grpc-performance-cover.jpg',
    'image/jpeg',
    198765,
    '/tmp/contraponto-images/grpc-performance-cover.jpg',
    'https://picsum.photos/id/6/800/600',
    TRUE,
    '2023-10-25 08:00:00'
),
(
    'img-observability-007',
    'observability-cover.jpg',
    'image/jpeg',
    345678,
    '/tmp/contraponto-images/observability-cover.jpg',
    'https://picsum.photos/id/7/800/600',
    TRUE,
    '2023-12-02 08:00:00'
),
(
    'img-loom-008',
    'virtual-threads-cover.jpg',
    'image/jpeg',
    234567,
    '/tmp/contraponto-images/virtual-threads-cover.jpg',
    'https://picsum.photos/id/8/800/600',
    TRUE,
    '2024-01-20 08:00:00'
),
(
    'img-draft-001',
    'draft-cover-1.jpg',
    'image/jpeg',
    123456,
    '/tmp/contraponto-images/draft-cover-1.jpg',
    'https://picsum.photos/id/9/800/600',
    TRUE,
    '2024-03-10 08:00:00'
),
(
    'img-draft-002',
    'draft-cover-2.jpg',
    'image/jpeg',
    234567,
    '/tmp/contraponto-images/draft-cover-2.jpg',
    'https://picsum.photos/id/10/800/600',
    TRUE,
    '2024-04-01 08:00:00'
);

-- ============================================
-- 3. Inserir posts publicados
-- ============================================

DO $$
DECLARE
    v_image_id_1 BIGINT;
    v_image_id_2 BIGINT;
    v_image_id_3 BIGINT;
    v_image_id_4 BIGINT;
    v_image_id_5 BIGINT;
    v_image_id_6 BIGINT;
    v_image_id_7 BIGINT;
    v_image_id_8 BIGINT;
    v_draft_img_1 BIGINT;
    v_draft_img_2 BIGINT;
BEGIN
    -- Buscar IDs das imagens
    SELECT id INTO v_image_id_1 FROM tb_images WHERE uuid = 'img-distributed-systems-001';
    SELECT id INTO v_image_id_2 FROM tb_images WHERE uuid = 'img-microservices-002';
    SELECT id INTO v_image_id_3 FROM tb_images WHERE uuid = 'img-kafka-003';
    SELECT id INTO v_image_id_4 FROM tb_images WHERE uuid = 'img-saga-004';
    SELECT id INTO v_image_id_5 FROM tb_images WHERE uuid = 'img-kubernetes-005';
    SELECT id INTO v_image_id_6 FROM tb_images WHERE uuid = 'img-grpc-006';
    SELECT id INTO v_image_id_7 FROM tb_images WHERE uuid = 'img-observability-007';
    SELECT id INTO v_image_id_8 FROM tb_images WHERE uuid = 'img-loom-008';
    SELECT id INTO v_draft_img_1 FROM tb_images WHERE uuid = 'img-draft-001';
    SELECT id INTO v_draft_img_2 FROM tb_images WHERE uuid = 'img-draft-002';

    -- Inserir posts publicados (alternando MARKDOWN e ASCIIDOC)
    -- Post 1: MARKDOWN
    INSERT INTO tb_posts(slug, title, author_id, description, content, format, published, created_at, updated_at, published_at, cover_id) 
    VALUES 
    (
        'introduction-to-distributed-systems-java', 
        'Introduction to Distributed Systems with Java', 
        1,  
        'Learn the fundamental concepts of distributed systems and how Java technologies enable building scalable, fault-tolerant applications.', 
        '## Understanding Distributed Systems in the Java Ecosystem

Distributed systems have become the backbone of modern software architecture. In this post, we explore the core principles that make distributed systems both powerful and challenging.

> "The art of distributed systems is not about avoiding failures, but about designing systems that can gracefully handle them."

### Why Java for Distributed Systems?

Java''s mature ecosystem provides unparalleled tools for distributed computing. From built-in networking capabilities to enterprise-grade frameworks, Java remains a top choice for building resilient distributed applications.

### Key Concepts Covered

- Network transparency and location transparency
- Consistency models (strong vs. eventual consistency)
- Fault tolerance strategies
- The CAP theorem in practice

### Real-World Example

Consider a simple key-value store distributed across three nodes. We''ll explore how different consistency models affect performance and availability.

```java
// Example: Distributed counter with eventual consistency
public class DistributedCounter {
    private final Map<String, Integer> localState = new ConcurrentHashMap<>();
    
    public void increment(String key) {
        localState.merge(key, 1, Integer::sum);
        replicateToPeers(key);
    }
    
    public int get(String key) {
        return localState.getOrDefault(key, 0);
    }
}
```

Stay tuned for hands-on examples using Java RMI, Spring Cloud, and Apache Kafka!', 
        'MARKDOWN',
        TRUE, 
        '2023-01-15 09:00:00', 
        '2023-01-15 09:00:00', 
        '2023-01-15 09:00:00',
        v_image_id_1
    ),
    -- Post 2: ASCIIDOC
    (
        'microservices-patterns-spring-boot', 
        'Essential Microservices Patterns with Spring Boot', 
        1,
        'Explore battle-tested microservices patterns and learn how to implement them using Spring Boot and Spring Cloud.', 
        '== Designing Robust Microservices with Spring Ecosystem

Microservices architecture offers tremendous flexibility but introduces complexity. This guide walks through proven patterns that address common distributed system challenges.

> "Microservices are not a silver bullet - they require disciplined implementation of proven patterns."

=== Patterns We''ll Cover

- *API Gateway Pattern* - Using Spring Cloud Gateway
- *Service Discovery* - Netflix Eureka integration
- *Circuit Breaker* - Resilience4j implementation
- *Distributed Tracing* - Sleuth and Zipkin
- *Configuration Management* - Spring Cloud Config Server

=== Implementation Example

[source,java]
----
// Circuit Breaker example with Resilience4j
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

public PaymentResponse fallbackPayment(PaymentRequest request, Exception e) {
    return new PaymentResponse("PENDING", "Payment queued for retry");
}
----

=== Real-world Example

We''ll build a simple e-commerce system demonstrating how these patterns work together to create a resilient, scalable application. Code examples included!', 
        'ASCIIDOC',
        TRUE, 
        '2023-02-20 14:30:00', 
        '2023-02-25 11:15:00', 
        '2023-02-20 14:30:00',
        v_image_id_2
    ),
    -- Post 3: MARKDOWN
    (
        'apache-kafka-spring-boot-tutorial', 
        'Building Event-Driven Systems with Apache Kafka and Spring Boot', 
        1,
        'A comprehensive guide to implementing event-driven architectures using Kafka and Spring Boot for real-time data processing.', 
        '## Event-Driven Architecture: The Future of Scalable Systems

Event-driven architecture enables loose coupling and high scalability. Apache Kafka combined with Spring Boot provides a powerful stack for building reactive systems.

### What You''ll Learn

- Kafka core concepts: topics, partitions, brokers
- Spring Kafka configuration and best practices
- Producer and consumer implementations
- Error handling and exactly-once semantics
- Schema management with Avro and Schema Registry

### Producer Example

```java
@Service
public class OrderEventProducer {
    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    public void sendOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(order.getId(), "CREATED", Instant.now());
        kafkaTemplate.send("orders", order.getId().toString(), event);
    }
}
```

### Consumer Example

```java
@KafkaListener(topics = "orders", groupId = "payment-service")
public void processOrder(OrderEvent event) {
    logger.info("Processing order: {}", event);
    // Process payment logic here
}
```

### Hands-on Project

We''ll build a real-time order processing system that demonstrates event sourcing and CQRS patterns. Complete source code available on GitHub.', 
        'MARKDOWN',
        TRUE, 
        '2023-04-05 10:00:00', 
        '2023-04-10 16:45:00', 
        '2023-04-05 10:00:00',
        v_image_id_3
    ),
    -- Post 4: ASCIIDOC
    (
        'distributed-transactions-saga-pattern', 
        'Distributed Transactions: Implementing the Saga Pattern', 
        1,
        'Learn how to maintain data consistency across microservices using the Saga pattern with practical Java implementations.', 
        '== Solving the Distributed Transaction Problem

Traditional ACID transactions don''t work across microservices. The Saga pattern offers a pragmatic approach to maintaining data consistency in distributed systems.

> "In distributed systems, we trade immediate consistency for eventual consistency and resilience."

=== Saga Coordination Approaches

- *Choreography-based Saga* - Event-driven coordination
- *Orchestration-based Saga* - Centralized control with compensation logic

=== Orchestration Example

[source,java]
----
@Component
public class TravelBookingSaga {
    @Autowired
    private FlightService flightService;
    @Autowired
    private HotelService hotelService;
    @Autowired
    private CarRentalService carRentalService;
    
    @Saga
    public BookingResponse bookTrip(TripRequest request) {
        try {
            FlightBooking flight = flightService.book(request.getFlight());
            HotelBooking hotel = hotelService.book(request.getHotel());
            CarBooking car = carRentalService.book(request.getCar());
            
            return new BookingResponse(flight, hotel, car);
        } catch (Exception e) {
            // Compensation logic
            compensate(flight, hotel, car);
            throw new SagaException("Booking failed", e);
        }
    }
}
----

=== Implementation Examples

We''ll explore both approaches using Spring Boot, with detailed code examples showing compensation strategies, idempotency, and monitoring. The examples include a travel booking system that coordinates flight, hotel, and car rental services.

=== Best Practices

Learn about timeout handling, retry mechanisms, and monitoring strategies to make your sagas production-ready.', 
        'ASCIIDOC',
        TRUE, 
        '2023-06-12 11:00:00', 
        '2023-06-15 09:30:00', 
        '2023-06-12 11:00:00',
        v_image_id_4
    ),
    -- Post 5: MARKDOWN
    (
        'kubernetes-java-microservices-deployment', 
        'Deploying Java Microservices on Kubernetes: A Complete Guide', 
        1,
        'Master the art of deploying and managing Spring Boot microservices in Kubernetes clusters with production-grade configurations.', 
        '## From Development to Production: Java on Kubernetes

Kubernetes has become the de facto standard for container orchestration. This guide shows how to effectively deploy Java microservices on Kubernetes with proper configuration and observability.

### Key Topics

- Containerizing Spring Boot applications with JVM optimizations
- Kubernetes manifests: Deployments, Services, ConfigMaps, Secrets
- Health checks and readiness probes for Java applications
- Horizontal Pod Autoscaling based on custom metrics
- Canary deployments and blue-green strategies

### Sample Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
      - name: payment-service
        image: payment-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-XX:MaxRAMPercentage=75.0"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

### Production Considerations

We''ll cover memory management for JVM in containers, logging aggregation with EFK stack, and monitoring with Prometheus and Grafana.', 
        'MARKDOWN',
        TRUE, 
        '2023-08-18 08:00:00', 
        '2023-08-22 14:20:00', 
        '2023-08-18 08:00:00',
        v_image_id_5
    ),
    -- Post 6: ASCIIDOC
    (
        'grpc-java-performance-comparison', 
        'gRPC vs REST: Performance Analysis in Java Microservices', 
        1,
        'Deep dive into gRPC performance characteristics compared to traditional REST APIs, with benchmarking results and implementation guides.', 
        '== Is gRPC Worth the Hype? Let''s Measure It

gRPC promises better performance than REST, but what does that mean in practice? We conducted extensive benchmarks comparing gRPC and REST in Java microservices.

=== Benchmark Results

- Throughput comparison under varying load
- Latency analysis at the 99th percentile
- Memory and CPU utilization
- Network bandwidth efficiency with Protocol Buffers

=== gRPC Service Definition

[source,protobuf]
----
// product.proto
syntax = "proto3";

service ProductService {
    rpc GetProduct(ProductRequest) returns (ProductResponse);
    rpc ListProducts(Empty) returns (stream ProductResponse);
}

message ProductRequest {
    string id = 1;
}

message ProductResponse {
    string id = 1;
    string name = 2;
    double price = 3;
}
----

=== Java Implementation

[source,java]
----
@GrpcService
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        Product product = productRepository.findById(request.getId());
        ProductResponse response = ProductResponse.newBuilder()
            .setId(product.getId())
            .setName(product.getName())
            .setPrice(product.getPrice())
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
----

=== When to Choose What

Practical recommendations on when gRPC makes sense and when traditional REST/GraphQL might be better suited for your use case.', 
        'ASCIIDOC',
        TRUE, 
        '2023-10-25 13:00:00', 
        '2023-10-28 10:15:00', 
        '2023-10-25 13:00:00',
        v_image_id_6
    ),
    -- Post 7: MARKDOWN
    (
        'observability-java-distributed-systems', 
        'Observability in Java Distributed Systems: Metrics, Logs, and Traces', 
        1,
        'Implement comprehensive observability for your Java microservices using OpenTelemetry, Prometheus, and Grafana.', 
        '## Understanding Distributed System Behavior

Observability is crucial for operating distributed systems. This post explores how to instrument Java applications for metrics, structured logging, and distributed tracing.

> "Observability isn''t just about monitoring - it''s about understanding why your system behaves the way it does."

### Three Pillars of Observability

- **Metrics** - Micrometer and Prometheus integration
- **Logging** - Structured logging with JSON and correlation IDs
- **Tracing** - OpenTelemetry with Jaeger/Zipkin

### OpenTelemetry Configuration

```java
@Configuration
public class ObservabilityConfig {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(
                            JaegerGrpcSpanExporter.builder()
                                .setEndpoint("http://jaeger:14250")
                                .build()
                        ).build()
                    )
                    .build()
            )
            .build();
    }
}
```

### Custom Metrics Example

```java
@Service
public class PaymentService {
    private final Counter paymentCounter;
    private final Timer paymentTimer;
    
    public PaymentService(MeterRegistry registry) {
        this.paymentCounter = Counter.builder("payments.total")
            .description("Total number of payments")
            .register(registry);
        this.paymentTimer = Timer.builder("payments.duration")
            .description("Payment processing duration")
            .register(registry);
    }
    
    public void processPayment(Payment payment) {
        paymentTimer.record(() -> {
            // Process payment logic
            paymentCounter.increment();
        });
    }
}
```

### Unified Observability Stack

We''ll build a complete observability pipeline: Spring Boot applications exporting telemetry data to OpenTelemetry collector, visualized in Grafana with Prometheus for metrics and Tempo for traces.

### Practical Alerting

Learn to set up meaningful alerts based on SLOs and SLIs to proactively detect issues before they impact users.', 
        'MARKDOWN',
        TRUE, 
        '2023-12-02 15:30:00', 
        '2024-01-05 11:45:00', 
        '2023-12-02 15:30:00',
        v_image_id_7
    ),
    -- Post 8: ASCIIDOC
    (
        'virtual-threads-project-loom', 
        'Project Loom: Virtual Threads Revolutionizing Java Concurrency', 
        1,
        'Explore how virtual threads in Java 21 are changing the game for concurrent programming in distributed systems.', 
        '== Concurrency Made Simple: Virtual Threads in Practice

Java 21 introduced virtual threads (Project Loom) as a preview feature, now finalized. This revolutionary feature dramatically simplifies concurrent programming while improving resource utilization.

> "Virtual threads bring back the simplicity of thread-per-request without the overhead."

=== What Are Virtual Threads?

Unlike platform threads that map 1:1 to OS threads, virtual threads are lightweight, managed by the JVM, enabling millions of concurrent tasks with minimal overhead.

=== Traditional vs Virtual Threads

[source,java]
----
// Traditional platform threads (heavy)
ExecutorService executor = Executors.newFixedThreadPool(200);
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> {
        // Handle request
        Thread.sleep(1000); // Blocks platform thread
    });
}

// Virtual threads (lightweight)
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
for (int i = 0; i < 100000; i++) {
    executor.submit(() -> {
        // Handle request
        Thread.sleep(1000); // Doesn''t block OS thread
    });
}
----

=== Structured Concurrency

[source,java]
----
Response handleRequest(Request request) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future<User> user = scope.fork(() -> fetchUser(request.userId()));
        Future<Order> order = scope.fork(() -> fetchOrder(request.orderId()));
        
        scope.join();
        scope.throwIfFailed();
        
        return new Response(user.resultNow(), order.resultNow());
    }
}
----

=== Impact on Distributed Systems

- Simplified code for handling many concurrent connections
- Better resource utilization in microservices
- Compatibility with existing Java frameworks
- Performance improvements in I/O-heavy workloads

=== Migration Guide

How to start using virtual threads in existing applications, common patterns, and performance benchmarks comparing traditional thread pools with virtual threads.', 
        'ASCIIDOC',
        TRUE, 
        '2024-01-20 09:00:00', 
        '2024-01-20 09:00:00', 
        '2024-01-20 09:00:00',
        v_image_id_8
    ),
    -- Post 9: MARKDOWN
    (
        'cloud-native-java-microprofile', 
        'Cloud Native Java with Eclipse MicroProfile', 
        1,
        'Explore MicroProfile specifications for building portable cloud-native Java microservices.', 
        '## MicroProfile: Java EE for the Cloud

Eclipse MicroProfile optimizes Enterprise Java for microservices architecture.

### Key Specifications

- **Config** - External configuration injection
- **Fault Tolerance** - Circuit breakers, retries, timeouts
- **Health** - Liveness and readiness probes
- **Metrics** - Prometheus-compatible metrics
- **OpenAPI** - API documentation

### Code Example

```java
@Path("/products")
@ApplicationScoped
public class ProductResource {
    
    @Inject
    @ConfigProperty(name = "max.products", defaultValue = "100")
    private int maxProducts;
    
    @GET
    @CircuitBreaker(requestVolumeThreshold = 4)
    public List<Product> getProducts() {
        // business logic
    }
}
```',
        'MARKDOWN',
        TRUE, 
        '2024-02-01 09:00:00', 
        '2024-02-01 09:00:00', 
        '2024-02-01 09:00:00',
        v_image_id_1
    ),
    -- Post 10: ASCIIDOC
    (
        'apache-pulsar-vs-kafka', 
        'Apache Pulsar vs Kafka: Which Event Streaming Platform to Choose?', 
        1,
        'Deep technical comparison between Apache Pulsar and Kafka, including architecture, performance, and use cases.', 
        '== Beyond Kafka: The Rise of Pulsar

Both platforms offer unique strengths. This post helps you decide.

=== Comparison Matrix

- *Architecture* - Pulsar separates serving and storage
- *Multi-tenancy* - Native in Pulsar
- *Geo-replication* - Both support, Pulsar simplifies
- *Queueing* - Pulsar supports both streaming and queueing

=== Performance Benchmarks

We tested both under varying loads. Results show Pulsar shines with many topics, while Kafka excels in simple throughput.', 
        'ASCIIDOC',
        TRUE, 
        '2024-02-10 10:30:00', 
        '2024-02-12 14:20:00', 
        '2024-02-10 10:30:00',
        v_image_id_3
    ),
    -- Post 11: MARKDOWN
    (
        'java-21-pattern-matching', 
        'Pattern Matching in Java 21: Switch, Instanceof, and Beyond', 
        1,
        'Learn how pattern matching simplifies code and reduces bugs with real-world examples.', 
        '## Write Safer Code with Pattern Matching

Java 21 finalizes pattern matching features that enhance expressiveness.

### Switch Pattern Matching

```java
record Point(int x, int y) {}
record Circle(Point center, int radius) {}

String describeShape(Object obj) {
    return switch(obj) {
        case null -> "null";
        case Point p -> "Point(%d,%d)".formatted(p.x(), p.y());
        case Circle c -> "Circle at (%d,%d), radius %d".formatted(c.center().x(), c.center().y(), c.radius());
        default -> "Unknown";
    };
}
```

### Guarded Patterns

Use `when` clauses for additional conditions.', 
        'MARKDOWN',
        TRUE, 
        '2024-02-18 11:00:00', 
        '2024-02-18 11:00:00', 
        '2024-02-18 11:00:00',
        v_image_id_8
    ),
    -- Post 12: ASCIIDOC
    (
        'spring-boot-3-native-docker', 
        'Spring Boot 3 Native Images with Docker: Complete Guide', 
        1,
        'Step-by-step tutorial to compile Spring Boot 3 applications into native executables and run them in Docker containers.', 
        '== From Source to Instant Startup

Spring Boot 3 with GraalVM native images reduces startup time to milliseconds.

=== Building Native Image

[source,dockerfile]
----
FROM ghcr.io/graalvm/native-image:ol9-java17-22.3 AS builder
WORKDIR /app
COPY . .
RUN ./mvnw -Pnative spring-boot:build-image
----

=== Running in Production

Measure startup time: 0.08 seconds vs 2.5 seconds for JVM mode. Memory footprint reduced by 40%.', 
        'ASCIIDOC',
        TRUE, 
        '2024-03-01 08:00:00', 
        '2024-03-03 16:30:00', 
        '2024-03-01 08:00:00',
        v_image_id_5
    ),
    -- Post 13: MARKDOWN
    (
        'resilience4j-practical-guide', 
        'Resilience4j: A Practical Guide to Fault Tolerance in Java', 
        1,
        'Hands-on examples of circuit breakers, retries, rate limiters, and bulkheads with Resilience4j.', 
        '## Build Resilient Systems Without Hystrix

Resilience4j is a lightweight fault tolerance library inspired by Netflix Hystrix.

### Circuit Breaker Example

```java
@Configuration
public class Resilience4jConfig {
    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(10000))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
        return CircuitBreaker.of("paymentService", config);
    }
}
```

### Retry with Backoff

```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(3)
    .waitDuration(Duration.ofMillis(500))
    .retryExceptions(TimeoutException.class)
    .build();
```

We''ll build a complete example of a resilient product service.', 
        'MARKDOWN',
        TRUE, 
        '2024-03-10 09:30:00', 
        '2024-03-12 13:15:00', 
        '2024-03-10 09:30:00',
        v_image_id_6
    ),
    -- Post 14: ASCIIDOC
    (
        'graalvm-polyglot-java-python', 
        'GraalVM Polyglot: Running Python, JavaScript, and Java Together', 
        1,
        'Discover how GraalVM allows seamless interoperability between JVM languages and scripting languages.', 
        '== The Polyglot Runtime

GraalVM enables Java applications to execute code written in Python, JavaScript, Ruby, and R.

=== Example: Python Script from Java

[source,java]
----
import org.graalvm.polyglot.*;

public class PolyglotDemo {
    public static void main(String[] args) {
        try (Context context = Context.create()) {
            Value result = context.eval("python", "2 + 3 * 4");
            System.out.println(result.asInt()); // prints 14
        }
    }
}
----

=== Use Cases

- *Machine Learning* - Run Python ML models from Java
- *Scripting* - Embed JavaScript for business rules
- *Migration* - Incrementally rewrite legacy systems

We''ll build a real-time sentiment analysis service that uses Python''s NLTK and Java Spring Boot.', 
        'ASCIIDOC',
        TRUE, 
        '2024-03-18 14:00:00', 
        '2024-03-20 10:45:00', 
        '2024-03-18 14:00:00',
        v_image_id_7
    ),
    -- Post 15: MARKDOWN
    (
        'observability-micrometer-prometheus', 
        'Micrometer + Prometheus: Advanced Metrics for Spring Boot', 
        1,
        'Learn to create custom metrics, export to Prometheus, and visualize in Grafana for deep insight into application behavior.', 
        '## Beyond Default Metrics

Micrometer provides a facade for metrics with support for multiple monitoring systems.

### Custom Metrics Creation

```java
@Service
public class OrderService {
    private final Counter orderCounter;
    private final Timer orderTimer;
    private final DistributionSummary orderSize;
    
    public OrderService(MeterRegistry registry) {
        orderCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .register(registry);
        orderTimer = Timer.builder("orders.processing.time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        orderSize = DistributionSummary.builder("orders.size")
            .baseUnit("bytes")
            .register(registry);
    }
}
```

### Grafana Dashboards

Create dashboards to monitor latency, throughput, and error rates. Alert on anomalies.', 
        'MARKDOWN',
        TRUE, 
        '2024-03-25 11:00:00', 
        '2024-03-27 15:30:00', 
        '2024-03-25 11:00:00',
        v_image_id_7
    ),
    -- Post 16: ASCIIDOC
    (
        'spring-cloud-gateway-jwt', 
        'Spring Cloud Gateway with JWT Authentication', 
        1,
        'Implement token-based authentication and routing using Spring Cloud Gateway and JWT.', 
        '== API Gateway as Security Enforcer

Spring Cloud Gateway can validate JWT tokens and route requests accordingly.

=== JWT Validation Filter

[source,java]
----
@Component
public class JwtAuthenticationFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        if (token != null && validateToken(token)) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
----

=== Route Configuration

[source,yaml]
----
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
          filters:
            - JwtAuthenticationFilter
----

Complete example with service discovery via Eureka included.', 
        'ASCIIDOC',
        TRUE, 
        '2024-04-01 08:00:00', 
        '2024-04-03 09:15:00', 
        '2024-04-01 08:00:00',
        v_image_id_2
    ),
    -- Post 17: MARKDOWN
    (
        'distributed-caching-java', 
        'Distributed Caching in Java: Redis, Hazelcast, and Infinispan', 
        1,
        'Compare leading caching solutions for Java microservices and learn best practices for cache invalidation.', 
        '## Caching Strategies for Scale

Distributed caching reduces latency and database load. We compare three popular solutions.

### Redis Example

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### Hazelcast Embedded

```java
@Bean
public Config hazelcastConfig() {
    Config config = new Config();
    config.setInstanceName("product-cache");
    MapConfig mapConfig = new MapConfig("products");
    mapConfig.setTimeToLiveSeconds(300);
    config.addMapConfig(mapConfig);
    return config;
}
```

### Cache Invalidation Patterns

Learn about write-through, write-behind, and cache-aside patterns with code examples.', 
        'MARKDOWN',
        TRUE, 
        '2024-04-08 10:00:00', 
        '2024-04-10 14:30:00', 
        '2024-04-08 10:00:00',
        v_image_id_4
    ),
    -- Post 18: ASCIIDOC
    (
        'java-memory-management-tuning', 
        'Java Memory Management: From Garbage Collection to Tuning', 
        1,
        'Deep dive into JVM memory areas, garbage collectors (G1, ZGC, Shenandoah), and practical tuning guidelines.', 
        '== Mastering the JVM Heap

Understanding memory management is crucial for performance.

=== Garbage Collectors Comparison

- *G1GC* - Default, good for most applications
- *ZGC* - Sub-millisecond pauses, large heaps
- *Shenandoah* - Concurrent compaction

=== Tuning Example

[source,bash]
----
-Xms4g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100
-XX:ParallelGCThreads=8
-XX:ConcGCThreads=2
----

=== Monitoring Tools

Learn jstat, jmap, and VisualVM to diagnose memory leaks and optimize GC. Sample heap dumps analysis included.', 
        'ASCIIDOC',
        TRUE, 
        '2024-04-12 09:45:00', 
        '2024-04-14 11:00:00', 
        '2024-04-12 09:45:00',
        v_image_id_5
    ),
    -- Post 19: MARKDOWN
    (
        'cdc-debezium-kafka', 
        'Change Data Capture (CDC) with Debezium and Kafka', 
        1,
        'Stream database changes in real-time using Debezium connectors and Kafka.', 
        '## Turning Database Tables into Event Streams

Debezium captures row-level changes and pushes them to Kafka.

### MySQL Source Connector

```json
{
  "name": "mysql-inventory-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",
    "database.server.name": "fullfillment",
    "table.include.list": "inventory.orders",
    "database.history.kafka.bootstrap.servers": "kafka:9092"
  }
}
```

### Consuming Change Events

```java
@KafkaListener(topics = "fullfillment.inventory.orders")
public void handleOrderChange(ConsumerRecord<String, String> record) {
    JsonNode payload = objectMapper.readTree(record.value()).path("payload");
    String op = payload.path("op").asText(); // c=create, u=update, d=delete
    // Process accordingly
}
```

Use cases: cache invalidation, search indexing, data synchronization between services.', 
        'MARKDOWN',
        TRUE, 
        '2024-04-15 13:00:00', 
        '2024-04-17 16:20:00', 
        '2024-04-15 13:00:00',
        v_image_id_3
    ),
    -- Post 20: ASCIIDOC
    (
        'testing-spring-boot-testcontainers', 
        'Integration Testing with Testcontainers in Spring Boot', 
        1,
        'Write reliable integration tests using real dependencies like databases, message brokers, and browsers with Testcontainers.', 
        '== Stop Mocking, Start Testing

Testcontainers spins up Docker containers for your integration tests.

=== Test Example

[source,java]
----
@Testcontainers
@SpringBootTest
class OrderServiceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @Test
    void testOrderCreation() {
        // real PostgreSQL and Kafka are used
    }
}
----

=== Complex Scenarios

Mock external APIs with WireMock containers, test database migrations, and more.', 
        'ASCIIDOC',
        TRUE, 
        '2024-04-20 08:30:00', 
        '2024-04-22 12:45:00', 
        '2024-04-20 08:30:00',
        v_image_id_1
    ),
    -- Post 21: MARKDOWN
    (
        'graphql-java-spring-boot', 
        'GraphQL with Java and Spring Boot: From Setup to Federation', 
        1,
        'Implement flexible APIs with GraphQL using Spring Boot and explore Apollo Federation for microservices.', 
        '## GraphQL: Query Exactly What You Need

GraphQL provides a strongly-typed, client-driven alternative to REST.

### Schema Definition

```graphql
type Product {
    id: ID!
    name: String!
    price: Float!
    reviews: [Review!]!
}

type Query {
    product(id: ID!): Product
    products(category: String): [Product!]!
}
```

### Java Resolver

```java
@Controller
public class ProductController {
    @QueryMapping
    public Product product(@Argument String id) {
        return productService.findById(id);
    }
    
    @SchemaMapping
    public List<Review> reviews(Product product) {
        return reviewService.findByProduct(product.getId());
    }
}
```

### Federation Example

Combine multiple GraphQL services into a single endpoint using Apollo Federation.', 
        'MARKDOWN',
        TRUE, 
        '2024-04-25 11:15:00', 
        '2024-04-27 15:00:00', 
        '2024-04-25 11:15:00',
        v_image_id_2
    ),
    -- Post 22: ASCIIDOC
    (
        'quarkus-superfast-java', 
        'Building Super-Fast Java Applications with Quarkus', 
        1,
        'Discover Quarkus, the Kubernetes-native Java stack that offers instant startup and low memory usage.', 
        '== Java, but Faster

Quarkus is designed for cloud-native environments, with build-time metadata processing.

=== Getting Started

[source,xml]
----
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-reactive</artifactId>
</dependency>
----

=== REST Endpoint

[source,java]
----
@Path("/hello")
public class GreetingResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus";
    }
}
----

=== Native Compilation

[source,bash]
----
./mvnw package -Pnative
----

Startup time: ~15ms, memory: ~10MB. Ideal for serverless and edge computing.', 
        'ASCIIDOC',
        TRUE, 
        '2024-04-28 09:00:00', 
        '2024-04-28 09:00:00', 
        '2024-04-28 09:00:00',
        v_image_id_8
    ),
    -- Post 23: MARKDOWN
    (
        'opentelemetry-distributed-tracing', 
        'Distributed Tracing with OpenTelemetry', 
        1,
        'Instrument your Java applications with OpenTelemetry to trace requests across microservices and identify bottlenecks.', 
        '## See the Whole Picture

OpenTelemetry provides a vendor-agnostic way to collect traces, metrics, and logs.

### Auto-Instrumentation

```bash
## Use Java agent
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=payment-service \
     -Dotel.traces.exporter=jaeger \
     -jar app.jar
```

### Manual Instrumentation

```java
@Autowired
private Tracer tracer;

public void processPayment(Payment payment) {
    Span span = tracer.spanBuilder("payment.process")
        .setAttribute("payment.id", payment.getId())
        .startSpan();
    try (Scope scope = span.makeCurrent()) {
        // business logic
    } finally {
        span.end();
    }
}
```

### Trace Visualization

See request flow across services: API Gateway → Auth → Order → Payment → Shipping.', 
        'MARKDOWN',
        TRUE, 
        '2024-05-01 10:00:00', 
        '2024-05-03 14:30:00', 
        '2024-05-01 10:00:00',
        v_image_id_7
    ),
    -- Post 24: ASCIIDOC
    (
        'java-modules-9-project-jigsaw', 
        'Java Modules (Project Jigsaw) in Real-World Applications', 
        1,
        'Practical guide to migrating to the Java Platform Module System, including benefits and common pitfalls.', 
        '== Encapsulate Your Code Better

Java 9 introduced modules, enabling reliable configuration and strong encapsulation.

=== Module Declaration

[source,java]
----
module com.myapp.payment {
    requires java.sql;
    requires com.myapp.common;
    exports com.myapp.payment.api;
    opens com.myapp.payment.internal to com.fasterxml.jackson.databind;
}
----

=== Migration Strategy

Start with `--class-path` mode, then gradually add `module-info.java` to leaf modules. Use jdeps to analyze dependencies.

=== Benefits

- Reliable configuration (no missing JARs)
- Strong encapsulation (internal packages hidden)
- Smaller runtime images with jlink

We''ll migrate a sample microservice to modules and measure startup time changes.', 
        'ASCIIDOC',
        TRUE, 
        '2024-05-05 12:00:00', 
        '2024-05-07 16:00:00', 
        '2024-05-05 12:00:00',
        v_image_id_6
    );

    -- ============================================
    -- 4. Inserir rascunhos (drafts) – mixing formats
    -- ============================================
    -- Draft 1: MARKDOWN
    INSERT INTO tb_posts(slug, title, author_id, description, content, format, published, created_at, updated_at, published_at, cover_id) 
    VALUES 
    (
        'draft-why-java-still-matters-2024', 
        'Why Java Still Matters in 2024 (Draft)', 
        1,
        'A draft exploring Java relevance despite newer languages. Work in progress - expecting to add benchmarks and community insights.', 
        '## Java in the Age of Polyglot Programming

This is a draft post. I intend to discuss how Java continues to evolve and why it remains a top choice for enterprise systems.

### Planned sections:

- Recent language features (Records, Pattern Matching, Switch Expressions)
- Performance improvements in recent JDKs
- Ecosystem stability vs. new languages
- Case studies from large companies

More content coming soon. Check back for updates!', 
        'MARKDOWN',
        FALSE, 
        '2024-03-15 10:00:00', 
        '2024-03-20 14:30:00', 
        NULL,
        v_draft_img_1
    ),
    -- Draft 2: ASCIIDOC
    (
        'draft-graalvm-native-image-spring-boot', 
        'GraalVM Native Image with Spring Boot: Performance Gains (Draft)', 
        1,
        'Analyzing startup time and memory improvements when compiling Spring Boot applications to native executables using GraalVM.', 
        '== Native Compilation: A Game Changer for Spring Boot?

Draft post exploring GraalVM native image capabilities with Spring Boot 3.x.

=== What I plan to cover:

- Setup and configuration challenges
- Startup time benchmarks (cold vs warm)
- Memory footprint analysis
- Limitations and workarounds

Need to run more tests and include real-world metrics.', 
        'ASCIIDOC',
        FALSE, 
        '2024-03-25 09:15:00', 
        '2024-03-28 16:20:00', 
        NULL,
        v_draft_img_2
    ),
    -- Draft 3: MARKDOWN
    (
        'draft-testing-distributed-systems', 
        'Testing Strategies for Distributed Systems (Draft - Early Stage)', 
        1,
        'A preliminary draft on testing approaches for microservices: contract testing, integration test environments, and chaos engineering.', 
        '## Testing Beyond Unit Tests

This is a very early draft. Need to flesh out examples and tools.

### Outline:

- Consumer-driven contract testing (Pact)
- Testcontainers for integration tests
- Chaos engineering with Chaos Monkey
- Performance and load testing strategies

Will add code snippets and best practices soon.', 
        'MARKDOWN',
        FALSE, 
        '2024-04-01 11:00:00', 
        '2024-04-01 11:00:00', 
        NULL,
        NULL
    ),
    -- Draft 4: ASCIIDOC
    (
        'draft-reactive-java-rxjava-webflux', 
        'Reactive Java: RxJava vs. Project Reactor (Draft)', 
        1,
        'Comparing reactive programming libraries in Java, their performance characteristics, and when to use each.', 
        '== Reactive Streams in Practice

Draft comparing RxJava 3 and Project Reactor (Spring WebFlux).

=== Planned content:

- Core concepts (Observables, Flux, Mono)
- Backpressure handling
- Performance benchmarks
- Learning curve and debugging

Code examples and comparison tables are being prepared.', 
        'ASCIIDOC',
        FALSE, 
        '2024-04-05 13:30:00', 
        '2024-04-08 09:45:00', 
        NULL,
        v_image_id_6
    ),
    -- Draft 5: MARKDOWN
    (
        'draft-security-java-microservices', 
        'Securing Java Microservices: OAuth2, JWT, and mTLS (Draft)', 
        1,
        'A comprehensive guide to authentication and authorization patterns in microservices architectures using Spring Security.', 
        '## Security in Distributed Systems

First draft - need to add configuration examples and best practices.

### Topics to cover:

- OAuth2 authorization server setup
- JWT token validation and revocation
- Mutual TLS (mTLS) for service-to-service
- API gateway security policies
- Secure configuration management

Will include Spring Security 6 examples and Keycloak integration.', 
        'MARKDOWN',
        FALSE, 
        '2024-04-10 15:00:00', 
        '2024-04-12 11:10:00', 
        NULL,
        v_image_id_5
    );

END $$;