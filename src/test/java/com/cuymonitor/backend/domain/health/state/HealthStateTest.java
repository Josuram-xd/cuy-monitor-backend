package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthStateTest {

    @Test
    void normalGoesToObservedOnASustainedAnomaly() {
        assertThat(new NormalState().onSustainedAnomaly().status()).isEqualTo(HealthStatus.OBSERVED);
    }

    @Test
    void observedGoesToAlertOnASustainedAnomaly() {
        assertThat(new ObservedState().onSustainedAnomaly().status()).isEqualTo(HealthStatus.ALERT);
    }

    @Test
    void alertGoesToCriticalOnASustainedAnomaly() {
        assertThat(new AlertState().onSustainedAnomaly().status()).isEqualTo(HealthStatus.CRITICAL);
    }

    @Test
    void criticalStaysCriticalOnASustainedAnomaly() {
        CriticalState critical = new CriticalState();

        assertThat(critical.onSustainedAnomaly()).isSameAs(critical);
    }

    @Test
    void normalStaysNormalOnRecovery() {
        NormalState normal = new NormalState();

        assertThat(normal.onSustainedRecovery()).isSameAs(normal);
    }

    @Test
    void observedGoesBackToNormalOnRecovery() {
        assertThat(new ObservedState().onSustainedRecovery().status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void alertGoesBackToNormalOnRecovery() {
        assertThat(new AlertState().onSustainedRecovery().status()).isEqualTo(HealthStatus.NORMAL);
    }

    @Test
    void criticalGoesBackToNormalOnRecovery() {
        assertThat(new CriticalState().onSustainedRecovery().status()).isEqualTo(HealthStatus.NORMAL);
    }
}
