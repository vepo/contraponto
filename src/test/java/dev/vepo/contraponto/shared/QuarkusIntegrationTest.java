package dev.vepo.contraponto.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

import io.quarkus.test.junit.QuarkusTest;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@QuarkusTest
@Tag(TestTags.QUARKUS)
public @interface QuarkusIntegrationTest {}
