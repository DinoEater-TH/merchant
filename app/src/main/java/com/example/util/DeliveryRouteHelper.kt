package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Resolves the real driving distance between the shop and a delivery point
 * using the public OSRM routing service (OpenStreetMap road network).
 */
object DeliveryRouteHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class Coordinate(val latitude: Double, val longitude: Double)

    fun parseCoordinate(raw: String?): Coordinate? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(",").map { it.trim() }
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return Coordinate(lat, lng)
    }

    /**
     * @return driving distance in kilometres, or null when the route cannot be resolved.
     */
    suspend fun drivingDistanceKm(
        from: Coordinate,
        to: Coordinate
    ): Double? = withContext(Dispatchers.IO) {
        val url = String.format(
            Locale.US,
            "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=false",
            from.longitude, from.latitude, to.longitude, to.latitude
        )
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DinoEater-Merchant/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val routes = json.optJSONArray("routes") ?: return@withContext null
                if (routes.length() == 0) return@withContext null
                val meters = routes.getJSONObject(0).optDouble("distance", Double.NaN)
                if (meters.isNaN() || meters <= 0.0) null else meters / 1000.0
            }
        } catch (_: Exception) {
            null
        }
    }
}
