# Changelog

## v1.20.0

### New layout
- **Settings** stays on the left page, and is now purely how the launcher looks and behaves.
  Its two-tab layout is gone.
- **Apps & Widgets** is the new right page: the launcher's apps (Study Manager, Alerts,
  Tasks, Study Popups, Goals), every Home widget's on/off switch, and the phone's apps
  (Favorites & groups, Hidden apps, Locked apps, notification badges), plus Themes.
- **The app drawer now slides up** over Home — swipe up from the bottom of Home to open
  it, swipe its header down or press Back to close it. It is no longer the right page.

### Details
- Nothing was removed in the move: the old Settings > Apps tab and the widget switches
  both live on the right page now.
- Hidden apps are reachable from the right page, behind the same vault PIN as before. The
  PIN dialog was moved into one shared component so no door can skip the lock.
- Unit tests now run in CI before the APK build. They report, they do not gate.

## v1.18.0
- Notification badges: unread counts on drawer and favourites icons.
- App shortcuts and APK sharing on long-press.
- Fixes: What's New after updates, completed classes in Time Table, fingerprint unlock,
  ghost entries in favourite groups.
