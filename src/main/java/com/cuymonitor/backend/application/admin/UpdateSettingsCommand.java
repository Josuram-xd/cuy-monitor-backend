package com.cuymonitor.backend.application.admin;

import com.cuymonitor.backend.application.SettingsService;
import com.cuymonitor.backend.domain.exception.InvalidAdminActionException;
import com.cuymonitor.backend.domain.model.settings.SettingView;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** Command — ConcreteCommand: changes some health rules or alert texts; one bad value changes nothing. */
public final class UpdateSettingsCommand implements AdminCommand<List<SettingView>> {

    private final SettingsService settings;
    private final UUID adminId;
    private final Map<String, String> changes;

    public UpdateSettingsCommand(SettingsService settings, UUID adminId, Map<String, String> changes) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.adminId = adminId;
        this.changes = Map.copyOf(Objects.requireNonNull(changes, "changes"));
    }

    @Override
    public List<SettingView> execute() {
        try {
            return settings.update(changes, adminId);
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminActionException(e.getMessage());
        }
    }

    @Override
    public String description() {
        return "change settings " + new TreeMap<>(changes);
    }
}
