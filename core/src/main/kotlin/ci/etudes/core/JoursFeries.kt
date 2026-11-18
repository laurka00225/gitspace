package ci.etudes.core

import java.time.LocalDate
import java.time.Month

data class JourFerie(
    val date: LocalDate,
    val nom: String,
    /** Vrai pour les fêtes musulmanes, dont la date dépend de l'observation de la lune. */
    val estime: Boolean = false,
)

/** Jours fériés de Côte d'Ivoire. */
object JoursFeries {

    fun pour(annee: Int): List<JourFerie> {
        val liste = mutableListOf(
            JourFerie(LocalDate.of(annee, Month.JANUARY, 1), "Jour de l'An"),
            JourFerie(LocalDate.of(annee, Month.MAY, 1), "Fête du Travail"),
            JourFerie(LocalDate.of(annee, Month.AUGUST, 7), "Fête de l'Indépendance"),
            JourFerie(LocalDate.of(annee, Month.AUGUST, 15), "Assomption"),
            JourFerie(LocalDate.of(annee, Month.NOVEMBER, 1), "Toussaint"),
            JourFerie(LocalDate.of(annee, Month.NOVEMBER, 15), "Journée nationale de la Paix"),
            JourFerie(LocalDate.of(annee, Month.DECEMBER, 25), "Noël"),
        )
        val paques = paques(annee)
        liste += JourFerie(paques.plusDays(1), "Lundi de Pâques")
        liste += JourFerie(paques.plusDays(39), "Ascension")
        liste += JourFerie(paques.plusDays(50), "Lundi de Pentecôte")
        liste += FETES_MUSULMANES.filter { it.date.year == annee }
        return liste.sortedBy { it.date }
    }

    fun entre(debut: LocalDate, fin: LocalDate): List<JourFerie> =
        (debut.year..fin.year).flatMap { pour(it) }
            .filter { !it.date.isBefore(debut) && !it.date.isAfter(fin) }

    /** Date de Pâques (calendrier grégorien, algorithme de Meeus/Jones/Butcher). */
    fun paques(annee: Int): LocalDate {
        val a = annee % 19
        val b = annee / 100
        val c = annee % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val mois = (h + l - 7 * m + 114) / 31
        val jour = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(annee, mois, jour)
    }

    /**
     * Dates estimées des fêtes musulmanes fériées en Côte d'Ivoire. Elles peuvent varier
     * d'un jour selon l'annonce officielle : l'élève peut les désactiver ou ajouter un jour libre.
     */
    private val FETES_MUSULMANES = listOf(
        "2026-03-17" to "Lendemain de la Nuit du Destin",
        "2026-03-20" to "Fête de fin du Ramadan",
        "2026-05-27" to "Tabaski",
        "2026-08-26" to "Lendemain du Maouloud",
        "2027-03-06" to "Lendemain de la Nuit du Destin",
        "2027-03-10" to "Fête de fin du Ramadan",
        "2027-05-17" to "Tabaski",
        "2027-08-16" to "Lendemain du Maouloud",
        "2028-02-23" to "Lendemain de la Nuit du Destin",
        "2028-02-27" to "Fête de fin du Ramadan",
        "2028-05-06" to "Tabaski",
        "2028-08-05" to "Lendemain du Maouloud",
    ).map { (d, n) -> JourFerie(LocalDate.parse(d), n, estime = true) }
}
