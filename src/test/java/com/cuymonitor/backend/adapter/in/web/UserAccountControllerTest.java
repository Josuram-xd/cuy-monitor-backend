package com.cuymonitor.backend.adapter.in.web;

import com.cuymonitor.backend.config.JwtConfig;
import com.cuymonitor.backend.config.SecurityConfig;
import com.cuymonitor.backend.domain.port.out.RevokedTokenRepository;
import com.cuymonitor.backend.domain.exception.AccountDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidCredentialsException;
import com.cuymonitor.backend.domain.model.user.User;
import com.cuymonitor.backend.domain.model.user.UserRole;
import com.cuymonitor.backend.domain.model.user.UserStatus;
import com.cuymonitor.backend.domain.port.in.ChangePasswordCommand;
import com.cuymonitor.backend.domain.port.in.ChangePasswordUseCase;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountCommand;
import com.cuymonitor.backend.domain.port.in.DeactivateAccountUseCase;
import com.cuymonitor.backend.domain.model.user.ProfilePhoto;
import com.cuymonitor.backend.domain.port.in.GetCurrentUserUseCase;
import com.cuymonitor.backend.domain.port.in.ProfilePhotoUseCase;
import com.cuymonitor.backend.domain.port.in.UpdateProfileCommand;
import com.cuymonitor.backend.domain.port.in.UpdateProfileUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAccountController.class,
        properties = "app.auth.jwt.secret=test-secret-with-at-least-32-bytes!!")
@Import({SecurityConfig.class, JwtConfig.class})
class UserAccountControllerTest {

    private static final UUID USER_ID = UUID.fromString("3f2a4b5c-0000-4000-8000-000000000001");
    private static final Instant CREATED = Instant.parse("2026-10-01T10:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;
    @MockitoBean
    private UpdateProfileUseCase updateProfileUseCase;
    @MockitoBean
    private ChangePasswordUseCase changePasswordUseCase;
    @MockitoBean
    private DeactivateAccountUseCase deactivateAccountUseCase;
    @MockitoBean
    private ProfilePhotoUseCase profilePhotoUseCase;
    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/account/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
        verifyNoInteractions(getCurrentUserUseCase);
    }

    @Test
    void getMeReturnsTheAccountOfTheTokenSubjectWithoutTheHash() throws Exception {
        given(getCurrentUserUseCase.getCurrentUser(USER_ID)).willReturn(user("Juan Perez"));

        mvc.perform(get("/api/v1/account/profile").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("juan"))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                // the dashboard decides whether to draw the admin section from this, since it cannot read
                // the HttpOnly session cookie
                .andExpect(jsonPath("$.role").value("BREEDER"));
    }

    @Test
    void theProfileCarriesTheNicknameAndTheNameToPrint() throws Exception {
        User user = user("Juan Perez");
        user.changeNickname("Juanito", CREATED);
        given(getCurrentUserUseCase.getCurrentUser(USER_ID)).willReturn(user);

        mvc.perform(get("/api/v1/account/profile").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Juanito"))
                // so no screen has to repeat the "nickname, or the full name" rule
                .andExpect(jsonPath("$.displayName").value("Juanito"))
                .andExpect(jsonPath("$.hasPhoto").value(false));
    }

    @Test
    void theProfileFallsBackToTheFullNameWithoutANickname() throws Exception {
        given(getCurrentUserUseCase.getCurrentUser(USER_ID)).willReturn(user("Juan Perez"));

        mvc.perform(get("/api/v1/account/profile").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").doesNotExist())
                .andExpect(jsonPath("$.displayName").value("Juan Perez"));
    }

    @Test
    void servesThePhotoWithItsOwnTypeAndWithoutSniffing() throws Exception {
        byte[] bytes = {1, 2, 3};
        given(profilePhotoUseCase.getPhoto(USER_ID)).willReturn(new ProfilePhoto(bytes, "image/webp"));

        mvc.perform(get("/api/v1/account/photo").with(token()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void thePhotoNeedsTheSession() throws Exception {
        mvc.perform(get("/api/v1/account/photo"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profilePhotoUseCase);
    }

    @Test
    void savesThePhotoItIsSent() throws Exception {
        String base64 = java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

        mvc.perform(put("/api/v1/account/photo").with(token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photo\":\"" + base64 + "\",\"photoContentType\":\"image/png\"}"))
                .andExpect(status().isNoContent());

        verify(profilePhotoUseCase).setPhoto(USER_ID, new ProfilePhoto(new byte[] {1, 2, 3}, "image/png"));
    }

    @Test
    void refusesAnImageTypeTheBrowserCouldBeTrickedWith() throws Exception {
        String base64 = java.util.Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

        mvc.perform(put("/api/v1/account/photo").with(token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photo\":\"" + base64 + "\",\"photoContentType\":\"image/svg+xml\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(profilePhotoUseCase);
    }

    @Test
    void removesThePhoto() throws Exception {
        mvc.perform(delete("/api/v1/account/photo").with(token()))
                .andExpect(status().isNoContent());

        verify(profilePhotoUseCase).removePhoto(USER_ID);
    }

    @Test
    void theProfileSaysWhenTheAccountIsAnAdmin() throws Exception {
        User admin = user("Juan Perez");
        admin.changeRoleTo(UserRole.ADMIN, CREATED);
        given(getCurrentUserUseCase.getCurrentUser(USER_ID)).willReturn(admin);

        mvc.perform(get("/api/v1/account/profile").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void disabledAccountWithValidTokenReturns401() throws Exception {
        given(getCurrentUserUseCase.getCurrentUser(USER_ID)).willThrow(new AccountDisabledException());

        mvc.perform(get("/api/v1/account/profile").with(token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("account is disabled"));
    }

    @Test
    void updateProfileReturnsTheUpdatedAccount() throws Exception {
        given(updateProfileUseCase.updateProfile(new UpdateProfileCommand(USER_ID, "Juan Carlos", null)))
                .willReturn(user("Juan Carlos"));

        mvc.perform(put("/api/v1/account/profile").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Juan Carlos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Juan Carlos"));
    }

    @Test
    void updateProfileWithBlankNameReturns400() throws Exception {
        mvc.perform(put("/api/v1/account/profile").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(updateProfileUseCase);
    }

    @Test
    void changePasswordReturns204() throws Exception {
        mvc.perform(put("/api/v1/account/password").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Secret-pass-1\",\"newPassword\":\"new-Secret-pass-1\"}"))
                .andExpect(status().isNoContent());

        verify(changePasswordUseCase).changePassword(
                new ChangePasswordCommand(USER_ID, "Secret-pass-1", "new-Secret-pass-1"));
    }

    @Test
    void changePasswordWithAnEndlessPasswordReturns400() throws Exception {
        String tooLong = "a".repeat(300);

        mvc.perform(put("/api/v1/account/password").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Secret-pass-1\",\"newPassword\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"))
                .andExpect(jsonPath("$.fields.newPassword").exists());
        verifyNoInteractions(changePasswordUseCase);
    }

    @Test
    void changePasswordWithWrongCurrentPasswordReturns401() throws Exception {
        willThrow(new InvalidCredentialsException()).given(changePasswordUseCase).changePassword(any());

        mvc.perform(put("/api/v1/account/password").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-Secret-pass-1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivateReturns204() throws Exception {
        mvc.perform(delete("/api/v1/account").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Secret-pass-1\"}"))
                .andExpect(status().isNoContent());

        verify(deactivateAccountUseCase).deactivate(new DeactivateAccountCommand(USER_ID, "Secret-pass-1"));
    }

    @Test
    void deactivateWithWrongPasswordReturns401() throws Exception {
        willThrow(new InvalidCredentialsException()).given(deactivateAccountUseCase).deactivate(any());

        mvc.perform(delete("/api/v1/account").with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor token() {
        return jwt().jwt(jwt -> jwt.subject(USER_ID.toString()));
    }

    private static User user(String fullName) {
        return User.restore(USER_ID, "juan", fullName, "juan@mail.com", "hash", UserStatus.ACTIVE, CREATED, CREATED);
    }
}
