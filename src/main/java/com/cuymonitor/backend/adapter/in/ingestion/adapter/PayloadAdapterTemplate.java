package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.dto.IngestionEvent;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthEvent;
import com.cuymonitor.backend.domain.model.HealthSignal;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * <b>Template Method pattern — AbstractClass.</b> {@link #adapt} is the template method: it fixes the order of
 * the steps every source goes through and is {@code final}, so no adapter can skip or reorder them:
 *
 * <ol>
 *   <li>check the envelope has the type this adapter handles,</li>
 *   <li>check every required payload field is there,</li>
 *   <li>read the payload into the signal class,</li>
 *   <li>wrap it in a {@link HealthEvent}.</li>
 * </ol>
 *
 * <p>The three primitive operations ({@link #supportedType}, {@link #requiredFields}, {@link #signalType}) are
 * what changes from one source to another; the concrete adapters (camera, audio, weight) only fill those in.
 * Plain Java: an abstract class and three subclasses, no framework involved.
 */
public abstract class PayloadAdapterTemplate implements EventSourceAdapter {

    private final ObjectMapper objectMapper;

    protected PayloadAdapterTemplate(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /** The template method: the same four steps, in the same order, for every source. */
    @Override
    public final HealthEvent adapt(IngestionEvent event) {
        checkType(event);
        checkRequiredFields(event);
        HealthSignal signal = readSignal(event);
        return new HealthEvent(event.eventId(), event.cageId(), event.timestamp(), event.source(), signal);
    }

    /** Primitive operation: the event type this adapter accepts. */
    protected abstract EventType supportedType();

    /** Primitive operation: the payload fields that must be present and not null. */
    protected abstract List<String> requiredFields();

    /** Primitive operation: the domain signal the payload becomes. */
    protected abstract Class<? extends HealthSignal> signalType();

    private void checkType(IngestionEvent event) {
        if (event.type() != supportedType()) {
            throw new IllegalArgumentException("Adapter does not support event type: " + event.type());
        }
    }

    private void checkRequiredFields(IngestionEvent event) {
        for (String field : requiredFields()) {
            if (!event.payload().containsKey(field) || event.payload().get(field) == null) {
                throw new IllegalArgumentException("Missing required payload field: " + field);
            }
        }
    }

    private HealthSignal readSignal(IngestionEvent event) {
        try {
            JsonNode payload = objectMapper.valueToTree(event.payload());
            return objectMapper.readerFor(signalType())
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(payload);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + supportedType() + " payload: " + exception.getMessage(), exception);
        }
    }
}
