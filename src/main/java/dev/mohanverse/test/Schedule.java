package dev.mohanverse.test;

import ai.timefold.solver.core.api.domain.solution.*;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import java.util.List;

@PlanningSolution
public class Schedule {
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<TimeSlot> timeSlots;

    @PlanningEntityCollectionProperty
    private List<Task> tasks;

    @PlanningScore
    private HardSoftScore score;

    public Schedule() {}
    public Schedule(List<TimeSlot> slots, List<Task> tasks) {
        this.timeSlots = slots;
        this.tasks = tasks;
    }

    public List<TimeSlot> getTimeSlots() { return timeSlots; }
    public List<Task> getTasks() { return tasks; }
    public HardSoftScore getScore() { return score; }
    public void setScore(HardSoftScore score) { this.score = score; }
}