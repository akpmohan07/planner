# Personal Weekly Optimizer

My week has genuinely irregular structure: part-time shifts (Dominos) to cover costs while
finishing an MSc (Secure Software Engineering, DCU) and actively job-hunting. Shift timing changes
weekly, sleep timing depends on the previous night's shift, and everything else — study, garden,
workout, cooking — has to fit around that. Most calendar tools assume a fixed 9-to-5 life. Manual
planning had broken down completely: "everything, nothing is getting planned."

That problem is what led to this project. It also turned into a fitting way to prepare for a
discovery call with Timefold, the company behind the open source constraint solver this planner is
built on, for their Optimization Model Engineer role (connection made at a Java User Group (JUG)
Dublin meetup). Rather than prepare by reading docs and building a toy tutorial, I built something
real on their solver — solving my actual problem above, not a contrived one.

So this isn't "build a scheduling app." It's "model my actual week honestly, and find out where
real optimization is needed versus where it's just undiscovered deterministic structure." That
distinction — and how much smaller the genuine solver problem turned out to be than expected — is
the main finding of the project, and it's what the rest of this README walks through.

## The core insight

Most of a personal week is **deterministic**, not a solver problem. Given a shift schedule, sleep
timing, travel, and study windows all follow fixed rules with no real choice involved — there's
one correct answer, computable directly, with nothing to search over. The genuine solver problem
turned out to be much narrower than I expected: placing a handful of flexible activities (cooking,
garden, workout) into whatever free time is actually left once the fixed structure of the week is
accounted for.

This split — deterministic fixed events vs. genuine solver-worthy decisions — is the main
architectural principle of the whole project, and it shows up directly in the package layout below.

## Architecture

```
dev.mohanverse.planner
├── domain
│   ├── TimeGrain.java       — one 30-min cell in the week (index, date, startTime, blocked, occupiedBy)
│   ├── Task.java            — @PlanningEntity, flexible activities the solver places
│   └── WeekSchedule.java    — @PlanningSolution
├── event
│   ├── FixedEvent.java      — interface: getStart/getEnd/getLabel/chainSubsequent()
│   ├── Shift.java           — work shift, chains into Travel
│   ├── Wakeup.java          — decides Travel + StudyTime chain
│   ├── Travel.java          — generic reusable block, no chaining logic of its own
│   ├── Sleep.java           — Sleep.forNight() adapts to shift end time; Sleep.blockWeek() applies daily
│   └── StudyTime.java       — plain block, deterministic study window
├── blocker
│   └── FixedEventBlocker.java — recursively blocks grains for an event + its chainSubsequent()
├── solver
│   └── PlannerConstraintProvider.java — overlap, spillover, date, and preference constraints
└── util
    ├── TimeGrainGenerator.java — generateWeek(year, weekNumber) → 336 grains (30-min × 48 × 7)
    └── CalendarPrinter.java    — pretty-prints the week with per-block hours and weekly totals
```

### Fixed events: a chain of responsibility

`FixedEvent` is a pure contract (start, end, label, `chainSubsequent()`). **Originating events**
(`Shift`, `Wakeup`) own real-world context and decide what follows them. **Building blocks**
(`Travel`, `Sleep`, `StudyTime`) are dumb — they don't invent their own follow-up, they just carry
whatever they're given.

```java
// Wakeup.chainSubsequent() — a weekday gets Travel + StudyTime, a weekend gets just Travel
if (isWeekday) {
    return List.of(travel, new StudyTime(travelEnd, travelEnd.plusHours(7)));
}
return List.of(travel);
```

`FixedEventBlocker.block(grains, event)` recursively blocks an event's own range, then recurses
into `event.chainSubsequent()`. A three-line `Shift` object in `Main` cascades into Travel, Sleep,
Wakeup, Travel, and StudyTime automatically — none of that chain is hand-wired in `Main` itself.

## The actual solver problem

`Task` is a `@PlanningEntity` with a single `@PlanningVariable TimeGrain startingTimeGrain`. Its
value range is the **shared list of free grains** on `WeekSchedule`, not a per-entity filtered
range — per Timefold's own docs, restricting the value range per entity effectively creates a
hidden hard constraint and removes the solver's freedom to escape local optima during search.

Constraints currently implemented in `PlannerConstraintProvider`:

| Constraint | Type | What it checks |
|---|---|---|
| `noOverlappingTasks` | hard | pairwise grain-index overlap between any two tasks |
| `noSpilloverIntoBlocked` | hard | a task's full duration doesn't run into an already-blocked grain |
| `taskOnCorrectDate` | hard | a task pinned to a specific calendar date actually lands there |
| `preferPreferredStartHour` | soft | penalizes `\|actualHour - preferredHour\|` |

### What's working end to end today

Cooking is the first task fully wired through the solver: seven 1-hour sessions, one per day,
each hard-pinned to its calendar date, each softly preferring 7pm. No day-of-week logic anywhere —
the solver just sees whatever's actually free that week and finds the closest available hour to
19:00 for each day, sliding earlier automatically on days a shift already occupies dinner time:

```
Cooking-0 (Mon) -> 19:00   Cooking-1 (Tue) -> 19:00   Cooking-2 (Wed) -> 19:00
Cooking-3 (Thu) -> 19:00   Cooking-4 (Fri) -> 18:00*  Cooking-5 (Sat) -> 18:00*
Cooking-6 (Sun) -> 18:00*                              (* shift starts at/after 19:00)
Score: 0hard/-3soft
```

`0hard` means every task landed on its correct day with no overlaps or spillover. The `-3soft` is
the true optimum for this model — there's no arrangement that gets any of the three shift-day
cooking sessions to exactly 7pm without spilling into the shift. If the shift schedule changes next
week, this keeps working with zero code changes, because nothing is hardcoded to specific days.

## Roadmap

- Generalize `Wakeup`'s Travel/StudyTime decision to react to whatever's actually blocked that day
  (e.g. a shift), instead of a fixed weekday/weekend calendar check.
- Wire Garden and Workout into the solver alongside Cooking.
- A priority-based "call family" slot — hard constraint for closest family, round-robin soft
  constraint for everyone else, driven by last-contacted date. The most genuinely solver-shaped
  piece of the whole project, not started yet.

## Running it

Requires Java 21.

```bash
./gradlew run       # runs dev.mohanverse.Main — blocks the week, runs the solver, prints results
./gradlew build      # compile + test
```

## Stack

Java 21 · Gradle · [Timefold Solver](https://timefold.ai) 2.2.0 · Lombok · Logback

## On the use of AI

I built this with Claude Code as a pair-programming partner, and want to be upfront about how the
work split.

Mine: the problem framing, the deterministic-vs-solver architectural split, deciding which
constraints should be hard vs. soft, working through the real-world constraints (Garden's actual
allotment opening hours, why Dominos shifts rule out a college day, how sleep should adapt to a
late shift), and catching design gaps as they showed up — e.g. an early solver run silently
clustered several `Cooking` tasks onto the same day because nothing in the model actually pinned a
task to its intended date, which is what led to the `taskOnCorrectDate` constraint. Every design
decision in this README was made through discussion before code was written, and reflects choices I
made, not ones handed to me.

Claude Code's role was implementation: writing the code once a design was agreed, pulling in
Timefold's own documentation to keep the solver-specific code idiomatic, and running the project to
verify behavior. I treated it the way I'd treat a capable pairing partner — useful for turning a
decision into working code quickly, not for making the decisions.
