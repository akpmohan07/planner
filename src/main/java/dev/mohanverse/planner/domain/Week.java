package dev.mohanverse.planner.domain;

import dev.mohanverse.planner.blocker.FixedEventBlocker;
import dev.mohanverse.planner.event.FixedEvent;
import dev.mohanverse.planner.event.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Week {

    private final List<TimeGrain> grains;
    private final List<Shift> shifts;

    public Week(List<TimeGrain> grains, List<Shift> shifts) {
        this.grains = grains;
        this.shifts = shifts;
    }

    public List<TimeGrain> getGrains() {
        return grains;
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public List<TimeGrain> freeGrains() {
        return grains.stream().filter(g -> !g.isBlocked()).toList();
    }

    public void block(FixedEvent event) {
        FixedEventBlocker.block(grains, event);
    }

    public boolean hasShiftOn(LocalDate date) {
        return grains.stream()
                .anyMatch(g -> g.getDate().equals(date) && "Dominos".equals(g.getOccupiedBy()));
    }

    /** Earliest shift start later the same day as {@code time}, if any. */
    public Optional<LocalDateTime> nextShiftStartAfter(LocalDateTime time) {
        LocalDate day = time.toLocalDate();
        return grains.stream()
                .filter(g -> "Dominos".equals(g.getOccupiedBy()))
                .map(g -> LocalDateTime.of(g.getDate(), g.getStartTime()))
                .filter(t -> t.toLocalDate().equals(day) && t.isAfter(time))
                .min(LocalDateTime::compareTo);
    }

    public int spilloverCount(TimeGrain start, int durationInGrains) {
        int startIndex = start.getIndex();
        int count = 0;
        for (int i = startIndex; i < startIndex + durationInGrains; i++) {
            if (i >= grains.size() || grains.get(i).isBlocked()) count++;
        }
        return count;
    }
}
