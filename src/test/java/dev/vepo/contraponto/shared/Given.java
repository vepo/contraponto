package dev.vepo.contraponto.shared;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.user.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;

public class Given {

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

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
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

        public void persist() {
            transaction(() -> {
                var entityManager = inject(EntityManager.class);
                entityManager.persist(new User(requireNonNull(username, "'username' cannot be null"),
                                               requireNonNull(email, "'email' cannot be null"),
                                               requireNonNull(name, "'name' cannot be null"),
                                               inject(PasswordService.class).hashPassword(requireNonNull(password, "'password' cannot be null"))));
            });
        }
    }

    public static void cleanup() {
        transaction(() -> {
            var entityManager = inject(EntityManager.class);
            var query = entityManager.createQuery("DELETE FROM User");
            query.executeUpdate();
        });
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

    public static <T> T inject(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }
}
