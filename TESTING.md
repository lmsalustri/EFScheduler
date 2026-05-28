# EFScheduler Testing Checklist

## Core task flow
- Create a task 10 minutes in the future
- Select 5 minutes before
- Save task
- Confirm early reminder fires
- Confirm final notification fires
- Confirm task disappears

## Edit flow
- Create a task
- Edit name
- Edit time
- Edit reminder
- Confirm only edited notification fires

## Delete flow
- Create a task
- Delete before reminder/final notification
- Confirm no notification fires

## Reboot flow
- Create future task
- Run `adb reboot`
- Confirm `EFSchedulerBoot` logs appear
- Confirm notification fires

## Permission flow
- Disable alarms/reminders
- Confirm app shows explanation dialog
- Confirm About / Permissions shows Disabled
- Re-enable permission
- Confirm dialog no longer appears

## Import/export
- Export schedule
- Delete task
- Import backup
- Confirm restored task appears
- Confirm restored notification fires
