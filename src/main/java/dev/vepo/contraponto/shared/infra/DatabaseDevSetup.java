package dev.vepo.contraponto.shared.infra;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
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
    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object event) throws IOException {
        if (!devImportEnabled) {
            return;
        }
        logger.info("Execuing /dev-import.sql script...");
        this.entityManager.createNativeQuery(new String(DatabaseDevSetup.class.getResourceAsStream("/dev-import.sql").readAllBytes()))
                          .executeUpdate();
        logger.info("/dev-import.sql script executed!!");
    }
}
