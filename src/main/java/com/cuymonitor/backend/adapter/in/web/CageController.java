package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.CageHealthResponse;
import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigResponse;
import com.cuymonitor.backend.adapter.in.web.dto.RegisterGuineaPigRequest;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Duration;
import java.util.List;

/** What is inside one cage. Every call goes through the cage access proxy, which checks the owner. */
@RestController
@RequestMapping("/api/v1/cages/{cageId}")
public class CageController {

    private final CageOperations cageOperations;

    public CageController(CageOperations cageOperations) {
        this.cageOperations = cageOperations;
    }

    @GetMapping("/health")
    public CageHealthResponse health(Principal principal, @PathVariable String cageId) {
        return CageHealthResponse.from(cageOperations.getHealth(Requester.idOf(principal), cageId));
    }

    @GetMapping("/guinea-pigs")
    public List<GuineaPigResponse> guineaPigs(Principal principal, @PathVariable String cageId) {
        return cageOperations.listGuineaPigs(Requester.idOf(principal), cageId).stream()
                .map(GuineaPigResponse::from)
                .toList();
    }

    @PostMapping("/guinea-pigs")
    @ResponseStatus(HttpStatus.CREATED)
    public GuineaPigResponse registerGuineaPig(Principal principal, @PathVariable String cageId,
                                               @Valid @RequestBody RegisterGuineaPigRequest request) {
        return GuineaPigResponse.from(cageOperations.registerGuineaPig(Requester.idOf(principal), cageId,
                request.markColor(), request.toPhoto(), request.profile()));
    }

    /**
     * The photo of a cuy, which is how the breeder tells them apart. Private to the session, cached by the
     * browser for a day: a photo never changes, a new cuy is a new id.
     */
    @GetMapping("/guinea-pigs/{guineaPigId}/photo")
    public ResponseEntity<byte[]> guineaPigPhoto(Principal principal, @PathVariable String cageId,
                                                 @PathVariable long guineaPigId) {
        GuineaPigPhoto photo = cageOperations.getGuineaPigPhoto(Requester.idOf(principal), cageId, guineaPigId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                // the bytes come from whoever registered the cuy: never let the browser guess another type
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline")
                .body(photo.content());
    }

    /** Soft delete: the guinea pig disappears from the cage and frees its color; its history is kept. */
    @DeleteMapping("/guinea-pigs/{guineaPigId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGuineaPig(Principal principal, @PathVariable String cageId, @PathVariable long guineaPigId) {
        cageOperations.deleteGuineaPig(Requester.idOf(principal), cageId, guineaPigId);
    }
}
