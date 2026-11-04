package com.cuymonitor.backend.domain.port.out;

/**
 * Picks the public code of a new cage ("cage-0a1b2c3d"). A port of its own (GRASP Pure Fabrication) so the cage
 * service only manages cages and tests can use predictable codes.
 */
public interface CageCodeGenerator {

    /** A code no other cage has. */
    String newCode();
}
