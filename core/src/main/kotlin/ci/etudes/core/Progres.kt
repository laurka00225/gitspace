package ci.etudes.core

import java.time.LocalDate

/** Statistiques de travail calculées à partir des séances cochées « faites ». */
object Progres {

    /** Dates où au moins une séance a été faite (les clés de séance commencent par la date ISO). */
    fun joursTravailles(faites: Set<String>): Set<LocalDate> =
        faites.mapNotNull { runCatching { LocalDate.parse(it.substringBefore(' ')) }.getOrNull() }.toSet()

    /**
     * Nombre de jours d'affilée avec au moins une séance faite, en finissant aujourd'hui
     * (ou hier, si l'élève n'a pas encore travaillé aujourd'hui).
     */
    fun serieEnCours(faites: Set<String>, aujourdhui: LocalDate): Int {
        val jours = joursTravailles(faites)
        var jour = if (aujourdhui in jours) aujourdhui else aujourdhui.minusDays(1)
        var n = 0
        while (jour in jours) {
            n++
            jour = jour.minusDays(1)
        }
        return n
    }

    /** Meilleure série de jours d'affilée. */
    fun meilleureSerie(faites: Set<String>): Int {
        val jours = joursTravailles(faites).sorted()
        var meilleure = 0
        var courante = 0
        var precedent: LocalDate? = null
        for (j in jours) {
            courante = if (precedent != null && precedent.plusDays(1) == j) courante + 1 else 1
            meilleure = maxOf(meilleure, courante)
            precedent = j
        }
        return meilleure
    }

    /** Part des séances prévues jusqu'à [jusqua] inclus qui ont été faites. */
    fun tauxRealisation(plan: PlanSemaine, faites: Set<String>, jusqua: LocalDate): Pair<Int, Int> {
        val prevues = plan.jours.filter { !it.date.isAfter(jusqua) }.flatMap { it.seances }
        return prevues.count { it.cle in faites } to prevues.size
    }
}
