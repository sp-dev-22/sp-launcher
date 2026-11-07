# SP Launcher

A minimal, student-friendly Android launcher built with Kotlin and Jetpack Compose.

It is a home-screen replacement that keeps study close at hand: a three-page swipe layout
(Settings on the left, Home in the centre, App Drawer on the right), with study widgets,
a study planner, a goals tracker and a card-based study reminder system.

**No internet permission.** SP Launcher cannot send anything anywhere — there is no
`INTERNET` permission in the manifest, no analytics, no ads and no account. Everything you
enter stays in the app's own storage on your phone.

## Screenshots

| Home | App Drawer |
| --- | --- |
| ![Home screen with clock, study status and goals](docs/screenshots/home.jpg) | ![App drawer with search](docs/screenshots/app-drawer.jpg) |

| Study Manager | Settings |
| --- | --- |
| ![Study Manager progress tree](docs/screenshots/study-manager.jpg) | ![Settings screen](docs/screenshots/settings.jpg) |

| Backup & Restore |
| --- |
| ![Backup and restore screen](docs/screenshots/backup-restore.jpg) |

## Features

- **Home** — a 12-hour clock with date, the Alerts widget (custom tags + optional reminders),
  a tasks checklist, a Goals widget you can tick from the home screen, a single **Study
  Manager Status** card (progress + tasks due over the next 7 days), and a **favourites bar**
  pinned to the bottom of the screen.
- **Favourites bar** — a taskbar-style row at the very bottom of Home. It holds **five slots in
  total**, each one either an app or an app group, and it stays put however many widgets are
  switched on (the widgets scroll above it). Tap a slot to launch, long-press to edit.
- **App groups** — pack up to **four apps into one slot**, so the bar's five slots can hold
  twenty apps. Groups are unnamed and only ever appear in the favourites bar: the slot shows the
  apps' icons in miniature, tap it to open them.
- **Real app icons** — the drawer, the favourites bar and the group sheet use the actual
  launcher icon of each installed app (cached, and falling back to the old coloured initial if
  an icon cannot be read).
- **App Drawer** — search, launch, and long-press actions (favourite, app info, uninstall).
- **Goals** — a regular-activity tracker. Each goal is either *limited* (a target, e.g. 2 litres
  a day) or *unlimited* (no finish line, it just keeps counting). A goal carries a starting
  text and, for limited goals, an end text it switches to when the target is reached, a
  per-click value, an optional button label (e.g. `100 Liter`), and an optional daily reset.
  Goals appear in a Home widget and are ticked from there; tapping the card opens the Goals
  screen to add, edit, reset or delete them.
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
- **Themes** — seven colour palettes plus night mode. The schedules (auto night window, theme
  rotation every hour or every day) and the animation toggles live on their own screen:
  **Settings > Auto & Animations**.
- **Diwali theme** — a rainbow palette that gives the whole app different colours, and switches
  the festival animations on by itself: **Lamps**, **Firecrackers**, **Rangoli**, **Chakri**,
  **Sky lanterns** and **Flower petals**. Each one has its own toggle under Settings > Auto &
  Animations, and they are drawn on the launcher only — never over other apps.
- **Widgets** — the Clock, Date, Alerts, Tasks, Study Manager and Goals widgets can each be
  switched on or off from the Widgets sheet.
- **Backup & Restore** — export and import everything as a single JSON file, plus an optional
  daily automatic backup into a folder you pick.
- **Hidden apps (the vault)** — keep selected apps out of the drawer, behind a four-digit PIN
  typed as a command in the app search bar:
  - `vault 1234` opens the hidden-apps screen (hide, unhide, change PIN).
  - `apps 1234` opens the same list in launch mode, so a hidden app can be started **without
    unhiding it first**.

  The default PIN is `1234`. The clock is no longer a door to the vault.

## Download

The APK is built by GitHub Actions and attached to the
[`apk-latest` release](https://github.com/sp-dev-22/sp-launcher/releases/tag/apk-latest).
Each version is kept as its own file (`sp-launcher-vX.Y.apk`) so older builds are never lost.

- `minSdk` 24 (Android 7 and above), `targetSdk` 36

## Building

```
gradle assembleDebug
```

Kotlin, Jetpack Compose, and no third-party runtime dependencies. The APK is rebuilt on every
push to `main` and attached to the `apk-latest` release.

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

## What's new in 1.11.2

- **Auto & Animations** is its own Settings screen now: auto night mode (with its two times),
  auto theme change (1 hour / 1 day) and a separate on/off for every animation. The Themes sheet
  holds just the palettes and night mode, so nothing is squeezed off the bottom of the screen.
- **Groups are smaller and simpler**: no name, up to four apps per group, mini icons inside the
  slot. Five slots, twenty apps.
- Three more animations: **Chakri**, **Sky lanterns** and **Flower petals**.
- **Animations over other apps removed.** They could swallow touches and drained the battery;
  the effects now stay inside the launcher.
- Goals widget: the dashed rule is gone — it reads `1 Pushup → 10 Pushups`, with the goal's
  start and end text underneath.
- From 1.11: real app icons, the five-slot favourites bar, the vault as search commands
  (`vault <pin>` / `apps <pin>`) and the PIN-change fix.

## Notes

- Reminders and card notifications use `AlarmManager.setAndAllowWhileIdle`, so no exact-alarm
  permission is needed.
- On aggressive battery-saving phones, allow the app to auto-start and set battery usage to
  "no restrictions", or background alarms may not fire.
- A launcher needs the `QUERY_ALL_PACKAGES` permission to list the apps installed on the phone.
  That is the only broad permission the app asks for, and the list never leaves the device.

## Support

SP Launcher is free and has no ads. If it saves you some time, you can send a tip on UPI —
any amount, once, no subscription.

- UPI: `sp.26@ptyes`
- GitHub: [@sp-dev-22](https://github.com/sp-dev-22)
- YouTube: [@sp-dev-22](https://youtube.com/@sp-dev-22)
- Email: [somu.22.2026@gmail.com](mailto:somu.22.2026@gmail.com)

## License

[PolyForm Noncommercial License 1.0.0](LICENSE) — Copyright 2026 Somdatt Prajapat.

In plain words: you may read, copy, modify and use this code for **personal, noncommercial**
purposes. You may **not** use it commercially or ship it as a product, and you may not use it
at production level, without permission.

Note that this is deliberately *not* an open-source licence in the OSI sense, so the app is not
eligible for F-Droid or other catalogues that require a free-software licence.
