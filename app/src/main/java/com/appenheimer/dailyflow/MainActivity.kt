package com.appenheimer.dailyflow

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

const val PREMIUM_PRODUCT_ID = "dailyflow_premium_lifetime"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DailyFlowApp() }
    }
}

data class DailyTask(val id: String = UUID.randomUUID().toString(), val text: String, val done: Boolean = false)
data class Habit(val id: String = UUID.randomUUID().toString(), val name: String, val streak: Int = 0, val lastDone: String = "")
data class Note(val id: String = UUID.randomUUID().toString(), val text: String, val created: String = nowStamp())
fun nowStamp(): String = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date())
fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

class Store(private val activity: Activity) : PurchasesUpdatedListener {
    private val prefs = activity.getSharedPreferences("dailyflow", Activity.MODE_PRIVATE)
    private val gson = Gson()
    var tasks by mutableStateOf(load<List<DailyTask>>("tasks") ?: seedTasks())
    var habits by mutableStateOf(load<List<Habit>>("habits") ?: listOf(Habit(name="Drink water"), Habit(name="Plan tomorrow")))
    var notes by mutableStateOf(load<List<Note>>("notes") ?: emptyList())
    var premium by mutableStateOf(prefs.getBoolean("premium", false))
    var billingReady by mutableStateOf(false)
    var purchaseStatus by mutableStateOf("Premium unlocks unlimited tasks, habits, and notes.")
    private var premiumDetails: ProductDetails? = null
    private val billing = BillingClient.newBuilder(activity).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).setListener(this).build()

    init { connectBilling() }
    private fun seedTasks() = listOf(DailyTask(text="Write today’s top 3 priorities"), DailyTask(text="Clean one small area"), DailyTask(text="Check budget before spending"))
    private inline fun <reified T> load(key: String): T? = prefs.getString(key, null)?.let { gson.fromJson<T>(it, object : TypeToken<T>(){}.type) }
    private fun save(key: String, value: Any) = prefs.edit().putString(key, gson.toJson(value)).apply()
    fun addTask(text: String) { if (text.isBlank()) return; if (!premium && tasks.size >= 7) { purchaseStatus = "Free limit reached. Premium unlocks unlimited tasks."; return }; tasks = tasks + DailyTask(text=text.trim()); save("tasks", tasks) }
    fun toggleTask(task: DailyTask) { tasks = tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it }; save("tasks", tasks) }
    fun deleteTask(id: String) { tasks = tasks.filterNot { it.id == id }; save("tasks", tasks) }
    fun addHabit(name: String) { if (name.isBlank()) return; if (!premium && habits.size >= 3) { purchaseStatus = "Free limit reached. Premium unlocks unlimited habits."; return }; habits = habits + Habit(name=name.trim()); save("habits", habits) }
    fun completeHabit(habit: Habit) { val today = todayKey(); if (habit.lastDone == today) return; habits = habits.map { if (it.id == habit.id) it.copy(streak = it.streak + 1, lastDone = today) else it }; save("habits", habits) }
    fun deleteHabit(id: String) { habits = habits.filterNot { it.id == id }; save("habits", habits) }
    fun addNote(text: String) { if (text.isBlank()) return; if (!premium && notes.size >= 5) { purchaseStatus = "Free limit reached. Premium unlocks unlimited notes."; return }; notes = listOf(Note(text=text.trim())) + notes; save("notes", notes) }
    fun deleteNote(id: String) { notes = notes.filterNot { it.id == id }; save("notes", notes) }
    fun devUnlock() { if (BuildConfig.DEBUG) { premium = true; prefs.edit().putBoolean("premium", true).apply(); purchaseStatus = "Debug premium enabled." } }

    private fun connectBilling() {
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                billingReady = result.responseCode == BillingClient.BillingResponseCode.OK
                if (billingReady) queryProductsAndPurchases() else purchaseStatus = "Billing unavailable: ${result.debugMessage}"
            }
            override fun onBillingServiceDisconnected() { billingReady = false }
        })
    }
    private fun queryProductsAndPurchases() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PREMIUM_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        billing.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { _, details -> premiumDetails = details.productDetailsList.firstOrNull() }
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { _, purchases -> purchases.forEach { handlePurchase(it) } }
    }
    fun buyPremium() {
        val details = premiumDetails
        if (!billingReady || details == null) { purchaseStatus = "Set up the product ID in Play Console, then test with a licensed tester."; return }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())).build()
        billing.launchBillingFlow(activity, params)
    }
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) purchases.forEach { handlePurchase(it) }
        else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) purchaseStatus = "Purchase failed: ${result.debugMessage}"
    }
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.products.contains(PREMIUM_PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            premium = true; prefs.edit().putBoolean("premium", true).apply(); purchaseStatus = "Premium unlocked. Thank you!"
            if (!purchase.isAcknowledged) billing.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DailyFlowApp() {
    val activity = LocalContext.current as Activity
    val store = remember { Store(activity) }
    var tab by remember { mutableStateOf(0) }
    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF0F766E), secondary = androidx.compose.ui.graphics.Color(0xFF334155))) {
        Scaffold(topBar = { TopAppBar(title = { Text("DailyFlow", fontWeight = FontWeight.Bold) }, actions = { AssistChip(onClick = { store.buyPremium() }, label = { Text(if (store.premium) "Premium" else "Upgrade") }) }) }, bottomBar = { NavigationBar { listOf("Today","Habits","Notes","Premium").forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(label) }) } } }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                when(tab) { 0 -> TodayScreen(store); 1 -> HabitsScreen(store); 2 -> NotesScreen(store); 3 -> PremiumScreen(store) }
            }
        }
    }
}

@Composable fun Header(title: String, subtitle: String) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(12.dp)) }
@Composable fun InputRow(placeholder: String, onAdd: (String)->Unit) { var text by remember { mutableStateOf("") }; Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text(placeholder) }, singleLine = true); Spacer(Modifier.width(8.dp)); FilledIconButton(onClick = { onAdd(text); text = "" }) { Icon(Icons.Default.Add, null) } } }

@Composable fun TodayScreen(store: Store) { Header("Today", "Keep your day simple and focused."); InputRow("Add a task") { store.addTask(it) }; Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(store.tasks) { task -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(task.done, { store.toggleTask(task) }); Text(task.text, Modifier.weight(1f)); IconButton({ store.deleteTask(task.id) }) { Icon(Icons.Default.Delete, null) } } } } } }
@Composable fun HabitsScreen(store: Store) { Header("Habits", "Build streaks one day at a time."); InputRow("Add a habit") { store.addHabit(it) }; Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(store.habits) { habit -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(habit.name, fontWeight = FontWeight.Bold); Text("${habit.streak} day streak") }; Button({ store.completeHabit(habit) }, enabled = habit.lastDone != todayKey()) { Text(if (habit.lastDone == todayKey()) "Done" else "Check in") }; IconButton({ store.deleteHabit(habit.id) }) { Icon(Icons.Default.Delete, null) } } } } } }
@Composable fun NotesScreen(store: Store) { Header("Quick Notes", "Capture ideas, lists, reminders, and thoughts."); InputRow("Write a quick note") { store.addNote(it) }; Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(store.notes) { note -> ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(note.created, Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary); IconButton({ store.deleteNote(note.id) }) { Icon(Icons.Default.Delete, null) } }; Text(note.text) } } } } }
@Composable fun PremiumScreen(store: Store) { Header("DailyFlow Premium", "A simple one-time unlock for power users."); ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (store.premium) "Premium is active" else "Unlock unlimited use", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Text("Free: 7 tasks, 3 habits, 5 notes. Premium: unlimited tasks, habits, and notes."); Text(store.purchaseStatus, color = MaterialTheme.colorScheme.secondary); Button(onClick = { store.buyPremium() }, enabled = !store.premium, modifier = Modifier.fillMaxWidth()) { Text(if (store.premium) "Unlocked" else "Buy Premium") }; if (BuildConfig.DEBUG) OutlinedButton({ store.devUnlock() }, Modifier.fillMaxWidth()) { Text("Debug unlock premium") } } } }
