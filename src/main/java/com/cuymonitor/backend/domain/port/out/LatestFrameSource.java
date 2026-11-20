package com.cuymonitor.backend.domain.port.out;

import java.util.Optional;

/** The last camera picture the AI service received for a cage, by the cage id the AI service knows it by. */
public interface LatestFrameSource {

    /** Empty when no picture has arrived for that cage yet. */
    Optional<byte[]> latest(String aiCageId);
}
