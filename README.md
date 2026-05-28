# EFScheduler

EFScheduler is a local-first Android app for task transitions and reminders, designed through a participatory process with neurodivergent users.

Unlike many productivity apps, EFScheduler is not built around accounts, calendars, cloud sync, or productivity tracking. It is built around transition support: helping users move from one task to another with clear timing, simple choices, and reliable local notifications.

## Design approach

Neurodivergent users were directly involved in shaping the app before and during development. The result is a transition-support tool built from their expressed needs and preferences, not a generic scheduling app with accessibility added later.

## Core features

EFScheduler supports creating, editing, deleting, importing, and exporting scheduled tasks. It sends local Android notifications for task start times and optional early reminders. Completed tasks are removed from the active task list after the final notification fires.

The app also restores future reminders after device reboot.

## Local-first model

EFScheduler stores tasks locally using Room and schedules reminders using Android’s local alarm and notification systems.

It does not use cloud storage, user accounts, server-side scheduling, Firebase, or network-based reminders. Manual JSON export and import are available for backup and restore.

## Permissions

EFScheduler uses notification permission so reminders can appear, and alarms/reminders permission so reminders can fire on time.

If alarm permission is disabled, the app explains why it is needed before sending the user to Android settings. The About / Permissions screen shows whether permissions are enabled or disabled.

## Technical notes

The Android app uses Kotlin, Jetpack Compose, Room, AlarmManager, BroadcastReceiver, and local JSON import/export.

Important reminder components include `NotificationReceiver` for posting reminders and `BootReceiver` for restoring future reminders after reboot.

Useful Logcat tags during testing:

```text
EFSchedulerRepo
EFSchedulerBoot
EFSchedulerNotif
MainActivity
```

The app currently includes a temporary Compose workaround for a LazyColumn prefetch crash:

```kotlin
ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = false
```

This should be revisited after future Compose updates.

## Running the app

Open the project in Android Studio and run it on an Android emulator or Android device.

For reboot testing:

```bash
adb reboot
```

If multiple devices are connected:

```bash
adb devices
adb -s <device_id> reboot
```

## Privacy

EFScheduler keeps schedule data on the device. Data only leaves the app when the user manually exports a backup file.
