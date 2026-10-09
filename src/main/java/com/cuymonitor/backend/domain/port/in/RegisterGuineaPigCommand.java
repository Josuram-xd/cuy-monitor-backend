package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigProfile;
import com.cuymonitor.backend.domain.model.MarkColor;

public record RegisterGuineaPigCommand(String cageCode, String name, MarkColor markColor, GuineaPigProfile profile) {

    public RegisterGuineaPigCommand(String cageCode, String name, MarkColor markColor) {
        this(cageCode, name, markColor, GuineaPigProfile.EMPTY);
    }
}
