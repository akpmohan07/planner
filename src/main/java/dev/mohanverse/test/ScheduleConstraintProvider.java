package dev.mohanverse.test;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public class ScheduleConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                noTwoTasksInSameSlot(factory)
        };
    }

    private Constraint noTwoTasksInSameSlot(ConstraintFactory factory) {
        return factory.forEachUniquePair(Task.class,
                        Joiners.equal(Task::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No two tasks in same slot");
    }
}