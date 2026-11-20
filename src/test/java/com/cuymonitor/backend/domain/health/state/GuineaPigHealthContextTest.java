package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuineaPigHealthContextTest {

    @Test
    void exposesItsGuineaPigAndInitialState() {
        var state = new NormalState();
        var context = new GuineaPigHealthContext(42L, state);

        assertEquals(42L, context.guineaPigId());
        assertSame(state, context.state());
        assertEquals(HealthStatus.NORMAL, context.status());
    }

    @Test
    void canReplaceItsCurrentState() {
        var context = new GuineaPigHealthContext(42L, new NormalState());
        var nextState = new AlertState();

        context.setState(nextState);

        assertSame(nextState, context.state());
        assertEquals(HealthStatus.ALERT, context.status());
    }

    @Test
    void rejectsNullStates() {
        assertThrows(NullPointerException.class, () -> new GuineaPigHealthContext(42L, null));

        var context = new GuineaPigHealthContext(42L, new NormalState());
        assertThrows(NullPointerException.class, () -> context.setState(null));
    }

    @Test
    void restoredContextKeepsTheLevelAndTheEscalationClock() {
        var since = Instant.parse("2026-11-04T10:00:00Z");
        var policy = new EscalationPolicy(Duration.ofMinutes(30), Duration.ofMinutes(60));
        var original = new GuineaPigHealthContext(42L, new ObservedState(), since);

        var restored = GuineaPigHealthContext.restore(42L, original.snapshot());

        assertEquals(HealthStatus.OBSERVED, restored.status());
        var justBefore = since.plus(Duration.ofMinutes(30)).minusSeconds(1);
        assertFalse(restored.anomalyConfirmed("still odd", justBefore, policy).isPresent());
        assertTrue(restored.anomalyConfirmed("still odd", since.plus(Duration.ofMinutes(30)), policy).isPresent());
    }

    @Test
    void snapshotAfterATransitionRecordsTheTimeOfThatTransition() {
        var now = Instant.parse("2026-11-04T10:05:00Z");
        var context = new GuineaPigHealthContext(42L, new NormalState());

        context.anomalyConfirmed("odd", now);

        assertEquals(new GuineaPigHealthContext.Snapshot(HealthStatus.OBSERVED, now), context.snapshot());
    }
}
