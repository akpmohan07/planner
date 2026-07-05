package dev.mohanverse.planner.blocker;

import dev.mohanverse.planner.domain.TimeGrain;
import dev.mohanverse.planner.event.FixedEvent;
import java.time.LocalDateTime;
import java.util.List;

public class FixedEventBlocker {

    // Blocks the given time grains based on the provided fixed event and its subsequent events.
    public static void block(List<TimeGrain> grains, FixedEvent event) {
        for (TimeGrain grain : grains) {
            LocalDateTime grainTime = LocalDateTime.of(grain.getDate(), grain.getStartTime());
            // Check if the grain time is within the event's start and end time
            if (!grainTime.isBefore(event.getStart()) && grainTime.isBefore(event.getEnd())) {
                grain.setBlocked(true);
                grain.setOccupiedBy(event.getLabel());
            }
        }

        for (FixedEvent next : event.chainSubsequent()) {
            block(grains, next);
        }
    }
}