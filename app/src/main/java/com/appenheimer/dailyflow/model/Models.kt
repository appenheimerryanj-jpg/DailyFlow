package com.appenheimer.dailyflow.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

const val PREMIUM_PRODUCT_ID = "dailyflow_premium_lifetime"
const val FREE_ACTIVE_TASK_LIMIT = 7
const val FREE_HABIT_LIMIT = 3
const val FREE_NOTE_LIMIT = 5
const val DAILY_MOMENTUM_GOAL = 60

enum class DelightType {
    TASK_COMPLETE,
    HABIT_COMPLETE,
    ALL_HABITS_DONE,
    NEW_BEST_STREAK,
    FIRST_TASK,
    FIRST_HABIT,
    FIRST_NOTE,
    CLEAR_COMPLETED,
    PREMIUM_UNLOCK,
    DAILY_GOAL_REACHED,
    FOCUS_START,
    LIMIT_REACHED
}

data class DelightEvent(
    val type: DelightType,
    val message: String,
    val createdAt: Long = nowMillis()
)

enum class TaskPriority(val label: String, val rank: Int) {
    LOW("Low", 2),
    MEDIUM("Medium", 1),
    HIGH("High", 0)
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

enum class FocusVibe(val label: String, val description: String) {
    DEEP_FOCUS("Deep Focus", "Steady and quiet for careful work."),
    MORNING_FLOW("Morning Flow", "Bright momentum for starting the day."),
    CHILL_RESET("Chill Reset", "Soft pacing for getting back on track."),
    ENERGY_BOOST("Energy Boost", "A little lift for low-energy moments."),
    NIGHT_PLANNING("Night Planning", "Calm reflection for closing the day.")
}

data class DailyTask(
    val id: String? = UUID.randomUUID().toString(),
    val text: String? = "",
    val done: Boolean = false,
    val priority: TaskPriority? = TaskPriority.MEDIUM,
    val dueDate: String? = "",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis()
)

data class Habit(
    val id: String? = UUID.randomUUID().toString(),
    val name: String? = "",
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastDone: String? = "",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis()
)

data class Note(
    val id: String? = UUID.randomUUID().toString(),
    val title: String? = "",
    val body: String? = "",
    val text: String? = "",
    val created: String? = "",
    val createdAt: Long? = System.currentTimeMillis(),
    val updatedAt: Long? = System.currentTimeMillis()
)

fun nowMillis(): Long = System.currentTimeMillis()

fun todayKey(): String = dayKey(0)

fun yesterdayKey(): String = dayKey(-1)

fun dayKey(offsetDays: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

fun formatTimestamp(value: Long?): String {
    val millis = value?.takeIf { it > 0L } ?: return "Recently"
    return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))
}

fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun DailyTask.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

fun DailyTask.safeText(): String = text?.trim().orEmpty()

fun DailyTask.safePriority(): TaskPriority = priority ?: TaskPriority.MEDIUM

fun DailyTask.sortedDueText(): String = dueDate?.trim().orEmpty()

fun Habit.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

fun Habit.safeName(): String = name?.trim().orEmpty()

fun Habit.doneToday(): Boolean = lastDone == todayKey()

fun Note.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

fun Note.noteTitle(): String {
    val explicitTitle = title?.trim().orEmpty()
    if (explicitTitle.isNotBlank()) return explicitTitle
    return noteBody().lineSequence().firstOrNull()?.take(48)?.ifBlank { "Untitled note" } ?: "Untitled note"
}

fun Note.noteBody(): String {
    val explicitBody = body?.trim().orEmpty()
    if (explicitBody.isNotBlank()) return explicitBody
    return text?.trim().orEmpty()
}

fun sortedTasks(tasks: List<DailyTask>): List<DailyTask> {
    return tasks.sortedWith(
        compareBy<DailyTask> { it.done }
            .thenBy { it.safePriority().rank }
            .thenBy { if (it.sortedDueText().isBlank()) "zzzzzz" else it.sortedDueText().lowercase(Locale.US) }
            .thenByDescending { it.updatedAt ?: 0L }
    )
}
