package app.transitos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.transitos.core.design.theme.TransitOSTheme
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TransitOSTheme {
                // Binds the application's Koin instance to this Compose tree so
                // `koinViewModel()` / `koinInject()` resolve explicitly rather
                // than silently through GlobalContext (which logs a warning on
                // every access and breaks testability).
                KoinAndroidContext {
                    TransitOSApp()
                }
            }
        }
    }
}
