package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.MarkColor;

public record RegisterGuineaPigCommand (String cageCode, String name, MarkColor markColor) {
}
