package com.cuymonitor.backend.domain.port.in;

import java.util.UUID;

public record VerifyOtpCommand (UUID challengeId, String code) {

    @Override
    public String toString(){
        return "id: " + challengeId + ", code: ***";
    }

}
