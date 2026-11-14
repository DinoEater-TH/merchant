package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Two-tier Firebase sync:
 *  1. Firebase SDK (anonymous auth + realtime listener) — preferred.
 *  2. Plain REST over HTTPS (identitytoolkit sign-in + database REST) — automatic fallback
 *     for phones where the SDK cannot sign in (missing/old Google Play services, SDK auth
 *     failure) or where the realtime websocket is blocked by the network.
 *
 * The fallback keeps the shop fully usable: reads poll every few seconds and writes go to
 * the same `/state` node, so the web admin and the app stay in sync.
 */
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

    // Connection mode flags. sdkAuthOk = FirebaseAuth signed in; realtimeOk = websocket alive.
    @Volatile
    private var sdkAuthOk: Boolean = false
    @Volatile
    private var realtimeOk: Boolean = false
    @Volatile
    private var realtimeSnapshotReceived: Boolean = false

    // Tokens used by the REST fallback.
    @Volatile
    private var restIdToken: String? = null
    @Volatile
    private var restRefreshToken: String? = null

    private var connectJob: Job? = null
    private var pollJob: Job? = null
    private var watchdogJob: Job? = null

    fun start() {
        connect()
    }

    /** Allow re-entry from the "เชื่อมต่อใหม่" button or when the app returns to foreground. */
    fun retry() {
        connect()
    }

    fun isConnected(): Boolean = hasReceivedInitialRemote && (realtimeOk || pollJob?.isActive == true)

    private fun connect() {
        connectJob?.cancel()
        // Drop any previous socket/polling before reconnecting so listeners never stack up.
        pollJob?.cancel()
        pollJob = null
        watchdogJob?.cancel()
        realtimeSnapshotReceived = false
        try {
            listener?.let { stateRef.removeEventListener(it) }
        } catch (_: Exception) {
        }
        listener = null

        connectJob = scope.launch {
            var attempt = 0
            while (true) {
                attempt++
                try {
                    _status.value = if (attempt == 1) {
                        "กำลังเชื่อมต่อ Firebase..."
                    } else {
                        "กำลังเชื่อมต่อ Firebase ใหม่ (ครั้งที่ $attempt)..."
                    }
                    if (!hasNetwork()) {
                        throw IllegalStateException("เครื่องนี้ยังไม่ได้ต่ออินเทอร์เน็ต")
                    }
                    val token = authenticate()
                    fetchInitialViaRest(token)
                    startRemoteSync()
                    return@launch
                } catch (e: Exception) {
                    val msg = describe(e)
                    if (attempt >= MAX_CONNECT_ATTEMPTS) {
                        _status.value = "เชื่อมต่อ Firebase ไม่ได้: $msg (กด 'เชื่อมต่อใหม่')"
                        return@launch
                    }
                    _status.value = "เชื่อมต่อ Firebase ไม่ได้: $msg — กำลังลองใหม่ ($attempt/$MAX_CONNECT_ATTEMPTS)"
                    delay(2000L * attempt)
                }
            }
        }
    }

    /**
     * Try the Firebase SDK first; if it fails (no Play services, blocked SDK auth, timeout),
     * fall back to a plain REST anonymous sign-in that only needs HTTPS.
     */
    private suspend fun authenticate(): String {
        sdkAuthOk = false
        var sdkError = "ไม่ทราบสาเหตุ"
        try {
            val authResult = withTimeout(SDK_AUTH_TIMEOUT_MS) {
                FirebaseAuth.getInstance().signInAnonymously().await()
            }
            val token = withTimeout(SDK_AUTH_TIMEOUT_MS) {
                authResult.user?.getIdToken(false)?.await()?.token
            }
            if (!token.isNullOrBlank()) {
                sdkAuthOk = true
                realtimeOk = true
                restIdToken = token
                return token
            }
            sdkError = "Firebase ไม่ส่ง token กลับมา"
        } catch (e: Exception) {
            sdkError = describe(e)
        }

        // Fallback: REST sign-in with the project's Android API key (no Play services needed).
        _status.value = "SDK Firebase ใช้ไม่ได้ ($sdkError) — กำลังใช้โหมดสำรอง..."
        val tokens = restSignIn()
        restIdToken = tokens.first
        restRefreshToken = tokens.second
        return tokens.first
    }

    private fun startRemoteSync() {
        watchdogJob?.cancel()
        if (sdkAuthOk) {
            observeRemote()
            // If the realtime socket never answers, switch to REST polling automatically.
            watchdogJob = scope.launch {
                delay(REALTIME_WATCHDOG_MS)
                if (!realtimeSnapshotReceived) {
                    fallbackToPolling("เรียลทามไม่ตอบสนอง")
                }
            }
        } else {
            fallbackToPolling("โหมดสำรอง")
        }
    }

    private fun fallbackToPolling(reason: String) {
        realtimeOk = false
        try {
            listener?.let { stateRef.removeEventListener(it) }
        } catch (_: Exception) {
        }
        listener = null
        if (pollJob?.isActive == true) return
        _status.value = "เชื่อมต่อ Firebase แล้ว (โหมดสำรอง: $reason)"
        pollJob = scope.launch {
            while (true) {
                try {
                    pollOnce()
                } catch (e: Exception) {
                    _status.value = "การเชื่อมต่อขัดข้อง: ${describe(e)} — กำลังลองใหม่..."
                    try {
                        refreshRestAuth()
                    } catch (_: Exception) {
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun pollOnce() {
        val token = restIdToken ?: restSignIn().let { restIdToken = it.first; restRefreshToken = it.second; it.first }
        val url = "$DATABASE_URL/$ROOT_PATH/state.json?auth=$token"
        val req = Request.Builder().url(url).get().build()
        httpClient.newCall(req).execute().use { resp ->
            if (resp.code == 401 || resp.code == 403) {
                refreshRestAuth()
                return
            }
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body?.string()?.trim() ?: return
            if (body.isEmpty() || body == "null") {
                markRemoteReadyAndStartLocalObserver(pushIfBlank = true)
                return
            }
            val innerJson = if (body.startsWith("\"")) {
                try {
                    stringAdapter.fromJson(body)
                } catch (_: Exception) {
                    null
                }
            } else {
                body
            }
            if (!innerJson.isNullOrBlank()) {
                applyIncomingJson(innerJson)
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
                    try {
                        stringAdapter.fromJson(body)
                    } catch (_: Exception) {
                        null
                    }
                } else {
                    body
                }
                if (!innerJson.isNullOrBlank()) {
                    applyIncomingJson(innerJson)
                }
            }
        } catch (_: Exception) {
            // The realtime listener (or REST polling) below will handle it if REST fails.
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
        val mode = if (realtimeOk) "" else " • โหมดสำรอง"
        _status.value = "เชื่อมต่อ Firebase แล้ว (${state.menuItems.size} เมนู)$mode"
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
        realtimeOk = true
        realtimeSnapshotReceived = false
        val valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                realtimeSnapshotReceived = true
                val json = when (val raw = snapshot.value) {
                    null -> null
                    is String -> raw
                    else -> try {
                        mapAdapter.toJson(raw)
                    } catch (_: Exception) {
                        null
                    }
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
                // Cannot keep the socket: keep working through REST polling instead of giving up.
                fallbackToPolling("เรียลทามขาดการเชื่อมต่อ (${error.code})")
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
            if (realtimeOk && sdkAuthOk) {
                try {
                    stateRef.setValue(json).await()
                    return@launch
                } catch (_: Exception) {
                    // Socket write failed: retry through REST so the shop never loses an edit.
                }
            }
            try {
                restPutState(json)
            } catch (_: Exception) {
                // Keep local data; retry on the next change.
            }
        }
    }

    /** Writes the state string through the Database REST API (fallback path). */
    private fun restPutState(json: String) {
        val token = restIdToken ?: restSignIn().let {
            restIdToken = it.first
            restRefreshToken = it.second
            it.first
        }
        val url = "$DATABASE_URL/$ROOT_PATH/state.json?auth=$token"
        val body = stringAdapter.toJson(json).toRequestBody(JSON_MEDIA_TYPE)
        val req = Request.Builder().url(url).put(body).build()
        httpClient.newCall(req).execute().use { resp ->
            if (resp.code == 401 || resp.code == 403) {
                refreshRestAuth()
                throw IllegalStateException("HTTP ${resp.code} (token refreshed)")
            }
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
        }
    }

    /** Anonymous sign-in through the identity toolkit REST API (no Play services needed). */
    private fun restSignIn(): Pair<String, String?> {
        val body = "{\"returnSecureToken\":true}".toRequestBody(JSON_MEDIA_TYPE)
        val req = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey()}")
            .post(body)
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw IllegalStateException("เข้าสู่ระบบไม่สำเร็จ (HTTP ${resp.code}${apiErrorSuffix(text)})")
            }
            val obj = JSONObject(text)
            val idToken = obj.optString("idToken")
            if (idToken.isBlank()) throw IllegalStateException("เข้าสู่ระบบไม่สำเร็จ (ไม่มี token)")
            return idToken to obj.optString("refreshToken").takeIf { it.isNotBlank() }
        }
    }

    private fun refreshRestAuth() {
        val refresh = restRefreshToken
        if (!refresh.isNullOrBlank()) {
            try {
                val body = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refresh)
                    .build()
                val req = Request.Builder()
                    .url("https://securetoken.googleapis.com/v1/token?key=${apiKey()}")
                    .post(body)
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val text = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        val obj = JSONObject(text)
                        val idToken = obj.optString("id_token")
                        if (idToken.isNotBlank()) {
                            restIdToken = idToken
                            obj.optString("refresh_token").takeIf { it.isNotBlank() }?.let { restRefreshToken = it }
                            return
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        val tokens = restSignIn()
        restIdToken = tokens.first
        restRefreshToken = tokens.second
    }

    private fun apiKey(): String = try {
        context.getString(R.string.google_api_key)
    } catch (_: Exception) {
        ""
    }

    private fun apiErrorSuffix(text: String): String = try {
        val message = JSONObject(text).optJSONObject("error")?.optString("message").orEmpty()
        if (message.isBlank()) "" else ": $message"
    } catch (_: Exception) {
        ""
    }

    private fun hasNetwork(): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }

    private fun describe(e: Exception): String {
        val message = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        return message.take(160)
    }

    fun stop() {
        connectJob?.cancel()
        pollJob?.cancel()
        watchdogJob?.cancel()
        try {
            listener?.let { stateRef.removeEventListener(it) }
        } catch (_: Exception) {
        }
        listener = null
        realtimeOk = false
    }

    companion object {
        private const val DATABASE_URL =
            "https://dinoeater-merchant-default-rtdb.asia-southeast1.firebasedatabase.app"
        private const val ROOT_PATH = "shops/dinoeater-merchant"
        private const val SDK_AUTH_TIMEOUT_MS = 20_000L
        private const val REALTIME_WATCHDOG_MS = 12_000L
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_CONNECT_ATTEMPTS = 5
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
