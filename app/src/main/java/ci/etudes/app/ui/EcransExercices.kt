@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ci.etudes.app.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ci.etudes.app.EtatApp
import ci.etudes.app.Rappels
import ci.etudes.app.Stockage
import ci.etudes.core.Cycle
import ci.etudes.core.Evaluation
import ci.etudes.core.Exercices
import ci.etudes.core.MatiereExercice
import ci.etudes.core.Question
import ci.etudes.core.Seance
import ci.etudes.core.TypeEvaluation
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate

private val VertReussite = Color(0xFF2E7D32)
private val RougeErreur = Color(0xFFC62828)

// ---------------------------------------------------------------- Exercices

@Composable
fun EcranExercices(
    etat: EtatApp,
    matiere: MatiereExercice?,
    onChoisir: (MatiereExercice) -> Unit,
    onTerminer: () -> Unit,
    modifier: Modifier,
) {
    if (matiere == null) {
        ChoixMatiereExercices(etat, onChoisir, modifier)
    } else {
        // Une nouvelle série à chaque ouverture.
        var essai by remember(matiere) { mutableIntStateOf(0) }
        val questions = remember(matiere, essai) { Exercices.serie(matiere, etat.reglages.niveau) }
        Quiz(
            questions = questions,
            modifier = modifier,
            onFin = { score -> etat.enregistrerScore(matiere.id, score) },
            onRecommencer = { essai++ },
            onQuitter = onTerminer,
        )
    }
}

@Composable
private fun ChoixMatiereExercices(etat: EtatApp, onChoisir: (MatiereExercice) -> Unit, modifier: Modifier) {
    val cycle = Cycle.de(etat.reglages.niveau)
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Séries de ${Exercices.QUESTIONS_PAR_SERIE} questions niveau ${cycle.libelle}, avec la correction expliquée. " +
                    "En maths, de nouveaux calculs sont créés à chaque série.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(MatiereExercice.entries) { m ->
            val meilleur = etat.scores[m.id]
            val couleur = couleurDe(ci.etudes.core.Matiere(m.id, m.nom, 1))
            Card(onClick = { onChoisir(m) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { (meilleur ?: 0) / 10f },
                            color = couleur,
                            trackColor = couleur.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp),
                        )
                        Text(m.nom.take(2), fontWeight = FontWeight.Bold, color = couleur)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (meilleur == null) "Pas encore commencé" else "Meilleur score : $meilleur/10",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("Commencer ›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Quiz(
    questions: List<Question>,
    modifier: Modifier,
    onFin: (Int) -> Unit,
    onRecommencer: () -> Unit,
    onQuitter: () -> Unit,
) {
    var index by remember(questions) { mutableIntStateOf(0) }
    var reponse by remember(questions) { mutableStateOf<Int?>(null) }
    var score by remember(questions) { mutableIntStateOf(0) }
    var termine by remember(questions) { mutableStateOf(false) }

    if (termine) {
        val sur10 = if (questions.isEmpty()) 0 else score * 10 / questions.size
        Column(
            modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                when {
                    sur10 >= 9 -> "🏆"
                    sur10 >= 7 -> "🎉"
                    sur10 >= 5 -> "👍"
                    else -> "💪"
                },
                fontSize = 64.sp,
            )
            Text("$score / ${questions.size}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(
                when {
                    sur10 >= 9 -> "Excellent ! Tu maîtrises très bien."
                    sur10 >= 7 -> "Très bien ! Encore un petit effort."
                    sur10 >= 5 -> "C'est un bon début. Relis les corrections."
                    else -> "Courage ! Revois ton cours puis recommence."
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onRecommencer, modifier = Modifier.fillMaxWidth()) { Text("Nouvelle série") }
            OutlinedButton(onClick = onQuitter, modifier = Modifier.fillMaxWidth()) { Text("Choisir une autre matière") }
        }
        return
    }

    val q = questions[index]
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(
            progress = { (index + (if (reponse != null) 1 else 0)).toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Question ${index + 1} / ${questions.size}  ·  Score : $score", style = MaterialTheme.typography.labelLarge)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(q.enonce, Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
        }
        q.choix.forEachIndexed { i, choix ->
            val couleurBord = when {
                reponse == null -> MaterialTheme.colorScheme.outline
                i == q.bonne -> VertReussite
                i == reponse -> RougeErreur
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            OutlinedCard(
                onClick = {
                    if (reponse == null) {
                        reponse = i
                        if (i == q.bonne) score++
                    }
                },
                border = BorderStroke(if (reponse != null && (i == q.bonne || i == reponse)) 2.dp else 1.dp, couleurBord),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${'A' + i}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(choix, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (reponse != null && i == q.bonne) Text("✓", color = VertReussite, fontWeight = FontWeight.Bold)
                    if (reponse == i && i != q.bonne) Text("✗", color = RougeErreur, fontWeight = FontWeight.Bold)
                }
            }
        }
        AnimatedVisibility(visible = reponse != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val juste = reponse == q.bonne
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = (if (juste) VertReussite else RougeErreur).copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (juste) "Bonne réponse !" else "Pas tout à fait. La bonne réponse : ${q.choix[q.bonne]}",
                            fontWeight = FontWeight.Bold,
                            color = if (juste) VertReussite else RougeErreur,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(q.explication)
                    }
                }
                Button(
                    onClick = {
                        if (index + 1 < questions.size) {
                            index++
                            reponse = null
                        } else {
                            termine = true
                            onFin(if (questions.isEmpty()) 0 else score * 10 / questions.size)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (index + 1 < questions.size) "Question suivante" else "Voir mon score") }
            }
        }
    }
}

// ---------------------------------------------------------------- Devoirs

@Composable
fun EcranDevoirs(etat: EtatApp, modifier: Modifier) {
    var ajout by remember { mutableStateOf(false) }
    val aujourdhui = LocalDate.now()
    val noms = etat.matieres.associate { it.id to it.nom }
    val aVenir = etat.evaluations.filter { !it.date.isBefore(aujourdhui) }
    val passes = etat.evaluations.filter { it.date.isBefore(aujourdhui) }.sortedByDescending { it.date }

    Box(modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Note tes devoirs, interrogations et compositions. La semaine qui précède, la matière " +
                        "reçoit plus de séances, et un rappel arrive la veille à 19h.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (aVenir.isEmpty()) {
                item {
                    Text(
                        "Aucun devoir à venir.",
                        Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(aVenir, key = { it.id }) { e -> LigneEvaluation(e, noms, aujourdhui, onSupprimer = { etat.supprimerEvaluation(e) }) }
            if (passes.isNotEmpty()) {
                item { Text("Passés", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
                items(passes, key = { it.id }) { e ->
                    LigneEvaluation(e, noms, aujourdhui, passe = true, onSupprimer = { etat.supprimerEvaluation(e) })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { ajout = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Ajouter") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
    if (ajout) DialogueEvaluation(etat, onFermer = { ajout = false })
}

@Composable
private fun LigneEvaluation(
    e: Evaluation,
    noms: Map<String, String>,
    aujourdhui: LocalDate,
    passe: Boolean = false,
    onSupprimer: () -> Unit,
) {
    val jours = Duration.between(aujourdhui.atStartOfDay(), e.date.atStartOfDay()).toDays()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!passe && jours <= 2) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${e.date.dayOfMonth}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(e.date.enCourt().substringAfter(' '), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${e.type.libelle} · ${noms[e.matiereId] ?: "?"}", fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        passe -> e.date.enLong()
                        jours == 0L -> "Aujourd'hui"
                        jours == 1L -> "Demain"
                        else -> "Dans $jours jours · ${e.date.enLong()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onSupprimer) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
        }
    }
}

@Composable
private fun DialogueEvaluation(etat: EtatApp, onFermer: () -> Unit) {
    val context = LocalContext.current
    val actives = etat.matieres.filter { it.active }
    var matiereId by remember { mutableStateOf(actives.firstOrNull()?.id) }
    var type by remember { mutableStateOf(TypeEvaluation.DEVOIR) }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Nouveau devoir ou contrôle") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Matière", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actives.forEach { m ->
                        FilterChip(selected = m.id == matiereId, onClick = { matiereId = m.id }, label = { Text(m.nom) })
                    }
                }
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeEvaluation.entries.forEach { t ->
                        FilterChip(selected = t == type, onClick = { type = t }, label = { Text(t.libelle) })
                    }
                }
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, a, m, j -> date = LocalDate.of(a, m + 1, j) },
                            date.year, date.monthValue - 1, date.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Date : ${date.enLong()}") }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Chapitre ou remarque (facultatif)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = matiereId != null,
                onClick = {
                    etat.ajouterEvaluation(
                        Evaluation("eval_" + System.currentTimeMillis(), matiereId!!, date, type, note.trim()),
                    )
                    onFermer()
                },
            ) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    )
}

// ---------------------------------------------------------------- Minuteur

/** Minuteur plein écran pour une séance. Une alarme sonne à la fin, même si l'appli est fermée. */
@Composable
fun EcranMinuteur(etat: EtatApp, seance: Seance, onFermer: () -> Unit) {
    val context = LocalContext.current
    val stockage = remember { Stockage(context) }
    val dureeMs = Duration.between(seance.debut, seance.fin).toMillis()
    var fin by remember {
        mutableLongStateOf(stockage.finMinuteur.takeIf { it > System.currentTimeMillis() } ?: 0L)
    }
    var maintenant by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fin) {
        while (fin > 0L) {
            maintenant = System.currentTimeMillis()
            delay(250)
        }
    }
    val restant = if (fin == 0L) dureeMs else (fin - maintenant).coerceAtLeast(0L)
    val terminee = fin != 0L && restant == 0L

    Dialog(onDismissRequest = onFermer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.extraSmall) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                Text(seance.matiere.nom, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(seance.activite, textAlign = TextAlign.Center)
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f - restant.toFloat() / dureeMs.coerceAtLeast(1L) },
                        strokeWidth = 12.dp,
                        color = couleurDe(seance.matiere),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(240.dp),
                    )
                    val sec = restant / 1000
                    Text(
                        "%02d:%02d".format(sec / 60, sec % 60),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                when {
                    terminee -> {
                        Text("Bravo, séance terminée ! 🎉", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = {
                            if (seance.cle !in etat.faites) etat.basculerFaite(seance.cle)
                            Rappels.arreterMinuteur(context)
                            onFermer()
                        }, modifier = Modifier.fillMaxWidth()) { Text("Marquer comme faite") }
                    }
                    fin == 0L -> Button(onClick = {
                        fin = System.currentTimeMillis() + dureeMs
                        Rappels.demarrerMinuteur(context, fin, seance.matiere.nom)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Démarrer (${dureeTexte(dureeMs / 60_000)})") }
                    else -> OutlinedButton(onClick = {
                        fin = 0L
                        Rappels.arreterMinuteur(context)
                    }, modifier = Modifier.fillMaxWidth()) { Text("Arrêter") }
                }
                Text(
                    "Téléphone retourné, pas de réseaux sociaux : concentre-toi ! Une alarme sonnera à la fin.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onFermer) { Text("Fermer") }
            }
        }
    }
}
