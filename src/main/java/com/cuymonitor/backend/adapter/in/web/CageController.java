package com.cuymonitor.backend.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import com.cuymonitor.backend.adapter.in.web.dto.CageHealthResponse;
import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigResponse;
import com.cuymonitor.backend.adapter.in.web.dto.RegisterGuineaPigRequest;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.port.in.DeleteGuineaPigUseCase;
import com.cuymonitor.backend.domain.port.in.GetCageHealthUseCase;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigCommand;
import com.cuymonitor.backend.domain.port.in.RegisterGuineaPigUseCase;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cages/{cageId}")
public class CageController {

    private final GetCageHealthUseCase getCageHealthUseCase;
    private final ListGuineaPigsUseCase listGuineaPigsUseCase;
    private final RegisterGuineaPigUseCase registerGuineaPigUseCase;
    private final DeleteGuineaPigUseCase deleteGuineaPigUseCase;
    private final GetGuineaPigPhotoUseCase getGuineaPigPhotoUseCase;

    public CageController(GetCageHealthUseCase getCageHealthUseCase, ListGuineaPigsUseCase listGuineaPigsUseCase,
                          RegisterGuineaPigUseCase registerGuineaPigUseCase,
                          DeleteGuineaPigUseCase deleteGuineaPigUseCase,
                          GetGuineaPigPhotoUseCase getGuineaPigPhotoUseCase) {
        this.getCageHealthUseCase = getCageHealthUseCase;
        this.listGuineaPigsUseCase = listGuineaPigsUseCase;
        this.registerGuineaPigUseCase = registerGuineaPigUseCase;
        this.deleteGuineaPigUseCase = deleteGuineaPigUseCase;
        this.getGuineaPigPhotoUseCase = getGuineaPigPhotoUseCase;
    }

    @GetMapping("/health")
    public CageHealthResponse health(@PathVariable String cageId) {
        return CageHealthResponse.from(getCageHealthUseCase.getHealth(cageId));
    }

    @GetMapping("/guinea-pigs")
    public List<GuineaPigResponse> guineaPigs(@PathVariable String cageId) {
        return listGuineaPigsUseCase.list(cageId).stream().map(GuineaPigResponse::from).toList();
    }

    @PostMapping("/guinea-pigs")
    @ResponseStatus(HttpStatus.CREATED)
    public GuineaPigResponse registerGuineaPig(@PathVariable String cageId,
                                               @Valid @RequestBody RegisterGuineaPigRequest request) {
        return GuineaPigResponse.from(registerGuineaPigUseCase.register(
                new RegisterGuineaPigCommand(cageId, request.markColor(), request.toPhoto(), request.profile())));
    }

    /**
     * The photo of a cuy, which is how the breeder tells them apart. Private to the session, cached by the
     * browser for a day: a photo never changes, a new cuy is a new id.
     */
    @GetMapping("/guinea-pigs/{guineaPigId}/photo")
    public ResponseEntity<byte[]> guineaPigPhoto(@PathVariable String cageId, @PathVariable long guineaPigId) {
        GuineaPigPhoto photo = getGuineaPigPhotoUseCase.getPhoto(cageId, guineaPigId);
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
    public void deleteGuineaPig(@PathVariable String cageId, @PathVariable long guineaPigId) {
        deleteGuineaPigUseCase.delete(cageId, guineaPigId);
    }
}
