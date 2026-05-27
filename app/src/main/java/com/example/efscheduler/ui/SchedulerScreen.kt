package com.example.efscheduler.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.commandiron.wheel_picker_compose.WheelDateTimePicker
import com.commandiron.wheel_picker_compose.core.TimeFormat
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import com.example.efscheduler.R
import com.example.efscheduler.models.SchedulerViewModel
import com.example.efscheduler.models.ViewModelFactory
import com.example.efscheduler.ui.theme.Black
import com.example.efscheduler.ui.theme.DisabledGrey
import com.example.efscheduler.ui.theme.DisabledText
import com.example.efscheduler.ui.theme.InputBorder
import com.example.efscheduler.ui.theme.Lavender
import com.example.efscheduler.ui.theme.Pink
import com.example.efscheduler.ui.theme.TextLight
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import androidx.compose.runtime.DisposableEffect
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ==================== Navigation Routes ====================
sealed class Screen(val route: String) {
    object TaskList : Screen("taskList")
    object EnterTask : Screen("enterTask?taskName={taskName}") {
        fun passTaskName(taskName: String = "") = "enterTask?taskName=$taskName"
    }
    object PickTime : Screen("pickTime?taskName={taskName}") {
        fun passTaskName(taskName: String) = "pickTime?taskName=$taskName"
    }
    object Reminder : Screen("reminder")
    object Confirm : Screen("confirm")

    object AboutPermissions : Screen("aboutPermissions")
}

// ==================== Common Header ====================
@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(contentColor = Lavender)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_arrow),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back", style = MaterialTheme.typography.headlineSmall)
        }

        if (onCancel != null) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pink,
                    contentColor = Black
                )
            ) {
                Text("Cancel", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

// ==================== Reusable Progress Indicator ====================
@Composable
fun StepProgressIndicator(
    modifier: Modifier = Modifier,
    currentStep: Int,
    totalSteps: Int = 4, // Updated to 4 steps now (Task name, Time, Reminder, Confirm)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { currentStep / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp),
            color = Lavender,
            trackColor = Black,
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step $currentStep/$totalSteps",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ==================== Main App Entry ====================
@Composable
fun SchedulerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: SchedulerViewModel = viewModel(
        factory = ViewModelFactory(application)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.TaskList.route,
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {
        composable(Screen.TaskList.route) {
            TaskListScreen(navController, viewModel)
        }
        composable(
            route = Screen.EnterTask.route,
            arguments = listOf(navArgument("taskName") { defaultValue = "" })
        ) { backStackEntry ->
            val taskNameArg = backStackEntry.arguments?.getString("taskName") ?: ""
            LaunchedEffect(taskNameArg) {
                if (taskNameArg.isNotEmpty()) {
                    viewModel.startEditing(taskNameArg)
                } else {
                    viewModel.startNewTask()
                }
            }
            EnterTaskScreen(navController, viewModel)
        }
        composable(
            route = Screen.PickTime.route,
            arguments = listOf(navArgument("taskName") { defaultValue = "" })
        ) {
            PickTimeScreen(navController, viewModel)
        }
        composable(Screen.Reminder.route) {
            ReminderScreen(navController, viewModel)
        }
        composable(Screen.Confirm.route) {
            ConfirmationScreen(navController, viewModel)
        }
        composable(Screen.AboutPermissions.route) {
            AboutPermissionsScreen(navController, viewModel)
        }
    }
}

// ==================== Task List Screen ====================
// (unchanged – same as before)
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current
    val isListEditMode by viewModel::isListEditMode
    val taskToDelete by viewModel::taskToDelete

    var menuExpanded by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportTasksToUri(context, uri) { success, errorMessage ->
                Toast.makeText(
                    context,
                    if (success) "Schedule exported" else errorMessage ?: "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importTasksFromUri(
                context = context,
                uri = uri,
                replaceExisting = false
            ) { success, errorMessage ->
                Toast.makeText(
                    context,
                    if (success) "Schedule imported" else errorMessage ?: "Import failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Lavender
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Black
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Export schedule",
                                color = TextLight
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            exportLauncher.launch("EFScheduler-backup.json")
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Import schedule",
                                color = TextLight
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "About / Permissions",
                                color = TextLight
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Lavender
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            navController.navigate(Screen.AboutPermissions.route)
                        }
                    )
                }
            }

            if (tasks != null && tasks!!.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.toggleListEditMode() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Lavender
                    ),
                    border = BorderStroke(1.dp, Lavender)
                ) {
                    Text(
                        text = if (isListEditMode) "Done" else "Edit",
                        color = Lavender
                    )
                }
            }
        }

        if (tasks == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Lavender)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Your Tasks",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Tap the button below to add a new task.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (tasks!!.isEmpty()) {
                    item {
                        Text(
                            text = "No tasks yet",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DisabledText),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = tasks!!,
                        key = { task -> task.id }
                    ) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = TextLight.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .defaultMinSize(minHeight = 48.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isListEditMode) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = viewModel.formatDateTime(context, task.timestamp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.startEditingTask(task)
                                                navController.navigate(Screen.EnterTask.passTaskName(task.name))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Lavender
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.confirmDeleteTask(task) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Pink
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = viewModel.formatDateTime(context, task.timestamp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = {
                    Text(
                        text = "Delete Task",
                        color = Lavender
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete \"${taskToDelete?.name}\"?",
                        color = TextLight
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteConfirmed() }
                    ) {
                        Text(
                            text = "Delete",
                            color = Pink
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.cancelDelete() }
                    ) {
                        Text(
                            text = "Cancel",
                            color = Lavender
                        )
                    }
                },
                containerColor = Black,
                tonalElevation = 0.dp
            )
        }

        Button(
            onClick = {
                viewModel.startNewTask()
                navController.navigate(Screen.EnterTask.passTaskName())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Pink)
        ) {
            Text(
                text = "+ Add New Task",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Black,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// ==================== Enter Task Screen ====================
@Composable
fun EnterTaskScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val taskNameInput by viewModel::taskNameInput
    val isEditing by viewModel::isEditing
    val isNextEnabled by viewModel::isNextEnabled

    val title = if (isEditing) "Edit task name" else "What are you working on?"
    val helpText = if (isEditing) "Edit the task name below." else "Don't worry, you can change this later."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                onBack = {
                    if (isEditing) {
                        viewModel.finishRun()
                    }
                    navController.navigateUp()
                },
                onCancel = {
                    viewModel.cancelEditing()
                    navController.navigate(Screen.TaskList.route)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = helpText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(40.dp))

                BasicTextField(
                    value = taskNameInput,
                    onValueChange = { viewModel.updateTaskNameInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center
                    ).merge(MaterialTheme.typography.bodyMedium),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .background(Black, shape = MaterialTheme.shapes.small)
                                .border(1.dp, InputBorder, MaterialTheme.shapes.small)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (taskNameInput.isEmpty()) {
                                Text(
                                    text = "e.g., Finish biology worksheet",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = DisabledText)
                                )
                            }
                            innerTextField()
                        }
                    },
                    cursorBrush = SolidColor(Lavender)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.confirmTaskName()
                    navController.navigate(Screen.PickTime.passTaskName(taskNameInput.trim()))
                },
                enabled = isNextEnabled,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNextEnabled) Pink else DisabledGrey,
                    contentColor = if (isNextEnabled) Black else DisabledText
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Next",
                    style = if (isNextEnabled)
                        MaterialTheme.typography.bodyLarge
                    else
                        MaterialTheme.typography.bodyLarge.copy(color = DisabledText)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StepProgressIndicator(
                currentStep = 1,
                totalSteps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

// ==================== Pick Time Screen ====================
@Composable
fun PickTimeScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val context = LocalContext.current
    val taskName = viewModel.currentRun.taskName

    // Disallow any date‑time before the current moment
    val minDateTime = LocalDateTime.now()

    // Determine initial DateTime, ensuring it's not before minDateTime
    val initialDateTime = viewModel.selectedTimestamp?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }?.let {
        if (it.isBefore(minDateTime)) minDateTime else it
    } ?: minDateTime

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                onBack = { navController.navigateUp() },
                onCancel = {
                    viewModel.finishRun()
                    navController.navigate(Screen.TaskList.route)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "When is your task?",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Select date and time for: $taskName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(40.dp))

                WheelDateTimePicker(
                    startDateTime = initialDateTime,
                    minDateTime = minDateTime,
                    timeFormat = TimeFormat.AM_PM,
                    size = DpSize(320.dp, 180.dp),
                    rowCount = 5,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    textColor = Lavender,
                    selectorProperties = WheelPickerDefaults.selectorProperties(
                        enabled = true,
                        shape = RoundedCornerShape(8.dp),
                        color = Pink.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Pink)
                    )
                ) { snappedDateTime ->
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.YEAR, snappedDateTime.year)
                        set(Calendar.MONTH, snappedDateTime.monthValue - 1)
                        set(Calendar.DAY_OF_MONTH, snappedDateTime.dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, snappedDateTime.hour)
                        set(Calendar.MINUTE, snappedDateTime.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.updateSelectedTimestamp(calendar.timeInMillis)
                }

                Spacer(modifier = Modifier.height(30.dp))

                if (viewModel.selectedTimestamp != null) {
                    val formatted = viewModel.formatDateTime(context, viewModel.selectedTimestamp!!)

                    Text(
                        text = "Selected: $formatted",
                        style = MaterialTheme.typography.titleLarge
                    )

                    viewModel.selectedTimestampWarning?.let { warning ->
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Pink,
                                fontWeight = FontWeight.SemiBold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "No date selected",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DisabledText)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val isNextEnabled = viewModel.isSelectedTimestampValid
            Button(
                onClick = {
                    if (viewModel.confirmDateTime()) {
                        navController.navigate(Screen.Reminder.route)
                    }
                },
                enabled = isNextEnabled,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNextEnabled) Pink else DisabledGrey,
                    contentColor = if (isNextEnabled) Black else DisabledText
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Next",
                    style = if (isNextEnabled)
                        MaterialTheme.typography.bodyLarge
                    else
                        MaterialTheme.typography.bodyLarge.copy(color = DisabledText)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StepProgressIndicator(
                currentStep = 2,
                totalSteps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

// ==================== Reminder Screen ====================
@Composable
fun ReminderScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val selectedReminder by viewModel::selectedReminder

    // Reminder options with display text
    val reminderOptions = viewModel.availableReminderOptions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                onBack = { navController.navigateUp() },
                onCancel = {
                    viewModel.finishRun()
                    navController.navigate(Screen.TaskList.route)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val beforeReminderOptions = reminderOptions.filter { it.first > 0 }
                val hasBeforeReminderOptions = beforeReminderOptions.isNotEmpty()

                Text(
                    text = if (hasBeforeReminderOptions) {
                        "When would you like your reminder?"
                    } else {
                        "No early reminder available"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (hasBeforeReminderOptions) {
                        "Choose how early to be notified."
                    } else {
                        "This task is too soon for an early reminder."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))

                // Vertical list of options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (beforeReminderOptions.isEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                    } else {
                        reminderOptions.forEach { (minutes, label) ->
                            val isSelected = selectedReminder == minutes

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        TextLight.copy(alpha = 0.12f)
                                    } else {
                                        Black
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Pink else Lavender
                                )
                            ) {
                                TextButton(
                                    onClick = { viewModel.updateSelectedReminder(minutes) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Pink else Lavender,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val isNextEnabled = viewModel.isSelectedReminderValid
            Button(
                onClick = {
                    if (viewModel.confirmReminder()) {
                        navController.navigate(Screen.Confirm.route)
                    }
                },
                enabled = isNextEnabled,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNextEnabled) Pink else DisabledGrey,
                    contentColor = if (isNextEnabled) Black else DisabledText
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Next",
                    style = if (isNextEnabled)
                        MaterialTheme.typography.bodyLarge
                    else
                        MaterialTheme.typography.bodyLarge.copy(color = DisabledText)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StepProgressIndicator(
                currentStep = 3,
                totalSteps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

// ==================== Confirmation Screen ====================
@Composable
fun ConfirmationScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val currentRun = viewModel.currentRun
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus(context)
    }

    val reminderMessage = if (currentRun.reminderMinutes > 0) {
        val reminderText = when (currentRun.reminderMinutes) {
            5 -> "5 minutes"
            10 -> "10 minutes"
            15 -> "15 minutes"
            30 -> "30 minutes"
            60 -> "1 hour"
            120 -> "2 hours"
            else -> "${currentRun.reminderMinutes} minutes"
        }
        "Notifications: $reminderText before and when the task starts"
    } else {
        "Notification: when the task starts"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                onBack = { navController.navigateUp() },
                onCancel = {
                    viewModel.finishRun()
                    navController.navigate(Screen.TaskList.route)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (viewModel.isEditing) "Review changes" else "Review task",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminderMessage,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "\"${currentRun.taskName}\"",
                    style = MaterialTheme.typography.titleLarge
                )
                currentRun.startTimestamp?.let { timestamp ->
                    Text(
                        text = viewModel.formatDateTime(context, timestamp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (currentRun.reminderMinutes > 0) {
                        val reminderText = when (currentRun.reminderMinutes) {
                            5 -> "5 mins before"
                            10 -> "10 mins before"
                            15 -> "15 mins before"
                            30 -> "30 mins before"
                            60 -> "1 hr before"
                            120 -> "2 hrs before"
                            else -> "${currentRun.reminderMinutes} mins before"
                        }
                        Text(
                            text = "Reminder $reminderText",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Lavender),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val permissionWarning = viewModel.reminderPermissionWarning

            if (permissionWarning != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Pink.copy(alpha = 0.18f)
                    ),
                    border = BorderStroke(1.dp, Pink)
                ) {
                    Text(
                        text = permissionWarning,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextLight,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.addCurrentRunToTasks()
                    viewModel.finishRun()
                    navController.navigate(Screen.TaskList.route) {
                        popUpTo(Screen.TaskList.route) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Black)
            ) {
                Text(
                    text = if (viewModel.isEditing) "Save changes" else "Save task",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StepProgressIndicator(
                currentStep = 4,
                totalSteps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    statusText: String,
    statusGood: Boolean,
    buttonText: String,
    showButton: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TextLight.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                PermissionStatusBadge(
                    text = statusText,
                    isGood = statusGood
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )

            if (showButton) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Pink,
                        contentColor = Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionStatusBadge(
    text: String,
    isGood: Boolean
) {
    val green = androidx.compose.ui.graphics.Color(0xFF4CAF50)
    val red = androidx.compose.ui.graphics.Color(0xFFE57373)

    val badgeColor = if (isGood) green else red

    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = badgeColor,
                shape = RoundedCornerShape(50)
            )
            .background(
                color = badgeColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = badgeColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AboutPermissionsScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationsEnabled by viewModel::notificationsEnabled
    val exactAlarmsEnabled by viewModel::exactAlarmsEnabled

    DisposableEffect(lifecycleOwner) {
        viewModel.refreshPermissionStatus(context)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ScreenHeader(
                onBack = { navController.navigateUp() },
                onCancel = null
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "About EFScheduler",
                style = MaterialTheme.typography.headlineLarge,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "EFScheduler helps you schedule tasks and transition reminders. Your schedule is stored locally on this device. Import and export are used for manual backup and restore.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissions",
                style = MaterialTheme.typography.headlineMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionCard(
                title = "Notifications",
                description = "Allows EFScheduler to show reminders when it is time to transition.",
                statusText = if (notificationsEnabled) "Enabled" else "Disabled",
                statusGood = notificationsEnabled,
                buttonText = "Open notification settings",
                showButton = !notificationsEnabled,
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                title = "Alarms & reminders",
                description = "Allows EFScheduler to schedule reminders reliably, even when the app is closed.",
                statusText = if (exactAlarmsEnabled) "Enabled" else "Disabled",
                statusGood = exactAlarmsEnabled,
                buttonText = "Open alarm settings",
                showButton = !exactAlarmsEnabled,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = "package:$context.packageName".toUri()
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Data storage",
                style = MaterialTheme.typography.headlineMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your schedule is not cloud synced. It stays on your device unless you choose to export it.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )
        }
    }
}

@Composable
fun ExactAlarmPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enable alarms & reminders",
                color = Lavender
            )
        },
        text = {
            Text(
                text = "EFScheduler needs this permission so you'll get your reminders on time, even when the app is closed.",
                color = TextLight
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = "Open settings",
                    color = Pink
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Not now",
                    color = Lavender
                )
            }
        },
        containerColor = Black,
        tonalElevation = 0.dp
    )
}