package com.example.data.repository

import android.content.Context
import com.example.data.model.CustomPromo
import com.example.data.model.ShopInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class FirebaseSyncManager(
    private val context: Context,
    private val repository: MeatballRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(MeatballRepository.PersistedState::class.java)
    private val stringAdapter: JsonAdapter<String> = moshi.adapter(String::class.java)
    // Used when the state node is stored as a JSON object instead of a JSON string.
    private val mapAdapter: JsonAdapter<Any> = moshi.adapter(
        Types.newParameterizedType(java.util.Map::class.java, String::class.java, Any::class.java)
    )
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val database = FirebaseDatabase.getInstance(DATABASE_URL)
    private val stateRef = database.getReference("$ROOT_PATH/state")

    private val _status = MutableStateFlow("กำลังเชื่อมต่อ Firebase...")
    val status: StateFlow<String> = _status.asStateFlow()

    @Volatile
    private var lastPushedJson: String? = null
    @Volatile
    private var lastAppliedJson: String? = null
    @Volatile
    private var hasReceivedInitialRemote: Boolean = false
    @Volatile
    private var localObserverStarted: Boolean = false
    private var listener: ValueEventListener? = null

    fun start() {
        scope.launch {
            try {
                _status.value = "กำลังลงชื่อเข้าใช้ Firebase..."
                val authResult = FirebaseAuth.getInstance().signInAnonymously().await()
                _status.value = "กำลังดึงรายการเมนูจาก Firebase..."
                // 1) Fetch initial remote snapshot directly first so we NEVER overwrite server state
                //    with empty initial local data, and so large payloads (with base64 images) always load.
                fetchInitialViaRest(authResult.user?.getIdToken(false)?.await()?.token)
                // 2) Attach realtime listener for live updates.
                observeRemote()
            } catch (e: Exception) {
                _status.value = "Firebase ยังไม่พร้อม: ${e.message ?: "unknown"}"
            }
        }
    }

    private fun fetchInitialViaRest(idToken: String?) {
        if (idToken.isNullOrBlank()) return
        try {
            val url = "$DATABASE_URL/$ROOT_PATH/state.json?auth=$idToken"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val body = resp.body?.string()?.trim() ?: return
                if (body.isEmpty() || body == "null") {
                    markRemoteReadyAndStartLocalObserver(pushIfBlank = true)
                    return
                }
                val innerJson = if (body.startsWith("\"")) {
                    try { stringAdapter.fromJson(body) } catch (_: Exception) { null }
                } else {
                    body
                }
                if (!innerJson.isNullOrBlank()) {
                    applyIncomingJson(innerJson)
                }
            }
        } catch (_: Exception) {
            // Realtime listener below will handle it if REST fails.
        }
    }

    private fun applyIncomingJson(json: String) {
        if (json == lastPushedJson || json == lastAppliedJson) {
            markRemoteReadyAndStartLocalObserver(pushIfBlank = false)
            return
        }
        val state = try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            _status.value = "Read data error: ${e.message ?: "parse failed"}"
            null
        } ?: return
        lastAppliedJson = json
        repository.applyRemoteState(state)
        _status.value = "เชื่อมต่อ Firebase แล้ว (${state.menuItems.size} เมนู)"
        markRemoteReadyAndStartLocalObserver(pushIfBlank = false)
    }

    @Synchronized
    private fun markRemoteReadyAndStartLocalObserver(pushIfBlank: Boolean) {
        val wasReady = hasReceivedInitialRemote
        hasReceivedInitialRemote = true
        if (!localObserverStarted) {
            localObserverStarted = true
            observeLocal()
        }
        if (!wasReady && pushIfBlank) {
            pushCurrent()
        }
    }

    private fun observeRemote() {
        val valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val json = when (val raw = snapshot.value) {
                    null -> null
                    is String -> raw
                    else -> try { mapAdapter.toJson(raw) } catch (_: Exception) { null }
                }
                if (json.isNullOrBlank()) {
                    markRemoteReadyAndStartLocalObserver(pushIfBlank = true)
                    return
                }
                scope.launch {
                    applyIncomingJson(json)
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
            val shop = repository.shopInfo
            val categories = repository.categories
            val menu = repository.menuItems
            val orders = repository.orders
            val users = repository.users
            val costs = repository.hiddenCosts
            val promos = repository.customPromos

            kotlinx.coroutines.flow.combine(
                shop, categories, menu, orders, users, costs, promos
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val shopInfo = values[0] as ShopInfo
                @Suppress("UNCHECKED_CAST")
                val categoryList = values[1] as List<com.example.data.model.Category>
                @Suppress("UNCHECKED_CAST")
                val menuList = values[2] as List<com.example.data.model.MenuItem>
                @Suppress("UNCHECKED_CAST")
                val orderList = values[3] as List<com.example.data.model.Order>
                @Suppress("UNCHECKED_CAST")
                val userList = values[4] as Map<String, com.example.data.model.MemberUser>
                @Suppress("UNCHECKED_CAST")
                val costList = values[5] as List<com.example.data.model.HiddenCostLog>
                @Suppress("UNCHECKED_CAST")
                val promoList = values[6] as List<CustomPromo>
                MeatballRepository.PersistedState(
                    shopInfo = shopInfo,
                    categories = categoryList,
                    menuItems = menuList,
                    orders = orderList,
                    users = userList,
                    hiddenCosts = costList,
                    customPromos = promoList,
                    currentCustomerPhone = "",
                    currentActiveOrderId = null,
                    adminPin = ""
                )
            }
                // Drop the initial state emission at subscription time so we only push real user edits.
                .drop(1)
                .debounce(500)
                .distinctUntilChanged()
                .onEach { state ->
                    if (!hasReceivedInitialRemote || repository.isApplyingRemote) return@onEach
                    pushState(state)
                }
                .launchIn(this)
            awaitClose { }
        }.launchIn(scope)
    }

    private fun pushCurrent() {
        if (!hasReceivedInitialRemote) return
        val state = MeatballRepository.PersistedState(
            shopInfo = repository.shopInfo.value,
            categories = repository.categories.value,
            menuItems = repository.menuItems.value,
            orders = repository.orders.value,
            users = repository.users.value,
            hiddenCosts = repository.hiddenCosts.value,
            customPromos = repository.customPromos.value,
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
