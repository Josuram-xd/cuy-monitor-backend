package com.cuymonitor.backend.adapter.out.code;

import com.cuymonitor.backend.domain.port.out.CageCodeGenerator;
import com.cuymonitor.backend.domain.port.out.CageRepository;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * A prefix and 8 hex characters: "cage-" + 8 hex for the farmers' cages (short enough to type on the camera and the
 * scale, random so it is not guessable), "sim-" + 8 hex for the simulation cage.
 */
public class RandomCageCodeGenerator implements CageCodeGenerator {

    private static final String FARM_PREFIX = "cage-";
    private static final int RANDOM_BYTES = 4;
    private static final int MAX_ATTEMPTS = 10;

    private final CageRepository cages;
    private final String prefix;
    private final SecureRandom random = new SecureRandom();

    public RandomCageCodeGenerator(CageRepository cages) {
        this(cages, FARM_PREFIX);
    }

    public RandomCageCodeGenerator(CageRepository cages, String prefix) {
        this.cages = cages;
        this.prefix = prefix;
    }

    @Override
    public String newCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = prefix + randomHex();
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
