package dev.mohanverse.planner.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@PlanningEntity
public class Task {

    @PlanningId
    private String name;

    private int durationInGrains;

    @PlanningVariable
    private TimeGrain startingTimeGrain;

    public Task(String name, int durationInGrains) {
        this.name = name;
        this.durationInGrains = durationInGrains;
    }
}