package dev.mohanverse.planner.event;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class Wakeup implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;

    @Override
    public String getLabel() { return "Wakeup"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
        LocalDateTime travelStart = end;
        LocalDateTime travelEnd = travelStart.plusHours(1);

        Travel travel = new Travel(travelStart, travelEnd);

        DayOfWeek day = end.getDayOfWeek();
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        if (isWeekday) {
            StudyTime study = new StudyTime(travelEnd, travelEnd.plusHours(7));
            return List.of(travel, study);
        }

        return List.of(travel);
    }
}