package com.cuymonitor.backend.adapter.out.assistant;

import com.cuymonitor.backend.domain.exception.AssistantUnavailableException;
import com.cuymonitor.backend.domain.model.assistant.ChatMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BedrockAssistantModelTest {

    private final BedrockRuntimeClient client = mock(BedrockRuntimeClient.class);
    private final BedrockAssistantModel model = new BedrockAssistantModel(client, "us.amazon.nova-2-lite-v1:0", 400);

    private static ConverseResponse answer(String... texts) {
        Message message = Message.builder().role(ConversationRole.ASSISTANT)
                .content(java.util.Arrays.stream(texts).map(ContentBlock::fromText).toList()).build();
        return ConverseResponse.builder().output(ConverseOutput.fromMessage(message)).build();
    }

    @Test
    void sendsTheInstructionsAndTheConversationThroughConverse() {
        given(client.converse(any(ConverseRequest.class))).willReturn(answer("Hola,", "soy Cuchi."));

        String reply = model.reply("be nice", List.of(ChatMessage.user("hola"), ChatMessage.assistant("¡hola!"),
                ChatMessage.user("¿qué es alerta?")));

        assertThat(reply).isEqualTo("Hola,\nsoy Cuchi.");
        ArgumentCaptor<ConverseRequest> sent = ArgumentCaptor.forClass(ConverseRequest.class);
        verify(client).converse(sent.capture());
        ConverseRequest request = sent.getValue();
        assertThat(request.modelId()).isEqualTo("us.amazon.nova-2-lite-v1:0");
        assertThat(request.system().getFirst().text()).isEqualTo("be nice");
        assertThat(request.messages()).extracting(Message::role)
                .containsExactly(ConversationRole.USER, ConversationRole.ASSISTANT, ConversationRole.USER);
        assertThat(request.messages().getLast().content().getFirst().text()).isEqualTo("¿qué es alerta?");
        assertThat(request.inferenceConfig().maxTokens()).isEqualTo(400);
    }

    @Test
    void anAwsErrorBecomesUnavailable() {
        given(client.converse(any(ConverseRequest.class))).willThrow(SdkClientException.create("no network"));

        assertThatThrownBy(() -> model.reply("x", List.of(ChatMessage.user("hola"))))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void anAnswerWithoutAMessageIsUnavailable() {
        given(client.converse(any(ConverseRequest.class))).willReturn(ConverseResponse.builder().build());

        assertThatThrownBy(() -> model.reply("x", List.of(ChatMessage.user("hola"))))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void theDisabledModelAlwaysSaysItIsOff() {
        assertThatThrownBy(() -> new DisabledAssistantModel().reply("x", List.of(ChatMessage.user("hola"))))
                .isInstanceOf(AssistantUnavailableException.class);
    }
}
