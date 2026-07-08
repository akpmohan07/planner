package dev.mohanverse;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import dev.mohanverse.planner.blocker.FixedEventBlocker;
import dev.mohanverse.planner.domain.Task;
import dev.mohanverse.planner.domain.TimeGrain;
import dev.mohanverse.planner.domain.WeekSchedule;
import dev.mohanverse.planner.event.Shift;
import dev.mohanverse.planner.event.Sleep;
import dev.mohanverse.planner.solver.PlannerConstraintProvider;
import dev.mohanverse.planner.util.CalendarPrinter;
import dev.mohanverse.planner.util.TimeGrainGenerator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
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

        Sleep.blockWeek(2026, 28, allGrains);

        // print
        allGrains.stream()
                .filter(TimeGrain::isBlocked)
                .forEach(g -> System.out.println(g.getDate() + " " + g.getStartTime() + " is blocked by " + g.getOccupiedBy()));



        CalendarPrinter.print(allGrains);
        List<TimeGrain> freeGrains = allGrains.stream()
                .filter(g -> !g.isBlocked())
                .toList();
        List<Task> tasks = new ArrayList<>();
        WeekFields weekFields = WeekFields.ISO;
        LocalDate monday = LocalDate.of(2026, 1, 1)
                .with(weekFields.weekOfYear(), 28)
                .with(weekFields.dayOfWeek(), 1);
        for (int day = 0; day < 7; day++) {
            LocalDate date = monday.plusDays(day);
            tasks.add(new Task("Cooking-" + day, 2, 19, date)); // 1h, prefer 7PM
        }

        Task.setAllGrains(allGrains);

        SolverFactory<WeekSchedule> factory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(WeekSchedule.class)
                        .withEntityClasses(Task.class)
                        .withConstraintProviderClass(PlannerConstraintProvider.class)
                        .withTerminationSpentLimit(Duration.ofSeconds(10))
        );

        Solver<WeekSchedule> solver = factory.buildSolver();
        WeekSchedule solution = solver.solve(new WeekSchedule(freeGrains, tasks));

        System.out.println();
        System.out.println("=== Solved Tasks ===");
        solution.getTasks().forEach(t -> {
            var grain = t.getStartingTimeGrain();
            System.out.println(t.getName() + " (" + t.getDurationInGrains() + " grains) -> "
                    + grain.getDate() + " " + grain.getStartTime());
        });
        System.out.println("Score: " + solution.getScore());
    }
}