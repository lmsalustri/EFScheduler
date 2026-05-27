# EFScheduler

EFScheduler is a local-first Android task and transition reminder app built with Kotlin and Jetpack Compose.

The app is designed to help users schedule tasks and receive transition reminders without relying on cloud storage or server-based notifications. All schedule data is stored locally on the device.

## Current Status

EFScheduler currently supports the core scheduling flow:

- Create tasks
- Pick a future task time
- Choose an early reminder when available
- Save tasks locally
- Edit task name, time, and reminder
- Delete tasks and cancel pending notifications
- Receive before-reminders
- Receive final task notifications
- Automatically remove completed tasks after the final notification fires
- Reschedule future notifications after device reboot
- Export schedule data to a local JSON backup
- Import schedule data from a local JSON backup
- View notification and alarm permission status
- Open permission settings from the app

## Local-First Design

EFScheduler does not use cloud storage.

Task data is stored locally using Room. Import and export are provided for manual backup and restore.

This means:

- No Firebase
- No cloud database
- No server-side scheduling
- No account required
- No internet connection required for saved Android reminders to fire

## Android Scheduling

EFScheduler uses Android's local scheduling system for reminders.

Main components:

- `AlarmManager` for exact scheduled reminders
- `NotificationReceiver` for posting notifications
- `BootReceiver` for rescheduling future tasks after reboot
- `Room` for persistent local task storage

When a task is saved, EFScheduler schedules:

1. A final notification at the task start time
2. An optional before-reminder if the selected reminder time is still in the future

When the final notification fires, the task is removed from Room so completed tasks do not stay in the active task list.

## Permissions

EFScheduler needs two main permissions for reliable reminders:

### Notifications

Required so the app can display reminders.

### Alarms & reminders

Required so reminders can fire on time, even if the app is closed.

If alarm permission is disabled, EFScheduler shows an in-app explanation dialog instead of immediately sending the user to Android settings. The user can choose to open settings or dismiss the prompt.

The About / Permissions screen also shows permission status:

- Enabled
- Disabled

Settings buttons are shown only when a permission is disabled.

## Backup and Restore

EFScheduler supports local JSON import/export.

Export creates a local backup file such as:

```text
EFScheduler-backup.json
```

The backup includes:

- Task ID
- Task name
- Task timestamp
- Reminder minutes
- Schema version
- Export timestamp

Import restores tasks into Room and reschedules notifications.

## Validation Rules

EFScheduler prevents common scheduling mistakes.

### Task time validation

Tasks must be scheduled at least one minute in the future.

If the selected time is too soon, the app shows:

```text
Choose a time at least 1 minute from now.
```

The Next button stays disabled until the selected time is valid.

### Reminder validation

Before-reminders are only shown if they would still occur in the future.

For example:

- If a task is 3 minutes away, early reminders are not shown.
- If a task is 10 minutes away, 5 minutes before may be shown.
- If a task is 2 hours away, all reminder options may be shown.

If no early reminder is possible, the app displays:

```text
No early reminder available
This task is too soon for an early reminder.
```

The user can still continue with a start-time notification.

## Confirmed Working

The following behaviors have been tested successfully:

- Task creation
- Task editing
- Task deletion
- Before-reminder notifications
- Final task notifications
- Task auto-removal after final notification
- Notification cancellation after delete
- Notification update after edit
- Reboot rescheduling through `BootReceiver`
- Import/export
- Permission status display
- Alarm permission prompt dialog

## Known Development Notes

### Emulator performance

The Android emulator may show skipped frame warnings or slow UI behavior. This is expected during development and does not necessarily indicate a scheduling failure.

Common harmless logs include:

```text
Skipped frames
Davey!
ashmem Pinning is deprecated
Autofill popup isn't shown
```

The important failures to watch for are:

```text
FATAL EXCEPTION
SecurityException
SQLiteException
IllegalStateException
```

### Compose prefetch workaround

The app currently disables pausable composition in LazyColumn prefetch as a temporary workaround for a Compose crash:

```kotlin
ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false
```

This should be revisited later as Compose versions update.

## Important Log Tags

Useful Logcat tags during testing:

```text
EFSchedulerRepo
EFSchedulerBoot
EFSchedulerNotif
MainActivity
```

These help confirm:

- Tasks are saved to Room
- Tasks are deleted from Room
- Notifications are scheduled
- Notifications are cancelled
- BootReceiver runs after reboot
- NotificationReceiver fires at the correct time

## Recommended Test Checklist

Before sharing a build, run this checklist:

### Basic task flow

- Create a task 10 minutes in the future
- Choose 5 minutes before
- Save task
- Confirm before-reminder fires
- Confirm final notification fires
- Confirm task disappears after final notification

### Edit flow

- Create a task
- Edit task name
- Edit task time
- Edit reminder
- Confirm only the edited notification fires

### Delete flow

- Create a task
- Delete it before the reminder fires
- Confirm no reminder or final notification appears

### Reboot flow

- Create a future task
- Reboot the device with `adb reboot`
- Confirm `BootReceiver` logs appear
- Confirm the notification still fires

### Import/export flow

- Create a task
- Export schedule
- Delete task
- Import schedule
- Confirm task appears again
- Confirm notification still fires

### Permission flow

- Disable notification permission
- Confirm About / Permissions shows Disabled
- Disable alarm permission
- Confirm app shows the alarm permission dialog on launch
- Confirm review screen warns that reminders may not work
- Re-enable permissions
- Confirm status returns to Enabled

## Reboot Testing

To reboot an emulator or connected Android device from Android Studio terminal:

```bash
adb reboot
```

If multiple devices are connected:

```bash
adb devices
adb -s <device_id> reboot
```

Example:

```bash
adb -s emulator-5554 reboot
```

After reboot, search Logcat for:

```text
EFSchedulerBoot
```

## Project Direction

EFScheduler is currently Android-first.

The Android version can remain fully local because Android supports local storage, exact alarms, local notification receivers, and reboot receivers.

An iPhone PWA would not be equivalent because reliable web push notifications usually require a network/server-based push flow. A future iPhone version should ideally be a native iOS app so it can preserve the same local-first design.

## Future Improvements

Potential next improvements:

- Debug-only logging
- Better release logging cleanup
- Real-device battery saver testing
- More polished empty state
- Optional completed task history
- Optional JSON import conflict handling
- Native iOS version later
- Accessibility review for font size and contrast
- Small beta test with 1 to 2 Android users

## Build Notes

This project is built with:

- Kotlin
- Jetpack Compose
- Room
- AlarmManager
- BroadcastReceiver
- Material 3
- Local JSON import/export

## Privacy

EFScheduler is designed to keep schedule data on the user's device.

Schedule data is not cloud synced. The only time data leaves the app is when the user manually exports a JSON backup file.
