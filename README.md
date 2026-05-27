# EFScheduler

EFScheduler is a local-first Android task and transition reminder app built with Kotlin and Jetpack Compose.

It helps users create tasks, schedule transition reminders, and receive local notifications without relying on cloud storage, accounts, or a remote server.

## Overview

EFScheduler is designed around a simple idea: reminders should work locally and privately.

The app stores schedule data on the device and uses Android's local notification and alarm systems to remind users when a task is starting. It is intended for task transitions, routines, and time-based support where reliable local reminders matter.

## Features

- Create scheduled tasks
- Choose task date and time
- Add an optional early reminder
- Edit task name, time, and reminder
- Delete tasks and cancel pending notifications
- Receive local Android notifications
- Automatically remove tasks after the final notification fires
- Reschedule future reminders after device reboot
- Export schedules to a local JSON backup
- Import schedules from a local JSON backup
- View notification and alarm permission status
- Open required Android permission settings from the app

## Local-First Design

EFScheduler does not use cloud storage.

Task data is stored locally on the device using Room. Import and export are available for manual backup and restore.

This means:

- No cloud database
- No account required
- No server-side scheduling
- No internet connection required for saved Android reminders to fire

## Permissions

EFScheduler uses two Android permissions for reliable reminders.

### Notifications

Required so the app can display reminders.

### Alarms & reminders

Required so reminders can fire on time, even when the app is closed.

If alarm permission is disabled, EFScheduler shows an in-app explanation before sending the user to Android settings. The About / Permissions screen also shows whether each permission is enabled or disabled.

## How Scheduling Works

EFScheduler uses Android local scheduling.

Core pieces:

- `Room` stores task data locally.
- `AlarmManager` schedules reminders.
- `NotificationReceiver` posts notifications.
- `BootReceiver` restores future reminders after reboot.

When a task is saved, the app schedules a final notification at the task start time. If the user selected an early reminder and that reminder time is still valid, the app schedules that too.

When the final notification fires, the task is removed from the active task list.

## Backup and Restore

EFScheduler supports local JSON import and export.

Export creates a backup file such as:

```text
EFScheduler-backup.json
```

Backups include task names, timestamps, reminder settings, and schema metadata.

Import restores tasks into the local Room database and reschedules future notifications.

## Current Status

The current MVP has been tested for:

- Task creation
- Task editing
- Task deletion
- Early reminders
- Final task notifications
- Automatic task removal after final notification
- Notification cancellation after delete
- Notification updates after edit
- Reboot recovery through `BootReceiver`
- Import and export
- Permission status indicators
- Alarm permission prompt flow

## Development Notes

Useful Logcat tags during testing:

```text
EFSchedulerRepo
EFSchedulerBoot
EFSchedulerNotif
MainActivity
```

The Android emulator may show skipped frame warnings, autofill messages, or graphics-related logs during development. These are usually emulator noise unless paired with a crash or exception.

The app currently includes a temporary Compose workaround for a LazyColumn prefetch crash:

```kotlin
ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false
```

This should be revisited as Compose updates.

## Build and Run

Open the project in Android Studio and run it on an Android emulator or physical Android device.

For reboot testing with ADB:

```bash
adb reboot
```

If multiple devices are attached:

```bash
adb devices
adb -s <device_id> reboot
```

## Recommended Manual Test Pass

Before sharing a build, test:

1. Create a task with an early reminder.
2. Confirm the early reminder fires.
3. Confirm the final notification fires.
4. Confirm the task disappears after the final notification.
5. Edit a task and confirm only the edited notification fires.
6. Delete a task and confirm no notification fires.
7. Reboot with a future task scheduled and confirm it still fires.
8. Export, delete, import, and confirm the restored task works.
9. Disable permissions and confirm the app warns the user clearly.

## Project Direction

EFScheduler is Android-first.

The Android version can remain fully local because Android supports local storage, exact alarms, local notification receivers, and reboot receivers.

A future iPhone version should ideally be native iOS rather than a cloud-based workaround, so the app can preserve the same local-first privacy model.

## Privacy

EFScheduler keeps schedule data on the user's device.

Data is not cloud synced. The only time schedule data leaves the app is when the user manually exports a backup file.
