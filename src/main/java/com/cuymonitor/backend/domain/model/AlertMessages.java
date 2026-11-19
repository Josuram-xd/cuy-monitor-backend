package com.cuymonitor.backend.domain.model;

import java.util.Objects;

/** The texts of the alerts the breeder reads, editable by an admin ("Reglas y mensajes"). */
public record AlertMessages(String alert, String critical, String distress) {

    public static final AlertMessages DEFAULTS = new AlertMessages(
            "Comportamiento fuera de lo normal de forma sostenida",
            "El comportamiento fuera de lo normal empeoró y sigue",
            "Se escucharon chillidos de angustia en la jaula");

    public AlertMessages {
        Objects.requireNonNull(alert, "alert");
        Objects.requireNonNull(critical, "critical");
        Objects.requireNonNull(distress, "distress");
    }
}
