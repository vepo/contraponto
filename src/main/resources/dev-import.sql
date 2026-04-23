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
        '<h2>Understanding Distributed Systems in the Java Ecosystem</h2>
        <p>Distributed systems have become the backbone of modern software architecture. In this post, we explore the core principles that make distributed systems both powerful and challenging.</p>
        
        <div class="pull-quote">
            "The art of distributed systems is not about avoiding failures, but about designing systems that can gracefully handle them."
        </div>
        
        <h3>Why Java for Distributed Systems?</h3>
        <p>Java''s mature ecosystem provides unparalleled tools for distributed computing. From built-in networking capabilities to enterprise-grade frameworks, Java remains a top choice for building resilient distributed applications.</p>
        
        <h3>Key Concepts Covered</h3>
        <ul>
            <li>Network transparency and location transparency</li>
            <li>Consistency models (strong vs. eventual consistency)</li>
            <li>Fault tolerance strategies</li>
            <li>The CAP theorem in practice</li>
        </ul>
        
        <h3>Real-World Example</h3>
        <p>Consider a simple key-value store distributed across three nodes. We''ll explore how different consistency models affect performance and availability.</p>
        
        <pre><code>
// Example: Distributed counter with eventual consistency
public class DistributedCounter {
    private final Map&lt;String, Integer&gt; localState = new ConcurrentHashMap&lt;&gt;();
    
    public void increment(String key) {
        localState.merge(key, 1, Integer::sum);
        replicateToPeers(key);
    }
    
    public int get(String key) {
        return localState.getOrDefault(key, 0);
    }
}
        </code></pre>
        
        <p>Stay tuned for hands-on examples using Java RMI, Spring Cloud, and Apache Kafka!</p>', 
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
        '<h2>Designing Robust Microservices with Spring Ecosystem</h2>
        <p>Microservices architecture offers tremendous flexibility but introduces complexity. This guide walks through proven patterns that address common distributed system challenges.</p>
        
        <div class="pull-quote">
            "Microservices are not a silver bullet - they require disciplined implementation of proven patterns."
        </div>
        
        <h3>Patterns We''ll Cover</h3>
        <ul>
            <li><strong>API Gateway Pattern</strong> - Using Spring Cloud Gateway</li>
            <li><strong>Service Discovery</strong> - Netflix Eureka integration</li>
            <li><strong>Circuit Breaker</strong> - Resilience4j implementation</li>
            <li><strong>Distributed Tracing</strong> - Sleuth and Zipkin</li>
            <li><strong>Configuration Management</strong> - Spring Cloud Config Server</li>
        </ul>
        
        <h3>Implementation Example</h3>
        <pre><code>
// Circuit Breaker example with Resilience4j
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPayment")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.process(request);
}

public PaymentResponse fallbackPayment(PaymentRequest request, Exception e) {
    return new PaymentResponse("PENDING", "Payment queued for retry");
}
        </code></pre>
        
        <h3>Real-world Example</h3>
        <p>We''ll build a simple e-commerce system demonstrating how these patterns work together to create a resilient, scalable application. Code examples included!</p>', 
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
        '<h2>Event-Driven Architecture: The Future of Scalable Systems</h2>
        <p>Event-driven architecture enables loose coupling and high scalability. Apache Kafka combined with Spring Boot provides a powerful stack for building reactive systems.</p>
        
        <h3>What You''ll Learn</h3>
        <ul>
            <li>Kafka core concepts: topics, partitions, brokers</li>
            <li>Spring Kafka configuration and best practices</li>
            <li>Producer and consumer implementations</li>
            <li>Error handling and exactly-once semantics</li>
            <li>Schema management with Avro and Schema Registry</li>
        </ul>
        
        <h3>Producer Example</h3>
        <pre><code>
@Service
public class OrderEventProducer {
    @Autowired
    private KafkaTemplate&lt;String, OrderEvent&gt; kafkaTemplate;
    
    public void sendOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(order.getId(), "CREATED", Instant.now());
        kafkaTemplate.send("orders", order.getId().toString(), event);
    }
}
        </code></pre>
        
        <h3>Consumer Example</h3>
        <pre><code>
@KafkaListener(topics = "orders", groupId = "payment-service")
public void processOrder(OrderEvent event) {
    logger.info("Processing order: {}", event);
    // Process payment logic here
}
        </code></pre>
        
        <h3>Hands-on Project</h3>
        <p>We''ll build a real-time order processing system that demonstrates event sourcing and CQRS patterns. Complete source code available on GitHub.</p>', 
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
        '<h2>Solving the Distributed Transaction Problem</h2>
        <p>Traditional ACID transactions don''t work across microservices. The Saga pattern offers a pragmatic approach to maintaining data consistency in distributed systems.</p>
        
        <div class="pull-quote">
            "In distributed systems, we trade immediate consistency for eventual consistency and resilience."
        </div>
        
        <h3>Saga Coordination Approaches</h3>
        <ul>
            <li><strong>Choreography-based Saga</strong> - Event-driven coordination</li>
            <li><strong>Orchestration-based Saga</strong> - Centralized control with compensation logic</li>
        </ul>
        
        <h3>Orchestration Example</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Implementation Examples</h3>
        <p>We''ll explore both approaches using Spring Boot, with detailed code examples showing compensation strategies, idempotency, and monitoring. The examples include a travel booking system that coordinates flight, hotel, and car rental services.</p>
        
        <h3>Best Practices</h3>
        <p>Learn about timeout handling, retry mechanisms, and monitoring strategies to make your sagas production-ready.</p>', 
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
        '<h2>From Development to Production: Java on Kubernetes</h2>
        <p>Kubernetes has become the de facto standard for container orchestration. This guide shows how to effectively deploy Java microservices on Kubernetes with proper configuration and observability.</p>
        
        <h3>Key Topics</h3>
        <ul>
            <li>Containerizing Spring Boot applications with JVM optimizations</li>
            <li>Kubernetes manifests: Deployments, Services, ConfigMaps, Secrets</li>
            <li>Health checks and readiness probes for Java applications</li>
            <li>Horizontal Pod Autoscaling based on custom metrics</li>
            <li>Canary deployments and blue-green strategies</li>
        </ul>
        
        <h3>Sample Kubernetes Deployment</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Production Considerations</h3>
        <p>We''ll cover memory management for JVM in containers, logging aggregation with EFK stack, and monitoring with Prometheus and Grafana.</p>', 
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
        '<h2>Is gRPC Worth the Hype? Let''s Measure It</h2>
        <p>gRPC promises better performance than REST, but what does that mean in practice? We conducted extensive benchmarks comparing gRPC and REST in Java microservices.</p>
        
        <h3>Benchmark Results</h3>
        <ul>
            <li>Throughput comparison under varying load</li>
            <li>Latency analysis at the 99th percentile</li>
            <li>Memory and CPU utilization</li>
            <li>Network bandwidth efficiency with Protocol Buffers</li>
        </ul>
        
        <h3>gRPC Service Definition</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Java Implementation</h3>
        <pre><code>
@GrpcService
public class ProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {
    @Override
    public void getProduct(ProductRequest request, StreamObserver&lt;ProductResponse&gt; responseObserver) {
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
        </code></pre>
        
        <h3>When to Choose What</h3>
        <p>Practical recommendations on when gRPC makes sense and when traditional REST/GraphQL might be better suited for your use case.</p>', 
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
        '<h2>Understanding Distributed System Behavior</h2>
        <p>Observability is crucial for operating distributed systems. This post explores how to instrument Java applications for metrics, structured logging, and distributed tracing.</p>
        
        <div class="pull-quote">
            "Observability isn''t just about monitoring - it''s about understanding why your system behaves the way it does."
        </div>
        
        <h3>Three Pillars of Observability</h3>
        <ul>
            <li><strong>Metrics</strong> - Micrometer and Prometheus integration</li>
            <li><strong>Logging</strong> - Structured logging with JSON and correlation IDs</li>
            <li><strong>Tracing</strong> - OpenTelemetry with Jaeger/Zipkin</li>
        </ul>
        
        <h3>OpenTelemetry Configuration</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Custom Metrics Example</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Unified Observability Stack</h3>
        <p>We''ll build a complete observability pipeline: Spring Boot applications exporting telemetry data to OpenTelemetry collector, visualized in Grafana with Prometheus for metrics and Tempo for traces.</p>
        
        <h3>Practical Alerting</h3>
        <p>Learn to set up meaningful alerts based on SLOs and SLIs to proactively detect issues before they impact users.</p>', 
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
        '<h2>Concurrency Made Simple: Virtual Threads in Practice</h2>
        <p>Java 21 introduced virtual threads (Project Loom) as a preview feature, now finalized. This revolutionary feature dramatically simplifies concurrent programming while improving resource utilization.</p>
        
        <div class="pull-quote">
            "Virtual threads bring back the simplicity of thread-per-request without the overhead."
        </div>
        
        <h3>What Are Virtual Threads?</h3>
        <p>Unlike platform threads that map 1:1 to OS threads, virtual threads are lightweight, managed by the JVM, enabling millions of concurrent tasks with minimal overhead.</p>
        
        <h3>Traditional vs Virtual Threads</h3>
        <pre><code>
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
        </code></pre>
        
        <h3>Structured Concurrency</h3>
        <pre><code>
Response handleRequest(Request request) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future&lt;User&gt; user = scope.fork(() -> fetchUser(request.userId()));
        Future&lt;Order&gt; order = scope.fork(() -> fetchOrder(request.orderId()));
        
        scope.join();
        scope.throwIfFailed();
        
        return new Response(user.resultNow(), order.resultNow());
    }
}
        </code></pre>
        
        <h3>Impact on Distributed Systems</h3>
        <ul>
            <li>Simplified code for handling many concurrent connections</li>
            <li>Better resource utilization in microservices</li>
            <li>Compatibility with existing Java frameworks</li>
            <li>Performance improvements in I/O-heavy workloads</li>
        </ul>
        
        <h3>Migration Guide</h3>
        <p>How to start using virtual threads in existing applications, common patterns, and performance benchmarks comparing traditional thread pools with virtual threads.</p>', 
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
        '<h2>Java in the Age of Polyglot Programming</h2>
        <p>This is a draft post. I intend to discuss how Java continues to evolve and why it remains a top choice for enterprise systems.</p>
        
        <h3>Planned sections:</h3>
        <ul>
            <li>Recent language features (Records, Pattern Matching, Switch Expressions)</li>
            <li>Performance improvements in recent JDKs</li>
            <li>Ecosystem stability vs. new languages</li>
            <li>Case studies from large companies</li>
        </ul>
        
        <p>More content coming soon. Check back for updates!</p>', 
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
        '<h2>Native Compilation: A Game Changer for Spring Boot?</h2>
        <p>Draft post exploring GraalVM native image capabilities with Spring Boot 3.x.</p>
        
        <h3>What I plan to cover:</h3>
        <ul>
            <li>Setup and configuration challenges</li>
            <li>Startup time benchmarks (cold vs warm)</li>
            <li>Memory footprint analysis</li>
            <li>Limitations and workarounds</li>
        </ul>
        
        <p>Need to run more tests and include real-world metrics.</p>', 
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
        '<h2>Testing Beyond Unit Tests</h2>
        <p>This is a very early draft. Need to flesh out examples and tools.</p>
        
        <h3>Outline:</h3>
        <ul>
            <li>Consumer-driven contract testing (Pact)</li>
            <li>Testcontainers for integration tests</li>
            <li>Chaos engineering with Chaos Monkey</li>
            <li>Performance and load testing strategies</li>
        </ul>
        
        <p>Will add code snippets and best practices soon.</p>', 
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
        '<h2>Reactive Streams in Practice</h2>
        <p>Draft comparing RxJava 3 and Project Reactor (Spring WebFlux).</p>
        
        <h3>Planned content:</h3>
        <ul>
            <li>Core concepts (Observables, Flux, Mono)</li>
            <li>Backpressure handling</li>
            <li>Performance benchmarks</li>
            <li>Learning curve and debugging</li>
        </ul>
        
        <p>Code examples and comparison tables are being prepared.</p>', 
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
        '<h2>Security in Distributed Systems</h2>
        <p>First draft - need to add configuration examples and best practices.</p>
        
        <h3>Topics to cover:</h3>
        <ul>
            <li>OAuth2 authorization server setup</li>
            <li>JWT token validation and revocation</li>
            <li>Mutual TLS (mTLS) for service-to-service</li>
            <li>API gateway security policies</li>
            <li>Secure configuration management</li>
        </ul>
        
        <p>Will include Spring Security 6 examples and Keycloak integration.</p>', 
        FALSE, 
        '2024-04-10 15:00:00', 
        '2024-04-12 11:10:00', 
        NULL,
        v_image_id_5  -- reuse Kubernetes cover image
    );

END $$;