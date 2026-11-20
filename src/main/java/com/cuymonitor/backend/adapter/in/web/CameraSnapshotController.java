package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.port.in.GetLatestCameraFrameUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * The live picture of a cage: the bytes of the last camera frame, for an img tag (like the guinea pig photos). Only
 * the owner of the cage gets it, and the browser must not keep it.
 */
@RestController
@RequestMapping("/api/v1/cages/{cageId}/camera")
public class CameraSnapshotController {

    private final CageOperations cages;
    private final GetLatestCameraFrameUseCase camera;

    public CameraSnapshotController(CageOperations cages, GetLatestCameraFrameUseCase camera) {
        this.cages = cages;
        this.camera = camera;
    }

    @GetMapping("/latest")
    public ResponseEntity<byte[]> latest(Principal principal, @PathVariable String cageId) {
        cages.getCage(Requester.idOf(principal), cageId);
        return camera.latest(cageId)
                .map(frame -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                        .header("X-Content-Type-Options", "nosniff")
                        .body(frame))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
