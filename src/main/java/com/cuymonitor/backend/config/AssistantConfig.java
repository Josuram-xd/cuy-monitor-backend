package com.cuymonitor.backend.config;

import com.cuymonitor.backend.adapter.out.assistant.BedrockAssistantModel;
import com.cuymonitor.backend.adapter.out.assistant.DisabledAssistantModel;
import com.cuymonitor.backend.application.AssistantService;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.out.AssistantModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.time.Clock;

/**
 * Wires the help assistant. The service reads the cages through the {@link CageOperations} proxy, so it can only
 * ever see the cages of the account that asks.
 */
@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {

    @Bean
    public AssistantModel assistantModel(AssistantProperties properties) {
        if (!properties.enabled()) {
            return new DisabledAssistantModel();
        }
        // the light JDK HTTP client: one call every few seconds does not need Netty or Apache
        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(Region.of(properties.region()))
                .httpClient(UrlConnectionHttpClient.create())
                .overrideConfiguration(o -> o.apiCallTimeout(properties.timeout()))
                .build();
        return new BedrockAssistantModel(client, properties.modelId(), properties.maxTokens());
    }

    @Bean
    public AssistantService assistantService(AssistantModel assistantModel, CageOperations cageOperations,
                                             Clock clock, AssistantProperties properties) {
        return new AssistantService(assistantModel, cageOperations, clock, properties.maxQuestions(),
                properties.questionWindow());
    }
}
