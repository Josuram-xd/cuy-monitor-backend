package com.cuymonitor.backend.domain.exception;

/** The camera can only be seen or changed from the cage the shared kit is connected to right now. Answered with 409. */
public class EquipmentNotConnectedException extends RuntimeException {

    public EquipmentNotConnectedException() {
        super("the equipment is not connected to this cage");
    }
}
