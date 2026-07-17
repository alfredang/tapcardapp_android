package com.tertiaryinfotech.tapcard.ui

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tertiaryinfotech.tapcard.model.Appointment
import com.tertiaryinfotech.tapcard.model.Task
import com.tertiaryinfotech.tapcard.model.TaskTypes
import com.tertiaryinfotech.tapcard.model.taskTypeLabel
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlue
import com.tertiaryinfotech.tapcard.ui.theme.BrandBlueDeep
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DoneGreen = Color(0xFF059669)
private val OverdueRed = Color(0xFFDC2626)
private val TodayAccent = Color(0xFF2563EB)

// Each appointment gets a stable colour from its name, so the list reads with
// variety instead of a wall of identical purple blocks.
private val ApptPalette = listOf(
    Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFEA580C),
    Color(0xFF059669), Color(0xFF0891B2), Color(0xFFD97706), Color(0xFF4F46E5),
)

private fun apptColor(seed: String): Color =
    ApptPalette[kotlin.math.abs(seed.hashCode()) % ApptPalette.size]

/** Per-type accent colour for task chips + checkboxes. */
private fun typeColor(type: String): Color = when (type) {
    "REMINDER" -> Color(0xFFF59E0B) // amber
    "MEETING" -> Color(0xFF7C3AED)  // violet
    else -> Color(0xFF2563EB)       // FOLLOW_UP → blue
}

@Composable
fun PlannerScreen(vm: CardViewModel) {
    if (!vm.isAuthenticated) {
        Box(Modifier.fillMaxSize()) {
            SignedOutState(
                icon = Icons.Filled.EventNote,
                title = "Plan your follow-ups",
                message = "Sign in to keep tasks and appointments synced across your devices.",
                onSignIn = vm::openAuth,
            )
        }
        return
    }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var showAddTask by remember { mutableStateOf(false) }
    var showAddAppt by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editingAppt by remember { mutableStateOf<Appointment?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var apptToDelete by remember { mutableStateOf<Appointment?>(null) }

    val appts = vm.appointments
    val tasks = vm.tasks
    val apptsByDate = remember(appts) { appts.groupBy { localDateOf(it.startAt) } }
    val datedTasksByDate = remember(tasks) {
        tasks.filter { !it.dueAt.isNullOrBlank() }.groupBy { localDateOf(it.dueAt!!) }
    }
    val markedDates = remember(apptsByDate, datedTasksByDate) { apptsByDate.keys + datedTasksByDate.keys }

    val dayAppts = apptsByDate[selected].orEmpty().sortedBy { it.startAt }
    val dayTasks = datedTasksByDate[selected].orEmpty().sortedWith(compareBy({ it.isDone }, { it.dueAt }))

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            // Pinned header: title + month calendar stay put while the day's list scrolls.
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                ScreenTitle("Planner", "Your day at a glance — synced everywhere")
                Spacer(Modifier.height(16.dp))
                MonthCalendar(
                    month = currentMonth,
                    selected = selected,
                    datesWithAppts = markedDates,
                    onPrev = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) },
                    onSelect = { selected = it },
                )
                Spacer(Modifier.height(14.dp))
                DayHeader(selected)
                Spacer(Modifier.height(10.dp))
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (dayAppts.isEmpty() && dayTasks.isEmpty()) {
                    item {
                        Text(
                            "Nothing scheduled for this day.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
                        )
                    }
                }
                if (dayAppts.isNotEmpty()) {
                    item { GroupHeader("Appointments", dayAppts.size) }
                    items(dayAppts, key = { "a-${it.id}" }) { AgendaAppointment(it, onEdit = { editingAppt = it }, onDelete = { apptToDelete = it }) }
                }
                if (dayTasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(2.dp)); GroupHeader("Tasks", dayTasks.size) }
                    items(dayTasks, key = { "t-${it.id}" }) { TaskCard(it, onToggle = { vm.toggleTask(it) }, onEdit = { editingTask = it }, onDelete = { taskToDelete = it }) }
                }
            }
        }

        // Add button with an appointment / task chooser.
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Row(
                Modifier
                    .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = BrandBlue, ambientColor = BrandBlue)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(BrandBlue, BrandBlueDeep)))
                    .clickable { showAddMenu = true }
                    .padding(horizontal = 22.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                DropdownMenuItem(
                    text = { Text("New appointment") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    onClick = { showAddMenu = false; showAddAppt = true },
                )
                DropdownMenuItem(
                    text = { Text("New task") },
                    leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    onClick = { showAddMenu = false; showAddTask = true },
                )
            }
        }
    }

    if (showAddTask) {
        TaskDialog(initial = null, onDismiss = { showAddTask = false }) { title, type, dueIso ->
            showAddTask = false
            vm.addTask(title, type, dueIso)
        }
    }
    editingTask?.let { task ->
        TaskDialog(initial = task, onDismiss = { editingTask = null }) { title, type, dueIso ->
            editingTask = null
            vm.editTask(task, title, type, dueIso)
        }
    }
    if (showAddAppt) {
        AppointmentDialog(initial = null, onDismiss = { showAddAppt = false }) { name, email, startIso, endIso, notes ->
            showAddAppt = false
            vm.addAppointment(name, email, startIso, endIso, notes)
        }
    }
    editingAppt?.let { appt ->
        AppointmentDialog(initial = appt, onDismiss = { editingAppt = null }) { name, email, startIso, endIso, notes ->
            editingAppt = null
            vm.editAppointment(appt, name, email, startIso, endIso, notes)
        }
    }
    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete task?",
            message = "“${task.title}” will be permanently removed.",
            onDismiss = { taskToDelete = null },
            onConfirm = { taskToDelete = null; vm.removeTask(task) },
        )
    }
    apptToDelete?.let { appt ->
        ConfirmDeleteDialog(
            title = "Delete appointment?",
            message = "“${appt.name}” will be permanently removed.",
            onDismiss = { apptToDelete = null },
            onConfirm = { apptToDelete = null; vm.removeAppointment(appt) },
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(62.dp).clip(CircleShape).background(OverdueRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = OverdueRed, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                title,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = OverdueRed, ambientColor = OverdueRed)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OverdueRed)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)) {
        Text(
            text.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Tasks ───────────────────────────────────────────────────────────────────────
@Composable
private fun TaskCard(task: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accent = typeColor(task.type)
    val checkScale by animateFloatAsState(if (task.isDone) 1f else 0f, label = "check")
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f), ambientColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (task.isDone) DoneGreen else Color.Transparent)
                .border(2.dp, if (task.isDone) DoneGreen else MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Toggle done", tint = Color.White, modifier = Modifier.size((15 * checkScale).dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEdit)
                .padding(vertical = 2.dp),
        ) {
            Text(
                task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(taskTypeLabel(task.type), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
                dueLabel(task.dueAt, task.isDone)?.let { (label, overdue) ->
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (overdue) "$label · overdue" else label,
                        fontSize = 12.sp,
                        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (overdue) OverdueRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        DeleteButton(onDelete)
    }
}

// ─── Appointments ────────────────────────────────────────────────────────────────
@Composable
private fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    datesWithAppts: Set<LocalDate>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            NavArrow(Icons.Filled.ChevronLeft, "Previous month", onPrev)
            Text(month.format(monthYearFmt), fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            NavArrow(Icons.Filled.ChevronRight, "Next month", onNext)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))
        val leading = month.atDay(1).dayOfWeek.value - 1 // Monday = 0
        val daysInMonth = month.lengthOfMonth()
        val rows = (leading + daysInMonth + 6) / 7
        Column {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val dayNum = row * 7 + col - leading + 1
                        Box(Modifier.weight(1f).aspectRatio(1.35f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..daysInMonth) {
                                val date = month.atDay(dayNum)
                                DayCell(
                                    day = dayNum,
                                    isToday = date == today,
                                    isSelected = date == selected,
                                    hasAppt = date in datesWithAppts,
                                    onClick = { onSelect(date) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, isSelected: Boolean, hasAppt: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) BrandBlue else Color.Transparent)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, BrandBlue, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> BrandBlue
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        // Small dot marks days that have appointments or tasks — overlaid so it doesn't
        // grow the cell height. Hidden on the selected day (its fill already stands out,
        // and the dot underneath would read as a map pin).
        if (hasAppt && !isSelected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.6f)),
            )
        }
    }
}

/** Opens the device calendar (Google/Samsung/etc.) pre-filled with this appointment. */
private fun addAppointmentToCalendar(context: Context, appt: Appointment) {
    val start = runCatching { Instant.parse(appt.startAt).toEpochMilli() }.getOrNull()
    val end = runCatching { Instant.parse(appt.endAt).toEpochMilli() }.getOrNull()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, appt.name)
        if (start != null) putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        if (end != null) putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        if (!appt.notes.isNullOrBlank()) putExtra(CalendarContract.Events.DESCRIPTION, appt.notes)
        if (!appt.email.isNullOrBlank()) putExtra(Intent.EXTRA_EMAIL, appt.email)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { toast(context, "No calendar app found") }
}

@Composable
private fun AgendaAppointment(appt: Appointment, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val color = apptColor(appt.name.ifBlank { appt.id })
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f), ambientColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading time tile (like a calendar event chip).
        Column(
            Modifier
                .width(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatHm(appt.startAt), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(formatAmPm(appt.startAt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                appt.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${formatTime(appt.startAt)} – ${formatTime(appt.endAt)}",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!appt.email.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(appt.email!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!appt.notes.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(appt.notes!!, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(30.dp).clip(CircleShape).clickable { addAppointmentToCalendar(context, appt) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.EventAvailable, contentDescription = "Add to calendar", tint = BrandBlue.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        }
        DeleteButton(onDelete)
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    val isToday = date == LocalDate.now()
    Text(
        dayHeaderLabel(date).uppercase(),
        color = if (isToday) TodayAccent else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    Box(Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
    }
}

// ─── Add task dialog ─────────────────────────────────────────────────────────────
@Composable
private fun TaskDialog(initial: Task?, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    val context = LocalContext.current
    val initialDt = initial?.dueAt?.let { runCatching { zoned(it) }.getOrNull() }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "FOLLOW_UP") }
    // A due date is required, so new tasks pre-fill to today at the next half hour.
    var dueDate by remember { mutableStateOf<LocalDate?>(initialDt?.toLocalDate() ?: LocalDate.now()) }
    var dueTime by remember { mutableStateOf<LocalTime?>(initialDt?.toLocalTime() ?: nextHalfHour()) }
    val editing = initial != null
    var showDueDate by remember { mutableStateOf(false) }
    var showDueTime by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            DialogHeader(Icons.Filled.Check, if (editing) "Edit task" else "New task")
            Spacer(Modifier.height(16.dp))
            PlannerField("What needs doing?", title, KeyboardType.Text) { title = it }
            Spacer(Modifier.height(14.dp))

            Text("Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskTypes.forEach { t ->
                    val active = t == type
                    val c = typeColor(t)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) c.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, if (active) c else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { type = t }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(taskTypeLabel(t), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (active) c else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            DateTimePickerRow(
                label = "Due date",
                date = dueDate,
                time = dueTime,
                onPickDate = { showDueDate = true },
                onPickTime = { showDueTime = true },
                optional = false,
                onClear = {},
            )

            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (editing) "Save changes" else "Add task") {
                val d = dueDate ?: LocalDate.now()
                val t = dueTime ?: (if (d == LocalDate.now()) nextHalfHour() else LocalTime.of(9, 0))
                onSave(title, type, toIso(d, t))
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    if (showDueDate) {
        DatePickerModal(initial = dueDate ?: LocalDate.now(), onDismiss = { showDueDate = false }) { picked -> dueDate = picked }
    }
    if (showDueTime) {
        TimePickerModal("DUE TIME", dueTime ?: nextHalfHour(), onDismiss = { showDueTime = false }) { picked ->
            if (!editing && isPast(dueDate ?: LocalDate.now(), picked)) toast(context, "Due time can't be in the past")
            else dueTime = picked
        }
    }
}

// ─── Appointment dialog (add + edit) ─────────────────────────────────────────────
@Composable
private fun AppointmentDialog(initial: Appointment?, onDismiss: () -> Unit, onSave: (String, String?, String, String, String?) -> Unit) {
    val context = LocalContext.current
    val startDt = initial?.startAt?.let { runCatching { zoned(it) }.getOrNull() }
    val endDt = initial?.endAt?.let { runCatching { zoned(it) }.getOrNull() }
    val defaultStart = remember { nextHalfHour() }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var date by remember { mutableStateOf(startDt?.toLocalDate() ?: LocalDate.now()) }
    var start by remember { mutableStateOf(startDt?.toLocalTime() ?: defaultStart) }
    var end by remember { mutableStateOf(endDt?.toLocalTime() ?: defaultStart.plusMinutes(30)) }
    val editing = initial != null
    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            DialogHeader(Icons.Filled.CalendarMonth, if (editing) "Edit appointment" else "New appointment")
            Spacer(Modifier.height(16.dp))
            PlannerField("Who with?", name, KeyboardType.Text) { name = it }
            Spacer(Modifier.height(10.dp))
            PlannerField("Email (optional)", email, KeyboardType.Email) { email = it }

            Spacer(Modifier.height(14.dp))
            PickerButton("Date", formatLocalDate(date), Icons.Filled.CalendarMonth) { showDate = true }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    PickerButton("Start", formatLocalTime(start), Icons.Filled.Schedule) { showStart = true }
                }
                Box(Modifier.weight(1f)) {
                    PickerButton("End", formatLocalTime(end), Icons.Filled.Schedule) { showEnd = true }
                }
            }

            Spacer(Modifier.height(10.dp))
            PlannerField("Notes (optional)", notes, KeyboardType.Text, singleLine = false) { notes = it }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (editing) "Save changes" else "Add appointment") {
                if (end <= start) { toast(context, "End time must be after the start"); return@PrimaryButton }
                if (!editing && isPast(date, start)) { toast(context, "Pick a start time in the future"); return@PrimaryButton }
                onSave(name, email, toIso(date, start), toIso(date, end), notes)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    if (showDate) {
        DatePickerModal(initial = date, onDismiss = { showDate = false }) { picked ->
            date = picked
            // Switching to today can make the current time now be in the past — bump it forward.
            if (isPast(date, start)) { start = nextHalfHour(); end = start.plusMinutes(30) }
        }
    }
    if (showStart) {
        TimePickerModal("START TIME", start, onDismiss = { showStart = false }) { picked ->
            if (isPast(date, picked)) toast(context, "Start time can't be in the past")
            else { start = picked; if (end <= picked) end = picked.plusMinutes(30) }
        }
    }
    if (showEnd) {
        TimePickerModal("END TIME", end, onDismiss = { showEnd = false }) { picked ->
            if (picked <= start) toast(context, "End time must be after the start")
            else end = picked
        }
    }
}

@Composable
private fun DialogHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DateTimePickerRow(
    label: String,
    date: LocalDate?,
    time: LocalTime?,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
    optional: Boolean,
    onClear: () -> Unit,
) {
    Text(if (optional) "$label (optional)" else label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(7.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            PickerButton("Date", date?.let(::formatLocalDate) ?: "Pick", Icons.Filled.CalendarMonth, onClick = onPickDate)
        }
        Box(Modifier.weight(1f)) {
            PickerButton("Time", time?.let(::formatLocalTime) ?: "9:00 AM", Icons.Filled.Schedule, onClick = onPickTime)
        }
        if (date != null && optional) {
            Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClear), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PickerButton(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PlannerField(label: String, value: String, keyboardType: KeyboardType, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ─── Date helpers ────────────────────────────────────────────────────────────────
private val hmFmt = DateTimeFormatter.ofPattern("h:mm")
private val amPmFmt = DateTimeFormatter.ofPattern("a")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val localDateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val dayHeaderFmt = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val shortDateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val monthYearFmt = DateTimeFormatter.ofPattern("MMMM yyyy")

private fun zoned(iso: String) = Instant.parse(iso).atZone(ZoneId.systemDefault())
private fun localDateOf(iso: String): LocalDate = runCatching { zoned(iso).toLocalDate() }.getOrDefault(LocalDate.now())
private fun formatHm(iso: String): String = runCatching { zoned(iso).format(hmFmt) }.getOrDefault("")
private fun formatAmPm(iso: String): String = runCatching { zoned(iso).format(amPmFmt) }.getOrDefault("")
private fun formatTime(iso: String): String = runCatching { zoned(iso).format(timeFmt) }.getOrDefault("")
private fun formatLocalDate(d: LocalDate): String = d.format(localDateFmt)
private fun formatLocalTime(t: LocalTime): String = t.format(timeFmt)
private fun toIso(date: LocalDate, time: LocalTime): String =
    date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toString()

private fun dayHeaderLabel(date: LocalDate): String {
    val today = LocalDate.now()
    val datePart = date.format(shortDateFmt) // e.g. "Tue, 14 Jul"
    return when (date) {
        today -> "Today · $datePart"
        today.plusDays(1) -> "Tomorrow · $datePart"
        today.minusDays(1) -> "Yesterday · $datePart"
        else -> date.format(dayHeaderFmt)
    }
}

/** Returns (friendly label, isOverdue) for a task due date, or null when unset. */
private fun dueLabel(iso: String?, done: Boolean): Pair<String, Boolean>? {
    if (iso.isNullOrBlank()) return null
    val dt = runCatching { zoned(iso) }.getOrNull() ?: return null
    val date = dt.toLocalDate()
    val today = LocalDate.now()
    val time = dt.format(timeFmt)
    val overdue = !done && dt.toInstant().isBefore(Instant.now())
    val label = when {
        date == today -> "Today · $time"
        date == today.plusDays(1) -> "Tomorrow · $time"
        else -> dt.format(shortDateFmt)
    }
    return label to overdue
}

// ─── Themed Material 3 date + time pickers ───────────────────────────────────────
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.utcToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Material 3 calendar dialog. Days before [minDate] are greyed out and unselectable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initial: LocalDate,
    minDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val minMillis = minDate.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (if (initial.isBefore(minDate)) minDate else initial).toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            override fun isSelectableYear(year: Int) = year >= minDate.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(it.utcToLocalDate()) }
                onDismiss()
            }) { Text("Done", color = BrandBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    ) {
        DatePicker(
            state = state,
            title = null,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                selectedDayContainerColor = BrandBlue,
                todayDateBorderColor = BrandBlue,
                todayContentColor = BrandBlue,
            ),
        )
    }
}

/** Material 3 clock dialog, wrapped in a themed rounded surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        selectorColor = BrandBlue,
                        timeSelectorSelectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                        timeSelectorSelectedContentColor = BrandBlue,
                        periodSelectorSelectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                        periodSelectorSelectedContentColor = BrandBlue,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        onConfirm(LocalTime.of(state.hour, state.minute))
                        onDismiss()
                    }) { Text("Done", color = BrandBlue, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

/** True when the given day + time is earlier than right now (used to reject past selections). */
private fun isPast(date: LocalDate, time: LocalTime): Boolean =
    LocalDateTime.of(date, time).isBefore(LocalDateTime.now())

/** A sensible default start for a new appointment: now rounded up to the next half hour. */
private fun nextHalfHour(): LocalTime {
    val now = LocalTime.now().withSecond(0).withNano(0)
    val add = (30 - now.minute % 30) % 30
    return now.plusMinutes(if (add == 0) 30L else add.toLong())
}

private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
