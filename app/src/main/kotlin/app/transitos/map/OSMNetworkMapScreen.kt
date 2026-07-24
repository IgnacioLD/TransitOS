package app.transitos.map

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OSMNetworkMapRoute(
    onBack: () -> Unit,
    onOpenPdf: () -> Unit,
) {
    val context = LocalContext.current

    val jsBridge = remember {
        object {
            @JavascriptInterface
            @Suppress("unused")
            fun onStationTap(name: String) {
                // station tap handled via Leaflet popup — no extra action needed
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa Metrovalencia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPdf) {
                        Icon(Icons.Outlined.Map, contentDescription = "Plano PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        domStorageEnabled = true
                    }
                    addJavascriptInterface(jsBridge, "AndroidBridge")
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            return false
                        }
                    }
                    loadUrl("file:///android_asset/mapa_interactivo.html")
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}
