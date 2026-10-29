package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigProfile;

public record RegisterGuineaPigCommand(String cageCode, String name, String markColor, GuineaPigProfile profile) {

    public RegisterGuineaPigCommand(String cageCode, String name, String markColor) {
        this(cageCode, name, markColor, GuineaPigProfile.EMPTY);
    }
}
