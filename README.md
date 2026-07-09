## Running it

Requires Java 21.

```bash
./gradlew run        # runs dev.mohanverse.Main: blocks the week, runs the solver, prints results
```

# Personal Weekly Optimizer

People never skip the deterministic parts of their week. The shift starts, so you're there. The
class starts, so you're there. Nobody says "I didn't have time" about a fixed commitment, because
the time was never optional in the first place. But the flexible parts — the workout, the garden,
the thing you actually wanted to do — get dropped constantly, and "no time" is almost always the
excuse. It's rarely true. The time exists. It just was never actually planned for, so whatever's
fixed leaks into it by default.

My own week makes this obvious. Part-time shift work to cover living costs, an MSc in
Secure Software Engineering at DCU, a job search running alongside both — the deterministic load is
heavy, and it changes shape every week. Garden, workout, cooking, family time kept losing by
default, not by choice, and manual planning had genuinely broken down: everything, and nothing, was
getting planned.

So this isn't a scheduling app. It's what happens when you stop treating "no time" as an answer and
instead compute, precisely, how much time is actually left once every fixed commitment is accounted
for — then treat placing the flexible stuff into that remainder as a real problem worth solving
properly, not an afterthought.

## The core insight

Most of a personal week is deterministic, not a solver problem. Shift schedule, sleep timing,
travel, and study windows all follow fixed rules with no real choice in them. There's one correct
answer and it's computable directly. The genuine solver problem is much narrower than that:
fitting a handful of flexible activities (cooking, garden, workout) into whatever free time is
actually left once the fixed parts of the week are accounted for.

That split, between what's deterministic and what's actually worth solving, is the main
architectural idea in this codebase. It shows up directly in the package layout below.

## Architecture

```
dev.mohanverse.planner
├── domain
│   ├── TimeGrain.java       — one 30-min cell in the week (index, date, startTime, blocked, occupiedBy)
│   ├── Task.java            — @PlanningEntity, flexible activities the solver places
│   ├── Week.java            — owns the grains + shift list, answers hasShiftOn/nextShiftStartAfter/spillover
│   └── WeekSchedule.java    — @PlanningSolution
├── event
│   ├── FixedEvent.java      — interface: getStart/getEnd/getLabel/chainSubsequent()
│   ├── Shift.java           — work shift, chains into Travel
│   ├── Wakeup.java          — shift-aware: pre-shift block on shift days, Travel + StudyTime otherwise
│   ├── Travel.java          — generic reusable block, no chaining logic of its own
│   ├── Sleep.java           — Sleep.forNight() adapts to shift end time; Sleep.blockWeek() applies daily
│   └── StudyTime.java       — plain block, deterministic study window (non-shift days only)
├── blocker
│   └── FixedEventBlocker.java — recursively blocks grains for an event + its chainSubsequent()
├── solver
│   └── PlannerConstraintProvider.java — overlap, spillover, date, shift-adjacency, and preference constraints
└── util
    ├── TimeGrainGenerator.java — generateWeek(year, weekNumber) → 336 grains (30-min × 48 × 7)
    └── CalendarPrinter.java    — pretty-prints the week with per-block hours and weekly totals
```

### Fixed events: a chain of responsibility

`FixedEvent` is a pure contract: start, end, label, `chainSubsequent()`. Originating events
(`Shift`, `Wakeup`) own real-world context and decide what happens next. Building blocks (`Travel`,
`Sleep`, `StudyTime`) are dumb — they don't invent their own follow-up, they just carry whatever
they're given.

`Wakeup` is the interesting one: it looks ahead at `Week` to check whether a shift starts later
that same day, not just at the calendar day of week. On an ordinary day, the answer really is fixed
— library on weekdays, home on weekends, no real choice either way. On a shift day, forcing a fixed
7-hour study block would just run the day straight into work with no room for anything else. So
instead of guessing a shorter fixed number, `Wakeup` reserves only what's genuinely non-negotiable —
a 1-hour get-ready-and-commute block ending exactly at shift start — and leaves the rest as ordinary
free time for the solver to fill:

```java
Optional<LocalDateTime> shiftStart = week.nextShiftStartAfter(end);
if (shiftStart.isPresent()) {
    // Shift later today: reserve the commute, leave the rest genuinely free
    Travel preShiftTravel = new Travel(shiftStart.get().minusHours(1), shiftStart.get());
    return List.of(preShiftTravel);
}
// No shift today: Study is still fixed — library on weekdays, home on weekends
```

`FixedEventBlocker.block(grains, event)` blocks an event's own range, then recurses into
`event.chainSubsequent()`. A three-line `Shift` object in `Main` cascades into Travel, Sleep,
Wakeup, and whatever Wakeup decides, automatically. None of that chain is hand-wired in `Main`.

## The actual solver problem

`Task` is a `@PlanningEntity` with one `@PlanningVariable`: `TimeGrain startingTimeGrain`. Its value
range is the shared list of free grains on `WeekSchedule`, not a range filtered per entity. Per
Timefold's own docs, restricting the value range per entity effectively creates a hidden hard
constraint, and it removes the solver's freedom to escape local optima during search.

Constraints currently implemented in `PlannerConstraintProvider`:

| Constraint | Type | What it checks |
|---|---|---|
| `noOverlappingTasks` | hard | pairwise grain-index overlap between any two tasks |
| `noSpilloverIntoBlocked` | hard | a task's full duration doesn't run into an already-blocked grain |
| `taskOnCorrectDate` | hard | a task pinned to a specific calendar date actually lands there |
| `shiftAdjacentDayRequired` | hard | a task's date has a shift itself, or the day before it does |
| `preferPreferredStartHour` | soft | penalizes `\|actualHour - preferredHour\|` |

### What's working end to end today
<img width="347" height="824" alt="image" src="https://github.com/user-attachments/assets/b8805e80-6b96-4044-82d8-a5c6e80aaa81" />


Four task types compete for the same free time: seven `Cooking` sessions pinned one-per-day, a
`Study` task on each shift day (filling whatever's left before work, now that `Wakeup` no longer
hardcodes a fixed study block there), and one `Entertainment` task restricted to a day that has a
shift or follows one. On Friday — a shift day — the solver works out a full, sensible afternoon on
its own, with nothing hand-placed:

```
Wakeup ends 09:00 → free → Entertainment 12:00–15:00 → Study 15:00–17:00 →
Cooking-4 17:00–18:00 → pre-shift Travel 18:00–19:00 → Dominos shift 19:00
Score: 0hard/-14soft
```

`0hard` means every task landed on a valid day with no overlaps or spillover, including the shift-
adjacency rule for `Entertainment`. The remaining soft penalty is just the usual tension between
tasks' preferred hours. None of this is hardcoded to Friday specifically — the shift-adjacency check
and the pre-shift reservation are both computed off `Week`'s actual blocked grains, so this keeps
working unchanged if the shift schedule moves to different days next week.

## Roadmap

- Wire Garden and Workout into the solver alongside Cooking, Study, and Entertainment.
- **A priority-based "call family" slot:** hard constraint for closest family, round-robin soft
  constraint for everyone else, driven by last-contacted date. The most genuinely solver-shaped
  piece of the project, and it hasn't been started yet.



## Stack

Java 21 · Gradle · [Timefold Solver](https://timefold.ai) 2.2.0 · Lombok · Logback

## On the use of AI

I built this with Claude Code as a pair-programming partner, and I want to be upfront about how the
work actually split.

Mine: The problem framing, the deterministic-vs-solver architectural split, deciding which constraints
should be hard vs. soft, and working through the real-world constraints (Garden's actual allotment
hours, why a Dominos shift rules out a college day, how sleep should adapt to a late shift) were
mine. So was catching the design gaps as they came up. An early solver run, for instance, silently
clustered several `Cooking` tasks onto the same day, because nothing in the model actually pinned a
task to its intended date. That's what led to the `taskOnCorrectDate` constraint. Every design
decision here came out of discussion before any code got written.

Claude Code wrote the code once a design was agreed on, pulled in Timefold's own documentation to
keep the solver-specific parts idiomatic, and ran the project to check behavior. It did the typing.
The decisions were mine.
