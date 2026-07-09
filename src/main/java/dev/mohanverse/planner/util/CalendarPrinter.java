package dev.mohanverse.planner.util;

import dev.mohanverse.planner.domain.Task;
import dev.mohanverse.planner.domain.TimeGrain;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarPrinter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    /** Deterministic-only view: fixed events vs Free, no solved Tasks yet. */
    public static void print(List<TimeGrain> grains) {
        print(grains, List.of());
    }

    /**
     * Prints the full week as one timeline per day, merging deterministic fixed events
     * (Sleep, Wakeup, Travel, StudyTime, Dominos) with solver-placed Tasks, tagged
     * [fixed] or [solver] so the two regimes this project is actually about stay
     * visually distinct instead of looking like one undifferentiated calendar.
     */
    public static void print(List<TimeGrain> grains, List<Task> tasks) {
        Map<Integer, Task> taskByGrainIndex = new HashMap<>();
        for (Task task : tasks) {
            TimeGrain start = task.getStartingTimeGrain();
            if (start == null) continue;
            for (int i = start.getIndex(); i < start.getIndex() + task.getDurationInGrains(); i++) {
                taskByGrainIndex.put(i, task);
            }
        }

        Map<LocalDate, List<TimeGrain>> byDate = grains.stream()
                .collect(Collectors.groupingBy(TimeGrain::getDate, TreeMap::new, Collectors.toList()));

        Map<String, Integer> totalGrainsByLabel = new TreeMap<>();

        for (Map.Entry<LocalDate, List<TimeGrain>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<TimeGrain> dayGrains = entry.getValue();
            dayGrains.sort(Comparator.comparing(TimeGrain::getStartTime));

            System.out.println();
            System.out.println("=== " + date + " (" + date.getDayOfWeek() + ") ===");

            String currentLabel = null;
            Source currentSource = null;
            LocalTime blockStart = null;
            LocalTime blockEnd = null;

            for (TimeGrain g : dayGrains) {
                String label;
                Source source;
                if (g.isBlocked()) {
                    label = g.getOccupiedBy();
                    source = Source.FIXED;
                } else {
                    Task task = taskByGrainIndex.get(g.getIndex());
                    if (task != null) {
                        label = task.getName();
                        source = Source.SOLVER;
                    } else {
                        label = "Free";
                        source = Source.FREE;
                    }
                }
                totalGrainsByLabel.merge(label, 1, Integer::sum);

                if (!label.equals(currentLabel)) {
                    if (currentLabel != null) {
                        printLine(blockStart, blockEnd, currentLabel, currentSource);
                    }
                    currentLabel = label;
                    currentSource = source;
                    blockStart = g.getStartTime();
                }
                blockEnd = g.getStartTime().plusMinutes(30);
            }
            if (currentLabel != null) {
                printLine(blockStart, blockEnd, currentLabel, currentSource);
            }
        }

        printSummary(totalGrainsByLabel);
    }

    private enum Source { FIXED, SOLVER, FREE }

    private static void printLine(LocalTime start, LocalTime end, String label, Source source) {
        double hours = end.isAfter(start)
                ? Duration.between(start, end).toMinutes() / 60.0
                : (24 * 60 - Duration.between(end, start).toMinutes()) / 60.0;

        String tag = switch (source) {
            case FIXED -> " [fixed]";
            case SOLVER -> " [solver]";
            case FREE -> "";
        };

        System.out.printf("  %s - %s (%.1fh)  |  %s%s%n",
                start.format(TIME_FORMAT), end.format(TIME_FORMAT), hours, label, tag);
    }

    private static void printSummary(Map<String, Integer> totalGrainsByLabel) {
        System.out.println();
        System.out.println("=== Weekly Totals ===");
        for (Map.Entry<String, Integer> entry : totalGrainsByLabel.entrySet()) {
            double hours = entry.getValue() * 0.5;
            System.out.printf("  %-14s : %.1f hours%n", entry.getKey(), hours);
        }
    }
}