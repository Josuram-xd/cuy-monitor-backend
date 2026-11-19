package com.cuymonitor.backend.domain.port.out;

import com.cuymonitor.backend.domain.model.AccountActivity;
import com.cuymonitor.backend.domain.model.AiLogEntry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only reports for the admin screen, across every account. */
public interface AdminReportRepository {

    /** One entry per account that owns at least one active cage; the rest have nothing to report. */
    Map<UUID, AccountActivity> activityByAccount();

    /** The newest entries first, at most limit. */
    List<AiLogEntry> latestAiLog(int limit);
}
