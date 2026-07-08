package dev.mohanverse.planner.event;

import dev.mohanverse.planner.blocker.FixedEventBlocker;
import dev.mohanverse.planner.domain.TimeGrain;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.List;

@Data
@AllArgsConstructor
public class Sleep implements FixedEvent {
    private LocalDateTime start;
    private LocalDateTime end;

    @Override
    public String getLabel() { return "Sleep"; }

    @Override
    public List<FixedEvent> chainSubsequent() {
        Wakeup wakeup = new Wakeup(end, end.plusHours(2));
        return List.of(wakeup);
    }

    /**
     * Figures out when sleep should actually start for a given night.
     * Default is 11PM, but if a shift/travel already blocked grains past
     * 11PM (a late night), sleep is pushed to start right after that instead.
     * Sleep always lasts 8 hours from whenever it actually starts.
     */
    public static Sleep forNight(LocalDate night, List<TimeGrain> grains) {
        LocalDateTime defaultStart = LocalDateTime.of(night, LocalTime.of(23, 0));
        LocalDateTime windowEnd = defaultStart.plusHours(8);

        // look for the latest non-sleep blocked grain (Dominos/Travel) within
        // the default sleep window, that becomes the real sleep start time
        LocalDateTime actualStart = grains.stream()
                .filter(TimeGrain::isBlocked)
                .filter(g -> !"Sleep".equals(g.getOccupiedBy()))
                .map(g -> LocalDateTime.of(g.getDate(), g.getStartTime()).plusMinutes(30))
                .filter(t -> !t.isBefore(defaultStart) && t.isBefore(windowEnd))
                .max(LocalDateTime::compareTo)
                .orElse(defaultStart);

        return new Sleep(actualStart, actualStart.plusHours(8));
    }

    /**
     * Applies sleep blocking to every day of the given week, independent of
     * whether a shift happened that night. This guarantees a minimum sleep
     * window daily, not just after shifts.
     */
    public static void blockWeek(int year, int weekNumber, List<TimeGrain> grains) {
        WeekFields weekFields = WeekFields.ISO;
        LocalDate monday = LocalDate.of(year, 1, 1)
                .with(weekFields.weekOfYear(), weekNumber)
                .with(weekFields.dayOfWeek(), 1);

        // cover the tail end of the previous night too, so Monday morning is protected
        for (int day = -1; day < 7; day++) {
            LocalDate night = monday.plusDays(day);
            Sleep sleep = forNight(night, grains);
            FixedEventBlocker.block(grains, sleep);
        }
    }
}