package com.example.ui.components

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val LONGDO_KEY = "45f4bedb69da4ebbcdacd2ac7c4ac724"

/**
 * A dialog that embeds a Longdo Map (via WebView) so the admin can tap on the
 * map to pick a shop location. The selected coordinates are delivered through
 * [onLocationSelected].
 */
@Composable
fun LongdoMapPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lat = initialLat ?: 16.4421
    val lng = initialLng ?: 102.8358
    val html = remember(lat, lng) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <script src="https://api.longdo.com/map3/?key=$LONGDO_KEY"></script>
        <style>html,body,#map{margin:0;padding:0;width:100%;height:100%;}</style>
        </head><body>
        <div id="map"></div>
        <script>
          let map, marker;
          window.onload = () => {
            map = new longdo.Map({
              placeholder: document.getElementById('map'),
              layer: [longdo.Layers.LIGHT],
              ui: longdo.UiComponent.Mobile,
              lastView: false
            });
            map.Event.bind('ready', () => {
              try { map.zoom(15); map.location({ lon: $lng, lat: $lat }, true); } catch (e) {}
            });
            map.Event.bind('click', e => {
              if (e && e.lon != null && e.lat != null) {
                if (marker) try { map.Overlays.remove(marker); } catch (e) {}
                marker = new longdo.Marker({ lat: e.lat, lon: e.lon }, { title: 'ร้าน' });
                map.Overlays.add(marker);
                if (window.AndroidBridge) window.AndroidBridge.onLocationSelected(e.lat, e.lon);
              }
            });
          };
        </script>
        </body></html>
        """.trimIndent()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ปักหมุดตำแหน่งร้านบน Longdo Map") },
        text = {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onLocationSelected(lat: Double, lng: Double) {
                                onLocationSelected(lat, lng)
                            }
                        }, "AndroidBridge")
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://map.longdo.com", html, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(420.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ปิด") }
        }
    )
}