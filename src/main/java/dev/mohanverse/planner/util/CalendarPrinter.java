package dev.mohanverse.planner.util;
package dev.mohanverse.planner.util;

import dev.mohanverse.planner.domain.TimeGrain;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarPrinter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public static void print(List<TimeGrain> grains) {
        Map<LocalDate, List<TimeGrain>> byDate = grains.stream()
                .collect(Collectors.groupingBy(TimeGrain::getDate, TreeMap::new, Collectors.toList()));

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
    }

    private static void printLine(LocalTime start, LocalTime end, String label) {
        System.out.printf("  %s - %s  |  %s%n",
                start.format(TIME_FORMAT), end.format(TIME_FORMAT), label);
    }
}