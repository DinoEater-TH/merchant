package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Locale

/**
 * Small helper around the platform LocationManager so the customer app can attach
 * a real GPS coordinate to a delivery order without pulling in Google Play Services.
 */
object LocationHelper {

    private const val TIMEOUT_MS = 10_000L
    private val PROVIDERS = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(context: Context, onResult: (String?) -> Unit) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            onResult(null)
            return
        }

        val enabledProviders = PROVIDERS.filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        val provider = enabledProviders.firstOrNull { it == LocationManager.GPS_PROVIDER }
            ?: enabledProviders.firstOrNull()

        if (provider == null) {
            onResult(lastKnown(manager)?.toCoordinate())
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                onResult((location ?: lastKnown(manager))?.toCoordinate())
            }
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var finished = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (finished) return
                finished = true
                manager.removeUpdates(this)
                handler.removeCallbacksAndMessages(null)
                onResult(location.toCoordinate())
            }

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        handler.postDelayed({
            if (!finished) {
                finished = true
                manager.removeUpdates(listener)
                onResult(lastKnown(manager)?.toCoordinate())
            }
        }, TIMEOUT_MS)

        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (_: SecurityException) {
            finished = true
            handler.removeCallbacksAndMessages(null)
            onResult(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        return PROVIDERS
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun Location.toCoordinate(): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
}
