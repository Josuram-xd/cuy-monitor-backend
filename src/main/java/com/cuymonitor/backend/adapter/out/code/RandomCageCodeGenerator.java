package com.cuymonitor.backend.adapter.out.code;

import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;

import java.security.SecureRandom;
import java.util.HexFormat;

/** "cage-" + 8 hex characters: short enough to type on the camera and the scale, random so it is not guessable. */
public class RandomCageCodeGenerator implements CageCodeGenerator {

    private static final String PREFIX = "cage-";
    private static final int RANDOM_BYTES = 4;
    private static final int MAX_ATTEMPTS = 10;

    private final CageRepository cages;
    private final SecureRandom random = new SecureRandom();

    public RandomCageCodeGenerator(CageRepository cages) {
        this.cages = cages;
    }

    @Override
    public String newCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = PREFIX + randomHex();
            if (!cages.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not pick a free cage code");
    }

    private String randomHex() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
