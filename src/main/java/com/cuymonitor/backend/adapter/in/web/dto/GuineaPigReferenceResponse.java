package com.cuymonitor.backend.adapter.in.web.dto;

import com.cuymonitor.backend.domain.model.GuineaPig;
import com.cuymonitor.backend.domain.model.GuineaPigPhoto;
import com.cuymonitor.backend.domain.model.GuineaPigProfile;

import java.util.Base64;

// photo is null for a guinea pig registered without one; the ai-service only needs the picture and the coat
public record GuineaPigReferenceResponse(long id, String markColor, String coatColor, String breed,
                                         String photoContentType, String photo) {

    public static GuineaPigReferenceResponse from(GuineaPig guineaPig, GuineaPigPhoto photo) {
        GuineaPigProfile profile = guineaPig.getProfile();
        return new GuineaPigReferenceResponse(guineaPig.getId(), guineaPig.getMarkColor(), profile.coatColor(),
                profile.breed(), photo == null ? null : photo.contentType(),
                photo == null ? null : Base64.getEncoder().encodeToString(photo.content()));
    }
}
