package dev.vepo.contraponto.shared.infra;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DatabaseDevSetup {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDevSetup.class);

    private final EntityManager entityManager;
    private final boolean devImportEnabled;

    @Inject
    public DatabaseDevSetup(EntityManager entityManager,
                            @ConfigProperty(name = "app.dev-import.enabled", defaultValue = "false") boolean devImportEnabled) {
        this.entityManager = entityManager;
        this.devImportEnabled = devImportEnabled;
    }

    @Transactional
    public void onStart(@Observes StartupEvent event) throws IOException {
        if (!devImportEnabled) {
            return;
        }
        logger.info("Executing /dev-import.sql script...");
        try (var script = DatabaseDevSetup.class.getResourceAsStream("/dev-import.sql")) {
            if (script == null) {
                throw new IllegalStateException("Missing classpath resource /dev-import.sql");
            }
            this.entityManager.createNativeQuery(new String(script.readAllBytes())).executeUpdate();
        }
        logger.info("/dev-import.sql script executed");
    }
}
