package com.cuymonitor.backend.application.fake;

import com.cuymonitor.backend.domain.model.GuineaPigPhoto;

/** A photo for the tests. The bytes are not a real image: nothing in the backend decodes them. */
public final class Photos {

    private Photos() {
    }

    public static GuineaPigPhoto png() {
        return new GuineaPigPhoto(new byte[] {1, 2, 3}, "image/png");
    }
}
