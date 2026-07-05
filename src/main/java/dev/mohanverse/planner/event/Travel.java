package dev.mohanverse.planner.event;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class Travel implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;

    @Override
    public String getLabel() { return "Travel"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
      return List.of();
    }
}