package dev.mohanverse.planner.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudyTime implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;

    public StudyTime(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String getLabel() { return "StudyTime"; }
}