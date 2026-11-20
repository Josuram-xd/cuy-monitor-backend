package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.port.in.GetLatestCameraFrameUseCase;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CameraSnapshotController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class CameraSnapshotControllerTest {

    private static final UUID ACCOUNT = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final String URL = "/api/v1/cages/cage-1/camera/latest";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, 9, 8, 7};

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CageOperations cages;
    @MockitoBean
    private GetLatestCameraFrameUseCase camera;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void needsASession() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(cages, camera);
    }

    @Test
    void theOwnerGetsThePictureAsAJpegThatIsNotKept() throws Exception {
        given(camera.latest("cage-1")).willReturn(Optional.of(JPEG));

        mvc.perform(get(URL).with(token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(JPEG));
    }

    @Test
    void aCageWithoutPictureIs404() throws Exception {
        given(camera.latest("cage-1")).willReturn(Optional.empty());

        mvc.perform(get(URL).with(token())).andExpect(status().isNotFound());
    }

    @Test
    void someoneElsesCageIsForbidden() throws Exception {
        willThrow(new CageAccessDeniedException()).given(cages).getCage(ACCOUNT, "cage-1");

        mvc.perform(get(URL).with(token())).andExpect(status().isForbidden());
        verifyNoInteractions(camera);
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(j -> j.subject(ACCOUNT.toString()));
    }
}
