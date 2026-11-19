package com.cuymonitor.backend.domain.port.out;

import java.util.Map;
import java.util.UUID;

/** The admin's changes to the settings (table app_setting). Only what differs from the defaults is kept. */
public interface SettingRepository {

    /** key -> value, as stored. */
    Map<String, String> findAll();

    /** Leaves exactly these changes stored: an empty map is "back to the defaults". */
    void replaceAll(Map<String, String> changes, UUID changedBy);
}
