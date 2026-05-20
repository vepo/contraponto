package dev.vepo.contraponto.shared;

/**
 * JUnit 5 tag names for parallel test execution ({@code mvn test -Ptest-unit},
 * etc.). {@code mvn clean test} with no profile runs every tag.
 */
public final class TestTags {

    public static final String UNIT = "unit";
    public static final String QUARKUS = "quarkus";
    public static final String WEB = "web";

    private TestTags() {}
}
