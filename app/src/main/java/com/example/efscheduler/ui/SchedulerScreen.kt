package com.example.efscheduler.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

// ==================== Navigation Routes ====================
sealed class Screen(val route: String) {
    object TaskList : Screen("taskList")
    object EnterTask : Screen("enterTask?taskName={taskName}") {
        fun passTaskName(taskName: String = "") = "enterTask?taskName=$taskName"
    }
    object PickTime : Screen("pickTime?taskName={taskName}") {
        fun passTaskName(taskName: String) = "pickTime?taskName=$taskName"
    }
    object Confirm : Screen("confirm")
}

// ==================== Common Header ====================
@Composable
fun ScreenHeader(
    onBack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
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

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = Pink,
                contentColor = Black
            )
        ) {
            Text("Cancel", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ==================== Reusable Progress Indicator ====================
@Composable
fun StepProgressIndicator(
    modifier: Modifier = Modifier,
    currentStep: Int,
    totalSteps: Int = 3,
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
    val viewModel: SchedulerViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.TaskList.route,

        // remove the white crossfade that compose uses by default
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
        composable(Screen.Confirm.route) {
            ConfirmationScreen(navController, viewModel)
        }
    }
}

// ==================== Task List Screen ====================
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: SchedulerViewModel
) {
    val tasks = viewModel.tasks
    val context = LocalContext.current
    val isListEditMode by viewModel::isListEditMode
    val taskToDelete by viewModel::taskToDelete

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
            if (tasks.isNotEmpty()) {
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
            if (tasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DisabledText),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                items(tasks) { task ->
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

        // Delete confirmation dialog
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
                } else {
                    Text(
                        text = "No date selected",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DisabledText)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val isNextEnabled = viewModel.selectedTimestamp != null
            Button(
                onClick = {
                    viewModel.confirmDateTime()
                    navController.navigate(Screen.Confirm.route)
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
                    text = "Scheduled!",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You'll get a reminder when it's time.",
                    style = MaterialTheme.typography.titleMedium
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
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    text = "Back to task list",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StepProgressIndicator(
                currentStep = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}