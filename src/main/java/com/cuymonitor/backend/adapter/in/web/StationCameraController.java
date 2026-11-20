package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase.CameraSettings;
import com.cuymonitor.backend.domain.port.in.ConfigureStationCameraUseCase.CameraView;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/** The camera of the kit, as the owner of the cage the kit is connected to sees and sets it. */
@RestController
@RequestMapping("/api/v1/cages/{cageId}/equipment/camera")
public class StationCameraController {
    private final CageOperations cages;
    private final ConfigureStationCameraUseCase camera;

    public StationCameraController(CageOperations cages, ConfigureStationCameraUseCase camera) {
        this.cages = cages;
        this.camera = camera;
    }

    @GetMapping
    public CameraView view(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        return camera.view(cageId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void save(Principal principal, @PathVariable String cageId, @RequestBody CameraSettings settings) {
        cages.getCage(Requester.idOf(principal), cageId);
        camera.save(cageId, settings);
    }
}
