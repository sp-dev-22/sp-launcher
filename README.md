# SP Launcher

A minimal, student-friendly Android launcher built with Kotlin and Jetpack Compose.

It is a home-screen replacement that keeps study close at hand: a three-page swipe layout
(Settings on the left, Home in the centre, App Drawer on the right), with study widgets,
a study planner, and a card-based study reminder system.

## Features

- **Home** — a 12-hour clock (e.g. `8:19 PM`) with date, the Alerts widget (custom tags +
  optional reminders), a tasks checklist, a single **Study Manager Status** card (progress +
  tasks due over the next 7 days), and favourite-app pills.
- **App Drawer** — search, launch, and long-press actions (favourite, app info, uninstall).
- **Goals** — a regular-activity tracker. Each goal is either *limited* (a target, e.g. 2 litres
  a day) or *unlimited* (no finish line, it just keeps counting). A goal carries a starting
  text and, for limited goals, an end text it switches to when the target is reached, a
  per-click value, an optional button label (e.g. `100 Liter`), and an optional daily reset.
  Goals appear in a Home widget and are ticked from there; tapping the card opens the Goals
  screen to add, edit or delete them.
- **Calendar** — a month view of everything already dated in the app: study deadlines and
  reminders, task due dates and reminders, note reminders and card review days. Tapping a row
  opens the screen that owns it. Completed checklist tasks stay on the day, struck through and
  cut to one line, until you clear them with the small cross; clearing hides them from the
  calendar only and leaves the task itself alone.
- **Study Manager** — a Course › Subject › Topic › Class tree, with deadlines, reminders,
  drag-and-drop reordering, and JSON / text / PDF export. Completing a task clears its
  Reminder & Deadline automatically, and a **Clean** button clears every task's Reminder &
  Deadline in one tap.
- **Quick Time Table** — set a daily study window and, per subject, its duration, rest and
  sessions per day (single / double / triple), pick a 1, 3 or 7 day plan, and the app lays it
  out from your un-completed classes, writing each session's reminder and deadline onto the
  real classes. If the day is too short for every session, nothing is generated and the app
  shows the exact calculation (available vs. needed minutes).
- **Study Popups** — card reminders delivered as notifications through the day: the front of
  the card, a "Show" button to reveal the back, then True/False. Correct cards are mastered;
  wrong cards come back after 1, 3, 5 and 7 days. Cards are imported from JSON files (one or
  many at once), organised into decks you can switch on or off. Never shown on the lock screen.
- **Themes** — six colour palettes plus night mode.
- **Widgets** — the Clock, Date, Alerts, Tasks, Study Manager and Goals widgets can each be
  switched on or off from the Widgets sheet.
- **Backup & Restore** — export and import everything as a single JSON file.

## Card file format

Each file is JSON — either a plain list, or an object with a `cards` list:

```json
{
  "deck": "RRB GK",
  "cards": [
    { "front": "Capital of India?", "back": "New Delhi" },
    { "front": "Largest planet in our solar system?",
      "options": ["Jupiter", "Saturn", "Earth", "Mars"], "correct": 1 }
  ]
}
```

`front` is required. Give `back` for the answer, or `options` + `correct` (1-based) for an
MCQ and the back is filled in as `Answer: 1) Jupiter`. `q`/`a` are accepted as aliases.

## Building

The APK is built by GitHub Actions on every push to `main` and attached to the
`apk-latest` release. Locally:

```
gradle assembleDebug
```

- `minSdk` 24, `targetSdk` 36
- Kotlin, Jetpack Compose, no third-party runtime dependencies

## Notes

- Reminders and card notifications use `AlarmManager.setAndAllowWhileIdle`, so no exact-alarm
  permission is needed.
- On aggressive battery-saving phones, allow the app to auto-start and set battery usage to
  "no restrictions", or background alarms may not fire.
