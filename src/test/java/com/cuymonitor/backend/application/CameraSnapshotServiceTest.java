package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.port.out.HardwareStation;
import com.cuymonitor.backend.domain.port.out.LatestFrameSource;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CameraSnapshotServiceTest {

    private static final byte[] FRAME = {1, 2, 3};

    private final HardwareStation station = mock(HardwareStation.class);
    private final LatestFrameSource frames = mock(LatestFrameSource.class);
    private final CameraSnapshotService service = new CameraSnapshotService(station, frames);

    @Test
    void theSimulationCageShowsItsOwnVideoFrames() {
        given(frames.latest("sim-0a1b2c3d")).willReturn(Optional.of(FRAME));

        assertThat(service.latest("sim-0a1b2c3d")).contains(FRAME);
    }

    @Test
    void theCageWithTheKitShowsTheKitsCamera() {
        given(station.connectedTo("cage-1")).willReturn(true);
        given(frames.latest(HardwareStation.DEVICE_CODE)).willReturn(Optional.of(FRAME));

        assertThat(service.latest("cage-1")).contains(FRAME);
        verify(frames).latest("farm-station");
    }

    @Test
    void aCageWithoutTheKitHasNoPicture() {
        given(station.connectedTo("cage-2")).willReturn(false);

        assertThat(service.latest("cage-2")).isEmpty();
        verifyNoInteractions(frames);
    }
}
