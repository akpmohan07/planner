package dev.mohanverse.planner.util;

import dev.mohanverse.planner.domain.TimeGrain;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

public class TimeGrainGenerator {

    public static List<TimeGrain> generateWeek(int year, int weekNumber) {
        WeekFields weekFields = WeekFields.ISO;
        LocalDate monday = LocalDate.of(year, 1, 1)
                .with(weekFields.weekOfYear(), weekNumber)
                .with(weekFields.dayOfWeek(), 1);

        List<TimeGrain> grains = new ArrayList<>();
        int index = 0;
        for (int day = 0; day < 7; day++) {
            LocalDate date = monday.plusDays(day);
            LocalTime time = LocalTime.of(0, 0);
            for (int i = 0; i < 48; i++) {
                grains.add(new TimeGrain(index, date, time, false, null));
                index++;
                time = time.plusMinutes(30);
            }
        }
        return grains;
    }
}