package dev.vepo.contraponto.shared;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;

public class Given {

    public static class PostBuilder {

        private String title;
        private String description;
        private String content;
        private User author;
        private String slug;

        private PostBuilder() {
            this.title = null;
            this.description = null;
            this.content = null;
            this.author = null;
            this.slug = null;
        }

        public Post persist() {
            return transaction(() -> {
                var post = new Post(title, slug, description, content, author, true, LocalDateTime.now());
                inject(EntityManager.class).persist(post);
                return post;
            });
        }

        public PostBuilder withAuthor(User author) {
            this.author = author;
            return this;
        }

        public PostBuilder withContent(String content) {
            this.content = content;
            return this;
        }

        public PostBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PostBuilder withSlug(String slug) {
            this.slug = slug;
            return this;
        }

        public PostBuilder withTitle(String title) {
            this.title = title;
            return this;
        }
    }

    public static class UserBuilder {

        private String username;
        private String email;
        private String name;
        private String password;

        private UserBuilder() {
            this.username = null;
            this.email = null;
            this.name = null;
            this.password = null;
        }

        public User persist() {
            return transaction(() -> {
                var entityManager = inject(EntityManager.class);
                var user = new User(requireNonNull(username, "'username' cannot be null"),
                                    requireNonNull(email, "'email' cannot be null"),
                                    requireNonNull(name, "'name' cannot be null"),
                                    inject(PasswordService.class).hashPassword(requireNonNull(password, "'password' cannot be null")));
                entityManager.persist(user);
                return user;
            });
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }
    }

    public static void cleanup() {
        transaction(() -> {
            var entityManager = inject(EntityManager.class);
            var query = entityManager.createQuery("DELETE FROM Post");
            query.executeUpdate();
            query = entityManager.createQuery("DELETE FROM User");
            query.executeUpdate();
        });
    }

    public static <T> T inject(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    public static PostBuilder post() {
        return new PostBuilder();
    }

    public static void transaction(Runnable code) {
        try {
            QuarkusTransaction.begin();
            code.run();
            QuarkusTransaction.commit();
        } catch (Exception e) {
            QuarkusTransaction.rollback();
            fail("Fail to create transaction!", e);
        }
    }

    public static <T> T transaction(Supplier<T> code) {
        try {
            QuarkusTransaction.begin();
            T value = code.get();
            QuarkusTransaction.commit();
            return value;
        } catch (Exception e) {
            QuarkusTransaction.rollback();
            fail("Fail to create transaction!", e);
            return null;
        }
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }
}
