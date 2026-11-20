package com.cuymonitor.backend.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraPasswordCipherTest {

    @Test
    void theStoredValueIsNotThePasswordAndDecryptsBack() {
        var cipher = new CameraPasswordCipher("key-one");

        String stored = cipher.encrypt("s3cret-pass");

        assertFalse(stored.contains("s3cret-pass"));
        assertEquals("s3cret-pass", cipher.decrypt(stored).orElseThrow());
    }

    @Test
    void theSameTextEncryptsDifferentlyEachTime() {
        var cipher = new CameraPasswordCipher("key-one");

        assertNotEquals(cipher.encrypt("pw"), cipher.encrypt("pw"));
    }

    @Test
    void anotherKeyOrGarbageCanNotBeRead() {
        String stored = new CameraPasswordCipher("key-one").encrypt("pw");

        assertTrue(new CameraPasswordCipher("key-two").decrypt(stored).isEmpty());
        assertTrue(new CameraPasswordCipher("key-one").decrypt("not-base64!").isEmpty());
        assertTrue(new CameraPasswordCipher("key-one").decrypt("AAAA").isEmpty());
        assertTrue(new CameraPasswordCipher("key-one").decrypt(null).isEmpty());
    }
}
