package com.cuymonitor.backend.domain.exception;

/** Another guinea pig of the cage already wears that mark color; the camera could not tell them apart. */
public class ColorAlreadyUsedException extends RuntimeException {

    public ColorAlreadyUsedException() {
        super("that mark color is already used by another guinea pig in the cage");
    }
}
