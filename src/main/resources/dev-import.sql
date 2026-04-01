-- Create test environment for blog posts about Java and Distributed Systems
-- Timeline spans from early 2023 to early 2024

INSERT INTO tb_posts(slug, title, author, description, content, published, created_at, updated_at, published_at) 
VALUES 
(
    'introduction-to-distributed-systems-java', 
    'Introduction to Distributed Systems with Java', 
    'Victor Osório',  
    'Learn the fundamental concepts of distributed systems and how Java technologies enable building scalable, fault-tolerant applications.', 
    '<h2>Understanding Distributed Systems in the Java Ecosystem</h2>
    <p>Distributed systems have become the backbone of modern software architecture. In this post, we explore the core principles that make distributed systems both powerful and challenging.</p>
    
    <h3>Why Java for Distributed Systems?</h3>
    <p>Java''s mature ecosystem provides unparalleled tools for distributed computing. From built-in networking capabilities to enterprise-grade frameworks, Java remains a top choice for building resilient distributed applications.</p>
    
    <h3>Key Concepts Covered</h3>
    <ul>
        <li>Network transparency and location transparency</li>
        <li>Consistency models (strong vs. eventual consistency)</li>
        <li>Fault tolerance strategies</li>
        <li>The CAP theorem in practice</li>
    </ul>
    
    <p>Stay tuned for hands-on examples using Java RMI, Spring Cloud, and Apache Kafka!</p>', 
    TRUE, 
    '2023-01-15 09:00:00', 
    '2023-01-15 09:00:00', 
    '2023-01-15 09:00:00'
),

(
    'microservices-patterns-spring-boot', 
    'Essential Microservices Patterns with Spring Boot', 
    'Victor Osório',
    'Explore battle-tested microservices patterns and learn how to implement them using Spring Boot and Spring Cloud.', 
    '<h2>Designing Robust Microservices with Spring Ecosystem</h2>
    <p>Microservices architecture offers tremendous flexibility but introduces complexity. This guide walks through proven patterns that address common distributed system challenges.</p>
    
    <h3>Patterns We''ll Cover</h3>
    <ul>
        <li><strong>API Gateway Pattern</strong> - Using Spring Cloud Gateway</li>
        <li><strong>Service Discovery</strong> - Netflix Eureka integration</li>
        <li><strong>Circuit Breaker</strong> - Resilience4j implementation</li>
        <li><strong>Distributed Tracing</strong> - Sleuth and Zipkin</li>
        <li><strong>Configuration Management</strong> - Spring Cloud Config Server</li>
    </ul>
    
    <h3>Real-world Example</h3>
    <p>We''ll build a simple e-commerce system demonstrating how these patterns work together to create a resilient, scalable application. Code examples included!</p>', 
    TRUE, 
    '2023-02-20 14:30:00', 
    '2023-02-25 11:15:00', 
    '2023-02-20 14:30:00'
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
    
    <h3>Hands-on Project</h3>
    <p>We''ll build a real-time order processing system that demonstrates event sourcing and CQRS patterns. Complete source code available on GitHub.</p>', 
    TRUE, 
    '2023-04-05 10:00:00', 
    '2023-04-10 16:45:00', 
    '2023-04-05 10:00:00'
),

(
    'distributed-transactions-saga-pattern', 
    'Distributed Transactions: Implementing the Saga Pattern', 
    'Victor Osório',
    'Learn how to maintain data consistency across microservices using the Saga pattern with practical Java implementations.', 
    '<h2>Solving the Distributed Transaction Problem</h2>
    <p>Traditional ACID transactions don''t work across microservices. The Saga pattern offers a pragmatic approach to maintaining data consistency in distributed systems.</p>
    
    <h3>Saga Coordination Approaches</h3>
    <ul>
        <li><strong>Choreography-based Saga</strong> - Event-driven coordination</li>
        <li><strong>Orchestration-based Saga</strong> - Centralized control with compensation logic</li>
    </ul>
    
    <h3>Implementation Examples</h3>
    <p>We''ll explore both approaches using Spring Boot, with detailed code examples showing compensation strategies, idempotency, and monitoring. The examples include a travel booking system that coordinates flight, hotel, and car rental services.</p>
    
    <h3>Best Practices</h3>
    <p>Learn about timeout handling, retry mechanisms, and monitoring strategies to make your sagas production-ready.</p>', 
    TRUE, 
    '2023-06-12 11:00:00', 
    '2023-06-15 09:30:00', 
    '2023-06-12 11:00:00'
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
    
    <h3>Production Considerations</h3>
    <p>We''ll cover memory management for JVM in containers, logging aggregation with EFK stack, and monitoring with Prometheus and Grafana.</p>', 
    TRUE, 
    '2023-08-18 08:00:00', 
    '2023-08-22 14:20:00', 
    '2023-08-18 08:00:00'
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
    
    <h3>Implementation Guide</h3>
    <p>Step-by-step tutorial on building gRPC services with Java, including protocol buffer definitions, service implementation, and client integration with Spring Boot.</p>
    
    <h3>When to Choose What</h3>
    <p>Practical recommendations on when gRPC makes sense and when traditional REST/GraphQL might be better suited for your use case.</p>', 
    TRUE, 
    '2023-10-25 13:00:00', 
    '2023-10-28 10:15:00', 
    '2023-10-25 13:00:00'
),

(
    'observability-java-distributed-systems', 
    'Observability in Java Distributed Systems: Metrics, Logs, and Traces', 
    'Victor Osório',
    'Implement comprehensive observability for your Java microservices using OpenTelemetry, Prometheus, and Grafana.', 
    '<h2>Understanding Distributed System Behavior</h2>
    <p>Observability is crucial for operating distributed systems. This post explores how to instrument Java applications for metrics, structured logging, and distributed tracing.</p>
    
    <h3>Three Pillars of Observability</h3>
    <ul>
        <li><strong>Metrics</strong> - Micrometer and Prometheus integration</li>
        <li><strong>Logging</strong> - Structured logging with JSON and correlation IDs</li>
        <li><strong>Tracing</strong> - OpenTelemetry with Jaeger/Zipkin</li>
    </ul>
    
    <h3>Unified Observability Stack</h3>
    <p>We''ll build a complete observability pipeline: Spring Boot applications exporting telemetry data to OpenTelemetry collector, visualized in Grafana with Prometheus for metrics and Tempo for traces.</p>
    
    <h3>Practical Alerting</h3>
    <p>Learn to set up meaningful alerts based on SLOs and SLIs to proactively detect issues before they impact users.</p>', 
    TRUE, 
    '2023-12-02 15:30:00', 
    '2024-01-05 11:45:00', 
    '2023-12-02 15:30:00'
),

(
    'virtual-threads-project-loom', 
    'Project Loom: Virtual Threads Revolutionizing Java Concurrency', 
    'Victor Osório',
    'Explore how virtual threads in Java 21 are changing the game for concurrent programming in distributed systems.', 
    '<h2>Concurrency Made Simple: Virtual Threads in Practice</h2>
    <p>Java 21 introduced virtual threads (Project Loom) as a preview feature, now finalized. This revolutionary feature dramatically simplifies concurrent programming while improving resource utilization.</p>
    
    <h3>What Are Virtual Threads?</h3>
    <p>Unlike platform threads that map 1:1 to OS threads, virtual threads are lightweight, managed by the JVM, enabling millions of concurrent tasks with minimal overhead.</p>
    
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
    '2024-01-20 09:00:00'
);