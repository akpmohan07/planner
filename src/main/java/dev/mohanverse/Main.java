package dev.mohanverse;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import dev.mohanverse.planner.blocker.FixedEventBlocker;
import dev.mohanverse.planner.domain.Task;
import dev.mohanverse.planner.domain.TimeGrain;
import dev.mohanverse.planner.domain.WeekSchedule;
import dev.mohanverse.planner.event.Shift;
import dev.mohanverse.planner.solver.PlannerConstraintProvider;
import dev.mohanverse.planner.util.TimeGrainGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TimeGrain> allGrains = TimeGrainGenerator.generateWeek(2026, 28);

        Shift friday = new Shift(
                LocalDateTime.of(2026, 7, 10, 19, 0),
                LocalDateTime.of(2026, 7, 11, 3, 0));
        Shift saturday = new Shift(
                LocalDateTime.of(2026, 7, 11, 19, 30),
                LocalDateTime.of(2026, 7, 12, 3, 0));
        Shift sunday = new Shift(
                LocalDateTime.of(2026, 7, 12, 19, 0),
                LocalDateTime.of(2026, 7, 13, 2, 0));

        FixedEventBlocker.block(allGrains, friday);
        FixedEventBlocker.block(allGrains, saturday);
        FixedEventBlocker.block(allGrains, sunday);



        List<TimeGrain> freeGrains = allGrains.stream()
                .filter(g -> !g.isBlocked())
                .toList();

        List<Task> tasks = List.of(
                new Task("Study", 4),
                new Task("Garden", 6),
                new Task("Workout", 2)
        );

        SolverFactory<WeekSchedule> factory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(WeekSchedule.class)
                        .withEntityClasses(Task.class)
                        .withConstraintProviderClass(PlannerConstraintProvider.class)
                        .withTerminationSpentLimit(Duration.ofMinutes(5))
        );

        Solver<WeekSchedule> solver = factory.buildSolver();
        WeekSchedule solution = solver.solve(new WeekSchedule(freeGrains, tasks));

        solution.getTasks().forEach(t -> {
            var grain = t.getStartingTimeGrain();
            System.out.println(t.getName() + " (" + t.getDurationInGrains() + " grains) -> "
                    + grain.getDate() + " " + grain.getStartTime());
        });
    }
}