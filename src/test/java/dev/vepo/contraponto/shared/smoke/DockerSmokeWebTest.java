package dev.vepo.contraponto.shared.smoke;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.vepo.contraponto.shared.TestTags;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag(TestTags.DOCKER_SMOKE)
@ExtendWith({ DockerSmokeStackExtension.class, DockerSmokeWebExtension.class })
public @interface DockerSmokeWebTest {}
