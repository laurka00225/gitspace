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
        val connues = FETES_MUSULMANES.filter { it.date.year == annee }
        liste += connues.ifEmpty { fetesMusulmanesCalculees(annee) }
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
     * Estimation par le calendrier hégirien tabulaire, pour les années absentes de la table
     * ci-dessous. L'écart avec la date officielle est en général d'un ou deux jours.
     */
    internal fun fetesMusulmanesCalculees(annee: Int): List<JourFerie> {
        // Années hégiriennes qui peuvent tomber dans l'année grégorienne demandée.
        val h0 = ((annee - 622) * 33) / 32
        return (h0 - 1..h0 + 2).flatMap { h ->
            listOf(
                hegirienVersGregorien(h, 9, 27) to "Lendemain de la Nuit du Destin",
                hegirienVersGregorien(h, 10, 1) to "Fête de fin du Ramadan",
                hegirienVersGregorien(h, 12, 10) to "Tabaski",
                hegirienVersGregorien(h, 3, 13) to "Lendemain du Maouloud",
            )
        }.filter { it.first.year == annee }
            .map { (d, n) -> JourFerie(d, n, estime = true) }
    }

    /** Conversion du calendrier hégirien tabulaire (arithmétique) vers le grégorien. */
    internal fun hegirienVersGregorien(annee: Int, mois: Int, jour: Int): LocalDate {
        val jourJulien = jour +
            Math.ceil(29.5 * (mois - 1)).toLong() +
            (annee - 1) * 354L +
            Math.floorDiv(3L + 11L * annee, 30L) +
            1948439L
        return LocalDate.ofEpochDay(jourJulien - 2440588L)
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
