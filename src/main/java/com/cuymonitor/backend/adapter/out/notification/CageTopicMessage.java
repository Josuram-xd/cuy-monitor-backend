package com.cuymonitor.backend.adapter.out.notification;

import java.time.Instant;

// envelope of every message on /topic/cages/{cageId} (docs/contracts/rest-api.md, section 4)
public record CageTopicMessage(String type, String cageId, Instant occurredAt, Object data) {
}
