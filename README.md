# SP Launcher

A minimal, student-friendly Android launcher built with Kotlin and Jetpack Compose.

It is a home-screen replacement that keeps study close at hand: a three-page swipe layout
(Settings on the left, Home in the centre, App Drawer on the right), with study widgets,
a study planner, and a card-based study reminder system.

## Features

- **Home** — clock/date, the Alerts widget (custom tags + optional reminders), a tasks
  checklist, a "study due" card, a study-cards progress line, and favourite-app pills.
- **App Drawer** — search, launch, and long-press actions (favourite, app info, uninstall).
- **Study Manager** — a Course › Subject › Topic › Class tree, with deadlines, reminders,
  drag-and-drop reordering, and JSON / text / PDF export.
- **Quick Time Table** — set a daily study window and a duration + rest per subject, and the
  app lays out the next 7 days from your un-completed classes, writing each session's
  reminder and deadline onto the real classes.
- **Study Popups** — card reminders delivered as notifications through the day: the front of
  the card, a "Show" button to reveal the back, then True/False. Correct cards are mastered;
  wrong cards come back after 1, 3, 5 and 7 days. Cards are imported from JSON files (one or
  many at once), organised into decks you can switch on or off. Never shown on the lock screen.
- **Themes** — six colour palettes plus night mode.
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
