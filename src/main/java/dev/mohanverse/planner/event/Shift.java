package dev.mohanverse.planner.event;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class Shift implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;

    @Override
    public String getLabel() { return "Dominos"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
        Travel travel = new Travel(end.plusHours(1), end.plusHours(9));
        return List.of(travel);
    }
}