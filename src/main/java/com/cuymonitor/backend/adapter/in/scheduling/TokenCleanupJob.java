package com.cuymonitor.backend.adapter.in.scheduling;

import com.cuymonitor.backend.domain.port.in.PurgeExpiredTokensUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Once a day, drops revoked and refresh tokens that already expired; they can no longer matter. */
@Component
public class TokenCleanupJob {

    private final PurgeExpiredTokensUseCase purgeExpiredTokens;

    public TokenCleanupJob(PurgeExpiredTokensUseCase purgeExpiredTokens) {
        this.purgeExpiredTokens = purgeExpiredTokens;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        purgeExpiredTokens.purgeExpired();
    }
}
