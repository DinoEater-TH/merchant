package com.example.data.repository

import android.content.Context
import com.example.data.model.ShopInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseSyncManager(
    private val context: Context,
    private val repository: MeatballRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val adapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(MeatballRepository.PersistedState::class.java)

    private val database = FirebaseDatabase.getInstance(DATABASE_URL)
    private val stateRef = database.getReference("$ROOT_PATH/state")

    private val _status = MutableStateFlow("กำลังเชื่อมต่อ Firebase...")
    val status: StateFlow<String> = _status.asStateFlow()

    private var lastPushedJson: String? = null
    private var lastAppliedJson: String? = null
    private var listener: ValueEventListener? = null

    fun start() {
        scope.launch {
            try {
                _status.value = "กำลังลงชื่อเข้าใช้ Firebase..."
                FirebaseAuth.getInstance().signInAnonymously().await()
                _status.value = "กำลังเชื่อมต่อ Firebase..."
                observeRemote()
                observeLocal()
                _status.value = "เชื่อมต่อ Firebase แล้ว"
            } catch (e: Exception) {
                _status.value = "Firebase ยังไม่พร้อม: ${e.message ?: "unknown"}"
            }
        }
    }

    private fun observeRemote() {
        val valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val json = snapshot.getValue(String::class.java)
                if (json.isNullOrBlank()) {
                    pushCurrent()
                    return
                }
                if (json == lastPushedJson || json == lastAppliedJson) return
                val state = try {
                    adapter.fromJson(json)
                } catch (_: Exception) {
                    null
                } ?: return
                lastAppliedJson = json
                scope.launch {
                    repository.applyRemoteState(state)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _status.value = "Firebase error: ${error.message}"
            }
        }
        listener = valueListener
        stateRef.addValueEventListener(valueListener)
    }

    private fun observeLocal() {
        callbackFlow<Unit> {
            // Combine the shared state flows into one emission.
            val shop = repository.shopInfo
            val categories = repository.categories
            val menu = repository.menuItems
            val orders = repository.orders
            val users = repository.users
            val costs = repository.hiddenCosts

            // Simple polling-free approach: collect each flow independently is not possible here,
            // so we rely on a single combined collector using kotlinx coroutines flow combine.
            kotlinx.coroutines.flow.combine(
                shop, categories, menu, orders, users, costs
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val shopInfo = values[0] as ShopInfo
                val categoryList = values[1] as List<com.example.data.model.Category>
                val menuList = values[2] as List<com.example.data.model.MenuItem>
                val orderList = values[3] as List<com.example.data.model.Order>
                val userList = values[4] as Map<String, com.example.data.model.MemberUser>
                val costList = values[5] as List<com.example.data.model.HiddenCostLog>
                MeatballRepository.PersistedState(
                    shopInfo = shopInfo,
                    categories = categoryList,
                    menuItems = menuList,
                    orders = orderList,
                    users = userList,
                    hiddenCosts = costList,
                    currentCustomerPhone = "",
                    currentActiveOrderId = null,
                    adminPin = ""
                )
            }.debounce(500)
                .distinctUntilChanged()
                .onEach { state ->
                    if (repository.isApplyingRemote) return@onEach
                    pushState(state)
                }
                .launchIn(this)
            awaitClose { }
        }.launchIn(scope)
    }

    private fun pushCurrent() {
        val state = MeatballRepository.PersistedState(
            shopInfo = repository.shopInfo.value,
            categories = repository.categories.value,
            menuItems = repository.menuItems.value,
            orders = repository.orders.value,
            users = repository.users.value,
            hiddenCosts = repository.hiddenCosts.value,
            currentCustomerPhone = "",
            currentActiveOrderId = null,
            adminPin = ""
        )
        pushState(state)
    }

    private fun pushState(state: MeatballRepository.PersistedState) {
        val json = try {
            adapter.toJson(state)
        } catch (_: Exception) {
            return
        }
        if (json == lastPushedJson || json == lastAppliedJson) return
        lastPushedJson = json
        scope.launch {
            try {
                stateRef.setValue(json).await()
            } catch (_: Exception) {
                // Keep local data; retry on next change.
            }
        }
    }

    fun stop() {
        listener?.let { stateRef.removeEventListener(it) }
        listener = null
    }

    companion object {
        private const val DATABASE_URL =
            "https://dinoeater-merchant-default-rtdb.asia-southeast1.firebasedatabase.app"
        private const val ROOT_PATH = "shops/dinoeater-merchant"
    }
}
