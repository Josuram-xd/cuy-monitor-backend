package com.cuymonitor.backend.domain.model;

/**
 * What stands out in one behavior window compared with what is normal for that guinea pig. The same list feeds the
 * live monitoring panel ("Cuy naranja: casi no se movió") and the note the assistant writes on an alert.
 */
public enum BehaviorSymptom {
    /** the AI service itself flagged the window */
    UNUSUAL,
    /** still much longer than usual for it */
    VERY_STILL,
    /** usually goes to the feeder, this time it did not */
    NOT_EATING,
    /** much farther from the group than usual */
    APART
}
