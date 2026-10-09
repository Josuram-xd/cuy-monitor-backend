package com.cuymonitor.backend.domain.health.state;

import com.cuymonitor.backend.domain.model.HealthStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
