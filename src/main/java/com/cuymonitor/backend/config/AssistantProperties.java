package com.cuymonitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The help assistant (application.yml, app.assistant).
 *
 * @param enabled        false = the assistant always answers that it is off (no AWS call)
 * @param region         AWS region of Amazon Bedrock
 * @param modelId        Bedrock model or inference profile, e.g. moonshotai.kimi-k2.5
 * @param maxTokens      longest answer the model may write
 * @param timeout        longest wait for the model before telling the breeder to try later
 * @param maxQuestions   questions an account may ask in each questionWindow (every one is a paid call)
 * @param questionWindow the sliding window of maxQuestions
 */
@ConfigurationProperties(prefix = "app.assistant")
public record AssistantProperties(boolean enabled, String region, String modelId, int maxTokens, Duration timeout,
                                  int maxQuestions, Duration questionWindow) {
}
