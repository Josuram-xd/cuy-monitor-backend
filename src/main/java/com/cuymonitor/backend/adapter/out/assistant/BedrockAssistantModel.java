package com.cuymonitor.backend.adapter.out.assistant;

import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import com.cuymonitor.backend.domain.model.assistant.ChatRole;
import com.cuymonitor.backend.domain.port.out.AssistantModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The help assistant on Amazon Bedrock, through the Converse API (the same request for Nova, Claude or Llama:
 * changing app.assistant.model-id is enough). On the EC2 the credentials come from the instance role, the same one
 * the AI service already uses for Bedrock; there is no key in the configuration.
 */
public class BedrockAssistantModel implements AssistantModel {

    private static final Logger log = LoggerFactory.getLogger(BedrockAssistantModel.class);
    // low: the answers are explanations of the data, not creative writing
    private static final float TEMPERATURE = 0.3f;

    private final BedrockRuntimeClient client;
    private final String modelId;
    private final int maxTokens;

    public BedrockAssistantModel(BedrockRuntimeClient client, String modelId, int maxTokens) {
        this.client = Objects.requireNonNull(client, "client");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.maxTokens = maxTokens;
    }

    @Override
    public String reply(String instructions, List<ChatMessage> conversation) {
        ConverseRequest request = ConverseRequest.builder()
                .modelId(modelId)
                .system(SystemContentBlock.fromText(instructions))
                .messages(conversation.stream().map(BedrockAssistantModel::toBedrock).toList())
                .inferenceConfig(InferenceConfiguration.builder()
                        .maxTokens(maxTokens)
                        .temperature(TEMPERATURE)
                        .build())
                .build();
        ConverseResponse response;
        try {
            response = client.converse(request);
        } catch (SdkException ex) {
            // throttling, no access to the model, network...: the breeder only needs "try again later"
            log.warn("Bedrock could not answer the assistant: {}", ex.getMessage());
            throw new AssistantUnavailableException("the assistant model did not answer", ex);
        }
        if (response.output() == null || response.output().message() == null) {
            throw new AssistantUnavailableException("the assistant model gave no message");
        }
        return response.output().message().content().stream()
                .map(ContentBlock::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private static Message toBedrock(ChatMessage message) {
        return Message.builder()
                .role(message.role() == ChatRole.USER ? ConversationRole.USER : ConversationRole.ASSISTANT)
                .content(ContentBlock.fromText(message.text()))
                .build();
    }
}
