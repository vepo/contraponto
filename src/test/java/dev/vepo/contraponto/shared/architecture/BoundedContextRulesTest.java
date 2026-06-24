package dev.vepo.contraponto.shared.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import io.quarkus.qute.TemplateExtension;
import jakarta.persistence.Entity;

@Tag("architecture")
class BoundedContextRulesTest {

    private static final String ROOT = "dev.vepo.contraponto";

    /** Documented kernel → identity exceptions for HTTP/session wiring. */
    private static final Set<String> SHARED_KERNEL_DOMAIN_ALLOWLIST = Set.of(
                                                                             "dev.vepo.contraponto.shared.infra.GenericExceptionMapper",
                                                                             "dev.vepo.contraponto.shared.infra.LoggedFilter");

    private static final DescribedPredicate<JavaClass> NOT_KERNEL_ALLOWLISTED = new DescribedPredicate<>("not documented kernel allowlist") {
        @Override
        public boolean test(JavaClass input) {
            return !SHARED_KERNEL_DOMAIN_ALLOWLIST.contains(input.getFullName());
        }
    };

    private static final JavaClasses CLASSES = new ClassFileImporter()
                                                                      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                                                      .importPackages(ROOT);

    @Test
    void entitiesMustNotResideInSharedKernel() {
        ArchRule rule = classes()
                                 .that()
                                 .areAnnotatedWith(Entity.class)
                                 .should()
                                 .resideOutsideOfPackage("..shared..");

        rule.check(CLASSES);
    }

    @Test
    void repositoriesMustNotDependOnEndpointsOrServices() {
        ArchRule rule = noClasses()
                                   .that()
                                   .haveSimpleNameEndingWith("Repository")
                                   .should()
                                   .dependOnClassesThat()
                                   .haveSimpleNameEndingWith("Endpoint")
                                   .orShould()
                                   .dependOnClassesThat()
                                   .haveSimpleNameEndingWith("Service");

        rule.check(CLASSES);
    }

    @Test
    void sharedKernelMustNotDependOnOtherContextsExceptAllowlist() {
        ArchRule rule = noClasses()
                                   .that()
                                   .resideInAnyPackage("..shared..")
                                   .and(NOT_KERNEL_ALLOWLISTED)
                                   .should()
                                   .dependOnClassesThat()
                                   .resideInAnyPackage(ROOT + "..")
                                   .andShould()
                                   .dependOnClassesThat()
                                   .resideOutsideOfPackages("..shared..", "java..", "javax..", "jakarta..", "io..", "org..",
                                                            "com..", "net..");

        rule.check(CLASSES);
    }

    @Test
    void templateExtensionsMustNotLiveInSharedInfra() {
        ArchRule rule = classes()
                                 .that()
                                 .areAnnotatedWith(TemplateExtension.class)
                                 .should()
                                 .resideOutsideOfPackage("..shared.infra..");

        rule.check(CLASSES);
    }
}
