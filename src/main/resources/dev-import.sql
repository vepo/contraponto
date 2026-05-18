-- Dev seed data for contraponto (loaded on every dev startup via DatabaseDevSetup)
-- All dev users share the same bcrypt hash as admin (login with your admin password).

-- ============================================
-- 1. Reset content (keep schema seed: admin user + blog + footer pages)
-- ============================================
TRUNCATE TABLE tb_email_notification_log CASCADE;
TRUNCATE TABLE tb_notifications CASCADE;
TRUNCATE TABLE tb_blog_audience CASCADE;
TRUNCATE TABLE tb_post_publication_tags CASCADE;
TRUNCATE TABLE tb_post_publications CASCADE;
TRUNCATE TABLE tb_post_tags CASCADE;
TRUNCATE TABLE tb_views CASCADE;
TRUNCATE TABLE tb_posts CASCADE;
TRUNCATE TABLE tb_series CASCADE;
TRUNCATE TABLE tb_tags CASCADE;
TRUNCATE TABLE tb_images CASCADE;

DELETE FROM tb_user_roles WHERE user_id IN (SELECT id FROM tb_users WHERE username <> 'admin');
DELETE FROM tb_blogs WHERE owner_id IN (SELECT id FROM tb_users WHERE username <> 'admin');
DELETE FROM tb_users WHERE username <> 'admin';

INSERT INTO tb_user_roles (user_id, role)
SELECT u.id, r.role
FROM tb_users u
CROSS JOIN (VALUES ('ADMIN'), ('USER_ADMINISTRATOR')) AS r(role)
WHERE u.username = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;

-- ============================================
-- 2. Users and roles
-- ============================================
INSERT INTO tb_users (username, email, name, password_hash, active, created_at, updated_at)
VALUES
    ('editor', 'editor@contraponto.blog', 'Site Editor', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('alice', 'alice@contraponto.blog', 'Alice Ferreira', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('bob', 'bob@contraponto.blog', 'Bob Martins', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('carol', 'carol@contraponto.blog', 'Carol Silva', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('dave', 'dave@contraponto.blog', 'Dave Reader', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('eve', 'eve@contraponto.blog', 'Eve Subscriber', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW());

INSERT INTO tb_user_roles (user_id, role)
SELECT u.id, v.role
FROM (VALUES
    ('editor', 'USER'),
    ('editor', 'EDITOR'),
    ('alice', 'USER'),
    ('bob', 'USER'),
    ('carol', 'USER'),
    ('dave', 'USER'),
    ('eve', 'USER')
) AS v(username, role)
JOIN tb_users u ON u.username = v.username;

-- ============================================
-- 3. Blogs
-- ============================================
INSERT INTO tb_blogs (name, slug, description, owner_id, main, active, created_at)
SELECT 'Alice on Systems', 'alice', 'Distributed systems, Java, and cloud-native patterns', u.id, TRUE, TRUE, NOW()
FROM tb_users u WHERE u.username = 'alice';

INSERT INTO tb_blogs (name, slug, description, owner_id, main, active, created_at)
SELECT 'Bob Writes Code', 'bob', 'Architecture notes and backend craft', u.id, TRUE, TRUE, NOW()
FROM tb_users u WHERE u.username = 'bob';

INSERT INTO tb_blogs (name, slug, description, owner_id, main, active, created_at)
SELECT 'Architecture Notes', 'architecture-notes', 'DDD, boundaries, and event-driven design', u.id, FALSE, TRUE, NOW()
FROM tb_users u WHERE u.username = 'bob';

INSERT INTO tb_blogs (name, slug, description, owner_id, main, active, created_at)
SELECT 'Carol Explores APIs', 'carol', 'GraphQL, REST, and API design', u.id, TRUE, TRUE, NOW()
FROM tb_users u WHERE u.username = 'carol';

-- ============================================
-- 4. Images (blog_id and uploaded_by_user_id required since V0.0.2)
-- ============================================
INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-distributed-001', 'distributed.jpg', 'image/jpeg', 200000, '/tmp/contraponto-images/distributed.jpg', 'https://picsum.photos/id/1/800/600', TRUE, '2024-01-10 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-microservices-002', 'microservices.jpg', 'image/jpeg', 180000, '/tmp/contraponto-images/microservices.jpg', 'https://picsum.photos/id/2/800/600', TRUE, '2024-02-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-kafka-003', 'kafka.jpg', 'image/jpeg', 210000, '/tmp/contraponto-images/kafka.jpg', 'https://picsum.photos/id/3/800/600', TRUE, '2024-03-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-observability-004', 'observability.jpg', 'image/jpeg', 195000, '/tmp/contraponto-images/observability.jpg', 'https://picsum.photos/id/4/800/600', TRUE, '2024-04-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-loom-005', 'loom.jpg', 'image/jpeg', 175000, '/tmp/contraponto-images/loom.jpg', 'https://picsum.photos/id/5/800/600', TRUE, '2024-05-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-ddd-006', 'ddd.jpg', 'image/jpeg', 160000, '/tmp/contraponto-images/ddd.jpg', 'https://picsum.photos/id/6/800/600', TRUE, '2024-06-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.slug = 'architecture-notes';

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-graphql-007', 'graphql.jpg', 'image/jpeg', 185000, '/tmp/contraponto-images/graphql.jpg', 'https://picsum.photos/id/7/800/600', TRUE, '2024-07-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'carol' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-draft-008', 'draft.jpg', 'image/jpeg', 120000, '/tmp/contraponto-images/draft.jpg', 'https://picsum.photos/id/8/800/600', TRUE, '2024-08-01 08:00:00', b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

-- ============================================
-- 5. Series
-- ============================================
INSERT INTO tb_series (blog_id, title, slug, created_at)
SELECT b.id, 'Distributed systems foundations', 'distributed-foundations', NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_series (blog_id, title, slug, created_at)
SELECT b.id, 'Event streaming & messaging', 'event-streaming', NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_series (blog_id, title, slug, created_at)
SELECT b.id, 'Spring ecosystem & resilience', 'spring-ecosystem', NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_series (blog_id, title, slug, created_at)
SELECT b.id, 'Domain-driven design', 'ddd', NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.slug = 'architecture-notes';

-- ============================================
-- 6. Tags
-- ============================================
INSERT INTO tb_tags (slug, name, description) VALUES
    ('java', 'Java', 'JVM and the Java ecosystem'),
    ('spring-boot', 'Spring Boot', 'Spring Boot and Spring Cloud'),
    ('kafka', 'Kafka', 'Apache Kafka and event streaming'),
    ('kubernetes', 'Kubernetes', 'Container orchestration'),
    ('microservices', 'Microservices', 'Microservices architecture'),
    ('distributed-systems', 'Distributed systems', 'Consistency, resilience, and scale'),
    ('observability', 'Observability', 'Metrics, logs, and tracing'),
    ('graphql', 'GraphQL', 'GraphQL APIs'),
    ('ddd', 'DDD', 'Domain-driven design'),
    ('testing', 'Testing', 'Integration and contract testing'),
    ('platform', 'Platform', 'Platform announcements')
ON CONFLICT (slug) DO NOTHING;

-- ============================================
-- 7. Posts (writers own content; admin has one welcome post)
-- ============================================
DO $$
DECLARE
    v_admin_blog BIGINT;
    v_alice_blog BIGINT;
    v_bob_blog BIGINT;
    v_bob_arch BIGINT;
    v_carol_blog BIGINT;
    v_serie_dist BIGINT;
    v_serie_events BIGINT;
    v_serie_spring BIGINT;
    v_serie_ddd BIGINT;
    v_img1 BIGINT; v_img2 BIGINT; v_img3 BIGINT; v_img4 BIGINT; v_img5 BIGINT;
    v_img6 BIGINT; v_img7 BIGINT; v_img8 BIGINT;
    v_post_intro BIGINT;
    v_post_micro BIGINT;
    v_post_kafka BIGINT;
    v_post_obs BIGINT;
    v_post_loom BIGINT;
    v_post_ddd BIGINT;
    v_post_events BIGINT;
    v_post_graphql BIGINT;
    v_post_welcome BIGINT;
    v_post_draft BIGINT;
BEGIN
    SELECT b.id INTO v_admin_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'admin' AND b.main;
    SELECT b.id INTO v_alice_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;
    SELECT b.id INTO v_bob_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.main;
    SELECT b.id INTO v_bob_arch FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.slug = 'architecture-notes';
    SELECT b.id INTO v_carol_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'carol' AND b.main;

    SELECT id INTO v_serie_dist FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'distributed-foundations';
    SELECT id INTO v_serie_events FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'event-streaming';
    SELECT id INTO v_serie_spring FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'spring-ecosystem';
    SELECT id INTO v_serie_ddd FROM tb_series WHERE blog_id = v_bob_arch AND slug = 'ddd';

    SELECT id INTO v_img1 FROM tb_images WHERE uuid = 'img-distributed-001';
    SELECT id INTO v_img2 FROM tb_images WHERE uuid = 'img-microservices-002';
    SELECT id INTO v_img3 FROM tb_images WHERE uuid = 'img-kafka-003';
    SELECT id INTO v_img4 FROM tb_images WHERE uuid = 'img-observability-004';
    SELECT id INTO v_img5 FROM tb_images WHERE uuid = 'img-loom-005';
    SELECT id INTO v_img6 FROM tb_images WHERE uuid = 'img-ddd-006';
    SELECT id INTO v_img7 FROM tb_images WHERE uuid = 'img-graphql-007';
    SELECT id INTO v_img8 FROM tb_images WHERE uuid = 'img-draft-008';

    -- Admin: platform welcome
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'welcome-to-contraponto',
        'Welcome to Contraponto',
        v_admin_blog,
        'A short note on what this platform is for.',
        '## Welcome

Contraponto is a multi-author publishing space. Explore blogs from **alice**, **bob**, and **carol**, follow writers you enjoy, and subscribe by email for new posts.

Sign in as `editor` to curate featured posts on the review page.',
        'MARKDOWN', TRUE, TRUE,
        '2024-01-01 10:00:00', '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL, NULL
    ) RETURNING id INTO v_post_welcome;

    -- Alice: published posts
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'introduction-to-distributed-systems-java',
        'Introduction to Distributed Systems with Java',
        v_alice_blog,
        'Core concepts of distributed systems and how Java helps you build them.',
        '## Why distributed systems matter

Network partitions, partial failures, and eventual consistency are everyday concerns.

```java
public class DistributedCounter {
    private final Map<String, Integer> state = new ConcurrentHashMap<>();
    public void increment(String key) { state.merge(key, 1, Integer::sum); }
}
```',
        'MARKDOWN', TRUE, TRUE,
        '2024-01-15 09:00:00', '2024-01-15 09:00:00', '2024-01-15 09:00:00', v_img1, v_serie_dist
    ) RETURNING id INTO v_post_intro;

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'microservices-patterns-spring-boot',
        'Essential Microservices Patterns with Spring Boot',
        v_alice_blog,
        'API Gateway, service discovery, circuit breakers, and tracing with Spring Cloud.',
        '== Patterns in production

This is the **live draft** with an expanded Resilience4j section and updated diagrams.

* Circuit breaker with fallbacks
* Distributed tracing with OpenTelemetry
* Config server for environment-specific settings',
        'ASCIIDOC', TRUE, TRUE,
        '2024-02-20 14:30:00', '2024-03-10 16:00:00', '2024-02-20 14:30:00', v_img2, v_serie_spring
    ) RETURNING id INTO v_post_micro;

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'apache-kafka-spring-boot-tutorial',
        'Building Event-Driven Systems with Kafka and Spring Boot',
        v_alice_blog,
        'Producers, consumers, and error handling with Spring Kafka.',
        '## Event-driven basics

Topics, partitions, and consumer groups explained with Spring Boot examples.',
        'MARKDOWN', TRUE, FALSE,
        '2024-04-05 10:00:00', '2024-04-05 10:00:00', '2024-04-05 10:00:00', v_img3, v_serie_events
    ) RETURNING id INTO v_post_kafka;

  INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'observability-java-distributed-systems',
        'Observability in Java Distributed Systems',
        v_alice_blog,
        'Metrics, structured logs, and distributed tracing with OpenTelemetry.',
        '## Observability in practice (draft edits)

This working copy adds a **Grafana dashboard** section not yet republished.

### Three pillars
- Metrics (Micrometer + Prometheus)
- Logs (JSON + correlation IDs)
- Traces (OpenTelemetry + Tempo)',
        'MARKDOWN', TRUE, TRUE,
        '2023-12-02 15:30:00', '2024-05-01 11:00:00', '2023-12-02 15:30:00', v_img4, v_serie_dist
    ) RETURNING id INTO v_post_obs;

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'virtual-threads-project-loom',
        'Project Loom: Virtual Threads in Java 21',
        v_alice_blog,
        'How virtual threads simplify concurrent I/O-heavy services.',
        '== Virtual threads

Lightweight threads managed by the JVM — millions of concurrent tasks without thread-pool tuning.',
        'ASCIIDOC', TRUE, FALSE,
        '2024-01-20 09:00:00', '2024-01-20 09:00:00', '2024-01-20 09:00:00', v_img5, v_serie_dist
    ) RETURNING id INTO v_post_loom;

    -- Alice: draft
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'draft-testing-distributed-systems',
        'Testing Strategies for Distributed Systems (Draft)',
        v_alice_blog,
        'Contract testing, Testcontainers, and chaos engineering — work in progress.',
        '## Outline

- Pact for consumer-driven contracts
- Testcontainers for Kafka and PostgreSQL
- Chaos Monkey experiments',
        'MARKDOWN', FALSE, FALSE,
        '2024-06-01 11:00:00', '2024-06-05 14:00:00', NULL, v_img8, NULL
    ) RETURNING id INTO v_post_draft;

    -- Bob: architecture blog
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'bounded-contexts-ddd',
        'Bounded Contexts in Domain-Driven Design',
        v_bob_arch,
        'How to draw boundaries that match how the business actually works.',
        '## Context maps

Upstream/downstream relationships, anti-corruption layers, and shared kernels.',
        'MARKDOWN', TRUE, TRUE,
        '2024-03-12 10:00:00', '2024-03-12 10:00:00', '2024-03-12 10:00:00', v_img6, v_serie_ddd
    ) RETURNING id INTO v_post_ddd;

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'event-sourcing-intro',
        'A Gentle Introduction to Event Sourcing',
        v_bob_arch,
        'Events as the source of truth — benefits and trade-offs.',
        '## Events first

Append-only stores, projections, and replay for auditing.',
        'MARKDOWN', TRUE, FALSE,
        '2024-04-18 09:00:00', '2024-04-18 09:00:00', '2024-04-18 09:00:00', v_img6, v_serie_ddd
    ) RETURNING id INTO v_post_events;

    -- Carol
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'graphql-java-spring-boot',
        'GraphQL with Java and Spring Boot',
        v_carol_blog,
        'Schema-first APIs with Spring GraphQL.',
        '## GraphQL on the JVM

Types, resolvers, and N+1 avoidance with batch loaders.',
        'MARKDOWN', TRUE, TRUE,
        '2024-05-10 12:00:00', '2024-05-10 12:00:00', '2024-05-10 12:00:00', v_img7, NULL
    ) RETURNING id INTO v_post_graphql;

    -- ============================================
    -- 8. Post tags
    -- ============================================
    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p JOIN tb_tags t ON t.slug = 'platform'
    WHERE p.id = v_post_welcome;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.id IN (v_post_intro, v_post_obs, v_post_loom) AND t.slug IN ('java', 'distributed-systems');

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.id = v_post_micro AND t.slug IN ('java', 'spring-boot', 'microservices');

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.id = v_post_kafka AND t.slug IN ('java', 'kafka', 'spring-boot');

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.id IN (v_post_ddd, v_post_events) AND t.slug IN ('ddd', 'distributed-systems');

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p JOIN tb_tags t ON t.slug = 'graphql'
    WHERE p.id = v_post_graphql;

    -- ============================================
    -- 9. Publication history (versioned snapshots)
    -- ============================================

    -- Simple v1 for most published posts
    INSERT INTO tb_post_publications (post_id, version, published_at, slug, title, description, content, format, cover_id)
    SELECT p.id, 1, p.published_at, p.slug, p.title, p.description,
           CASE p.id
               WHEN v_post_obs THEN '## Observability (v1)

Initial publish — metrics only.'
               WHEN v_post_micro THEN '== Microservices (v1)

First version: API Gateway and Eureka only.'
               ELSE p.content
           END,
           p.format, p.cover_id
    FROM tb_posts p
    WHERE p.published = TRUE AND p.id NOT IN (v_post_obs, v_post_micro);

    -- Observability: v1, v2, v3 (live = v3; post row has newer draft)
    INSERT INTO tb_post_publications (post_id, version, published_at, slug, title, description, content, format, cover_id)
    VALUES
        (v_post_obs, 1, '2023-12-02 15:30:00', 'observability-java-distributed-systems',
         'Observability in Java Distributed Systems',
         'Metrics, structured logs, and distributed tracing with OpenTelemetry.',
         '## Observability (v1)

Initial publish — metrics with Micrometer only.', 'MARKDOWN', v_img4),
        (v_post_obs, 2, '2024-02-01 10:00:00', 'observability-java-distributed-systems',
         'Observability in Java Distributed Systems',
         'Metrics, structured logs, and distributed tracing with OpenTelemetry.',
         '## Observability (v2)

Added structured JSON logging and correlation IDs.', 'MARKDOWN', v_img4),
        (v_post_obs, 3, '2024-04-15 09:00:00', 'observability-java-distributed-systems',
         'Observability in Java Distributed Systems',
         'Metrics, structured logs, and distributed tracing with OpenTelemetry.',
         '## Observability in practice

### Three pillars
- Metrics (Micrometer + Prometheus)
- Logs (JSON + correlation IDs)
- Traces (OpenTelemetry + Tempo)', 'MARKDOWN', v_img4);

    -- Microservices: v1, v2 (live = v2; post has draft v3 content)
    INSERT INTO tb_post_publications (post_id, version, published_at, slug, title, description, content, format, cover_id)
    VALUES
        (v_post_micro, 1, '2024-02-20 14:30:00', 'microservices-patterns-spring-boot',
         'Essential Microservices Patterns with Spring Boot',
         'API Gateway, service discovery, circuit breakers, and tracing with Spring Cloud.',
         '== Microservices (v1)

First version: API Gateway and Eureka only.', 'ASCIIDOC', v_img2),
        (v_post_micro, 2, '2024-03-01 11:00:00', 'microservices-patterns-spring-boot',
         'Essential Microservices Patterns with Spring Boot',
         'API Gateway, service discovery, circuit breakers, and tracing with Spring Cloud.',
         '== Patterns in production

* Circuit breaker with fallbacks
* Distributed tracing with Zipkin', 'ASCIIDOC', v_img2);

    -- Point live publications
    UPDATE tb_posts SET live_publication_id = pub.id
    FROM tb_post_publications pub
    WHERE pub.post_id = tb_posts.id
      AND pub.version = (SELECT MAX(p2.version) FROM tb_post_publications p2 WHERE p2.post_id = tb_posts.id)
      AND tb_posts.published = TRUE;

    -- Publication tags mirror post tags at publish time
    INSERT INTO tb_post_publication_tags (publication_id, tag_id)
    SELECT pub.id, pt.tag_id
    FROM tb_post_publications pub
    JOIN tb_post_tags pt ON pt.post_id = pub.post_id;

    -- ============================================
    -- 10. Follow / subscribe (notifications feature)
    -- ============================================
    INSERT INTO tb_blog_audience (user_id, blog_id, followed, email_subscribed, created_at, updated_at)
    SELECT d.id, b.id, TRUE, FALSE, NOW(), NOW()
    FROM tb_users d, tb_blogs b
    JOIN tb_users owner ON b.owner_id = owner.id
    WHERE d.username = 'dave' AND owner.username = 'alice' AND b.main;

    INSERT INTO tb_blog_audience (user_id, blog_id, followed, email_subscribed, created_at, updated_at)
    SELECT e.id, b.id, FALSE, TRUE, NOW(), NOW()
    FROM tb_users e, tb_blogs b
    JOIN tb_users owner ON b.owner_id = owner.id
    WHERE e.username = 'eve' AND owner.username = 'alice' AND b.main;

    INSERT INTO tb_blog_audience (user_id, blog_id, followed, email_subscribed, created_at, updated_at)
    SELECT d.id, b.id, TRUE, FALSE, NOW(), NOW()
    FROM tb_users d, tb_blogs b
    JOIN tb_users owner ON b.owner_id = owner.id
    WHERE d.username = 'dave' AND owner.username = 'bob' AND b.slug = 'architecture-notes';

    -- ============================================
    -- 11. Sample in-app notifications
    -- ============================================
    INSERT INTO tb_notifications (recipient_user_id, type, blog_id, post_id, publication_id, actor_user_id, read, created_at)
    SELECT d.id, 'NEW_POST', b.id, p.id, pub.id, owner.id, FALSE, '2024-05-10 12:05:00'
    FROM tb_users d, tb_users owner, tb_blogs b, tb_posts p, tb_post_publications pub
    WHERE d.username = 'dave' AND owner.username = 'carol'
      AND b.owner_id = owner.id AND b.main
      AND p.blog_id = b.id AND p.slug = 'graphql-java-spring-boot'
      AND pub.post_id = p.id AND pub.version = 1;

    INSERT INTO tb_notifications (recipient_user_id, type, blog_id, post_id, publication_id, actor_user_id, read, created_at)
    SELECT owner.id, 'NEW_FOLLOW', b.id, NULL, NULL, d.id, FALSE, '2024-03-01 08:00:00'
    FROM tb_users owner, tb_users d, tb_blogs b
    WHERE owner.username = 'alice' AND d.username = 'dave'
      AND b.owner_id = owner.id AND b.main;

    INSERT INTO tb_notifications (recipient_user_id, type, blog_id, post_id, publication_id, actor_user_id, read, created_at)
    SELECT owner.id, 'NEW_SUBSCRIBE', b.id, NULL, NULL, e.id, TRUE, '2024-03-02 09:00:00'
    FROM tb_users owner, tb_users e, tb_blogs b
    WHERE owner.username = 'alice' AND e.username = 'eve'
      AND b.owner_id = owner.id AND b.main;

    INSERT INTO tb_notifications (recipient_user_id, type, blog_id, post_id, publication_id, actor_user_id, read, created_at)
    SELECT d.id, 'NEW_POST', b.id, p.id, pub.id, owner.id, TRUE, '2024-04-18 09:30:00'
    FROM tb_users d, tb_users owner, tb_blogs b, tb_posts p, tb_post_publications pub
    WHERE d.username = 'dave' AND owner.username = 'bob'
      AND b.owner_id = owner.id AND b.slug = 'architecture-notes'
      AND p.blog_id = b.id AND p.slug = 'event-sourcing-intro'
      AND pub.post_id = p.id AND pub.version = 1;

    -- ============================================
    -- 12. View counts
    -- ============================================
    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, NULL, 'anon-session-1', '2024-05-01 10:00:00'
    FROM tb_posts p WHERE p.slug = 'introduction-to-distributed-systems-java';

    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, NULL, 'anon-session-2', '2024-05-02 11:00:00'
    FROM tb_posts p WHERE p.slug = 'introduction-to-distributed-systems-java';

    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, (SELECT id FROM tb_users WHERE username = 'dave'), 'dave-session', '2024-05-03 12:00:00'
    FROM tb_posts p WHERE p.slug = 'graphql-java-spring-boot';

END $$;
