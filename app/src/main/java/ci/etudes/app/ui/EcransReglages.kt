@file:OptIn(ExperimentalLayoutApi::class)

package ci.etudes.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ci.etudes.app.EtatApp
import ci.etudes.app.Rappels
import ci.etudes.core.JourLibrePerso
import ci.etudes.core.JoursFeries
import ci.etudes.core.Matiere
import ci.etudes.core.Plage
import ci.etudes.core.Planificateur
import ci.etudes.core.Reglages
import ci.etudes.core.TypeJour
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------------------------------------------------------------- Matières

@Composable
fun EcranMatieres(etat: EtatApp, modifier: Modifier) {
    var ajout by remember { mutableStateOf(false) }
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Plus la priorité est haute, plus la matière a de séances dans la semaine. " +
                    "Désactive une matière que tu n'as pas.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(etat.matieres, key = { it.id }) { m -> CarteMatiere(etat, m) }
        item {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ajout = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter")
                }
                OutlinedButton(onClick = { etat.retablirMatieres() }) { Text("Matières de ma classe") }
            }
        }
    }
    if (ajout) DialogueAjoutMatiere(onFermer = { ajout = false }, onAjouter = { nom, p -> etat.ajouterMatiere(nom, p) })
}

@Composable
private fun CarteMatiere(etat: EtatApp, m: Matiere) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(couleurDe(m)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { etat.modifierMatiere(m.copy(priorite = (m.priorite - 1).coerceAtLeast(1))) },
                        enabled = m.active && m.priorite > 1,
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text("Priorité ${m.priorite}/5")
                    TextButton(
                        onClick = { etat.modifierMatiere(m.copy(priorite = (m.priorite + 1).coerceAtMost(5))) },
                        enabled = m.active && m.priorite < 5,
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
            }
            if (m.personnalisee) {
                IconButton(onClick = { etat.supprimerMatiere(m) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                }
            }
            Switch(checked = m.active, onCheckedChange = { etat.modifierMatiere(m.copy(active = it)) })
        }
    }
}

@Composable
private fun DialogueAjoutMatiere(onFermer: () -> Unit, onAjouter: (String, Int) -> Unit) {
    var nom by remember { mutableStateOf("") }
    var priorite by remember { mutableStateOf(3) }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Nouvelle matière") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, singleLine = true)
                Text("Priorité")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { p ->
                        FilterChip(selected = p == priorite, onClick = { priorite = p }, label = { Text("$p") })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAjouter(nom, priorite); onFermer() },
                enabled = nom.isNotBlank(),
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}

// ---------------------------------------------------------------- Réglages

/** Fenêtre dans laquelle une plage d'un type de journée doit se trouver. */
private fun limites(type: TypeJour): Pair<LocalTime, LocalTime> = when (type) {
    TypeJour.COURS -> LocalTime.of(17, 30) to LocalTime.of(22, 0)
    TypeJour.MERCREDI -> LocalTime.of(12, 30) to LocalTime.of(22, 0)
    TypeJour.LIBRE -> LocalTime.of(6, 0) to LocalTime.of(22, 0)
}

private fun Reglages.avecPlages(type: TypeJour, plages: List<Plage>): Reglages {
    val triees = plages.sortedBy { it.debut }
    return when (type) {
        TypeJour.COURS -> copy(plagesCours = triees)
        TypeJour.MERCREDI -> copy(plagesMercredi = triees)
        TypeJour.LIBRE -> copy(plagesLibre = triees)
    }
}

@Composable
fun EcranReglages(etat: EtatApp, nav: Navigation, modifier: Modifier) {
    val r = etat.reglages
    var confirmerRaz by remember { mutableStateOf(false) }
    var ajoutJourLibre by remember { mutableStateOf(false) }

    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Section("Ma classe") }
        item {
            ChoixClasse(r.niveau, r.serie, onChange = { n, s -> etat.changerClasse(n, s) })
        }

        item {
            OutlinedButton(onClick = nav.ouvrirMatieres, modifier = Modifier.fillMaxWidth()) {
                Text("Mes matières et leurs priorités (${etat.matieres.count { it.active }} actives)")
            }
        }

        item {
            FilledTonalButton(onClick = nav.ouvrirEmploi, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (r.emploiScolaire.isEmpty()) "Importer l'emploi du temps de l'école (PDF, photo)"
                    else "Emploi du temps de l'école (${r.emploiScolaire.size} cours${if (r.utiliserEmploiScolaire) ", utilisé" else ""})",
                )
            }
        }

        item { Section("Rappels et alarmes") }
        item { ReglagesDesRappels(etat) }

        item { Section("Apparence") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Couleurs du téléphone")
                    Text(
                        "Utilise les couleurs de ton fond d'écran (Android 12 et plus).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = etat.couleursTelephone,
                    onCheckedChange = { etat.changerCouleursTelephone(it) },
                    enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                )
            }
        }

        item { Section("Séances") }
        item {
          Column {
            Text("Durée d'une séance")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60, 90).forEach { d ->
                    FilterChip(
                        selected = r.dureeSeanceMin == d,
                        onClick = { etat.modifierReglages { it.copy(dureeSeanceMin = d) } },
                        label = { Text("$d min") },
                    )
                }
            }
            Text("Pause entre deux séances", Modifier.padding(top = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { p ->
                    FilterChip(
                        selected = r.pauseMin == p,
                        onClick = { etat.modifierReglages { it.copy(pauseMin = p) } },
                        label = { Text("$p min") },
                    )
                }
            }
          }
        }

        item { Section("Heures d'études") }
        item {
            Text(
                "Cours du lundi au vendredi de 7h30 à 17h30, pas de cours le mercredi après-midi, " +
                    "coucher à 22h. Adapte les plages à ton rythme (repas, trajet...).",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { EditeurPlages(etat, TypeJour.COURS, "Lundi, mardi, jeudi, vendredi") }
        item { EditeurPlages(etat, TypeJour.MERCREDI, "Mercredi") }
        item { EditeurPlages(etat, TypeJour.LIBRE, "Samedi, dimanche, jours fériés et vacances") }

        item { Section("Vacances et jours libres") }
        if (r.joursLibresPerso.isEmpty()) {
            item { Text("Aucun. Ajoute tes vacances scolaires ou un congé exceptionnel.", style = MaterialTheme.typography.bodySmall) }
        }
        items(r.joursLibresPerso, key = { it.id }) { j ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(j.nom, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (j.debut == j.fin) j.debut.enLong() else "Du ${j.debut.enCourt()} au ${j.fin.enCourt()} ${j.fin.year}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = {
                    etat.modifierReglages { re -> re.copy(joursLibresPerso = re.joursLibresPerso.filterNot { it.id == j.id }) }
                }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
            }
        }
        item {
            OutlinedButton(onClick = { ajoutJourLibre = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter des jours libres")
            }
        }

        item { Section("Jours fériés (Côte d'Ivoire)") }
        item {
            Text(
                "Un jour férié devient une journée libre. Les dates des fêtes musulmanes sont estimées : " +
                    "désactive-les ou ajoute un jour libre si la date officielle est différente.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val debut = LocalDate.now()
        items(JoursFeries.entre(debut, debut.plusMonths(12)), key = { "ferie-${it.date}-${it.nom}" }) { f ->
            val cle = f.date.toString()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.nom + if (f.estime) " (date estimée)" else "")
                    Text(f.date.enLong() + " ${f.date.year}", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = cle !in r.feriesIgnores,
                    onCheckedChange = { actif ->
                        etat.modifierReglages {
                            it.copy(feriesIgnores = if (actif) it.feriesIgnores - cle else it.feriesIgnores + cle)
                        }
                    },
                )
            }
        }

        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item {
            OutlinedButton(onClick = { confirmerRaz = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Tout réinitialiser")
            }
        }
    }

    if (ajoutJourLibre) {
        DialogueJourLibre(
            onFermer = { ajoutJourLibre = false },
            onAjouter = { j -> etat.modifierReglages { it.copy(joursLibresPerso = (it.joursLibresPerso + j).sortedBy { x -> x.debut }) } },
        )
    }
    if (confirmerRaz) {
        AlertDialog(
            onDismissRequest = { confirmerRaz = false },
            title = { Text("Tout réinitialiser ?") },
            text = { Text("Ta classe, tes matières, tes plages et les séances cochées seront effacées.") },
            confirmButton = {
                TextButton(onClick = { confirmerRaz = false; etat.toutReinitialiser() }) { Text("Effacer") }
            },
            dismissButton = { TextButton(onClick = { confirmerRaz = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Section(titre: String) {
    Text(
        titre,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EditeurPlages(etat: EtatApp, type: TypeJour, titre: String) {
    val context = LocalContext.current
    val plages = etat.reglages.plages(type)
    val (min, max) = limites(type)

    /** Demande l'heure de début puis l'heure de fin, puis appelle [ok]. */
    fun choisirPlage(initiale: Plage?, ok: (Plage) -> Unit) {
        val autres = plages.filter { it != initiale }
        val d0 = initiale?.debut ?: min
        TimePickerDialog(context, { _, hd, md ->
            val debut = LocalTime.of(hd, md)
            val f0 = initiale?.fin ?: debut.plusHours(1).let { if (it.isAfter(max) || it.isBefore(debut)) max else it }
            TimePickerDialog(context, { _, hf, mf ->
                val fin = LocalTime.of(hf, mf)
                when {
                    !fin.isAfter(debut) ->
                        Toast.makeText(context, "La fin doit être après le début.", Toast.LENGTH_LONG).show()
                    debut.isBefore(min) || fin.isAfter(max) ->
                        Toast.makeText(
                            context,
                            "Pour ce jour, choisis entre ${min.enHeure()} et ${max.enHeure()}.",
                            Toast.LENGTH_LONG,
                        ).show()
                    Planificateur.chevauche(Plage(debut, fin), autres) ->
                        Toast.makeText(context, "Cette plage recouvre une autre plage.", Toast.LENGTH_LONG).show()
                    else -> ok(Plage(debut, fin))
                }
            }, f0.hour, f0.minute, true).apply { setTitle("Fin de la plage") }.show()
        }, d0.hour, d0.minute, true).apply { setTitle("Début de la plage") }.show()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val total = plages.sumOf { it.minutes }
            Text("Disponible : ${dureeTexte(total)}", style = MaterialTheme.typography.bodySmall)
            plages.forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        choisirPlage(p) { nouvelle ->
                            etat.modifierReglages { it.avecPlages(type, plages.toMutableList().apply { set(i, nouvelle) }) }
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("${p.debut.enHeure()} – ${p.fin.enHeure()}", Modifier.fillMaxWidth())
                    }
                    IconButton(onClick = {
                        etat.modifierReglages { it.avecPlages(type, plages.filterIndexed { j, _ -> j != i }) }
                    }) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer la plage") }
                }
            }
            TextButton(onClick = {
                choisirPlage(null) { nouvelle -> etat.modifierReglages { it.avecPlages(type, plages + nouvelle) } }
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ajouter une plage")
            }
        }
    }
}

@Composable
private fun DialogueJourLibre(onFermer: () -> Unit, onAjouter: (JourLibrePerso) -> Unit) {
    val context = LocalContext.current
    var nom by remember { mutableStateOf("Vacances") }
    var debut by remember { mutableStateOf(LocalDate.now()) }
    var fin by remember { mutableStateOf(LocalDate.now()) }

    fun choisirDate(initiale: LocalDate, ok: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, a, m, j -> ok(LocalDate.of(a, m + 1, j)) },
            initiale.year, initiale.monthValue - 1, initiale.dayOfMonth,
        ).show()
    }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Jours libres") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, singleLine = true)
                OutlinedButton(onClick = {
                    choisirDate(debut) { d -> debut = d; if (fin.isBefore(d)) fin = d }
                }, modifier = Modifier.fillMaxWidth()) { Text("Du : ${debut.enLong()}") }
                OutlinedButton(onClick = {
                    choisirDate(fin) { d -> fin = if (d.isBefore(debut)) debut else d }
                }, modifier = Modifier.fillMaxWidth()) { Text("Au : ${fin.enLong()}") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAjouter(JourLibrePerso("libre_" + System.currentTimeMillis(), nom.trim(), debut, fin))
                    onFermer()
                },
                enabled = nom.isNotBlank(),
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}

@Composable
private fun ReglagesDesRappels(etat: EtatApp) {
    val context = LocalContext.current
    val r = etat.rappels
    var peutNotifier by remember { mutableStateOf(Rappels.peutNotifier(context)) }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        peutNotifier = ok
        Rappels.planifierProchain(context)
    }
    val prochain = remember(r, etat.reglages, etat.evaluations, etat.changements) {
        Rappels.aVenir(context, LocalDateTime.now(), limite = 1).firstOrNull()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LigneInterrupteur("Rappel avant chaque séance", r.actif) {
                etat.modifierRappels(r.copy(actif = it))
                if (it && !peutNotifier && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    demande.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (r.actif) {
                Text("Prévenir", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { m ->
                        FilterChip(
                            selected = r.minutesAvant == m,
                            onClick = { etat.modifierRappels(r.copy(minutesAvant = m)) },
                            label = { Text(if (m == 0) "À l'heure" else "$m min avant") },
                        )
                    }
                }
                LigneInterrupteur("Sonnerie d'alarme (plus forte)", r.alarme) { etat.modifierRappels(r.copy(alarme = it)) }
                LigneInterrupteur("Rappel du coucher à 21h50", r.coucher) { etat.modifierRappels(r.copy(coucher = it)) }
                LigneInterrupteur("La veille d'un devoir, à 19h", r.veilleDevoirs) { etat.modifierRappels(r.copy(veilleDevoirs = it)) }
                prochain?.let {
                    Text(
                        "Prochain rappel : ${it.quand.toLocalDate().enLong()} à ${it.quand.toLocalTime().enHeure()} · ${it.titre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (r.actif && !peutNotifier) {
                Text(
                    "Les notifications sont bloquées : autorise-les pour recevoir les rappels.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        demande.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                }) { Text("Autoriser les notifications") }
            }
            if (r.actif && !Rappels.peutAlarmePrecise(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Text(
                    "Les rappels peuvent arriver avec quelques minutes de retard.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)),
                        )
                    }
                }) { Text("Autoriser les alarmes à l'heure exacte") }
            }
        }
    }
}

@Composable
private fun LigneInterrupteur(texte: String, actif: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texte, Modifier.weight(1f))
        Switch(checked = actif, onCheckedChange = onChange)
    }
}
