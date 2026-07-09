package dev.mohanverse.planner.event;

import dev.mohanverse.planner.domain.Week;
import lombok.Data;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Data
@AllArgsConstructor
public class Wakeup implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;
    private Week week;

    @Override
    public String getLabel() { return "Wakeup"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
        Optional<LocalDateTime> shiftStart = week.nextShiftStartAfter(end);

        if (shiftStart.isPresent()) {
            // A shift starts later today: reserve a fixed get-ready + travel-to-work
            // block ending exactly at shift start. No fixed Study here — the leftover
            // time between Wakeup and this block is genuinely free time, and Study
            // becomes a flexible Task competing for it instead (see Main).
            LocalDateTime preShiftStart = shiftStart.get().minusHours(1);
            Travel preShiftTravel = new Travel(preShiftStart, shiftStart.get());
            return List.of(preShiftTravel);
        }

        DayOfWeek day = end.getDayOfWeek();
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        if (isWeekday) {
            LocalDateTime travelStart = end;
            LocalDateTime travelEnd = travelStart.plusHours(1);
            Travel travel = new Travel(travelStart, travelEnd);
            StudyTime study = new StudyTime(travelEnd, travelEnd.plusHours(7));
            return List.of(travel, study);
        }

        // Weekend, no shift today: college library is closed, so study happens
        // at home instead — no commute needed
        StudyTime study = new StudyTime(end, end.plusHours(7));
        return List.of(study);
    }
}