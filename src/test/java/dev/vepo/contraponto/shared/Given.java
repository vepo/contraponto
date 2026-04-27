package dev.vepo.contraponto.shared;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.image.ImageService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.user.User;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;

public interface Given {

    public static class PostBuilder {

        private String title;
        private String description;
        private String content;
        private User author;
        private Image cover;
        private String slug;
        private boolean published;

        private PostBuilder() {
            this.title = null;
            this.description = null;
            this.content = null;
            this.author = null;
            this.slug = null;
            this.published = true;
        }

        public Post persist() {
            return transaction(() -> {
                var post = new Post(title, cover, slug, description, content, author, this.published, LocalDateTime.now());
                if (Objects.isNull(post.getSlug()) || post.getSlug().isBlank()) {
                    post.setSlug(post.getTitle().toLowerCase().replaceAll("[^a-zA-Z0-9\\-]", "-"));
                }
                if (Objects.isNull(post.getDescription()) || post.getDescription().isBlank()) {
                    post.setDescription(post.getTitle());
                }
                inject(EntityManager.class).persist(post);
                return post;
            });
        }

        public PostBuilder withAuthor(User author) {
            this.author = author;
            return this;
        }

        public PostBuilder withCover(Image cover) {
            this.cover = cover;
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

        public PostBuilder withPublished(boolean published) {
            this.published = published;
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

    public static Image randomCover() {
        var image = randomImage();
        try {
            var imageResp = inject(ImageService.class).uploadImage(image.getFileName().toString(),
                                                                   "image/png", new FileInputStream(image.toFile()), Files.size(image));
            var imageDb = inject(ImageRepository.class).findByUuid(imageResp.getId());
            assertTrue(imageDb.isPresent());
            return imageDb.get();
        } catch (IOException ioe) {
            fail("Cannot create image!", ioe);
            return null;
        }
    }

    public static Path randomImage() {
        try {
            var tempImage = Files.createTempFile("test-cover", ".png");
            // Create a simple 1x1 red pixel PNG
            var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0xFFFF0000);
            ImageIO.write(image, "png", tempImage.toFile());
            return tempImage;
        } catch (IOException ioe) {
            fail("Fail to create random image!", ioe);
            return null;
        }
    }

    public static UserBuilder user() {
        return new UserBuilder();
    }
}
