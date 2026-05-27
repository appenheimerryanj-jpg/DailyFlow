package com.appenheimer.dailyflow

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

const val PREMIUM_PRODUCT_ID = "dailyflow_premium_lifetime"

private const val FREE_ACTIVE_TASK_LIMIT = 7
private const val FREE_HABIT_LIMIT = 3
private const val FREE_NOTE_LIMIT = 5

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyFlowApp() }
    }
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

enum class AppSection(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    TASKS("Tasks", Icons.Filled.CheckCircle),
    HABITS("Habits", Icons.Filled.Favorite),
    NOTES("Notes", Icons.Filled.Edit),
    PREMIUM("Premium", Icons.Filled.Star),
    SETTINGS("Settings", Icons.Filled.Settings)
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

private fun nowMillis(): Long = System.currentTimeMillis()

private fun todayKey(): String = dayKey(0)

private fun yesterdayKey(): String = dayKey(-1)

private fun dayKey(offsetDays: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun formatTimestamp(value: Long?): String {
    val millis = value?.takeIf { it > 0L } ?: return "Recently"
    return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(millis))
}

private fun DailyTask.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

private fun DailyTask.safeText(): String = text?.trim().orEmpty()

private fun DailyTask.safePriority(): TaskPriority = priority ?: TaskPriority.MEDIUM

private fun Habit.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

private fun Habit.safeName(): String = name?.trim().orEmpty()

private fun Note.safeId(): String = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

private fun Note.noteTitle(): String {
    val explicitTitle = title?.trim().orEmpty()
    if (explicitTitle.isNotBlank()) return explicitTitle
    return noteBody().lineSequence().firstOrNull()?.take(48)?.ifBlank { "Untitled note" } ?: "Untitled note"
}

private fun Note.noteBody(): String {
    val explicitBody = body?.trim().orEmpty()
    if (explicitBody.isNotBlank()) return explicitBody
    return text?.trim().orEmpty()
}

class Store(private val activity: Activity) : PurchasesUpdatedListener {
    private val prefs = activity.getSharedPreferences("dailyflow", Activity.MODE_PRIVATE)
    private val gson = Gson()

    var dataWarning by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var tasks by mutableStateOf(sanitizeTasks(loadJson<List<DailyTask>>("tasks") ?: seedTasks()))
    var habits by mutableStateOf(sanitizeHabits(loadJson<List<Habit>>("habits") ?: seedHabits()))
    var notes by mutableStateOf(sanitizeNotes(loadJson<List<Note>>("notes") ?: emptyList()))
    var premium by mutableStateOf(prefs.getBoolean("premium", false))
    var billingReady by mutableStateOf(false)
    var billingLoading by mutableStateOf(true)
    var purchaseStatus by mutableStateOf("Premium unlocks unlimited tasks, habits, and notes.")

    private var premiumDetails: ProductDetails? = null
    private val billing = BillingClient.newBuilder(activity)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener(this)
        .build()

    val activeTaskCount: Int get() = tasks.count { !it.done }
    val completedTaskCount: Int get() = tasks.count { it.done }
    val doneHabitsTodayCount: Int get() = habits.count { it.lastDone == todayKey() }
    val noteCount: Int get() = notes.size

    init {
        normalizeMissedHabits()
        saveAll()
        connectBilling()
    }

    private fun seedTasks(): List<DailyTask> = listOf(
        DailyTask(text = "Pick the top 3 priorities for today", priority = TaskPriority.HIGH),
        DailyTask(text = "Plan a focused 25 minute work block", priority = TaskPriority.MEDIUM),
        DailyTask(text = "Close one small loose end", priority = TaskPriority.LOW)
    )

    private fun seedHabits(): List<Habit> = listOf(
        Habit(name = "Drink water"),
        Habit(name = "Plan tomorrow")
    )

    private inline fun <reified T> loadJson(key: String): T? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson<T>(raw, object : TypeToken<T>() {}.type)
        } catch (error: Exception) {
            dataWarning = "Some saved $key data could not be read, so DailyFlow loaded a clean copy."
            null
        }
    }

    private fun saveJson(key: String, value: Any) {
        prefs.edit().putString(key, gson.toJson(value)).apply()
    }

    private fun saveAll() {
        saveJson("tasks", tasks)
        saveJson("habits", habits)
        saveJson("notes", notes)
    }

    private fun sanitizeTasks(input: List<DailyTask>): List<DailyTask> {
        val now = nowMillis()
        return input.mapNotNull { task ->
            val cleanText = task.safeText()
            if (cleanText.isBlank()) return@mapNotNull null
            task.copy(
                id = task.safeId(),
                text = cleanText,
                priority = task.safePriority(),
                dueDate = task.dueDate?.trim().orEmpty(),
                createdAt = task.createdAt?.takeIf { it > 0L } ?: now,
                updatedAt = task.updatedAt?.takeIf { it > 0L } ?: now
            )
        }
    }

    private fun sanitizeHabits(input: List<Habit>): List<Habit> {
        val now = nowMillis()
        return input.mapNotNull { habit ->
            val cleanName = habit.safeName()
            if (cleanName.isBlank()) return@mapNotNull null
            habit.copy(
                id = habit.safeId(),
                name = cleanName,
                streak = habit.streak.coerceAtLeast(0),
                lastDone = habit.lastDone?.trim().orEmpty(),
                createdAt = habit.createdAt?.takeIf { it > 0L } ?: now,
                updatedAt = habit.updatedAt?.takeIf { it > 0L } ?: now
            )
        }
    }

    private fun sanitizeNotes(input: List<Note>): List<Note> {
        val now = nowMillis()
        return input.mapNotNull { note ->
            val cleanTitle = note.title?.trim().orEmpty()
            val cleanBody = note.noteBody()
            if (cleanTitle.isBlank() && cleanBody.isBlank()) return@mapNotNull null
            note.copy(
                id = note.safeId(),
                title = cleanTitle,
                body = cleanBody,
                text = "",
                createdAt = note.createdAt?.takeIf { it > 0L } ?: now,
                updatedAt = note.updatedAt?.takeIf { it > 0L } ?: note.createdAt ?: now
            )
        }.sortedByDescending { it.updatedAt ?: 0L }
    }

    private fun normalizeMissedHabits() {
        val today = todayKey()
        val yesterday = yesterdayKey()
        val normalized = habits.map { habit ->
            val lastDone = habit.lastDone.orEmpty()
            if (habit.streak > 0 && lastDone.isNotBlank() && lastDone != today && lastDone != yesterday) {
                habit.copy(streak = 0, lastDone = "")
            } else {
                habit
            }
        }
        if (normalized != habits) {
            habits = normalized
            saveJson("habits", habits)
        }
    }

    fun canAddTask(): Boolean = premium || activeTaskCount < FREE_ACTIVE_TASK_LIMIT

    fun canAddHabit(): Boolean = premium || habits.size < FREE_HABIT_LIMIT

    fun canAddNote(): Boolean = premium || notes.size < FREE_NOTE_LIMIT

    fun limitMessage(kind: String): String = when (kind) {
        "tasks" -> "Free plan allows $FREE_ACTIVE_TASK_LIMIT active tasks. Complete or delete one, or unlock Premium."
        "habits" -> "Free plan allows $FREE_HABIT_LIMIT habits. Delete one, or unlock Premium."
        "notes" -> "Free plan allows $FREE_NOTE_LIMIT notes. Delete one, or unlock Premium."
        else -> "Premium removes DailyFlow limits."
    }

    fun upsertTask(existingId: String?, text: String, priority: TaskPriority, dueDate: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            notice = "Task needs a title."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddTask()) {
                notice = limitMessage("tasks")
                return false
            }
            tasks = tasks + DailyTask(
                text = cleanText,
                priority = priority,
                dueDate = dueDate.trim(),
                createdAt = now,
                updatedAt = now
            )
        } else {
            tasks = tasks.map { task ->
                if (task.id == existingId) {
                    task.copy(text = cleanText, priority = priority, dueDate = dueDate.trim(), updatedAt = now)
                } else {
                    task
                }
            }
        }
        saveJson("tasks", tasks)
        return true
    }

    fun toggleTask(task: DailyTask) {
        if (task.done && !canAddTask()) {
            notice = limitMessage("tasks")
            return
        }
        tasks = tasks.map { item ->
            if (item.id == task.id) item.copy(done = !item.done, updatedAt = nowMillis()) else item
        }
        saveJson("tasks", tasks)
    }

    fun deleteTask(id: String?) {
        tasks = tasks.filterNot { it.id == id }
        saveJson("tasks", tasks)
    }

    fun upsertHabit(existingId: String?, name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            notice = "Habit needs a name."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddHabit()) {
                notice = limitMessage("habits")
                return false
            }
            habits = habits + Habit(name = cleanName, createdAt = now, updatedAt = now)
        } else {
            habits = habits.map { habit ->
                if (habit.id == existingId) habit.copy(name = cleanName, updatedAt = now) else habit
            }
        }
        saveJson("habits", habits)
        return true
    }

    fun completeHabit(habit: Habit) {
        val today = todayKey()
        val yesterday = yesterdayKey()
        if (habit.lastDone == today) {
            notice = "${habit.safeName()} is already done today."
            return
        }
        habits = habits.map { item ->
            if (item.id == habit.id) {
                val previous = item.lastDone.orEmpty()
                val nextStreak = if (previous == yesterday) item.streak.coerceAtLeast(0) + 1 else 1
                item.copy(streak = nextStreak, lastDone = today, updatedAt = nowMillis())
            } else {
                item
            }
        }
        saveJson("habits", habits)
    }

    fun resetHabit(habit: Habit) {
        habits = habits.map { item ->
            if (item.id == habit.id) item.copy(streak = 0, lastDone = "", updatedAt = nowMillis()) else item
        }
        saveJson("habits", habits)
    }

    fun deleteHabit(id: String?) {
        habits = habits.filterNot { it.id == id }
        saveJson("habits", habits)
    }

    fun upsertNote(existingId: String?, title: String, body: String): Boolean {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() && cleanBody.isBlank()) {
            notice = "Note needs a title or body."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddNote()) {
                notice = limitMessage("notes")
                return false
            }
            notes = listOf(Note(title = cleanTitle, body = cleanBody, createdAt = now, updatedAt = now)) + notes
        } else {
            notes = notes.map { note ->
                if (note.id == existingId) note.copy(title = cleanTitle, body = cleanBody, updatedAt = now) else note
            }.sortedByDescending { it.updatedAt ?: 0L }
        }
        saveJson("notes", notes)
        return true
    }

    fun deleteNote(id: String?) {
        notes = notes.filterNot { it.id == id }
        saveJson("notes", notes)
    }

    fun resetData() {
        tasks = emptyList()
        habits = emptyList()
        notes = emptyList()
        saveAll()
        notice = "DailyFlow data reset. Premium status was kept."
    }

    fun devUnlock() {
        if (BuildConfig.DEBUG) {
            premium = true
            prefs.edit().putBoolean("premium", true).apply()
            purchaseStatus = "Debug premium enabled."
            notice = "Premium enabled for this debug build."
        }
    }

    private fun connectBilling() {
        billingLoading = true
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                activity.runOnUiThread {
                    billingReady = result.responseCode == BillingClient.BillingResponseCode.OK
                    billingLoading = false
                    if (!billingReady) {
                        purchaseStatus = "Billing unavailable: ${result.debugMessage}"
                    }
                }
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductsAndPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                activity.runOnUiThread {
                    billingReady = false
                    billingLoading = false
                    purchaseStatus = "Billing disconnected. Try restore purchases in a moment."
                }
            }
        })
    }

    private fun queryProductsAndPurchases() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        billing.queryProductDetailsAsync(params) { _, details ->
            val productDetails = details.productDetailsList.firstOrNull()
            activity.runOnUiThread {
                premiumDetails = productDetails
                if (productDetails == null && !premium) {
                    purchaseStatus = "Premium product is not available yet. Check the Play Console product setup."
                }
            }
        }
        restorePurchases(showEmptyMessage = false)
    }

    fun buyPremium() {
        val details = premiumDetails
        if (premium) {
            notice = "Premium is already active."
            return
        }
        if (!billingReady || details == null) {
            purchaseStatus = "Set up $PREMIUM_PRODUCT_ID in Play Console, then test with a licensed tester."
            notice = purchaseStatus
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billing.launchBillingFlow(activity, params)
    }

    fun restorePurchases(showEmptyMessage: Boolean = true) {
        if (!billingReady) {
            if (showEmptyMessage) {
                notice = "Billing is still connecting. Try again in a moment."
            }
            return
        }
        billing.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { _, purchases ->
            val restored = purchases.any { handlePurchase(it) }
            activity.runOnUiThread {
                if (showEmptyMessage) {
                    notice = if (restored) {
                        "Premium purchase restored."
                    } else {
                        "No premium purchase found for this Google account."
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            activity.runOnUiThread {
                purchaseStatus = "Purchase failed: ${result.debugMessage}"
                notice = purchaseStatus
            }
        }
    }

    private fun handlePurchase(purchase: Purchase): Boolean {
        val matchesPremium = purchase.products.contains(PREMIUM_PRODUCT_ID) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        if (!matchesPremium) return false
        activity.runOnUiThread {
            premium = true
            prefs.edit().putBoolean("premium", true).apply()
            purchaseStatus = "Premium unlocked. Thank you!"
        }
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billing.acknowledgePurchase(params) {}
        }
        return true
    }

    fun close() {
        if (billing.isReady) billing.endConnection()
    }
}

private val DailyFlowColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F1E9),
    onPrimaryContainer = Color(0xFF08251F),
    secondary = Color(0xFF59628D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E4FF),
    onSecondaryContainer = Color(0xFF171B35),
    tertiary = Color(0xFFB65C00),
    tertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFFEFFFC),
    surfaceVariant = Color(0xFFE4EAE4),
    outline = Color(0xFF727971),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFlowApp() {
    val activity = LocalContext.current as Activity
    val store = remember { Store(activity) }
    val snackbarHostState = remember { SnackbarHostState() }
    var sectionName by rememberSaveable { mutableStateOf(AppSection.HOME.name) }
    val currentSection = AppSection.valueOf(sectionName)

    LaunchedEffect(store.notice) {
        val message = store.notice
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            store.notice = null
        }
    }

    DisposableEffect(store) {
        onDispose { store.close() }
    }

    MaterialTheme(colorScheme = DailyFlowColors) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("DailyFlow", fontWeight = FontWeight.Bold)
                            Text(
                                currentSection.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = { sectionName = AppSection.PREMIUM.name },
                            label = { Text(if (store.premium) "Premium" else "Free") },
                            leadingIcon = {
                                Icon(
                                    if (store.premium) Icons.Filled.WorkspacePremium else Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentSection == item,
                            onClick = { sectionName = item.name },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentSection) {
                    AppSection.HOME -> HomeScreen(store) { sectionName = it.name }
                    AppSection.TASKS -> TasksScreen(store)
                    AppSection.HABITS -> HabitsScreen(store)
                    AppSection.NOTES -> NotesScreen(store)
                    AppSection.PREMIUM -> PremiumScreen(store)
                    AppSection.SETTINGS -> SettingsScreen(store)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(store: Store, navigate: (AppSection) -> Unit) {
    ScreenList {
        item {
            HeroCard(store)
        }
        item {
            WarningCard(store.dataWarning)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Active tasks",
                    value = store.activeTaskCount.toString(),
                    subtitle = "${store.completedTaskCount} completed",
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f),
                    onClick = { navigate(AppSection.TASKS) }
                )
                MetricCard(
                    title = "Habits today",
                    value = "${store.doneHabitsTodayCount}/${store.habits.size}",
                    subtitle = "checked in",
                    icon = Icons.Filled.Favorite,
                    modifier = Modifier.weight(1f),
                    onClick = { navigate(AppSection.HABITS) }
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Notes",
                    value = store.noteCount.toString(),
                    subtitle = "stored locally",
                    icon = Icons.Filled.Edit,
                    modifier = Modifier.weight(1f),
                    onClick = { navigate(AppSection.NOTES) }
                )
                MetricCard(
                    title = "Plan",
                    value = if (store.premium) "Pro" else "Free",
                    subtitle = if (store.premium) "unlimited" else "limits apply",
                    icon = Icons.Filled.Star,
                    modifier = Modifier.weight(1f),
                    onClick = { navigate(AppSection.PREMIUM) }
                )
            }
        }
        item {
            TodayProgressCard(store, navigate)
        }
        item {
            SectionCard(
                title = "Next tasks",
                actionLabel = "View all",
                onAction = { navigate(AppSection.TASKS) }
            ) {
                val nextTasks = store.tasks
                    .filterNot { it.done }
                    .sortedWith(compareByDescending<DailyTask> { it.safePriority().ordinal }.thenBy { it.createdAt })
                    .take(3)
                if (nextTasks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Check,
                        title = "No active tasks",
                        body = "Add a task when something deserves your attention."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        nextTasks.forEach { task ->
                            TaskCompactRow(task = task, onClick = { navigate(AppSection.TASKS) })
                        }
                    }
                }
            }
        }
        item {
            SectionCard(
                title = "Recent notes",
                actionLabel = "Open notes",
                onAction = { navigate(AppSection.NOTES) }
            ) {
                val recentNotes = store.notes.take(2)
                if (recentNotes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Edit,
                        title = "No notes yet",
                        body = "Capture ideas, lists, and reminders in Notes."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentNotes.forEach { note ->
                            Text(note.noteTitle(), fontWeight = FontWeight.SemiBold)
                            Text(
                                note.noteBody().ifBlank { "No body text" },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Updated ${formatTimestamp(note.updatedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (note != recentNotes.last()) HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(store: Store) {
    var filterName by rememberSaveable { mutableStateOf(TaskFilter.ALL.name) }
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<DailyTask?>(null) }
    val filter = TaskFilter.valueOf(filterName)
    val filteredTasks = when (filter) {
        TaskFilter.ALL -> store.tasks
        TaskFilter.ACTIVE -> store.tasks.filterNot { it.done }
        TaskFilter.COMPLETED -> store.tasks.filter { it.done }
    }.sortedWith(compareBy<DailyTask> { it.done }.thenByDescending { it.safePriority().ordinal })

    if (showEditor) {
        TaskEditorDialog(
            task = editingTask,
            onDismiss = { showEditor = false },
            onSave = { text, priority, dueDate ->
                if (store.upsertTask(editingTask?.id, text, priority, dueDate)) showEditor = false
            }
        )
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Tasks",
                subtitle = "Plan, prioritize, and finish the small pieces of the day.",
                actionText = "Add task",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddTask()) {
                        editingTask = null
                        showEditor = true
                    } else {
                        store.notice = store.limitMessage("tasks")
                    }
                }
            )
        }
        item {
            LimitCard(
                title = "Free active task limit",
                used = store.activeTaskCount,
                limit = FREE_ACTIVE_TASK_LIMIT,
                premium = store.premium,
                message = store.limitMessage("tasks")
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filterName = item.name },
                        label = { Text(item.label) }
                    )
                }
            }
        }
        if (filteredTasks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = when (filter) {
                        TaskFilter.ALL -> "No tasks yet"
                        TaskFilter.ACTIVE -> "No active tasks"
                        TaskFilter.COMPLETED -> "No completed tasks"
                    },
                    body = "Use Add task to capture a clear next action."
                )
            }
        } else {
            items(filteredTasks, key = { it.id ?: it.hashCode().toString() }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { store.toggleTask(task) },
                    onEdit = {
                        editingTask = task
                        showEditor = true
                    },
                    onDelete = { store.deleteTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun HabitsScreen(store: Store) {
    var showEditor by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    if (showEditor) {
        HabitEditorDialog(
            habit = editingHabit,
            onDismiss = { showEditor = false },
            onSave = { name ->
                if (store.upsertHabit(editingHabit?.id, name)) showEditor = false
            }
        )
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Habits",
                subtitle = "Keep streaks honest: a missed day resets the count.",
                actionText = "Add habit",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddHabit()) {
                        editingHabit = null
                        showEditor = true
                    } else {
                        store.notice = store.limitMessage("habits")
                    }
                }
            )
        }
        item {
            LimitCard(
                title = "Free habit limit",
                used = store.habits.size,
                limit = FREE_HABIT_LIMIT,
                premium = store.premium,
                message = store.limitMessage("habits")
            )
        }
        if (store.habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Favorite,
                    title = "No habits yet",
                    body = "Start with one habit that is easy enough to do today."
                )
            }
        } else {
            items(store.habits, key = { it.id ?: it.hashCode().toString() }) { habit ->
                HabitCard(
                    habit = habit,
                    onComplete = { store.completeHabit(habit) },
                    onReset = { store.resetHabit(habit) },
                    onEdit = {
                        editingHabit = habit
                        showEditor = true
                    },
                    onDelete = { store.deleteHabit(habit.id) }
                )
            }
        }
    }
}

@Composable
private fun NotesScreen(store: Store) {
    var query by rememberSaveable { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    val filteredNotes = store.notes.filter { note ->
        val needle = query.trim()
        needle.isBlank() ||
            note.noteTitle().contains(needle, ignoreCase = true) ||
            note.noteBody().contains(needle, ignoreCase = true)
    }

    if (showEditor) {
        NoteEditorDialog(
            note = editingNote,
            onDismiss = { showEditor = false },
            onSave = { title, body ->
                if (store.upsertNote(editingNote?.id, title, body)) showEditor = false
            }
        )
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Notes",
                subtitle = "Quick local notes for reminders, ideas, and lists.",
                actionText = "Add note",
                actionIcon = Icons.Filled.Add,
                onAction = {
                    if (store.canAddNote()) {
                        editingNote = null
                        showEditor = true
                    } else {
                        store.notice = store.limitMessage("notes")
                    }
                }
            )
        }
        item {
            LimitCard(
                title = "Free note limit",
                used = store.notes.size,
                limit = FREE_NOTE_LIMIT,
                premium = store.premium,
                message = store.limitMessage("notes")
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                label = { Text("Search notes") }
            )
        }
        if (filteredNotes.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Edit,
                    title = if (query.isBlank()) "No notes yet" else "No notes found",
                    body = if (query.isBlank()) {
                        "Add a note to keep useful details close."
                    } else {
                        "Try a different search term."
                    }
                )
            }
        } else {
            items(filteredNotes, key = { it.id ?: it.hashCode().toString() }) { note ->
                NoteCard(
                    note = note,
                    onEdit = {
                        editingNote = note
                        showEditor = true
                    },
                    onDelete = { store.deleteNote(note.id) }
                )
            }
        }
    }
}

@Composable
private fun PremiumScreen(store: Store) {
    ScreenList {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (store.premium) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (store.premium) Icons.Filled.WorkspacePremium else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (store.premium) "Premium is active" else "Unlock DailyFlow Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (store.premium) {
                            "Unlimited tasks, habits, and notes are enabled on this device."
                        } else {
                            "DailyFlow is useful for free. Premium removes local item limits with a one-time purchase."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                if (store.billingLoading) "Billing connecting" else if (store.billingReady) "Billing ready" else "Billing unavailable"
                            )
                        }
                    )
                }
            }
        }
        item {
            SectionCard(title = "Free plan") {
                FeatureLine("$FREE_ACTIVE_TASK_LIMIT active tasks")
                FeatureLine("$FREE_HABIT_LIMIT habits")
                FeatureLine("$FREE_NOTE_LIMIT notes")
                FeatureLine("Offline-first local storage")
            }
        }
        item {
            SectionCard(title = "Premium benefits") {
                FeatureLine("Unlimited active tasks")
                FeatureLine("Unlimited habits and streak tracking")
                FeatureLine("Unlimited notes and search")
                FeatureLine("One-time lifetime unlock")
            }
        }
        item {
            SectionCard(title = "Purchase") {
                Text(store.purchaseStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { store.buyPremium() },
                    enabled = !store.premium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (store.premium) "Premium active" else "Unlock Premium")
                }
                OutlinedButton(
                    onClick = { store.restorePurchases() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore purchases")
                }
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { store.devUnlock() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Debug unlock premium")
                    }
                }
                Text(
                    "Product ID: $PREMIUM_PRODUCT_ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(store: Store) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset DailyFlow data?") },
            text = { Text("This clears tasks, habits, and notes stored on this device. Premium status is kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        store.resetData()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ScreenList {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "App details, privacy, and local data controls."
            )
        }
        item {
            SectionCard(title = "App") {
                SettingsRow("Name", "DailyFlow")
                SettingsRow("Version", BuildConfig.VERSION_NAME)
                SettingsRow("Premium", if (store.premium) "Active" else "Free plan")
            }
        }
        item {
            SectionCard(title = "Privacy") {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "DailyFlow stores tasks, habits, notes, and premium status locally on this device for now. No cloud sync is used in this MVP.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            SectionCard(title = "Appearance") {
                SettingsRow("Theme", "Light theme active. Theme choices are planned.")
            }
        }
        item {
            SectionCard(title = "Data") {
                Text(
                    "Reset local content if you want a fresh start.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset local data")
                }
            }
        }
    }
}

@Composable
private fun ScreenList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionIcon: ImageVector = Icons.Filled.Add,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionText != null && onAction != null) {
            Spacer(Modifier.width(12.dp))
            Button(onClick = onAction) {
                Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionText)
            }
        }
    }
}

@Composable
private fun HeroCard(store: Store) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Today at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "A calmer place for the tasks, habits, and notes that make the day move.",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, enabled = false, label = { Text("${store.activeTaskCount} active") })
                AssistChip(onClick = {}, enabled = false, label = { Text("${store.doneHabitsTodayCount} habits done") })
            }
        }
    }
}

@Composable
private fun WarningCard(message: String?) {
    if (message.isNullOrBlank()) return
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayProgressCard(store: Store, navigate: (AppSection) -> Unit) {
    val totalHabits = store.habits.size.coerceAtLeast(1)
    val habitProgress = store.doneHabitsTodayCount.toFloat() / totalHabits.toFloat()
    SectionCard(title = "Today's progress", actionLabel = "Habits", onAction = { navigate(AppSection.HABITS) }) {
        Text("${store.doneHabitsTodayCount} of ${store.habits.size} habits done")
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { habitProgress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            if (store.habits.isEmpty()) "Add a habit to start tracking today." else "Keep the chain light and realistic.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScopeMarker.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) { Text(actionLabel) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

typealias ColumnScopeMarker = androidx.compose.foundation.layout.ColumnScope

@Composable
private fun LimitCard(title: String, used: Int, limit: Int, premium: Boolean, message: String) {
    if (premium) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Premium active: unlimited items are enabled.", fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }
    val progress = (used.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = if (used >= limit) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$used/$limit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            if (used >= limit) {
                Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskCompactRow(task: DailyTask, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PriorityBadge(task.safePriority())
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(task.safeText(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!task.dueDate.isNullOrBlank()) {
                Text("Due ${task.dueDate}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun TaskCard(task: DailyTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    task.safeText(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(task.safePriority())
                    if (!task.dueDate.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            leadingIcon = { Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(task.dueDate.orEmpty()) }
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit task") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete task") }
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    onComplete: () -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val doneToday = habit.lastDone == todayKey()
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(habit.safeName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (habit.streak > 0) "${habit.streak} day streak" else "No active streak",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(containerColor = if (doneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(if (doneToday) "Done" else "Today")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onComplete, enabled = !doneToday, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (doneToday) "Done today" else "Mark done")
                }
                OutlinedButton(onClick = onReset) {
                    Text("Reset")
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit habit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete habit") }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(note.noteTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Updated ${formatTimestamp(note.updatedAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit note") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete note") }
            }
            Text(
                note.noteBody().ifBlank { "No body text" },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        TaskPriority.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) {
        Text(
            priority.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}

@Composable
private fun SettingsRow(title: String, value: String) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(value) }
    )
}

@Composable
private fun TaskEditorDialog(
    task: DailyTask?,
    onDismiss: () -> Unit,
    onSave: (String, TaskPriority, String) -> Unit
) {
    var text by remember(task?.id) { mutableStateOf(task?.safeText().orEmpty()) }
    var dueDate by remember(task?.id) { mutableStateOf(task?.dueDate.orEmpty()) }
    var priority by remember(task?.id) { mutableStateOf(task?.safePriority() ?: TaskPriority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "Add task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Due date (optional)") },
                    placeholder = { Text("Today, Friday, Jun 12...") },
                    singleLine = true
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { item ->
                        FilterChip(
                            selected = priority == item,
                            onClick = { priority = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text, priority, dueDate) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HabitEditorDialog(habit: Habit?, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(habit?.id) { mutableStateOf(habit?.safeName().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (habit == null) "Add habit" else "Edit habit") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Habit") },
                placeholder = { Text("Read, walk, stretch...") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NoteEditorDialog(note: Note?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(note?.id) { mutableStateOf(note?.title?.trim().orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.noteBody().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "Add note" else "Edit note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    minLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, body) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
