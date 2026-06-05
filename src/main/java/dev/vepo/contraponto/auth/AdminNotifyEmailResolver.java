package dev.vepo.contraponto.auth;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AdminNotifyEmailResolver {

    private final UserRepository userRepository;
    private final Optional<String> configuredEmails;

    @Inject
    public AdminNotifyEmailResolver(UserRepository userRepository,
                                    @ConfigProperty(name = "app.admin.notify-email") Optional<String> configuredEmails) {
        this.userRepository = userRepository;
        this.configuredEmails = configuredEmails;
    }

    public List<String> resolve() {
        if (configuredEmails.isPresent() && !configuredEmails.get().isBlank()) {
            return Arrays.stream(configuredEmails.get().split(","))
                         .map(String::trim)
                         .filter(email -> !email.isBlank())
                         .distinct()
                         .toList();
        }
        return userRepository.findAdministratorEmails();
    }
}
