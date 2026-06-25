package dev.vepo.contraponto.shared;

/**
 * JUnit 5 tag names for parallel test execution ({@code mvn test -Ptest-unit},
 * etc.). {@code mvn clean test} with no profile runs every Surefire tag except
 * {@link #DOCKER_SMOKE} (Failsafe only, via {@code -Ptest-it}).
 */
public final class TestTags {

    public static final String UNIT = "unit";
    public static final String QUARKUS = "quarkus";
    public static final String WEB = "web";
    /**
     * Post/custom-page slugs may use
     * {@link dev.vepo.contraponto.custompage.CustomPagePaths#reservedSegments()}.
     */
    public static final String RESERVED_SLUGS = "reserved-slugs";
    /**
     * Packaged JVM image + prod-faithful docker compose stack ({@code -Ptest-it},
     * Failsafe).
     */
    public static final String DOCKER_SMOKE = "docker-smoke";

    private TestTags() {}
}
