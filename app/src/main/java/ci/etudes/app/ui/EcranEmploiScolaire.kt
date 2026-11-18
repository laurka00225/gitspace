@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ci.etudes.app.ui

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ci.etudes.app.EtatApp
import ci.etudes.app.LecteurEmploi
import ci.etudes.core.CoursScolaire
import ci.etudes.core.EmploiScolaire
import ci.etudes.core.Matiere
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val JOURS_ECOLE = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

private fun DayOfWeek.nom(): String =
    getDisplayName(TextStyle.FULL, Locale.FRENCH).replaceFirstChar { it.uppercase() }

/** Matières « hors planning » qu'on peut aussi mettre dans l'emploi du temps de l'école. */
private val AUTRES_COURS = listOf("EPS", "Arts / Musique", "Informatique")

/**
 * Import de l'emploi du temps de l'école (PDF, image ou photo), vérification et correction
 * des cours détectés, puis utilisation pour le planning d'études.
 */
@Composable
fun EcranEmploiScolaire(etat: EtatApp, onTerminer: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val r = etat.reglages
    var cours by remember { mutableStateOf(r.emploiScolaire) }
    var utiliser by remember { mutableStateOf(if (r.emploiScolaire.isEmpty()) true else r.utiliserEmploiScolaire) }
    var trajet by remember { mutableStateOf(r.trajetMin) }
    var lecture by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var aModifier by remember { mutableStateOf<CoursScolaire?>(null) }
    var nouveau by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    fun analyser(uri: Uri) {
        lecture = true
        message = null
        scope.launch {
            try {
                val res = LecteurEmploi.lire(context, uri)
                if (res.cours.isEmpty()) {
                    message = if (res.texteLu.isBlank()) {
                        "Aucun texte lisible. Essaie une photo plus nette, bien éclairée et prise de face."
                    } else {
                        "Je n'ai pas retrouvé le tableau (jours en haut ou à gauche, heures sur le côté). " +
                            "Tu peux ajouter les cours à la main."
                    }
                } else {
                    cours = res.cours
                    message = "${res.cours.size} cours trouvés. Vérifie-les et corrige si besoin avant d'enregistrer."
                }
            } catch (e: Exception) {
                message = "Lecture impossible : ${e.message ?: "erreur inconnue"}. " +
                    "À la première utilisation, le téléphone doit être connecté à Internet quelques instants " +
                    "pour installer le module de lecture."
            } finally {
                lecture = false
            }
        }
    }

    val choisirFichier = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) analyser(uri)
    }
    val prendrePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = photoUri
        if (ok && uri != null) analyser(uri)
    }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Importe l'emploi du temps de ta classe : l'appli lit les jours, les heures et les matières, " +
                    "puis place tes études après les cours. Elle te fait relire les cours du jour et préparer ceux du lendemain.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { choisirFichier.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/*")) },
                    enabled = !lecture,
                    modifier = Modifier.weight(1f),
                ) { Text("PDF ou image") }
                FilledTonalButton(
                    onClick = {
                        val dossier = File(context.cacheDir, "photos").apply { mkdirs() }
                        val fichier = File(dossier, "emploi_du_temps.jpg")
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fichiers", fichier)
                        photoUri = uri
                        prendrePhoto.launch(uri)
                    },
                    enabled = !lecture,
                    modifier = Modifier.weight(1f),
                ) { Text("Prendre une photo") }
            }
        }
        if (lecture) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Lecture de l'emploi du temps...")
                }
            }
        }
        message?.let { m ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(m, Modifier.padding(12.dp)) }
            }
        }

        item {
            Text(
                "Cours de la semaine (${cours.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        for (jour in JOURS_ECOLE) {
            val duJour = EmploiScolaire.coursDu(jour, cours)
            if (duJour.isEmpty() && jour == DayOfWeek.SATURDAY) continue
            item(key = "jour-$jour") {
                val plages = EmploiScolaire.plagesLibres(duJour, trajet)
                Column {
                    Text(jour.nom(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (duJour.isEmpty()) {
                            "Aucun cours : horaires d'études par défaut"
                        } else {
                            "Cours ${duJour.first().debut.enHeure()} – ${duJour.maxOf { it.fin }.enHeure()} · études : " +
                                plages.joinToString(", ") { "${it.debut.enHeure()}–${it.fin.enHeure()}" }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(duJour) { c ->
                LigneCours(
                    c, etat.matieres,
                    onClic = { aModifier = c },
                    onSupprimer = { cours = cours - c },
                )
            }
        }
        item {
            OutlinedButton(onClick = { nouveau = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter un cours")
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Utiliser pour mon planning", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Sinon, l'appli garde les horaires par défaut (cours de 7h30 à 17h30).",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(checked = utiliser, onCheckedChange = { utiliser = it })
                    }
                    Text("Trajet entre l'école et la maison", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60).forEach { t ->
                            FilterChip(selected = trajet == t, onClick = { trajet = t }, label = { Text("$t min") })
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    etat.modifierReglages {
                        it.copy(emploiScolaire = cours, utiliserEmploiScolaire = utiliser && cours.isNotEmpty(), trajetMin = trajet)
                    }
                    onTerminer()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer et mettre à jour mon planning") }
        }
        if (cours.isNotEmpty()) {
            item {
                TextButton(onClick = { cours = emptyList() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Effacer tous les cours")
                }
            }
        }
    }

    if (nouveau || aModifier != null) {
        DialogueCours(
            initial = aModifier,
            matieres = etat.matieres,
            onFermer = { nouveau = false; aModifier = null },
            onValider = { c ->
                val ancien = aModifier
                cours = (if (ancien != null) cours - ancien else cours) + c
                nouveau = false
                aModifier = null
            },
        )
    }
}

@Composable
private fun LigneCours(c: CoursScolaire, matieres: List<Matiere>, onClic: () -> Unit, onSupprimer: () -> Unit) {
    val matiere = matieres.firstOrNull { it.id == c.matiereId }
    val couleur = if (matiere != null) couleurDe(matiere) else MaterialTheme.colorScheme.outline
    Card(onClick = onClic, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(couleur))
            Spacer(Modifier.width(12.dp))
            Text("${c.debut.enHeure()} – ${c.fin.enHeure()}", Modifier.width(110.dp), fontWeight = FontWeight.SemiBold)
            Text(matiere?.nom ?: c.libelle, Modifier.weight(1f))
            IconButton(onClick = onSupprimer) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
        }
    }
}

@Composable
private fun DialogueCours(
    initial: CoursScolaire?,
    matieres: List<Matiere>,
    onFermer: () -> Unit,
    onValider: (CoursScolaire) -> Unit,
) {
    val context = LocalContext.current
    var jour by remember { mutableStateOf(initial?.jour ?: DayOfWeek.MONDAY) }
    var debut by remember { mutableStateOf(initial?.debut ?: LocalTime.of(7, 30)) }
    var fin by remember { mutableStateOf(initial?.fin ?: LocalTime.of(8, 25)) }
    var matiereId by remember { mutableStateOf(initial?.matiereId) }
    var libelle by remember { mutableStateOf(initial?.libelle ?: matieres.firstOrNull()?.nom ?: "") }
    if (initial == null && matiereId == null && matieres.isNotEmpty() && libelle == matieres.first().nom) {
        matiereId = matieres.first().id
    }

    fun heure(initiale: LocalTime, ok: (LocalTime) -> Unit) {
        TimePickerDialog(context, { _, h, m -> ok(LocalTime.of(h, m)) }, initiale.hour, initiale.minute, true).show()
    }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (initial == null) "Ajouter un cours" else "Modifier le cours") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jour", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JOURS_ECOLE.forEach { j ->
                        FilterChip(selected = j == jour, onClick = { jour = j }, label = { Text(j.nom().take(3)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { heure(debut) { debut = it; if (!fin.isAfter(it)) fin = it.plusMinutes(55) } }, modifier = Modifier.weight(1f)) {
                        Text("De ${debut.enHeure()}")
                    }
                    OutlinedButton(onClick = { heure(fin) { if (it.isAfter(debut)) fin = it } }, modifier = Modifier.weight(1f)) {
                        Text("À ${fin.enHeure()}")
                    }
                }
                Text("Matière", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    matieres.forEach { m ->
                        FilterChip(
                            selected = matiereId == m.id,
                            onClick = { matiereId = m.id; libelle = m.nom },
                            label = { Text(m.nom) },
                        )
                    }
                    AUTRES_COURS.forEach { a ->
                        FilterChip(
                            selected = matiereId == null && libelle == a,
                            onClick = { matiereId = null; libelle = a },
                            label = { Text(a) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = fin.isAfter(debut) && libelle.isNotBlank(),
                onClick = { onValider(CoursScolaire(jour, debut, fin, matiereId, libelle)) },
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}
