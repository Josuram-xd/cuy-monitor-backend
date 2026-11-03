package com.cuymonitor.backend.config;

import com.cuymonitor.backend.domain.access.CageOperations;
import com.cuymonitor.backend.domain.exception.CageAccessDeniedException;
import com.cuymonitor.backend.domain.exception.CageNotFoundException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Validates the JWT on the STOMP CONNECT frame. Browsers bring it in the access cookie (copied to the session
 * attributes by CookieHandshakeInterceptor); other clients can send an Authorization header on the frame.
 * On SUBSCRIBE to {@code /topic/cages/{cageCode}} it asks the cage access proxy (Proxy pattern) whether the account
 * owns that cage, so nobody can listen to someone else's cage. Throwing here makes Spring answer with an ERROR frame
 * and close the connection.
 */
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private static final String CAGE_TOPIC = "/topic/cages/";

    private final JwtDecoder decoder;
    private final CageOperations cageOperations;
    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    public JwtChannelInterceptor(JwtDecoder decoder, CageOperations cageOperations) {
        this.decoder = decoder;
        this.cageOperations = cageOperations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // the user stays attached to the session, so later frames carry it too
            accessor.setUser(authenticate(tokenFrom(accessor)));
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("subscription requires an authenticated session");
            }
            checkCageTopic(accessor.getUser(), accessor.getDestination());
        }
        return message;
    }

    // the same proxy the REST API uses: getCage only answers if the account owns the cage
    private void checkCageTopic(Principal user, String destination) {
        if (destination == null || !destination.startsWith(CAGE_TOPIC)) {
            return;
        }
        String cageCode = destination.substring(CAGE_TOPIC.length());
        try {
            cageOperations.getCage(UUID.fromString(user.getName()), cageCode);
        } catch (CageAccessDeniedException | CageNotFoundException | IllegalArgumentException e) {
            throw new AccessDeniedException("not allowed to follow cage " + cageCode, e);
        }
    }

    private static String tokenFrom(StompHeaderAccessor accessor) {
        Map<String, Object> session = accessor.getSessionAttributes();
        if (session != null && session.get(CookieHandshakeInterceptor.ACCESS_TOKEN_ATTRIBUTE) instanceof String cookie) {
            return cookie;
        }
        String header = accessor.getFirstNativeHeader(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER) && header.length() > BEARER.length()) {
            return header.substring(BEARER.length());
        }
        return null;
    }

    private Authentication authenticate(String token) {
        if (token == null) {
            throw new AuthenticationCredentialsNotFoundException("missing token");
        }
        Jwt jwt;
        try {
            // same decoder as the REST API: checks signature, issuer, expiration and revocation
            jwt = decoder.decode(token);
        } catch (JwtException e) {
            throw new BadCredentialsException("invalid or expired token", e);
        }
        return converter.convert(jwt);
    }
}
