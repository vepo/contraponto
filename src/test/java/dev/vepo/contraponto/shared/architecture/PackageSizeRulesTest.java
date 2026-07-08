package dev.vepo.contraponto.shared.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.ANONYMOUS_CLASSES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import dev.vepo.contraponto.shared.UnitTest;

@Tag("architecture")
@UnitTest
class PackageSizeRulesTest {

    private static final String ROOT = "dev.vepo.contraponto";

    private static final int DEFAULT_MAX_TYPES_PER_PACKAGE = 25;

    /**
     * Frozen ceilings for flat packages that predate the 25-type cap. Count must
     * not grow until the context is split into subpackages.
     */
    private static final Map<String, Integer> GRANDFATHERED_CEILINGS = Map.of(
                                                                              "dev.vepo.contraponto.auth",
                                                                              34,
                                                                              "dev.vepo.contraponto.messaging",
                                                                              44,
                                                                              "dev.vepo.contraponto.git",
                                                                              35,
                                                                              "dev.vepo.contraponto.image",
                                                                              33,
                                                                              "dev.vepo.contraponto.highlight",
                                                                              33,
                                                                              "dev.vepo.contraponto.notification",
                                                                              31);

    private static final DescribedPredicate<JavaClass> NAMED_TYPE = new DescribedPredicate<>("named type (not anonymous or synthetic)") {
        @Override
        public boolean test(JavaClass input) {
            return !input.getSimpleName().isEmpty();
        }
    };

    private static final JavaClasses CLASSES = new ClassFileImporter()
                                                                      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                                                      .importPackages(ROOT);

    @Test
    void activityPubAdminMustNotDependOnOtherSubpackages() {
        noClasses()
                   .that()
                   .resideInAnyPackage("..activitypub.admin..")
                   .should()
                   .dependOnClassesThat()
                   .resideInAnyPackage("..activitypub.actor..",
                                       "..activitypub.remote..",
                                       "..activitypub.security..",
                                       "..activitypub.discovery..",
                                       "..activitypub.inbox..",
                                       "..activitypub.outbox..",
                                       "..activitypub.delivery..")
                   .check(CLASSES);
    }

    @Test
    void activityPubDiscoveryMustNotDependOnDelivery() {
        noClasses()
                   .that()
                   .resideInAnyPackage("..activitypub.discovery..")
                   .should()
                   .dependOnClassesThat()
                   .resideInAnyPackage("..activitypub.delivery..")
                   .check(CLASSES);
    }

    @Test
    void activityPubInboxMustNotDependOnAdminOrDiscovery() {
        noClasses()
                   .that()
                   .resideInAnyPackage("..activitypub.inbox..")
                   .should()
                   .dependOnClassesThat()
                   .resideInAnyPackage("..activitypub.admin..", "..activitypub.discovery..")
                   .check(CLASSES);
    }

    @Test
    void activityPubOutboxMustNotDependOnInboxOrDelivery() {
        noClasses()
                   .that()
                   .resideInAnyPackage("..activitypub.outbox..")
                   .should()
                   .dependOnClassesThat()
                   .resideInAnyPackage("..activitypub.inbox..", "..activitypub.delivery..")
                   .check(CLASSES);
    }

    @Test
    void activityPubSecurityMustNotDependOnInboxOrDeliveryOrAdmin() {
        noClasses()
                   .that()
                   .resideInAnyPackage("..activitypub.security..")
                   .should()
                   .dependOnClassesThat()
                   .resideInAnyPackage("..activitypub.inbox..", "..activitypub.delivery..", "..activitypub.admin..")
                   .check(CLASSES);
    }

    @Test
    void packagesMustNotExceedTypeCountLimits() {
        var countsByPackage = new HashMap<String, Long>();
        for (var javaClass : CLASSES) {
            if (!NAMED_TYPE.test(javaClass) || ANONYMOUS_CLASSES.test(javaClass)) {
                continue;
            }
            if (javaClass.getEnclosingClass().isPresent()) {
                continue;
            }
            if (!javaClass.getPackageName().startsWith(ROOT)) {
                continue;
            }
            countsByPackage.merge(javaClass.getPackageName(), 1L, Long::sum);
        }

        var violations = new HashMap<String, String>();
        for (var entry : countsByPackage.entrySet()) {
            var packageName = entry.getKey();
            var count = entry.getValue().intValue();
            var maxAllowed = GRANDFATHERED_CEILINGS.getOrDefault(packageName, DEFAULT_MAX_TYPES_PER_PACKAGE);
            if (count > maxAllowed) {
                violations.put(packageName,
                               "%d types (max %d)".formatted(count, maxAllowed));
            }
        }

        assertThat(violations)
                              .as("Packages exceeding type count limits — split into subpackages or shrink before adding types")
                              .isEmpty();
    }
}
