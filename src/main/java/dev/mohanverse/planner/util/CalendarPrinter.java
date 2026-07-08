package dev.mohanverse.planner.util;

import dev.mohanverse.planner.domain.TimeGrain;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarPrinter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public static void print(List<TimeGrain> grains) {
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
            LocalTime blockStart = null;
            LocalTime blockEnd = null;

            for (TimeGrain g : dayGrains) {
                String label = g.isBlocked() ? g.getOccupiedBy() : "Free";
                totalGrainsByLabel.merge(label, 1, Integer::sum);

                if (!label.equals(currentLabel)) {
                    if (currentLabel != null) {
                        printLine(blockStart, blockEnd, currentLabel);
                    }
                    currentLabel = label;
                    blockStart = g.getStartTime();
                }
                blockEnd = g.getStartTime().plusMinutes(30);
            }
            if (currentLabel != null) {
                printLine(blockStart, blockEnd, currentLabel);
            }
        }

        printSummary(totalGrainsByLabel);
    }

    private static void printLine(LocalTime start, LocalTime end, String label) {
        Duration duration = Duration.between(start, end.equals(LocalTime.MIDNIGHT) ? LocalTime.of(23,59,59) : end);
        double hours = end.isAfter(start)
                ? Duration.between(start, end).toMinutes() / 60.0
                : (24 * 60 - Duration.between(end, start).toMinutes()) / 60.0;

        System.out.printf("  %s - %s (%.1fh)  |  %s%n",
                start.format(TIME_FORMAT), end.format(TIME_FORMAT), hours, label);
    }

    private static void printSummary(Map<String, Integer> totalGrainsByLabel) {
        System.out.println();
        System.out.println("=== Weekly Totals ===");
        for (Map.Entry<String, Integer> entry : totalGrainsByLabel.entrySet()) {
            double hours = entry.getValue() * 0.5;
            System.out.printf("  %-12s : %.1f hours%n", entry.getKey(), hours);
        }
    }
}