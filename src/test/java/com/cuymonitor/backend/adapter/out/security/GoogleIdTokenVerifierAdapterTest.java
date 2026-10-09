package com.cuymonitor.backend.adapter.out.security;

import com.cuymonitor.backend.domain.exception.GoogleSignInDisabledException;
import com.cuymonitor.backend.domain.exception.InvalidGoogleTokenException;
import com.cuymonitor.backend.domain.model.auth.GoogleIdentity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A real RS256 signature made with a throwaway key stands in for Google's. */
class GoogleIdTokenVerifierAdapterTest {

    private static final String CLIENT_ID = "123-abc.apps.googleusercontent.com";

    private static RSAPrivateKey privateKey;
    private static GoogleIdTokenVerifierAdapter verifier;

    @BeforeAll
    static void createKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        verifier = new GoogleIdTokenVerifierAdapter(
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build(), CLIENT_ID);
    }

    @Test
    void aTokenMadeForOurClientIsAccepted() throws Exception {
        GoogleIdentity identity = verifier.verify(token(claims().build()));

        assertThat(identity.subject()).isEqualTo("1100123");
        assertThat(identity.email()).isEqualTo("ana@gmail.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.name()).isEqualTo("Ana Ruiz");
    }

    @Test
    void bothSpellingsOfTheIssuerAreAccepted() throws Exception {
        assertThat(verifier.verify(token(claims().issuer("accounts.google.com").build())).subject()).isEqualTo("1100123");
    }

    @Test
    void aTokenMadeForAnotherAppIsRefused() throws Exception {
        String foreign = token(claims().audience(List.of("someone-elses-client.apps.googleusercontent.com")).build());

        assertThatThrownBy(() -> verifier.verify(foreign)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void aTokenFromAnotherIssuerIsRefused() throws Exception {
        String fake = token(claims().issuer("https://evil.example.com").build());

        assertThatThrownBy(() -> verifier.verify(fake)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void anExpiredTokenIsRefused() throws Exception {
        String old = token(claims().expirationTime(new Date(System.currentTimeMillis() - 3_600_000)).build());

        assertThatThrownBy(() -> verifier.verify(old)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRefused() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey other = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        SignedJWT forged = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims().build());
        forged.sign(new RSASSASigner(other));

        assertThatThrownBy(() -> verifier.verify(forged.serialize())).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void garbageIsRefused() {
        assertThatThrownBy(() -> verifier.verify("not-a-jwt")).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void aTokenWithoutEmailIsRefused() throws Exception {
        String noEmail = token(new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience(CLIENT_ID)
                .subject("1100123").expirationTime(new Date(System.currentTimeMillis() + 3_600_000)).build());

        assertThatThrownBy(() -> verifier.verify(noEmail)).isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void anUnverifiedEmailIsReportedAsSuch() throws Exception {
        String unverified = token(claims().claim("email_verified", false).build());

        assertThat(verifier.verify(unverified).emailVerified()).isFalse();
    }

    @Test
    void withoutAClientIdTheFeatureIsOff() throws Exception {
        GoogleIdTokenVerifierAdapter off = new GoogleIdTokenVerifierAdapter(token -> {
            throw new AssertionError("must not even try to decode");
        }, "");

        assertThatThrownBy(() -> off.verify(token(claims().build()))).isInstanceOf(GoogleSignInDisabledException.class);
    }

    private static JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("1100123")
                .claim("email", "ana@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Ana Ruiz")
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000));
    }

    private static String token(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }
}
