package ci.etudes.core

import java.time.LocalDate
import java.time.LocalTime

/** Classes du secondaire ivoirien, de la 6e à la Terminale. */
enum class Niveau(val libelle: String, val examen: String?) {
    SIXIEME("6e", null),
    CINQUIEME("5e", null),
    QUATRIEME("4e", null),
    TROISIEME("3e", "BEPC"),
    SECONDE("2nde", null),
    PREMIERE("1ère", null),
    TERMINALE("Tle", "BAC");

    val estLycee: Boolean get() = ordinal >= SECONDE.ordinal

    /** Séries possibles pour ce niveau (vide au collège). */
    val series: List<Serie>
        get() = when (this) {
            SECONDE -> listOf(Serie.A, Serie.C)
            PREMIERE, TERMINALE -> listOf(Serie.A, Serie.C, Serie.D)
            else -> emptyList()
        }
}

enum class Serie(val libelle: String) {
    A("Série A (littéraire)"),
    C("Série C (maths, physique)"),
    D("Série D (sciences de la vie)"),
}

/**
 * Une matière à réviser. [priorite] va de 1 (peu de temps) à 5 (beaucoup de temps) :
 * le nombre de séances de la semaine est proportionnel à la priorité.
 */
data class Matiere(
    val id: String,
    val nom: String,
    val priorite: Int,
    val active: Boolean = true,
    val personnalisee: Boolean = false,
)

/** Type de journée, qui détermine les plages d'études disponibles. */
enum class TypeJour(val libelle: String) {
    COURS("Jour de cours"),
    MERCREDI("Mercredi (après-midi libre)"),
    LIBRE("Journée libre"),
}

/** Une plage horaire pendant laquelle l'élève peut étudier. */
data class Plage(val debut: LocalTime, val fin: LocalTime) {
    init {
        require(fin.isAfter(debut)) { "La fin ($fin) doit être après le début ($debut)" }
    }

    val minutes: Long get() = java.time.Duration.between(debut, fin).toMinutes()
}

/** Un jour libre ajouté par l'élève (vacances, congé, journée pédagogique...). */
data class JourLibrePerso(
    val id: String,
    val nom: String,
    val debut: LocalDate,
    val fin: LocalDate,
) {
    fun contient(date: LocalDate) = !date.isBefore(debut) && !date.isAfter(fin)
}

/** Réglages de l'élève qui servent à générer le planning. */
data class Reglages(
    val niveau: Niveau = Niveau.SIXIEME,
    val serie: Serie? = null,
    val dureeSeanceMin: Int = 45,
    val pauseMin: Int = 10,
    val plagesCours: List<Plage> = PlagesParDefaut.COURS,
    val plagesMercredi: List<Plage> = PlagesParDefaut.MERCREDI,
    val plagesLibre: List<Plage> = PlagesParDefaut.LIBRE,
    val joursLibresPerso: List<JourLibrePerso> = emptyList(),
    /** Jours fériés officiels que l'élève a désactivés (clé = date ISO). */
    val feriesIgnores: Set<String> = emptySet(),
) {
    fun plages(type: TypeJour): List<Plage> = when (type) {
        TypeJour.COURS -> plagesCours
        TypeJour.MERCREDI -> plagesMercredi
        TypeJour.LIBRE -> plagesLibre
    }
}

/**
 * Plages proposées par défaut. Elles restent dans la fenêtre où l'élève est à la maison
 * et éveillé : à partir de 17h30 les jours de cours, jusqu'à 22h (heure du coucher),
 * en laissant le temps de rentrer, de se laver et de dîner.
 */
object PlagesParDefaut {
    private fun p(d: String, f: String) = Plage(LocalTime.parse(d), LocalTime.parse(f))

    /** Lundi, mardi, jeudi, vendredi : retour à 17h30, dîner vers 19h30. */
    val COURS = listOf(p("18:00", "19:30"), p("20:15", "22:00"))

    /** Mercredi : pas de cours l'après-midi. */
    val MERCREDI = listOf(p("15:00", "17:30"), p("18:00", "19:30"), p("20:15", "22:00"))

    /** Samedi, dimanche, jours fériés et vacances. */
    val LIBRE = listOf(p("08:30", "12:00"), p("15:00", "17:30"), p("20:15", "22:00"))
}

data class Seance(
    val date: LocalDate,
    val debut: LocalTime,
    val fin: LocalTime,
    val matiere: Matiere,
    val activite: String,
) {
    /** Clé stable d'une séance, pour mémoriser "faite" ou un changement de matière. */
    val cle: String get() = "$date $debut"
}

data class JourPlan(
    val date: LocalDate,
    val type: TypeJour,
    /** Nom du jour férié ou du jour libre, s'il y en a un. */
    val evenement: String?,
    val seances: List<Seance>,
)

data class PlanSemaine(val lundi: LocalDate, val jours: List<JourPlan>) {
    /** Minutes d'étude par matière sur la semaine. */
    fun minutesParMatiere(): Map<Matiere, Long> =
        jours.flatMap { it.seances }
            .groupBy { it.matiere }
            .mapValues { (_, s) -> s.sumOf { java.time.Duration.between(it.debut, it.fin).toMinutes() } }
}

enum class TypeEvaluation(val libelle: String) {
    INTERROGATION("Interrogation"),
    DEVOIR("Devoir"),
    COMPOSITION("Composition"),
    EXAMEN_BLANC("Examen blanc"),
    EXPOSE("Exposé"),
}

/** Un devoir ou un contrôle à venir, noté par l'élève. */
data class Evaluation(
    val id: String,
    val matiereId: String,
    val date: LocalDate,
    val type: TypeEvaluation,
    val note: String = "",
)
