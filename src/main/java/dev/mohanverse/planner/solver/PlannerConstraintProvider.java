package dev.mohanverse.planner.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import dev.mohanverse.planner.domain.Task;

public class PlannerConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                noOverlappingTasks(factory)
        };
    }

    private Constraint noOverlappingTasks(ConstraintFactory factory) {
        return factory.forEachUniquePair(Task.class)
                .filter(PlannerConstraintProvider::overlaps)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No overlapping tasks");
    }

    private static boolean overlaps(Task a, Task b) {
        if (a.getStartingTimeGrain() == null || b.getStartingTimeGrain() == null) return false;

        int aStart = a.getStartingTimeGrain().getIndex();
        int aEnd = aStart + a.getDurationInGrains();
        int bStart = b.getStartingTimeGrain().getIndex();
        int bEnd = bStart + b.getDurationInGrains();

        return aStart < bEnd && bStart < aEnd;
    }
}