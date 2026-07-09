package dev.mohanverse.planner.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@PlanningEntity
public class Task {

    @PlanningId
    private String name;

    private int durationInGrains;

    private Integer preferredStartHour;

    private LocalDate forDate;

    @PlanningVariable
    private TimeGrain startingTimeGrain;

    private boolean requiresShiftAdjacentDay; // hard constraint: date must have or follow a shift

    private static Week weekRef; // set once in Main before solving


    public Task(String name, int durationInGrains) {
        this.name = name;
        this.durationInGrains = durationInGrains;
    }

    public Task(String name, int durationInGrains, int preferredStartHour) {
        this.name = name;
        this.durationInGrains = durationInGrains;
        this.preferredStartHour = preferredStartHour;
    }

    public Task(String name, int durationInGrains, int preferredStartHour, LocalDate forDate) {
        this.name = name;
        this.durationInGrains = durationInGrains;
        this.preferredStartHour = preferredStartHour;
        this.forDate = forDate;
    }


    public static void setWeek(Week week) {
        weekRef = week;
    }

    public static Week getWeek() {
        return weekRef;
    }

    public int getSpilloverCount() {
        if (startingTimeGrain == null) return 0;
        return weekRef.spilloverCount(startingTimeGrain, durationInGrains);
    }
}