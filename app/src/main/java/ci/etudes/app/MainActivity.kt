package ci.etudes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import ci.etudes.app.ui.App
import ci.etudes.app.ui.ThemeEtudes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stockage = Stockage(applicationContext)
        setContent {
            val etat = remember { EtatApp(stockage) }
            ThemeEtudes {
                App(etat)
            }
        }
    }
}
