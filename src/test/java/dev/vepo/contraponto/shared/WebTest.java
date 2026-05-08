package dev.vepo.contraponto.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTestExtension;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({ QuarkusTestExtension.class, WebTestExtension.class })
public @interface WebTest {}