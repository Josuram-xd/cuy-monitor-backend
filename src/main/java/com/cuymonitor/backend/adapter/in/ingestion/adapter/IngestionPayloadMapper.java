package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.EventType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class IngestionPayloadMapper {

    private final ObjectMapper objectMapper;

    IngestionPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    <T> T toPayload(IngestionEvent event, EventType expectedType, Class<T> payloadType, String... requiredFields) {
        if (event.type() != expectedType) {
            throw new IllegalArgumentException("Adapter does not support event type: " + event.type());
        }
        for (String field : requiredFields) {
            if (!event.payload().containsKey(field) || event.payload().get(field) == null) {
                throw new IllegalArgumentException("Missing required payload field: " + field);
            }
        }
        try {
            JsonNode payload = objectMapper.valueToTree(event.payload());
            return objectMapper.readerFor(payloadType)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + expectedType + " payload: " + exception.getMessage(), exception);
        }
    }
}
