package dev.mohanverse.planner.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TimeGrain {
    private int index;
    private LocalDate date;
    private LocalTime startTime;
    private boolean blocked;
    private String occupiedBy;
}