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

    -- Inserir posts publicados
    INSERT INTO tb_posts(slug, title, author, description, content, published, created_at, updated_at, published_at, cover_id) 
    VALUES 
    (
        'introduction-to-distributed-systems-java', 
        'Introduction to Distributed Systems with Java', 
        'Victor Osório',  
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
        TRUE, 
        '2023-01-15 09:00:00', 
        '2023-01-15 09:00:00', 
        '2023-01-15 09:00:00',
        v_image_id_1
    ),
    (
        'microservices-patterns-spring-boot', 
        'Essential Microservices Patterns with Spring Boot', 
        'Victor Osório',
        'Explore battle-tested microservices patterns and learn how to implement them using Spring Boot and Spring Cloud.', 
        '## Designing Robust Microservices with Spring Ecosystem

Microservices architecture offers tremendous flexibility but introduces complexity. This guide walks through proven patterns that address common distributed system challenges.

> "Microservices are not a silver bullet - they require disciplined implementation of proven patterns."

### Patterns We''ll Cover

- **API Gateway Pattern** - Using Spring Cloud Gateway
- **Service Discovery** - Netflix Eureka integration
- **Circuit Breaker** - Resilience4j implementation
- **Distributed Tracing** - Sleuth and Zipkin
- **Configuration Management** - Spring Cloud Config Server

### Implementation Example

```java
// Circuit Breaker example with Resilience4j
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

public PaymentResponse fallbackPayment(PaymentRequest request, Exception e) {
    return new PaymentResponse("PENDING", "Payment queued for retry");
}
```

### Real-world Example

We''ll build a simple e-commerce system demonstrating how these patterns work together to create a resilient, scalable application. Code examples included!', 
        TRUE, 
        '2023-02-20 14:30:00', 
        '2023-02-25 11:15:00', 
        '2023-02-20 14:30:00',
        v_image_id_2
    ),
    (
        'apache-kafka-spring-boot-tutorial', 
        'Building Event-Driven Systems with Apache Kafka and Spring Boot', 
        'Victor Osório',
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
        TRUE, 
        '2023-04-05 10:00:00', 
        '2023-04-10 16:45:00', 
        '2023-04-05 10:00:00',
        v_image_id_3
    ),
    (
        'distributed-transactions-saga-pattern', 
        'Distributed Transactions: Implementing the Saga Pattern', 
        'Victor Osório',
        'Learn how to maintain data consistency across microservices using the Saga pattern with practical Java implementations.', 
        '## Solving the Distributed Transaction Problem

Traditional ACID transactions don''t work across microservices. The Saga pattern offers a pragmatic approach to maintaining data consistency in distributed systems.

> "In distributed systems, we trade immediate consistency for eventual consistency and resilience."

### Saga Coordination Approaches

- **Choreography-based Saga** - Event-driven coordination
- **Orchestration-based Saga** - Centralized control with compensation logic

### Orchestration Example

```java
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
```

### Implementation Examples

We''ll explore both approaches using Spring Boot, with detailed code examples showing compensation strategies, idempotency, and monitoring. The examples include a travel booking system that coordinates flight, hotel, and car rental services.

### Best Practices

Learn about timeout handling, retry mechanisms, and monitoring strategies to make your sagas production-ready.', 
        TRUE, 
        '2023-06-12 11:00:00', 
        '2023-06-15 09:30:00', 
        '2023-06-12 11:00:00',
        v_image_id_4
    ),
    (
        'kubernetes-java-microservices-deployment', 
        'Deploying Java Microservices on Kubernetes: A Complete Guide', 
        'Victor Osório',
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
        TRUE, 
        '2023-08-18 08:00:00', 
        '2023-08-22 14:20:00', 
        '2023-08-18 08:00:00',
        v_image_id_5
    ),
    (
        'grpc-java-performance-comparison', 
        'gRPC vs REST: Performance Analysis in Java Microservices', 
        'Victor Osório',
        'Deep dive into gRPC performance characteristics compared to traditional REST APIs, with benchmarking results and implementation guides.', 
        '## Is gRPC Worth the Hype? Let''s Measure It

gRPC promises better performance than REST, but what does that mean in practice? We conducted extensive benchmarks comparing gRPC and REST in Java microservices.

### Benchmark Results

- Throughput comparison under varying load
- Latency analysis at the 99th percentile
- Memory and CPU utilization
- Network bandwidth efficiency with Protocol Buffers

### gRPC Service Definition

```protobuf
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
```

### Java Implementation

```java
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
```

### When to Choose What

Practical recommendations on when gRPC makes sense and when traditional REST/GraphQL might be better suited for your use case.', 
        TRUE, 
        '2023-10-25 13:00:00', 
        '2023-10-28 10:15:00', 
        '2023-10-25 13:00:00',
        v_image_id_6
    ),
    (
        'observability-java-distributed-systems', 
        'Observability in Java Distributed Systems: Metrics, Logs, and Traces', 
        'Victor Osório',
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
        TRUE, 
        '2023-12-02 15:30:00', 
        '2024-01-05 11:45:00', 
        '2023-12-02 15:30:00',
        v_image_id_7
    ),
    (
        'virtual-threads-project-loom', 
        'Project Loom: Virtual Threads Revolutionizing Java Concurrency', 
        'Victor Osório',
        'Explore how virtual threads in Java 21 are changing the game for concurrent programming in distributed systems.', 
        '## Concurrency Made Simple: Virtual Threads in Practice

Java 21 introduced virtual threads (Project Loom) as a preview feature, now finalized. This revolutionary feature dramatically simplifies concurrent programming while improving resource utilization.

> "Virtual threads bring back the simplicity of thread-per-request without the overhead."

### What Are Virtual Threads?

Unlike platform threads that map 1:1 to OS threads, virtual threads are lightweight, managed by the JVM, enabling millions of concurrent tasks with minimal overhead.

### Traditional vs Virtual Threads

```java
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
```

### Structured Concurrency

```java
Response handleRequest(Request request) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future<User> user = scope.fork(() -> fetchUser(request.userId()));
        Future<Order> order = scope.fork(() -> fetchOrder(request.orderId()));
        
        scope.join();
        scope.throwIfFailed();
        
        return new Response(user.resultNow(), order.resultNow());
    }
}
```

### Impact on Distributed Systems

- Simplified code for handling many concurrent connections
- Better resource utilization in microservices
- Compatibility with existing Java frameworks
- Performance improvements in I/O-heavy workloads

### Migration Guide

How to start using virtual threads in existing applications, common patterns, and performance benchmarks comparing traditional thread pools with virtual threads.', 
        TRUE, 
        '2024-01-20 09:00:00', 
        '2024-01-20 09:00:00', 
        '2024-01-20 09:00:00',
        v_image_id_8
    );
END $$;

-- ============================================
-- 4. Inserir rascunhos (drafts) para teste
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
    -- Buscar IDs das imagens existentes para drafts
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

    -- Inserir rascunhos (drafts)
    INSERT INTO tb_posts(slug, title, author, description, content, published, created_at, updated_at, published_at, cover_id) 
    VALUES 
    (
        'draft-why-java-still-matters-2024', 
        'Why Java Still Matters in 2024 (Draft)', 
        'Victor Osório',
        'A draft exploring Java relevance despite newer languages. Work in progress - expecting to add benchmarks and community insights.', 
        '## Java in the Age of Polyglot Programming

This is a draft post. I intend to discuss how Java continues to evolve and why it remains a top choice for enterprise systems.

### Planned sections:

- Recent language features (Records, Pattern Matching, Switch Expressions)
- Performance improvements in recent JDKs
- Ecosystem stability vs. new languages
- Case studies from large companies

More content coming soon. Check back for updates!', 
        FALSE, 
        '2024-03-15 10:00:00', 
        '2024-03-20 14:30:00', 
        NULL,
        v_draft_img_1
    ),
    (
        'draft-graalvm-native-image-spring-boot', 
        'GraalVM Native Image with Spring Boot: Performance Gains (Draft)', 
        'Victor Osório',
        'Analyzing startup time and memory improvements when compiling Spring Boot applications to native executables using GraalVM.', 
        '## Native Compilation: A Game Changer for Spring Boot?

Draft post exploring GraalVM native image capabilities with Spring Boot 3.x.

### What I plan to cover:

- Setup and configuration challenges
- Startup time benchmarks (cold vs warm)
- Memory footprint analysis
- Limitations and workarounds

Need to run more tests and include real-world metrics.', 
        FALSE, 
        '2024-03-25 09:15:00', 
        '2024-03-28 16:20:00', 
        NULL,
        v_draft_img_2
    ),
    (
        'draft-testing-distributed-systems', 
        'Testing Strategies for Distributed Systems (Draft - Early Stage)', 
        'Victor Osório',
        'A preliminary draft on testing approaches for microservices: contract testing, integration test environments, and chaos engineering.', 
        '## Testing Beyond Unit Tests

This is a very early draft. Need to flesh out examples and tools.

### Outline:

- Consumer-driven contract testing (Pact)
- Testcontainers for integration tests
- Chaos engineering with Chaos Monkey
- Performance and load testing strategies

Will add code snippets and best practices soon.', 
        FALSE, 
        '2024-04-01 11:00:00', 
        '2024-04-01 11:00:00', 
        NULL,
        NULL  -- no cover image for this draft
    ),
    (
        'draft-reactive-java-rxjava-webflux', 
        'Reactive Java: RxJava vs. Project Reactor (Draft)', 
        'Victor Osório',
        'Comparing reactive programming libraries in Java, their performance characteristics, and when to use each.', 
        '## Reactive Streams in Practice

Draft comparing RxJava 3 and Project Reactor (Spring WebFlux).

### Planned content:

- Core concepts (Observables, Flux, Mono)
- Backpressure handling
- Performance benchmarks
- Learning curve and debugging

Code examples and comparison tables are being prepared.', 
        FALSE, 
        '2024-04-05 13:30:00', 
        '2024-04-08 09:45:00', 
        NULL,
        v_image_id_6  -- reuse gRPC cover image as placeholder
    ),
    (
        'draft-security-java-microservices', 
        'Securing Java Microservices: OAuth2, JWT, and mTLS (Draft)', 
        'Victor Osório',
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
        FALSE, 
        '2024-04-10 15:00:00', 
        '2024-04-12 11:10:00', 
        NULL,
        v_image_id_5  -- reuse Kubernetes cover image
    );

END $$;