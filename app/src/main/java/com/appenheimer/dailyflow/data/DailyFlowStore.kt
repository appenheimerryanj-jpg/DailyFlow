package com.appenheimer.dailyflow.data

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.appenheimer.dailyflow.BuildConfig
import com.appenheimer.dailyflow.model.DailyTask
import com.appenheimer.dailyflow.model.FREE_ACTIVE_TASK_LIMIT
import com.appenheimer.dailyflow.model.FREE_HABIT_LIMIT
import com.appenheimer.dailyflow.model.FREE_NOTE_LIMIT
import com.appenheimer.dailyflow.model.Habit
import com.appenheimer.dailyflow.model.Note
import com.appenheimer.dailyflow.model.PREMIUM_PRODUCT_ID
import com.appenheimer.dailyflow.model.TaskPriority
import com.appenheimer.dailyflow.model.nowMillis
import com.appenheimer.dailyflow.model.noteBody
import com.appenheimer.dailyflow.model.safeId
import com.appenheimer.dailyflow.model.safeName
import com.appenheimer.dailyflow.model.safePriority
import com.appenheimer.dailyflow.model.safeText
import com.appenheimer.dailyflow.model.todayKey
import com.appenheimer.dailyflow.model.yesterdayKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import kotlin.math.max

class DailyFlowStore(private val activity: Activity) : PurchasesUpdatedListener {
    private val prefs = activity.getSharedPreferences("dailyflow", Activity.MODE_PRIVATE)
    private val gson = Gson()

    var dataWarning by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)
    var onboardingCompleted by mutableStateOf(prefs.getBoolean("onboarding_completed", false))
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
            val current = habit.streak.coerceAtLeast(0)
            habit.copy(
                id = habit.safeId(),
                name = cleanName,
                streak = current,
                bestStreak = max(habit.bestStreak, current).coerceAtLeast(0),
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
                habit.copy(streak = 0, lastDone = "", updatedAt = nowMillis())
            } else {
                habit
            }
        }
        if (normalized != habits) {
            habits = normalized
            saveJson("habits", habits)
        }
    }

    fun completeOnboarding() {
        onboardingCompleted = true
        prefs.edit().putBoolean("onboarding_completed", true).apply()
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
                id = UUID.randomUUID().toString(),
                text = cleanText,
                priority = priority,
                dueDate = dueDate.trim(),
                createdAt = now,
                updatedAt = now
            )
            notice = "Task saved."
        } else {
            tasks = tasks.map { task ->
                if (task.id == existingId) {
                    task.copy(text = cleanText, priority = priority, dueDate = dueDate.trim(), updatedAt = now)
                } else {
                    task
                }
            }
            notice = "Task updated."
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
        notice = if (task.done) "Task reopened." else "Task completed."
    }

    fun deleteTask(id: String?) {
        tasks = tasks.filterNot { it.id == id }
        saveJson("tasks", tasks)
        notice = "Task deleted."
    }

    fun clearCompletedTasks() {
        val removed = tasks.count { it.done }
        tasks = tasks.filterNot { it.done }
        saveJson("tasks", tasks)
        notice = if (removed == 1) "Cleared 1 completed task." else "Cleared $removed completed tasks."
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
            habits = habits + Habit(id = UUID.randomUUID().toString(), name = cleanName, createdAt = now, updatedAt = now)
            notice = "Habit saved."
        } else {
            habits = habits.map { habit ->
                if (habit.id == existingId) habit.copy(name = cleanName, updatedAt = now) else habit
            }
            notice = "Habit updated."
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
                item.copy(
                    streak = nextStreak,
                    bestStreak = max(item.bestStreak, nextStreak),
                    lastDone = today,
                    updatedAt = nowMillis()
                )
            } else {
                item
            }
        }
        saveJson("habits", habits)
        notice = "Habit checked in."
    }

    fun resetHabit(habit: Habit) {
        habits = habits.map { item ->
            if (item.id == habit.id) item.copy(streak = 0, lastDone = "", updatedAt = nowMillis()) else item
        }
        saveJson("habits", habits)
        notice = "Habit streak reset. Best streak was kept."
    }

    fun deleteHabit(id: String?) {
        habits = habits.filterNot { it.id == id }
        saveJson("habits", habits)
        notice = "Habit deleted."
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
            notes = listOf(Note(id = UUID.randomUUID().toString(), title = cleanTitle, body = cleanBody, createdAt = now, updatedAt = now)) + notes
            notice = "Note saved."
        } else {
            notes = notes.map { note ->
                if (note.id == existingId) note.copy(title = cleanTitle, body = cleanBody, updatedAt = now) else note
            }.sortedByDescending { it.updatedAt ?: 0L }
            notice = "Note updated."
        }
        saveJson("notes", notes)
        return true
    }

    fun deleteNote(id: String?) {
        notes = notes.filterNot { it.id == id }
        saveJson("notes", notes)
        notice = "Note deleted."
    }

    fun resetData() {
        tasks = emptyList()
        habits = emptyList()
        notes = emptyList()
        saveAll()
        notice = "DailyFlow data reset. Premium status was kept."
    }

    fun exportPlaceholder() {
        notice = "Export data is planned. Your data stays local on this device for now."
    }

    fun importPlaceholder() {
        notice = "Import data is planned. Future versions can restore local backups here."
    }

    fun devUnlock() {
        if (BuildConfig.DEBUG) {
            premium = true
            prefs.edit().putBoolean("premium", true).apply()
            purchaseStatus = "Debug premium enabled."
            notice = "Debug/testing premium unlock enabled."
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
