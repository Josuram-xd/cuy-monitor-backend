package com.cuymonitor.backend.adapter.out.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Encrypts the camera password at rest. It must be reversible (the edge agent needs the plain value), so it is not a
 * hash. The key comes from APP_API_KEY: whoever can ask the agent endpoint already has that key.
 * Rotating APP_API_KEY makes the stored passwords unreadable; the dashboard then asks for them again.
 */
@Component
public class CameraPasswordCipher {

    private static final Logger log = LoggerFactory.getLogger(CameraPasswordCipher.class);
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CameraPasswordCipher(@Value("${app.api-key}") String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("cuy-camera-v1:" + apiKey).getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(digest, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** @return Base64 of the random IV followed by the ciphertext */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] body = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + body.length).put(iv).put(body).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("could not encrypt the camera password", e);
        }
    }

    /** @return the plain password, or empty when there is none or it was encrypted with another key */
    public Optional<String> decrypt(String stored) {
        if (stored == null) {
            return Optional.empty();
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            if (all.length <= IV_BYTES) {
                throw new IllegalArgumentException("too short");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            return Optional.of(new String(cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // never log the value: only that it could not be read
            log.warn("The stored camera password can not be decrypted (APP_API_KEY changed?); it must be entered again");
            return Optional.empty();
        }
    }
}
