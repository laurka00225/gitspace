package ci.etudes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import ci.etudes.app.ui.App
import ci.etudes.app.ui.ThemeEtudes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val contexte = applicationContext
        val stockage = Stockage(contexte)
        Rappels.creerCanaux(contexte)
        Rappels.planifierProchain(contexte)
        setContent {
            val etat = remember { EtatApp(stockage) { Rappels.planifierProchain(contexte) } }
            ThemeEtudes(couleursTelephone = etat.couleursTelephone) {
                App(etat)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Les permissions ont pu changer dans les paramètres du téléphone.
        Rappels.planifierProchain(applicationContext)
    }
}
