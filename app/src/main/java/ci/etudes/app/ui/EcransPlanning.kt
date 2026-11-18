@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ci.etudes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ci.etudes.app.EtatApp
import ci.etudes.core.JourPlan
import ci.etudes.core.MatiereExercice
import ci.etudes.core.Progres
import ci.etudes.core.Planificateur
import ci.etudes.core.Seance
import ci.etudes.core.TypeJour
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

/** Heure actuelle, rafraîchie chaque minute. */
@Composable
private fun maintenant(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }
    return now
}

@Composable
fun EcranAujourdhui(etat: EtatApp, nav: Navigation, modifier: Modifier) {
    val now = maintenant()
    val aujourdhui = now.toLocalDate()
    val jour = etat.jour(aujourdhui)
    val heure = now.toLocalTime()
    var aChanger by remember { mutableStateOf<Seance?>(null) }

    LazyColumn(
        modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(aujourdhui.enLong(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(jour.type.libelle + (jour.evenement?.let { " · $it" } ?: ""))
                    val faites = jour.seances.count { it.cle in etat.faites }
                    if (jour.seances.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { faites.toFloat() / jour.seances.size },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        Text("$faites / ${jour.seances.size} séances faites", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { CarteProgres(etat, aujourdhui) }
        val proches = etat.evaluations.filter {
            !it.date.isBefore(aujourdhui) && !it.date.isAfter(aujourdhui.plusDays(Planificateur.JOURS_PREPARATION))
        }
        if (proches.isNotEmpty()) {
            item {
                val noms = etat.matieres.associate { it.id to it.nom }
                Card(
                    onClick = nav.ouvrirDevoirs,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("À préparer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        proches.forEach { e ->
                            val jours = java.time.temporal.ChronoUnit.DAYS.between(aujourdhui, e.date)
                            val quand = when (jours) {
                                0L -> "aujourd'hui"
                                1L -> "demain"
                                else -> "dans $jours jours"
                            }
                            Text("• ${e.type.libelle} de ${noms[e.matiereId] ?: "?"} $quand")
                        }
                    }
                }
            }
        }
        item {
            val message = when {
                !heure.isBefore(Planificateur.HEURE_COUCHER) -> "Il est 22h passées : au lit ! Bonne nuit."
                jour.type == TypeJour.COURS && heure.isBefore(java.time.LocalTime.of(17, 30)) ->
                    "Bonne journée de cours ! Tes études commencent ce soir."
                else -> {
                    val prochaine = jour.seances.firstOrNull { !it.fin.isBefore(heure) }
                    when {
                        prochaine == null -> "Plus de séance aujourd'hui. Repose-toi bien."
                        prochaine.debut.isAfter(heure) ->
                            "Prochaine séance à ${prochaine.debut.enHeure()} : ${prochaine.matiere.nom}."
                        else -> "En ce moment : ${prochaine.matiere.nom} jusqu'à ${prochaine.fin.enHeure()}."
                    }
                }
            }
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
        if (jour.seances.isEmpty()) {
            item { Text("Aucune séance prévue aujourd'hui. Ajoute des plages dans Réglages.") }
        }
        items(jour.seances, key = { it.cle }) { s ->
            val enCours = !heure.isBefore(s.debut) && heure.isBefore(s.fin)
            val exo = MatiereExercice.deId(s.matiere.id)
            LigneSeance(
                seance = s,
                faite = s.cle in etat.faites,
                enCours = enCours,
                onCocher = { etat.basculerFaite(s.cle) },
                onClic = { aChanger = s },
                actions = {
                    TextButton(onClick = { nav.ouvrirMinuteur(s) }) { Text("▶ Minuteur") }
                    if (exo != null) {
                        TextButton(onClick = { nav.ouvrirExercices(exo) }) { Text("✎ S'entraîner") }
                    }
                },
            )
        }
    }
    aChanger?.let { s -> DialogueChangerMatiere(etat, s, onFermer = { aChanger = null }) }
}

@Composable
fun EcranSemaine(etat: EtatApp, @Suppress("UNUSED_PARAMETER") nav: Navigation, modifier: Modifier) {
    val aujourdhui = LocalDate.now()
    var lundi by remember { mutableStateOf(Planificateur.lundiDe(aujourdhui)) }
    val plan = etat.plan(lundi)
    var aChanger by remember { mutableStateOf<Seance?>(null) }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { lundi = lundi.minusWeeks(1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Semaine précédente")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Du ${lundi.enCourt()} au ${lundi.plusDays(6).enCourt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (lundi != Planificateur.lundiDe(aujourdhui)) {
                    TextButton(onClick = { lundi = Planificateur.lundiDe(aujourdhui) }) { Text("Revenir à cette semaine") }
                }
            }
            IconButton(onClick = { lundi = lundi.plusWeeks(1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Semaine suivante")
            }
        }
        HorizontalDivider()
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            plan.jours.forEach { jour ->
                item(key = "titre-${jour.date}") { TitreJour(jour, estAujourdhui = jour.date == aujourdhui) }
                if (jour.seances.isEmpty()) {
                    item(key = "vide-${jour.date}") { Text("Pas de séance", style = MaterialTheme.typography.bodySmall) }
                }
                items(jour.seances, key = { it.cle }) { s ->
                    LigneSeance(
                        seance = s,
                        faite = s.cle in etat.faites,
                        enCours = false,
                        onCocher = { etat.basculerFaite(s.cle) },
                        onClic = { aChanger = s },
                    )
                }
            }
            item(key = "bilan") { BilanSemaine(plan.minutesParMatiere()) }
        }
    }
    aChanger?.let { s -> DialogueChangerMatiere(etat, s, onFermer = { aChanger = null }) }
}

@Composable
private fun TitreJour(jour: JourPlan, estAujourdhui: Boolean) {
    val total = jour.seances.sumOf { java.time.Duration.between(it.debut, it.fin).toMinutes() }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                jour.date.enLong() + if (estAujourdhui) " (aujourd'hui)" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (estAujourdhui) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                jour.type.court() + (jour.evenement?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (jour.evenement != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(dureeTexte(total), style = MaterialTheme.typography.labelLarge)
    }
}

fun dureeTexte(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "${h}h"
        else -> "${h}h${m.toString().padStart(2, '0')}"
    }
}

@Composable
fun LigneSeance(
    seance: Seance,
    faite: Boolean,
    enCours: Boolean,
    onCocher: () -> Unit,
    onClic: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val couleur = couleurDe(seance.matiere)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClic),
        colors = CardDefaults.cardColors(
            containerColor = if (enCours) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(couleur))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).width(64.dp)) {
                Text(seance.debut.enHeure(), fontWeight = FontWeight.Bold)
                Text(seance.fin.enHeure(), style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(
                    seance.matiere.nom,
                    textDecoration = if (faite) TextDecoration.LineThrough else null,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (enCours) "En cours · ${seance.activite}" else seance.activite,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (actions != null) {
                    Row(Modifier.padding(top = 2.dp), content = actions)
                }
            }
            Checkbox(checked = faite, onCheckedChange = { onCocher() })
        }
    }
}

@Composable
private fun BilanSemaine(minutes: Map<ci.etudes.core.Matiere, Long>) {
    if (minutes.isEmpty()) return
    val max = minutes.values.max().coerceAtLeast(1)
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Temps d'études de la semaine : ${dureeTexte(minutes.values.sum())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            minutes.entries.sortedByDescending { it.value }.forEach { (m, min) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.nom, Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f).height(10.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth(min.toFloat() / max)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(couleurDe(m)),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(dureeTexte(min), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(52.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogueChangerMatiere(etat: EtatApp, seance: Seance, onFermer: () -> Unit) {
    val actives = etat.matieres.filter { it.active }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("${seance.debut.enHeure()} – ${seance.fin.enHeure()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Choisis la matière de cette séance :", Modifier.padding(bottom = 8.dp))
                actives.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                etat.changerMatiereSeance(seance.cle, m.id)
                                onFermer()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(couleurDe(m)))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            m.nom,
                            fontWeight = if (m.id == seance.matiere.id) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text("Fermer") }
        },
        dismissButton = {
            if (seance.cle in etat.changements) {
                TextButton(onClick = {
                    etat.changerMatiereSeance(seance.cle, null)
                    onFermer()
                }) { Text("Matière proposée") }
            }
        },
    )
}

@Composable
private fun CarteProgres(etat: EtatApp, aujourdhui: LocalDate) {
    val serie = Progres.serieEnCours(etat.faites, aujourdhui)
    val record = Progres.meilleureSerie(etat.faites)
    val (faites, prevues) = Progres.tauxRealisation(etat.plan(Planificateur.lundiDe(aujourdhui)), etat.faites, aujourdhui)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tuile(
            Modifier.weight(1f),
            valeur = "🔥 $serie",
            libelle = if (serie > 1) "jours d'affilée" else "jour d'affilée",
            detail = "Record : $record",
        )
        Tuile(
            Modifier.weight(1f),
            valeur = if (prevues == 0) "—" else "${faites * 100 / prevues} %",
            libelle = "de la semaine",
            detail = "$faites / $prevues séances",
        )
        Tuile(
            Modifier.weight(1f),
            valeur = "⭐ ${etat.seriesExercices}",
            libelle = "exercices",
            detail = "séries faites",
        )
    }
}

@Composable
private fun Tuile(modifier: Modifier, valeur: String, libelle: String, detail: String) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(valeur, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(libelle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
