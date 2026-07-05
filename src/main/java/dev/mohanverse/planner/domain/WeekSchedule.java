package dev.mohanverse.planner.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@PlanningSolution
public class WeekSchedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<TimeGrain> freeGrains;

    @PlanningEntityCollectionProperty
    private List<Task> tasks;

    @PlanningScore
    private HardSoftScore score;

    public WeekSchedule(List<TimeGrain> freeGrains, List<Task> tasks) {
        this.freeGrains = freeGrains;
        this.tasks = tasks;
    }
}