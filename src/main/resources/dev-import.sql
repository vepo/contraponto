-- Dev seed data for contraponto (loaded on every dev startup via DatabaseDevSetup)
-- All dev users share the same bcrypt hash as admin (login with your admin password).
--
-- Highlights: multi-blog URLs, version history, comments (/comments moderation),
-- custom pages (/page/..., /{user}/page/..., /{user}/{blog}/page/...), git on alice's blog,
-- follow/subscribe, notifications, email log, featured/review, drafts, series (home → post → serie), tags, RSS/search.

-- ============================================
-- 1. Reset content (keep schema seed: admin user + blog + footer pages)
-- ============================================
DELETE FROM tb_custom_page_image_dependencies;
DELETE FROM tb_custom_pages WHERE slug NOT IN ('/sobre', '/contato', '/privacidade', '/termos');
TRUNCATE TABLE tb_post_comments CASCADE;
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
-- Clear image FKs before delete (V0.0.4 profile/banner columns reference tb_images; TRUNCATE CASCADE would wipe tb_users/tb_blogs)
UPDATE tb_users SET profile_picture_id = NULL, default_banner_id = NULL;
UPDATE tb_blogs SET banner_id = NULL;
UPDATE tb_posts SET cover_id = NULL;
UPDATE tb_post_publications SET cover_id = NULL;
DELETE FROM tb_images;

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
    ('eve', 'eve@contraponto.blog', 'Eve Subscriber', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', TRUE, NOW(), NOW()),
    ('ghost', 'ghost@contraponto.blog', 'Inactive Account', '$2a$10$9JdzFX8ar0ZcKP5bQkduBuwhFBRBgMW7.pCV4btIDcPgf.zdDGcCO', FALSE, NOW(), NOW());

INSERT INTO tb_user_roles (user_id, role)
SELECT u.id, v.role
FROM (VALUES
    ('editor', 'USER'),
    ('editor', 'EDITOR'),
    ('alice', 'USER'),
    ('bob', 'USER'),
    ('carol', 'USER'),
    ('dave', 'USER'),
    ('eve', 'USER'),
    ('ghost', 'USER')
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

UPDATE tb_blogs b
SET git_enabled = TRUE,
    git_remote_url = 'https://github.com/contraponto-dev/alice-on-systems.git',
    git_branch = 'main',
    git_last_known_commit = '7f3a9c2e1b0d4f8a6e5d4c3b2a19087'
FROM tb_users u
WHERE b.owner_id = u.id AND u.username = 'alice' AND b.main;

-- ============================================
-- 3b. Custom pages (footer pages come from Flyway migration)
-- ============================================
INSERT INTO tb_custom_pages (slug, section, title, content, placement, blog_id, published, created_at, published_at, updated_at)
VALUES (
    '/dev-playbook',
    'Platform',
    'Developer playbook',
    '<h2>Local development</h2>
    <p>Seed users share the <code>admin</code> password: <code>alice</code>, <code>bob</code>, <code>carol</code>, <code>editor</code>, <code>dave</code>, <code>eve</code>.</p>
    <ul>
        <li><a href="/review">Featured review</a> — sign in as <code>editor</code></li>
        <li><a href="/comments">Comment moderation</a> — sign in as <code>alice</code></li>
        <li><a href="/users">User admin</a> — sign in as <code>admin</code></li>
    </ul>',
    'NONE',
    NULL,
    TRUE,
    NOW(),
    NOW(),
    NOW()
);

INSERT INTO tb_custom_pages (slug, section, title, content, placement, blog_id, published, created_at, published_at, updated_at)
SELECT '/about', 'About', 'About Alice', '<p>Alice writes about distributed systems, Kafka, and observability on this blog.</p>', 'SIDEBAR', b.id, TRUE, NOW(), NOW(), NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_custom_pages (slug, section, title, content, placement, blog_id, published, created_at, published_at, updated_at)
SELECT '/reading-list', 'Resources', 'Architecture reading list', '<ul><li>Domain-Driven Design — Evans</li><li>Building Microservices — Newman</li><li>Designing Data-Intensive Applications — Kleppmann</li></ul>', 'SIDEBAR', b.id, TRUE, NOW(), NOW(), NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.slug = 'architecture-notes';

INSERT INTO tb_custom_pages (slug, section, title, content, placement, blog_id, published, created_at, published_at, updated_at)
SELECT '/about-draft', 'About', 'About Bob (draft)', '<p>Work in progress — publish from <a href="/pages">Manage pages</a>.</p>', 'NONE', b.id, FALSE, NOW(), NULL, NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.main;

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

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-alice-profile', 'alice-profile.jpg', 'image/jpeg', 90000, '/tmp/contraponto-images/alice-profile.jpg', 'https://picsum.photos/id/64/400/400', TRUE, NOW(), b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-alice-default-banner', 'alice-default-banner.jpg', 'image/jpeg', 150000, '/tmp/contraponto-images/alice-default-banner.jpg', 'https://picsum.photos/id/65/1200/400', TRUE, NOW(), b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

INSERT INTO tb_images (uuid, filename, content_type, size, file_path, url, active, created_at, blog_id, uploaded_by_user_id)
SELECT 'img-alice-blog-banner', 'alice-blog-banner.jpg', 'image/jpeg', 160000, '/tmp/contraponto-images/alice-blog-banner.jpg', 'https://picsum.photos/id/66/1200/400', TRUE, NOW(), b.id, u.id
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main;

UPDATE tb_users u
SET profile_picture_id = (SELECT id FROM tb_images WHERE uuid = 'img-alice-profile'),
    default_banner_id = (SELECT id FROM tb_images WHERE uuid = 'img-alice-default-banner')
WHERE u.username = 'alice';

UPDATE tb_blogs b
SET banner_id = (SELECT id FROM tb_images WHERE uuid = 'img-alice-blog-banner')
FROM tb_users u
WHERE b.owner_id = u.id AND u.username = 'alice' AND b.main;

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

INSERT INTO tb_series (blog_id, title, slug, created_at)
SELECT b.id, 'API design journey', 'api-design', NOW()
FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'carol' AND b.main;

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
    v_serie_api BIGINT;
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
    v_post_bob_main BIGINT;
    v_comment_intro_root BIGINT;
    v_comment_eve_pending BIGINT;
BEGIN
    SELECT b.id INTO v_admin_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'admin' AND b.main = TRUE;
    SELECT b.id INTO v_alice_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'alice' AND b.main = TRUE;
    SELECT b.id INTO v_bob_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.main = TRUE;
    SELECT b.id INTO v_bob_arch FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'bob' AND b.slug = 'architecture-notes';
    SELECT b.id INTO v_carol_blog FROM tb_blogs b JOIN tb_users u ON b.owner_id = u.id WHERE u.username = 'carol' AND b.main = TRUE;

    IF v_admin_blog IS NULL THEN
        RAISE EXCEPTION 'Admin main blog not found; migration seed may be missing';
    END IF;

    SELECT id INTO v_serie_dist FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'distributed-foundations';
    SELECT id INTO v_serie_events FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'event-streaming';
    SELECT id INTO v_serie_spring FROM tb_series WHERE blog_id = v_alice_blog AND slug = 'spring-ecosystem';
    SELECT id INTO v_serie_ddd FROM tb_series WHERE blog_id = v_bob_arch AND slug = 'ddd';
    SELECT id INTO v_serie_api FROM tb_series WHERE blog_id = v_carol_blog AND slug = 'api-design';

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
        'MARKDOWN', TRUE, TRUE,
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

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'cap-theorem-practical-guide',
        'The CAP Theorem: A Practical Guide for Java Teams',
        v_alice_blog,
        'What consistency, availability, and partition tolerance mean when you ship services, not textbooks.',
        '## Pick two — in production

When the network splits, every datastore forces a trade-off. This post walks through real outage stories and how teams document their chosen guarantees.',
        'MARKDOWN', TRUE, FALSE,
        '2024-01-25 10:00:00', '2024-01-25 10:00:00', '2024-01-25 10:00:00', v_img1, v_serie_dist
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'consensus-raft-paxos-overview',
        'Consensus Algorithms: Raft and Paxos for Practitioners',
        v_alice_blog,
        'Leader election, log replication, and when to reach for etcd or Consul instead of rolling your own.',
        '## Why consensus matters

Single-leader replication keeps state machines aligned across nodes. We compare Raft''s understandability with Paxos''s theoretical roots.',
        'MARKDOWN', TRUE, TRUE,
        '2024-02-05 11:00:00', '2024-02-05 11:00:00', '2024-02-05 11:00:00', v_img1, v_serie_dist
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'idempotency-keys-distributed-apis',
        'Idempotency Keys for Distributed HTTP APIs',
        v_alice_blog,
        'Safe retries without duplicate side effects — patterns for gateways and microservices.',
        '## At-least-once delivery

Clients retry; servers must deduplicate. Store idempotency keys with TTLs and return the original response on replay.',
        'MARKDOWN', TRUE, FALSE,
        '2024-03-01 09:30:00', '2024-03-01 09:30:00', '2024-03-01 09:30:00', NULL, v_serie_dist
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'dead-letter-queues-kafka',
        'Dead Letter Queues with Kafka and Spring',
        v_alice_blog,
        'Route poison messages to a DLT, alert operators, and replay after fixes without blocking the main topic.',
        '## When consumers fail

Retry with backoff, then publish to a dead-letter topic with headers that preserve the original partition and offset context.',
        'MARKDOWN', TRUE, TRUE,
        '2024-04-12 14:00:00', '2024-04-12 14:00:00', '2024-04-12 14:00:00', v_img3, v_serie_events
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'transactional-outbox-pattern',
        'The Transactional Outbox Pattern in Spring Boot',
        v_alice_blog,
        'Publish domain events reliably by writing to an outbox table in the same database transaction.',
        '## Dual writes are hard

Couple aggregate persistence with outbox rows; a separate relay process publishes to Kafka after commit.',
        'MARKDOWN', TRUE, FALSE,
        '2024-04-20 10:00:00', '2024-04-20 10:00:00', '2024-04-20 10:00:00', v_img3, v_serie_events
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'spring-cloud-gateway-routing',
        'Spring Cloud Gateway: Routing and Filters',
        v_alice_blog,
        'Centralize cross-cutting concerns — auth, rate limits, and request logging — at the edge.',
        '== Gateway routes

Predicate factories match paths and headers; filter chains rewrite requests before they hit downstream services.',
        'ASCIIDOC', TRUE, FALSE,
        '2024-03-15 13:00:00', '2024-03-15 13:00:00', '2024-03-15 13:00:00', v_img2, v_serie_spring
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'resilience4j-circuit-breakers',
        'Resilience4j Circuit Breakers in Production',
        v_alice_blog,
        'Tune failure thresholds, half-open probes, and fallbacks so cascading outages stop at the boundary.',
        '## Fail fast, recover safely

Monitor call rates and slow calls; open the circuit before thread pools exhaust waiting on a sick dependency.',
        'MARKDOWN', TRUE, TRUE,
        '2024-03-22 16:00:00', '2024-03-22 16:00:00', '2024-03-22 16:00:00', v_img2, v_serie_spring
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'kubernetes-health-probes-java',
        'Kubernetes Health Probes for Java Services',
        v_alice_blog,
        'Liveness, readiness, and startup probes with Quarkus and Spring Actuator endpoints.',
        '## Probe semantics

Readiness removes pods from Service endpoints; liveness restarts stuck JVMs. Align timeouts with your slowest cold start.',
        'MARKDOWN', TRUE, FALSE,
        '2024-05-15 08:00:00', '2024-05-15 08:00:00', '2024-05-15 08:00:00', v_img4, NULL
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'contract-testing-pact',
        'Contract Testing with Pact and Testcontainers',
        v_alice_blog,
        'Consumer-driven contracts that catch breaking API changes before integration environments do.',
        '## Contracts as CI gates

Providers verify published pacts; consumers pin expectations per release train.',
        'MARKDOWN', TRUE, TRUE,
        '2024-05-20 10:00:00', '2024-05-20 10:00:00', '2024-05-20 10:00:00', NULL, NULL
    );

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

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'tactical-ddd-aggregates',
        'Tactical DDD: Aggregates and Repositories',
        v_bob_arch,
        'Consistency boundaries, invariant enforcement, and repository interfaces that match the domain.',
        '## One aggregate per transaction

Keep clusters small; reference other aggregates by identity, not object graphs.',
        'MARKDOWN', TRUE, TRUE,
        '2024-03-20 11:00:00', '2024-03-20 11:00:00', '2024-03-20 11:00:00', v_img6, v_serie_ddd
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'context-mapping-workshop',
        'Context Mapping Workshop Notes',
        v_bob_arch,
        'Facilitation tips for drawing bounded contexts with product and platform teams.',
        '## Workshop flow

Start from user journeys, name contexts, then negotiate upstream/downstream relationships on a wall-sized map.',
        'MARKDOWN', TRUE, FALSE,
        '2024-04-01 15:00:00', '2024-04-01 15:00:00', '2024-04-01 15:00:00', NULL, v_serie_ddd
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'cqrs-read-models',
        'CQRS Read Models Without the Hype',
        v_bob_arch,
        'When separate write and read models pay off — and when a single model is enough.',
        '## Projections

Optimize queries with denormalized views fed by domain events, but measure complexity before splitting stacks.',
        'MARKDOWN', TRUE, FALSE,
        '2024-05-05 09:00:00', '2024-05-05 09:00:00', '2024-05-05 09:00:00', v_img6, v_serie_ddd
    );

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
        '2024-05-10 12:00:00', '2024-05-10 12:00:00', '2024-05-10 12:00:00', v_img7, v_serie_api
    ) RETURNING id INTO v_post_graphql;

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'rest-api-versioning-strategies',
        'REST API Versioning Strategies That Scale',
        v_carol_blog,
        'URL paths, headers, and content negotiation — pick a default and document migration windows.',
        '## Versioning trade-offs

Explicit `/v2` paths are easy to cache; `Accept` headers keep URLs stable but complicate client libraries.',
        'MARKDOWN', TRUE, TRUE,
        '2024-05-18 10:00:00', '2024-05-18 10:00:00', '2024-05-18 10:00:00', v_img7, v_serie_api
    );

    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'cursor-pagination-rest-apis',
        'Cursor Pagination for REST APIs',
        v_carol_blog,
        'Stable pages under concurrent writes using opaque cursors instead of offset/limit.',
        '## Why offsets break

Large `OFFSET` scans get slow; cursors encode sort keys so clients always move forward through live data.',
        'MARKDOWN', TRUE, FALSE,
        '2024-05-22 14:00:00', '2024-05-22 14:00:00', '2024-05-22 14:00:00', NULL, v_serie_api
    );

    -- Bob: main blog (public URL /bob/post/... — distinct from architecture-notes)
    INSERT INTO tb_posts (slug, title, blog_id, description, content, format, published, featured, created_at, updated_at, published_at, cover_id, serie_id)
    VALUES (
        'pragmatic-monoliths',
        'Pragmatic Monoliths Before Microservices',
        v_bob_blog,
        'When a modular monolith beats premature distribution.',
        '## Start simple

Extract services only when team boundaries and scaling needs are clear — not because diagrams look modern.',
        'MARKDOWN', TRUE, FALSE,
        '2024-05-01 10:00:00', '2024-05-01 10:00:00', '2024-05-01 10:00:00', NULL, NULL
    ) RETURNING id INTO v_post_bob_main;

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
    WHERE p.blog_id = v_alice_blog AND p.serie_id = v_serie_dist
      AND p.id NOT IN (v_post_intro, v_post_obs, v_post_loom)
      AND t.slug IN ('java', 'distributed-systems')
    ON CONFLICT (post_id, tag_id) DO NOTHING;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.blog_id = v_alice_blog AND p.serie_id = v_serie_events AND t.slug IN ('java', 'kafka', 'spring-boot')
    ON CONFLICT (post_id, tag_id) DO NOTHING;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.blog_id = v_alice_blog AND p.serie_id = v_serie_spring AND t.slug IN ('java', 'spring-boot', 'microservices')
    ON CONFLICT (post_id, tag_id) DO NOTHING;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.blog_id = v_bob_arch AND p.serie_id = v_serie_ddd AND t.slug IN ('ddd', 'distributed-systems')
    ON CONFLICT (post_id, tag_id) DO NOTHING;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.blog_id = v_carol_blog AND p.serie_id = v_serie_api AND t.slug IN ('graphql', 'java')
    ON CONFLICT (post_id, tag_id) DO NOTHING;

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p JOIN tb_tags t ON t.slug = 'kubernetes'
    WHERE p.slug = 'kubernetes-health-probes-java';

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.slug = 'contract-testing-pact' AND t.slug IN ('java', 'testing', 'microservices');

    INSERT INTO tb_post_tags (post_id, tag_id)
    SELECT p.id, t.id FROM tb_posts p
    CROSS JOIN tb_tags t
    WHERE p.id = v_post_bob_main AND t.slug IN ('java', 'microservices', 'ddd');

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

    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, NULL, 'anon-session-3', '2024-05-18 09:00:00'
    FROM tb_posts p WHERE p.slug = 'consensus-raft-paxos-overview';

    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, NULL, 'anon-session-4', '2024-05-19 10:00:00'
    FROM tb_posts p WHERE p.slug = 'dead-letter-queues-kafka';

    INSERT INTO tb_views (post_id, user_id, session_id, viewed_at)
    SELECT p.id, NULL, 'anon-session-5', '2024-05-20 11:00:00'
    FROM tb_posts p WHERE p.slug = 'rest-api-versioning-strategies';

    -- ============================================
    -- 13. Post comments (threads, moderation, /comments inbox)
    -- ============================================
    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_intro,
        (SELECT id FROM tb_users WHERE username = 'dave'),
        NULL, NULL,
        'Clear explanation of CAP — especially the partition tolerance trade-off.',
        'APPROVED', '2024-05-04 10:00:00', '2024-05-04 10:00:00'
    ) RETURNING id INTO v_comment_intro_root;

    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_intro,
        (SELECT id FROM tb_users WHERE username = 'alice'),
        v_comment_intro_root, v_comment_intro_root,
        'Thanks Dave — a CRDT follow-up is on my draft list.',
        'APPROVED', '2024-05-04 11:30:00', '2024-05-04 11:30:00'
    );

    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_intro,
        (SELECT id FROM tb_users WHERE username = 'eve'),
        NULL, NULL,
        'Could you cover consensus algorithms in a future post?',
        'PENDING', '2024-05-05 09:00:00', NULL
    ) RETURNING id INTO v_comment_eve_pending;

    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_kafka,
        (SELECT id FROM tb_users WHERE username = 'bob'),
        NULL, NULL,
        'We use Spring Kafka daily — error handler tips would help.',
        'PENDING', '2024-05-06 14:00:00', NULL
    );

    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_graphql,
        (SELECT id FROM tb_users WHERE username = 'dave'),
        NULL, NULL,
        'Batch loaders saved us from N+1 queries on our schema.',
        'APPROVED', '2024-05-11 08:00:00', '2024-05-11 08:00:00'
    );

    INSERT INTO tb_post_comments (post_id, author_id, parent_id, root_id, body, status, created_at, approved_at)
    VALUES (
        v_post_intro,
        (SELECT id FROM tb_users WHERE username = 'bob'),
        NULL, NULL,
        'Promotional link removed by moderator.',
        'REJECTED', '2024-05-03 12:00:00', NULL
    );

    INSERT INTO tb_notifications (recipient_user_id, type, blog_id, post_id, publication_id, actor_user_id, read, created_at, comment_id)
    SELECT owner.id, 'NEW_COMMENT', b.id, v_post_intro, pub.id, e.id, FALSE, '2024-05-05 09:05:00', v_comment_eve_pending
    FROM tb_users owner, tb_users e, tb_blogs b, tb_post_publications pub
    WHERE owner.username = 'alice' AND e.username = 'eve'
      AND b.owner_id = owner.id AND b.main
      AND pub.post_id = v_post_intro
      AND pub.version = (SELECT MAX(p2.version) FROM tb_post_publications p2 WHERE p2.post_id = v_post_intro);

    -- ============================================
    -- 14. Email notification log (subscribe flow)
    -- ============================================
    INSERT INTO tb_email_notification_log (publication_id, user_id, sent_at)
    SELECT pub.id, e.id, '2024-04-05 10:30:00'
    FROM tb_users e, tb_post_publications pub
    WHERE e.username = 'eve' AND pub.post_id = v_post_kafka AND pub.version = 1
    ON CONFLICT (publication_id, user_id) DO NOTHING;

    INSERT INTO tb_email_notification_log (publication_id, user_id, sent_at)
    SELECT pub.id, e.id, '2024-05-10 12:10:00'
    FROM tb_users e, tb_post_publications pub
    WHERE e.username = 'eve' AND pub.post_id = v_post_graphql AND pub.version = 1
    ON CONFLICT (publication_id, user_id) DO NOTHING;

END $$;
