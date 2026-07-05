package dev.mohanverse.planner.event;

import java.time.LocalDateTime;
import java.util.List;

public interface FixedEvent {
    LocalDateTime getStart();
    LocalDateTime getEnd();
    String getLabel();

    default List<FixedEvent> chainSubsequent() {
        return List.of();
    }
}