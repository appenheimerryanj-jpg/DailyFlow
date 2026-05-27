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
import com.appenheimer.dailyflow.model.DelightEvent
import com.appenheimer.dailyflow.model.DelightType
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
    var delight by mutableStateOf<DelightEvent?>(null)
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
            dataWarning = "Flow found unreadable $key data and loaded a clean copy so the app could keep going."
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
        "tasks" -> "Flow says the free plan has room for $FREE_ACTIVE_TASK_LIMIT active tasks. Complete or delete one, or unlock Premium."
        "habits" -> "Flow says the free plan has room for $FREE_HABIT_LIMIT habits. Delete one, or unlock Premium."
        "notes" -> "Flow says the free plan has room for $FREE_NOTE_LIMIT notes. Delete one, or unlock Premium."
        else -> "Premium gives Flow unlimited room for your day."
    }

    private fun showDelight(type: DelightType, message: String) {
        delight = DelightEvent(type, message)
    }

    fun upsertTask(existingId: String?, text: String, priority: TaskPriority, dueDate: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            notice = "Flow needs a task title first."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddTask()) {
                notice = limitMessage("tasks")
                return false
            }
            val wasFirstTask = tasks.isEmpty()
            tasks = tasks + DailyTask(
                id = UUID.randomUUID().toString(),
                text = cleanText,
                priority = priority,
                dueDate = dueDate.trim(),
                createdAt = now,
                updatedAt = now
            )
            notice = "Flow saved that task."
            if (wasFirstTask) {
                showDelight(DelightType.FIRST_TASK, "First task placed. Flow is already moving with you.")
            }
        } else {
            tasks = tasks.map { task ->
                if (task.id == existingId) {
                    task.copy(text = cleanText, priority = priority, dueDate = dueDate.trim(), updatedAt = now)
                } else {
                    task
                }
            }
            notice = "Flow updated that task."
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
        notice = if (task.done) "Flow reopened that task." else "Nice work. Flow marked that task complete."
        if (!task.done) {
            showDelight(DelightType.TASK_COMPLETE, "Done. Flow felt that momentum.")
        }
    }

    fun deleteTask(id: String?) {
        tasks = tasks.filterNot { it.id == id }
        saveJson("tasks", tasks)
        notice = "Flow deleted that task."
    }

    fun clearCompletedTasks() {
        val removed = tasks.count { it.done }
        tasks = tasks.filterNot { it.done }
        saveJson("tasks", tasks)
        notice = if (removed == 1) "Flow cleared 1 completed task." else "Flow cleared $removed completed tasks."
        if (removed > 0) {
            showDelight(DelightType.CLEAR_COMPLETED, "Clean slate. Flow made a little more room.")
        }
    }

    fun upsertHabit(existingId: String?, name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            notice = "Flow needs a habit name first."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddHabit()) {
                notice = limitMessage("habits")
                return false
            }
            val wasFirstHabit = habits.isEmpty()
            habits = habits + Habit(id = UUID.randomUUID().toString(), name = cleanName, createdAt = now, updatedAt = now)
            notice = "Flow saved that habit."
            if (wasFirstHabit) {
                showDelight(DelightType.FIRST_HABIT, "Tiny habit, real momentum. Flow is cheering for the first check-in.")
            }
        } else {
            habits = habits.map { habit ->
                if (habit.id == existingId) habit.copy(name = cleanName, updatedAt = now) else habit
            }
            notice = "Flow updated that habit."
        }
        saveJson("habits", habits)
        return true
    }

    fun completeHabit(habit: Habit) {
        val today = todayKey()
        val yesterday = yesterdayKey()
        var newBestStreak = false
        if (habit.lastDone == today) {
            notice = "Flow already counted ${habit.safeName()} today."
            return
        }
        habits = habits.map { item ->
            if (item.id == habit.id) {
                val previous = item.lastDone.orEmpty()
                val nextStreak = if (previous == yesterday) item.streak.coerceAtLeast(0) + 1 else 1
                newBestStreak = nextStreak > item.bestStreak
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
        notice = "Momentum logged. Flow updated the streak."
        val allHabitsDone = habits.isNotEmpty() && habits.all { it.lastDone == today }
        when {
            allHabitsDone -> showDelight(DelightType.ALL_HABITS_DONE, "All habits checked in. Flow is celebrating the full set.")
            newBestStreak -> showDelight(DelightType.NEW_BEST_STREAK, "New best streak. Flow is sparkling for that consistency.")
            else -> showDelight(DelightType.HABIT_COMPLETE, "Habit checked in. Flow gave the streak a bounce.")
        }
    }

    fun resetHabit(habit: Habit) {
        habits = habits.map { item ->
            if (item.id == habit.id) item.copy(streak = 0, lastDone = "", updatedAt = nowMillis()) else item
        }
        saveJson("habits", habits)
        notice = "Flow reset the current streak and kept the best streak."
    }

    fun deleteHabit(id: String?) {
        habits = habits.filterNot { it.id == id }
        saveJson("habits", habits)
        notice = "Flow deleted that habit."
    }

    fun upsertNote(existingId: String?, title: String, body: String): Boolean {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isBlank() && cleanBody.isBlank()) {
            notice = "Flow needs a note title or body first."
            return false
        }
        val now = nowMillis()
        if (existingId == null) {
            if (!canAddNote()) {
                notice = limitMessage("notes")
                return false
            }
            val wasFirstNote = notes.isEmpty()
            notes = listOf(Note(id = UUID.randomUUID().toString(), title = cleanTitle, body = cleanBody, createdAt = now, updatedAt = now)) + notes
            notice = "Flow saved that note."
            if (wasFirstNote) {
                showDelight(DelightType.FIRST_NOTE, "First note captured. Flow tucked it somewhere easy to find.")
            }
        } else {
            notes = notes.map { note ->
                if (note.id == existingId) note.copy(title = cleanTitle, body = cleanBody, updatedAt = now) else note
            }.sortedByDescending { it.updatedAt ?: 0L }
            notice = "Flow updated that note."
        }
        saveJson("notes", notes)
        return true
    }

    fun deleteNote(id: String?) {
        notes = notes.filterNot { it.id == id }
        saveJson("notes", notes)
        notice = "Flow deleted that note."
    }

    fun resetData() {
        tasks = emptyList()
        habits = emptyList()
        notes = emptyList()
        saveAll()
        notice = "Flow reset your local workspace. Premium status was kept."
    }

    fun exportPlaceholder() {
        notice = "Export is planned. Flow is keeping your data local on this device for now."
    }

    fun importPlaceholder() {
        notice = "Import is planned. Flow will restore local backups here in a future version."
    }

    fun devUnlock() {
        if (BuildConfig.DEBUG) {
            premium = true
            prefs.edit().putBoolean("premium", true).apply()
            purchaseStatus = "Debug unlock active for testing."
            notice = "Debug unlock enabled for testing only."
            showDelight(DelightType.PREMIUM_UNLOCK, "Premium test unlock active. Flow has unlimited space.")
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
            notice = "Flow already sees Premium active."
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
                notice = "Flow is still connecting billing. Try again in a moment."
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
                        "Flow restored Premium."
                    } else {
                        "Flow did not find Premium for this Google account."
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
            purchaseStatus = "Premium unlocked. Flow has unlimited room now."
            showDelight(DelightType.PREMIUM_UNLOCK, "Premium unlocked. Flow has unlimited room now.")
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
