package com.liftsimulator.admin.config;

import java.util.List;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * Configures the auto-configured {@link JsonMapper} to enforce strict schema validation.
 */
@Configuration
public class JacksonConfiguration {

    private static final int MAX_JSON_NESTING_DEPTH = 100;
    private static final int MAX_JSON_STRING_LENGTH = 1_048_576;

    /**
     * Supplies the {@link JsonMapper.Builder} that Spring Boot builds its auto-configured
     * {@code JsonMapper} from, seeded with a {@link JsonFactory} that enforces our stricter
     * {@link StreamReadConstraints} (maximum nesting depth and string length).
     *
     * <p>Jackson 3 owns stream-read constraints on the {@link tools.jackson.core.TokenStreamFactory},
     * which a {@link JsonMapperBuilderCustomizer} cannot replace on an already-created builder.
     * Overriding Boot's {@code @ConditionalOnMissingBean} builder is therefore the supported way to
     * apply them. Every auto-configured {@link JsonMapperBuilderCustomizer} (module discovery,
     * {@code spring.jackson.*} properties, ProblemDetail support) is re-applied so no Boot default
     * is lost, and {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} is asserted last so JSON
     * with unexpected fields is rejected regardless of Jackson 3's flipped defaults.
     *
     * @param customizers the auto-configured Jackson builder customizers to apply
     * @return the strict JSON mapper builder
     */
    @Bean
    public JsonMapper.Builder jsonMapperBuilder(List<JsonMapperBuilderCustomizer> customizers) {
        StreamReadConstraints readConstraints = StreamReadConstraints.builder()
            .maxNestingDepth(MAX_JSON_NESTING_DEPTH)
            .maxStringLength(MAX_JSON_STRING_LENGTH)
            .build();
        JsonFactory jsonFactory = JsonFactory.builder()
            .streamReadConstraints(readConstraints)
            .build();

        JsonMapper.Builder builder = JsonMapper.builder(jsonFactory);
        customizers.forEach(customizer -> customizer.customize(builder));
        builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return builder;
    }
}
