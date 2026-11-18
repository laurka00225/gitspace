@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ci.etudes.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ci.etudes.app.EtatApp
import ci.etudes.app.Rappels
import ci.etudes.core.MatiereExercice
import ci.etudes.core.Seance
import ci.etudes.core.Niveau
import ci.etudes.core.Serie

private enum class Onglet(val titre: String, val icone: ImageVector) {
    AUJOURDHUI("Aujourd'hui", Icons.Filled.Home),
    SEMAINE("Semaine", Icons.Filled.DateRange),
    EXERCICES("Exercices", Icons.Filled.Star),
    DEVOIRS("Devoirs", Icons.Filled.Edit),
    REGLAGES("Réglages", Icons.Filled.Settings),
}

/** Actions de navigation partagées par les écrans. */
class Navigation(
    val ouvrirExercices: (MatiereExercice) -> Unit,
    val ouvrirMinuteur: (Seance) -> Unit,
    val ouvrirMatieres: () -> Unit,
    val ouvrirEmploi: () -> Unit,
    val ouvrirDevoirs: () -> Unit,
)

@Composable
fun App(etat: EtatApp) {
    if (!etat.configure) {
        EcranAccueil(onValider = { n, s -> etat.demarrer(n, s) })
        return
    }
    val context = LocalContext.current
    var onglet by rememberSaveable { mutableStateOf(Onglet.AUJOURDHUI) }
    var matieresOuvertes by rememberSaveable { mutableStateOf(false) }
    var emploiOuvert by rememberSaveable { mutableStateOf(false) }
    var quizMatiere by remember { mutableStateOf<MatiereExercice?>(null) }
    var minuteur by remember { mutableStateOf<Seance?>(null) }

    // Demande l'autorisation d'afficher les notifications (Android 13 et plus).
    val demandeNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Rappels.planifierProchain(context)
    }
    LaunchedEffect(Unit) {
        if (etat.rappels.actif && !Rappels.peutNotifier(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            demandeNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val nav = Navigation(
        ouvrirExercices = { m -> quizMatiere = m; onglet = Onglet.EXERCICES },
        ouvrirMinuteur = { s -> minuteur = s },
        ouvrirMatieres = { matieresOuvertes = true },
        ouvrirEmploi = { emploiOuvert = true },
        ouvrirDevoirs = { onglet = Onglet.DEVOIRS },
    )

    BackHandler(enabled = emploiOuvert || matieresOuvertes || quizMatiere != null || onglet != Onglet.AUJOURDHUI) {
        when {
            emploiOuvert -> emploiOuvert = false
            matieresOuvertes -> matieresOuvertes = false
            quizMatiere != null -> quizMatiere = null
            else -> onglet = Onglet.AUJOURDHUI
        }
    }

    val titre = when {
        emploiOuvert -> "Emploi du temps de l'école"
        matieresOuvertes -> "Mes matières"
        quizMatiere != null -> quizMatiere!!.nom
        else -> onglet.titre
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titre, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (emploiOuvert || matieresOuvertes || quizMatiere != null) {
                        IconButton(onClick = {
                            when {
                                emploiOuvert -> emploiOuvert = false
                                matieresOuvertes -> matieresOuvertes = false
                                else -> quizMatiere = null
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Onglet.entries.forEach { o ->
                    NavigationBarItem(
                        selected = o == onglet && !matieresOuvertes && !emploiOuvert,
                        onClick = {
                            onglet = o
                            matieresOuvertes = false
                            emploiOuvert = false
                            if (o != Onglet.EXERCICES) quizMatiere = null
                        },
                        icon = { Icon(o.icone, contentDescription = null) },
                        label = { Text(o.titre, maxLines = 1) },
                    )
                }
            }
        },
    ) { marges ->
        val modifier = Modifier.padding(marges).fillMaxSize()
        when {
            emploiOuvert -> EcranEmploiScolaire(etat, onTerminer = { emploiOuvert = false; onglet = Onglet.SEMAINE }, modifier = modifier)
            matieresOuvertes -> EcranMatieres(etat, modifier)
            else -> when (onglet) {
                Onglet.AUJOURDHUI -> EcranAujourdhui(etat, nav, modifier)
                Onglet.SEMAINE -> EcranSemaine(etat, nav, modifier)
                Onglet.EXERCICES -> EcranExercices(
                    etat, quizMatiere, onChoisir = { quizMatiere = it }, onTerminer = { quizMatiere = null }, modifier = modifier,
                )
                Onglet.DEVOIRS -> EcranDevoirs(etat, modifier)
                Onglet.REGLAGES -> EcranReglages(etat, nav, modifier)
            }
        }
    }
    minuteur?.let { s ->
        EcranMinuteur(etat, s, onFermer = { minuteur = null })
    }
}

/** Premier lancement : l'élève choisit sa classe (et sa série au lycée). */
@Composable
private fun EcranAccueil(onValider: (Niveau, Serie?) -> Unit) {
    var niveau by rememberSaveable { mutableStateOf<Niveau?>(null) }
    var serie by rememberSaveable { mutableStateOf<Serie?>(null) }
    Scaffold { marges ->
        Column(
            Modifier
                .padding(marges)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Bienvenue !", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "Cette application prépare ton emploi du temps d'études : le soir après les cours " +
                    "(17h30 – 22h), le mercredi après-midi, le week-end et les jours fériés.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("En quelle classe es-tu ?", style = MaterialTheme.typography.titleMedium)
            ChoixClasse(niveau, serie, onChange = { n, s -> niveau = n; serie = s })
            Spacer(Modifier.height(8.dp))
            val n = niveau
            Button(
                onClick = { if (n != null) onValider(n, serie) },
                enabled = n != null && (n.series.isEmpty() || serie != null),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Créer mon emploi du temps") }
        }
    }
}

@Composable
fun ChoixClasse(niveau: Niveau?, serie: Serie?, onChange: (Niveau, Serie?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Niveau.entries.forEach { n ->
                FilterChip(
                    selected = n == niveau,
                    onClick = {
                        val s = if (serie != null && serie in n.series) serie else n.series.firstOrNull()
                        onChange(n, s)
                    },
                    label = { Text(n.libelle) },
                )
            }
        }
        if (niveau != null && niveau.series.isNotEmpty()) {
            Text("Série", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                niveau.series.forEach { s ->
                    FilterChip(
                        selected = s == serie,
                        onClick = { onChange(niveau, s) },
                        label = { Text(s.libelle) },
                    )
                }
            }
        }
    }
}
