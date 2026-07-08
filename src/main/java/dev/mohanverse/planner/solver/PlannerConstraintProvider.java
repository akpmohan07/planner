package dev.mohanverse.planner.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import dev.mohanverse.planner.domain.Task;
import dev.mohanverse.planner.domain.TimeGrain;

public class PlannerConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                noOverlappingTasks(factory),
                noSpilloverIntoBlocked(factory),
                taskOnCorrectDate(factory),
                preferPreferredStartHour(factory)
        };
    }

    private Constraint noOverlappingTasks(ConstraintFactory factory) {
        return factory.forEachUniquePair(Task.class)
                .filter(PlannerConstraintProvider::tasksOverlap)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No overlapping tasks");
    }

    private Constraint noSpilloverIntoBlocked(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(t -> t.getStartingTimeGrain() != null)
                .penalize(HardSoftScore.ONE_HARD, Task::getSpilloverCount)
                .asConstraint("No spillover into blocked grains");
    }

    private Constraint taskOnCorrectDate(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(t -> t.getForDate() != null)
                .filter(t -> t.getStartingTimeGrain() != null)
                .filter(t -> !t.getForDate().equals(t.getStartingTimeGrain().getDate()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Task on correct date");
    }

    private Constraint preferPreferredStartHour(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(t -> t.getPreferredStartHour() != null)
                .filter(t -> t.getStartingTimeGrain() != null)
                .penalize(HardSoftScore.ONE_SOFT, PlannerConstraintProvider::hourDistance)
                .asConstraint("Prefer preferred start hour");
    }

    private static int hourDistance(Task task) {
        int actualHour = task.getStartingTimeGrain().getStartTime().getHour();
        return Math.abs(actualHour - task.getPreferredStartHour());
    }

    private static boolean tasksOverlap(Task a, Task b) {
        if (a.getStartingTimeGrain() == null || b.getStartingTimeGrain() == null) return false;
        int aStart = a.getStartingTimeGrain().getIndex();
        int aEnd = aStart + a.getDurationInGrains();
        int bStart = b.getStartingTimeGrain().getIndex();
        int bEnd = bStart + b.getDurationInGrains();
        return aStart < bEnd && bStart < aEnd;
    }
}