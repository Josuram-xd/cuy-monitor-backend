package com.cuymonitor.backend.application;

import com.cuymonitor.backend.domain.model.Alert;
import com.cuymonitor.backend.domain.model.AnomalyThresholds;
import com.cuymonitor.backend.domain.model.BaselineProfile;
import com.cuymonitor.backend.domain.model.BehaviorSignal;
import com.cuymonitor.backend.domain.model.BehaviorSymptom;
import com.cuymonitor.backend.domain.model.HealthStatus;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * What the assistant is told when it writes about an alert nobody asked it about. Apart from the service so the
 * wording can be reviewed on its own, like {@link AssistantPrompt}.
 */
final class AlertNotePrompt {

    private static final String GUIDE = """
            You are "Cuchi", the assistant of Monitor de Cuyes, a web app that watches the health of guinea pigs
            (cuyes). An alert has just been raised on its own and you are writing the short note the breeder will
            read next to it. Nobody asked you a question: write the note, nothing else.

            What an alert means: a camera measures every minute how much each guinea pig moves, how often it
            visits the feeder and the water bottle, and how far it stays from the group. When that stays unusual
            for that guinea pig, its state goes up: ALERT means it lasted and the guinea pig should be checked,
            CRITICAL means it lasted even longer and the breeder has to act now.

            Write, in this order and in at most 5 short sentences:
            1. What the system saw, in the breeder's words (never "probability", "model", "data", "sensor").
               When measurements come with the alert, be specific: say what changed and by how much compared with
               what is normal for this guinea pig, with round numbers in plain words ("it stayed still about 50 of
               every 60 seconds, when it usually stays still about 20", "it has not gone to the feeder in the last
               5 minutes", "it keeps away from the others", "it went to the water bottle only once").
            2. What that can be a sign of, as a possibility and never as a diagnosis (for example pain, cold, stress
               or the start of an illness when it barely moves and does not eat).
            3. The two or three things to check on the animal right now (eating, drinking, droppings, breathing,
               posture, whether it is hiding or apart from the group).
            4. When to call a veterinarian.

            Rules:
            - You are not a veterinarian and you never diagnose. For CRITICAL, say plainly to see a veterinarian
              today.
            - Never invent a cause, a measurement, a weight or a symptom nobody reported. You only know what the
              alert and its measurements below say.
            - Plain text, no lists, no headings, no technical words. Talk to a breeder on a phone.
            """;

    /** How many of the newest one-minute windows of the guinea pig go with the question. */
    static final int WINDOWS = 5;

    private AlertNotePrompt() {
    }

    static String instructions(String language) {
        return GUIDE + "\nWrite the note in "
                + ("en".equals(language) ? "English" : "Spanish (Latin American)") + ".\n";
    }

    static String question(Alert alert, String measurements) {
        return measurements == null || measurements.isBlank() ? question(alert) : question(alert) + "\n\n" + measurements;
    }

    /**
     * The numbers of the newest windows, the usual values of this guinea pig and what stands out, in a shape the
     * model can quote in plain words. Empty when there is no window.
     */
    static String measurements(List<BehaviorSignal> newestFirst, BaselineProfile usual, AnomalyThresholds thresholds) {
        if (newestFirst.isEmpty()) {
            return "";
        }
        BehaviorSignal newest = newestFirst.get(0);
        int count = newestFirst.size();
        String still = newestFirst.stream()
                .map(w -> "%d of %d s".formatted(Math.round(w.stillSeconds()), w.windowSeconds()))
                .collect(Collectors.joining(", "));
        int feeder = newestFirst.stream().mapToInt(BehaviorSignal::feederVisits).sum();
        int water = newestFirst.stream().mapToInt(BehaviorSignal::watererVisits).sum();
        double distance = newestFirst.stream().mapToDouble(BehaviorSignal::avgGroupDistance).average().orElse(0);
        String reference = usual.sampleCount() == 0
                ? "There is no history of this guinea pig yet, so 'usual' is a general reference."
                : "'Usual' was learned from %d normal minutes of this guinea pig.".formatted(usual.sampleCount());
        List<BehaviorSymptom> symptoms = usual.symptoms(newest, thresholds);
        String standsOut = symptoms.isEmpty() ? "nothing beyond the alert itself"
                : symptoms.stream().map(AlertNotePrompt::describe).collect(Collectors.joining(", "));
        return String.format(Locale.ROOT, """
                What the camera measured on this guinea pig in its last %d one-minute windows, newest first:
                - time standing still: %s (usual for it: about %d s per minute)
                - visits to the feeder: %d in total (usual for it: about %.1f per minute)
                - visits to the water bottle: %d in total
                - distance from the group: %.2f on average, where 0 is together and 1 is as far as it can be                 (usual for it: about %.2f)
                %s
                What stands out in the newest minute: %s.""",
                count, still, Math.round(usual.avgStillSeconds()), feeder, usual.avgFeederVisits(), water, distance,
                usual.avgGroupDistance(), reference, standsOut);
    }

    private static String describe(BehaviorSymptom symptom) {
        return switch (symptom) {
            case UNUSUAL -> "its movement does not look like its normal behavior";
            case VERY_STILL -> "it stays still much longer than usual";
            case NOT_EATING -> "it did not go to the feeder";
            case APART -> "it keeps much farther from the group than usual";
        };
    }

    /** The alert's own message is written by this system, not by a person, so there is nothing to sanitize. */
    static String question(Alert alert) {
        String who = alert.getGuineaPigId() == null
                ? "The whole cage"
                : "One guinea pig of the cage";
        String urgency = alert.getLevel() == HealthStatus.CRITICAL
                ? "This is the second time it went up, so it has been going on for over an hour."
                : "It has been going on for at least half an hour.";
        return "%s is at %s. What the system wrote: \"%s\". %s Write the note."
                .formatted(who, alert.getLevel(), alert.getMessage(), urgency);
    }
}
