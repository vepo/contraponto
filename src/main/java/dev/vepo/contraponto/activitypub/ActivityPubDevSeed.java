package dev.vepo.contraponto.activitypub;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.UserRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ActivityPubDevSeed {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubDevSeed.class);

    private static final String DEV_FEDERATION_USERNAME = "alice";

    private final boolean devImportEnabled;
    private final ActivityPubSettings settings;
    private final UserRepository userRepository;
    private final ActivityPubActorService actorService;

    @Inject
    public ActivityPubDevSeed(@ConfigProperty(name = "app.dev-import.enabled", defaultValue = "false") boolean devImportEnabled,
                              ActivityPubSettings settings,
                              UserRepository userRepository,
                              ActivityPubActorService actorService) {
        this.devImportEnabled = devImportEnabled;
        this.settings = settings;
        this.userRepository = userRepository;
        this.actorService = actorService;
    }

    @Transactional
    public void seedAliceActor(@Observes @Priority(Interceptor.Priority.APPLICATION - 100) StartupEvent event) {
        if (!devImportEnabled || !settings.enabled()) {
            return;
        }
        userRepository.findByUsername(DEV_FEDERATION_USERNAME).ifPresent(user -> {
            if (actorService.findByUserId(user.getId()).isEmpty()) {
                actorService.enableFederation(user);
                logger.info("ActivityPub dev seed: federation enabled for user {}", DEV_FEDERATION_USERNAME);
            }
        });
    }
}
