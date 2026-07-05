package dev.mohanverse.planner.event;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class Sleep implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;
    private FixedEvent next;

    @Override
    public String getLabel() { return "Sleep"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
        return next == null ? List.of() : List.of(next);
    }
}