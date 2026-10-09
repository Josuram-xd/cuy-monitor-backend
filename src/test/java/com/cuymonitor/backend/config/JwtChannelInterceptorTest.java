package com.cuymonitor.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtChannelInterceptorTest {

    private static final String ISSUER = "cuy-monitor-backend";
    private static final SecretKey KEY = key("test-secret-with-at-least-32-bytes!!");

    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(decoder());

    @Test
    void connectWithValidTokenAttachesTheUser() {
        String userId = UUID.randomUUID().toString();
        Message<byte[]> connect = frame(StompCommand.CONNECT, "Bearer " + token(KEY, ISSUER, userId, 600), null);

        Message<?> result = interceptor.preSend(connect, null);

        Principal user = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class).getUser();
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo(userId);
    }

    @Test
    void connectWithTheTokenFromTheHandshakeCookieAttachesTheUser() {
        String userId = UUID.randomUUID().toString();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>(Map.of("accessToken", token(KEY, ISSUER, userId, 600))));
        accessor.setLeaveMutable(true);
        Message<byte[]> connect = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(connect, null);

        assertThat(MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class).getUser().getName())
                .isEqualTo(userId);
    }

    @Test
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void connectWithoutBearerPrefixIsRejected() {
        String token = token(KEY, ISSUER, UUID.randomUUID().toString(), 600);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, token, null), null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void connectWithExpiredTokenIsRejected() {
        // well past the default 60 s clock skew
        String expired = token(KEY, ISSUER, UUID.randomUUID().toString(), -600);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + expired, null), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void connectWithTokenFromAnotherIssuerIsRejected() {
        String token = token(KEY, "someone-else", UUID.randomUUID().toString(), 600);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + token, null), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void connectWithTokenSignedWithAnotherKeyIsRejected() {
        String token = token(key("another-secret-with-at-least-32-bytes"), ISSUER, UUID.randomUUID().toString(), 600);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer " + token, null), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void connectWithGarbageTokenIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer not-a-jwt", null), null))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void subscribeFromUnauthenticatedSessionIsRejected() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null, null);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribeFromAuthenticatedSessionPasses() {
        Message<byte[]> subscribe = frame(StompCommand.SUBSCRIBE, null, new TestingAuthenticationToken("user", null));

        assertThat(interceptor.preSend(subscribe, null)).isSameAs(subscribe);
    }

    private static Message<byte[]> frame(StompCommand command, String authorization, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (command == StompCommand.SUBSCRIBE) {
            accessor.setDestination("/topic/cages/cage-1");
            accessor.setSubscriptionId("sub-0");
        }
        accessor.setUser(user);
        // like the real STOMP handler, so the interceptor can set the user on the same headers
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static String token(SecretKey key, String issuer, String subject, long expiresInSeconds) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plusSeconds(expiresInSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(expiresInSeconds > 0 ? now : expiresAt.minusSeconds(1800))
                .expiresAt(expiresAt)
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // same setup as JwtConfig
    private static NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
