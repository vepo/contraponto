package dev.vepo.contraponto.user;

import java.util.Optional;
import java.util.Set;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final PasswordService passwordService;
    private final UsernameValidator usernameValidator;

    @Inject
    public UserService(UserRepository userRepository,
                       BlogRepository blogRepository,
                       PasswordService passwordService,
                       UsernameValidator usernameValidator) {
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.passwordService = passwordService;
        this.usernameValidator = usernameValidator;
    }

    @Transactional
    public User createUser(String username, String name, String email, String password, Set<Role> roles, boolean active) {
        var user = new User();
        user.setUsername(username.trim());
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(passwordService.hashPassword(password));
        user.setActive(active);
        user.setRoles(roles.isEmpty() ? Set.of(Role.USER) : roles);
        userRepository.save(user);
        blogRepository.save(new Blog(user));
        return user;
    }

    public Optional<String> validateNewUser(String username, String name, String email, String password) {
        var usernameError = usernameValidator.validate(username);
        if (usernameError.isPresent()) {
            return usernameError;
        }

        if (name == null || name.isBlank()) {
            return Optional.of("Name is required.");
        }

        if (email == null || email.isBlank()) {
            return Optional.of("Email is required.");
        }

        if (!email.contains("@") || !email.contains(".")) {
            return Optional.of("Please enter a valid email address.");
        }

        if (password == null || password.isBlank()) {
            return Optional.of("Password is required.");
        }

        if (password.length() < 8) {
            return Optional.of("Password must be at least 8 characters.");
        }

        if (userRepository.existsByUsername(username.trim())) {
            return Optional.of("Username already taken.");
        }

        if (userRepository.existsByEmail(email.trim())) {
            return Optional.of("Email already registered.");
        }

        return Optional.empty();
    }
}
