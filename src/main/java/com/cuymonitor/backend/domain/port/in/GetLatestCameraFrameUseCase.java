package com.cuymonitor.backend.domain.port.in;

import java.util.Optional;

/** The live picture of a cage: the last camera frame the AI service received for it, if any. */
public interface GetLatestCameraFrameUseCase {

    /** Empty when the cage shows no camera (no kit connected, not a simulation) or no picture arrived yet. */
    Optional<byte[]> latest(String cageCode);
}
