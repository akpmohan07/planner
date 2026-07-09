package dev.mohanverse.planner.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import dev.mohanverse.planner.domain.Task;
import dev.mohanverse.planner.domain.TimeGrain;

import java.time.LocalDate;

public class PlannerConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                noOverlappingTasks(factory),
                noSpilloverIntoBlocked(factory),
                taskOnCorrectDate(factory),
                shiftAdjacentDayRequired(factory),
                preferPreferredStartHour(factory)
        };
    }

    // Hard: two flexible tasks can't claim overlapping real time, regardless of what they are.
    private Constraint noOverlappingTasks(ConstraintFactory factory) {
        return factory.forEachUniquePair(Task.class)
                .filter(PlannerConstraintProvider::tasksOverlap)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No overlapping tasks");
    }

    // Hard: a task's full duration must land entirely on free grains. Task::getSpilloverCount
    // counts how many of its grains are already owned by a fixed event, so the penalty scales
    // with how badly it overruns rather than firing as a flat yes/no.
    private Constraint noSpilloverIntoBlocked(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(t -> t.getStartingTimeGrain() != null)
                .penalize(HardSoftScore.ONE_HARD, Task::getSpilloverCount)
                .asConstraint("No spillover into blocked grains");
    }

    // Hard: some tasks (e.g. a Cooking session) are pinned to a specific calendar date.
    // Without this, nothing stops the solver from clustering all of them on whichever single
    // day happens to have the most free time that week.
    private Constraint taskOnCorrectDate(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(t -> t.getForDate() != null)
                .filter(t -> t.getStartingTimeGrain() != null)
                .filter(t -> !t.getForDate().equals(t.getStartingTimeGrain().getDate()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Task on correct date");
    }

    // Hard: some tasks (e.g. Entertainment) only make sense on a day that has a shift itself,
    // or the day right after one. Checked against the actual blocked grains in Week rather than
    // a fixed day-of-week, so it still holds if the shift schedule changes which days it lands on.
    private Constraint shiftAdjacentDayRequired(ConstraintFactory factory) {
        return factory.forEach(Task.class)
                .filter(Task::isRequiresShiftAdjacentDay)
                .filter(t -> t.getStartingTimeGrain() != null)
                .filter(t -> !isShiftAdjacentDay(t.getStartingTimeGrain().getDate()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Task must land on or after a shift day");
    }

    private static boolean isShiftAdjacentDay(LocalDate date) {
        var week = Task.getWeek();
        return week.hasShiftOn(date) || week.hasShiftOn(date.minusDays(1));
    }

    // Soft: nudges a task toward its ideal hour, but yields whenever a hard constraint above
    // forces it elsewhere — this is the only constraint the solver is free to compromise on.
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