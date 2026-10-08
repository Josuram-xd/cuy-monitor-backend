package com.cuymonitor.backend.domain.exception;

import com.cuymonitor.backend.domain.model.MarkColor;

// the camera tells guinea pigs apart only by their color, so two in one cage can't share it
public class MarkColorAlreadyUsedException extends RuntimeException {

    public MarkColorAlreadyUsedException(MarkColor color) {
        super("color " + color + " is already used in this cage");
    }
}
