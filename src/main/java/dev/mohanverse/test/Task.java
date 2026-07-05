package dev.mohanverse.test;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Task {
    @PlanningId
    private String name;

    @PlanningVariable
    private TimeSlot timeSlot;

    public Task() {}
    public Task(String name) { this.name = name; }

    public String getName() { return name; }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot t) { this.timeSlot = t; }
}
