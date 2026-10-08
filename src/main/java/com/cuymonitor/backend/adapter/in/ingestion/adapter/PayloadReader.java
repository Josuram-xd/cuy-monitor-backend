package com.cuymonitor.backend.adapter.in.ingestion.adapter;

import com.cuymonitor.backend.adapter.in.ingestion.InvalidEventException;

import java.util.Locale;
import java.util.Map;

// reads typed fields from the raw JSON payload and explains exactly which one is wrong
class PayloadReader {

    private final Map<String, Object> payload;

    PayloadReader(Map<String, Object> payload) {
        this.payload = payload;
    }

    double number(String field) {
        if (!(require(field) instanceof Number number)) {
            throw invalid(field, "must be a number");
        }
        return number.doubleValue();
    }

    int integer(String field) {
        double value = number(field);
        if (value != Math.rint(value)) {
            throw invalid(field, "must be a whole number");
        }
        return (int) value;
    }

    boolean bool(String field) {
        if (!(require(field) instanceof Boolean value)) {
            throw invalid(field, "must be true or false");
        }
        return value;
    }

    <E extends Enum<E>> E enumValue(String field, Class<E> type) {
        if (!(require(field) instanceof String text)) {
            throw invalid(field, "must be a string");
        }
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalid(field, "has an unknown value '" + text + "'");
        }
    }

    private Object require(String field) {
        Object value = payload.get(field);
        if (value == null) {
            throw invalid(field, "is required");
        }
        return value;
    }

    private static InvalidEventException invalid(String field, String problem) {
        return new InvalidEventException("payload." + field + " " + problem);
    }
}
