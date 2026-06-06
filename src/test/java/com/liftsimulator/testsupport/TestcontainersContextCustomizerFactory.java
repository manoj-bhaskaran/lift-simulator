package com.liftsimulator.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liftsimulator.BaseIntegrationTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Globally-registered factory (see {@code META-INF/spring.factories}) that
 * points database-backed Spring test contexts at the shared Testcontainers
 * PostgreSQL instance when no external database is configured.
 *
 * <p>Applying the wiring through a {@link ContextCustomizerFactory} rather than
 * a per-class {@code @DynamicPropertySource} means it covers the DB-backed test
 * slices uniformly — {@code @SpringBootTest} integration tests (subclasses of
 * {@link BaseIntegrationTest}) and the {@code @DataJpaTest} repository tests
 * alike — so {@code mvn test} runs with no pre-existing PostgreSQL. Web/unit
 * slices that need no datasource are skipped.
 *
 * <p>When {@code SPRING_DATASOURCE_URL} is set (as in CI) the customizer is a
 * no-op: the workflow's service-container datasource flows through the
 * {@code application-test.yml} placeholders unchanged.
 */
public class TestcontainersContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        // Defer to the externally-configured datasource (e.g. the CI service container).
        if (System.getenv("SPRING_DATASOURCE_URL") != null) {
            return null;
        }
        // Only DB-backed slices get the container, so web/unit Spring contexts
        // (e.g. @WebMvcTest) don't pay the cost of starting PostgreSQL.
        if (!requiresDatabase(testClass)) {
            return null;
        }
        return TestcontainersDatasourceCustomizer.INSTANCE;
    }

    private static boolean requiresDatabase(Class<?> testClass) {
        return BaseIntegrationTest.class.isAssignableFrom(testClass)
                || AnnotatedElementUtils.hasAnnotation(testClass, DataJpaTest.class);
    }

    /**
     * Adds a high-precedence property source pointing the datasource at the
     * shared container, starting it on first use.
     */
    static final class TestcontainersDatasourceCustomizer implements ContextCustomizer {

        static final TestcontainersDatasourceCustomizer INSTANCE = new TestcontainersDatasourceCustomizer();

        private static final String SOURCE_NAME = "testcontainers-postgres";

        private TestcontainersDatasourceCustomizer() {
        }

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
            var propertySources = context.getEnvironment().getPropertySources();
            if (propertySources.contains(SOURCE_NAME)) {
                return;
            }
            PostgreSQLContainer<?> postgres = SharedPostgresContainer.getStarted();
            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", postgres.getJdbcUrl());
            properties.put("spring.datasource.username", postgres.getUsername());
            properties.put("spring.datasource.password", postgres.getPassword());
            propertySources.addFirst(new MapPropertySource(SOURCE_NAME, properties));
        }

        // Equal for all instances so Spring's context cache treats contexts that
        // differ only by this customizer as identical (single shared container).
        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestcontainersDatasourceCustomizer;
        }

        @Override
        public int hashCode() {
            return TestcontainersDatasourceCustomizer.class.hashCode();
        }
    }
}
