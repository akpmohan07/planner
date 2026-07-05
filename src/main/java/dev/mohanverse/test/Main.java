package dev.mohanverse.test;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import java.time.Duration;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TimeSlot> slots = List.of(
                new TimeSlot("Mon 9AM"),
                new TimeSlot("Mon 10AM"),
                new TimeSlot("Mon 11AM"),
                new TimeSlot("Mon 1PM"),
                new TimeSlot("Mon 2PM")
        );

        List<Task> tasks = List.of(
                new Task("Study"),
                new Task("Job Search"),
                new Task("Garden")
        );

        SolverFactory<Schedule> factory = SolverFactory.create(
                new SolverConfig()
                        .withSolutionClass(Schedule.class)
                        .withEntityClasses(Task.class)
                        .withConstraintProviderClass(ScheduleConstraintProvider.class)
                        .withTerminationSpentLimit(Duration.ofSeconds(5))
        );

        Solver<Schedule> solver = factory.buildSolver();
        Schedule solution = solver.solve(new Schedule(slots, tasks));

        solution.getTasks().forEach(t ->
                System.out.println(t.getName() + " -> " + t.getTimeSlot().getLabel())
        );
    }
}