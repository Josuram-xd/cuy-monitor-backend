package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.adapter.in.web.dto.GuineaPigReferenceResponse;
import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.port.in.GetGuineaPigPhotoUseCase;
import com.cuymonitor.backend.domain.port.in.ListGuineaPigsUseCase;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The guinea pigs of a cage with their registered photos, for the ai-service to recognize them by their coat.
 * Machine access with the X-API-Key, like the ingestion endpoints: it is not the dashboard and not a user.
 */
@RestController
@RequestMapping("/api/v1/ingestion/cages/{cageCode}/guinea-pigs")
public class GuineaPigReferenceController {

    private final ApiKeyCheck apiKey;
    private final ListGuineaPigsUseCase listGuineaPigs;
    private final GetGuineaPigPhotoUseCase getGuineaPigPhoto;

    public GuineaPigReferenceController(@Value("${app.api-key}") String apiKey,
                                        ListGuineaPigsUseCase listGuineaPigs,
                                        GetGuineaPigPhotoUseCase getGuineaPigPhoto) {
        this.apiKey = new ApiKeyCheck(apiKey);
        this.listGuineaPigs = listGuineaPigs;
        this.getGuineaPigPhoto = getGuineaPigPhoto;
    }

    @GetMapping
    public ResponseEntity<Object> references(@RequestHeader(value = "X-API-Key", required = false) String providedKey,
                                             @PathVariable String cageCode) {
        // the key is checked before the cage is looked up: an unauthenticated caller learns nothing about cages
        if (!apiKey.matches(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiError.body(ApiError.UNAUTHORIZED, "invalid api key"));
        }
        List<GuineaPigReferenceResponse> references = listGuineaPigs.list(cageCode).stream()
                .map(guineaPig -> GuineaPigReferenceResponse.from(guineaPig, photoOf(cageCode, guineaPig)))
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(references);
    }

    private GuineaPigPhoto photoOf(String cageCode, GuineaPig guineaPig) {
        return guineaPig.hasPhoto() ? getGuineaPigPhoto.getPhoto(cageCode, guineaPig.getId()) : null;
    }
}
