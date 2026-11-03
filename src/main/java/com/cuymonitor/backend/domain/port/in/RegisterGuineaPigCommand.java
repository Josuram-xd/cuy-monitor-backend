package com.cuymonitor.backend.domain.port.in;

import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;

public record RegisterGuineaPigCommand(String cageCode, String markColor, GuineaPigPhoto photo,
                                       GuineaPigProfile profile) {

    public RegisterGuineaPigCommand(String cageCode, String markColor, GuineaPigPhoto photo) {
        this(cageCode, markColor, photo, GuineaPigProfile.EMPTY);
    }
}
