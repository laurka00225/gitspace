package ci.etudes.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Construit l'emploi du temps d'études d'une semaine.
 *
 * Règles :
 * - lundi, mardi, jeudi, vendredi : cours de 7h30 à 17h30, études le soir jusqu'à 22h ;
 * - mercredi : pas de cours l'après-midi ;
 * - samedi, dimanche, jours fériés et jours libres ajoutés : journée entière disponible ;
 * - 22h : coucher, aucune séance après.
 *
 * Les séances sont réparties entre les matières actives au prorata de leur priorité
 * (tourniquet pondéré), sans mettre deux fois de suite la même matière dans une journée.
 */
object Planificateur {

    val HEURE_COUCHER: LocalTime = LocalTime.of(22, 0)

    /** Durée minimale d'une séance raccourcie en fin de plage. */
    private const val SEANCE_MIN = 20

    fun lundiDe(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Type d'une journée, avec le nom de l'événement (jour férié ou jour libre) s'il y en a. */
    fun typeDe(date: LocalDate, reglages: Reglages): Pair<TypeJour, String?> {
        val perso = reglages.joursLibresPerso.firstOrNull { it.contient(date) }
        if (perso != null) return TypeJour.LIBRE to perso.nom
        val ferie = JoursFeries.pour(date.year)
            .firstOrNull { it.date == date && it.date.toString() !in reglages.feriesIgnores }
        if (ferie != null) return TypeJour.LIBRE to ferie.nom
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> TypeJour.LIBRE to null
            DayOfWeek.WEDNESDAY -> TypeJour.MERCREDI to null
            else -> TypeJour.COURS to null
        }
    }

    /** Découpe une plage en créneaux de séance, en respectant la pause et l'heure du coucher. */
    fun creneaux(plage: Plage, dureeMin: Int, pauseMin: Int): List<Pair<LocalTime, LocalTime>> {
        require(dureeMin > 0 && pauseMin >= 0)
        val fin = if (plage.fin.isAfter(HEURE_COUCHER)) HEURE_COUCHER else plage.fin
        val resultat = mutableListOf<Pair<LocalTime, LocalTime>>()
        var debut = plage.debut
        while (debut.isBefore(fin)) {
            val restant = java.time.Duration.between(debut, fin).toMinutes()
            if (restant < SEANCE_MIN) break
            val duree = minOf(dureeMin.toLong(), restant)
            val finSeance = debut.plusMinutes(duree)
            resultat += debut to finSeance
            val suivant = finSeance.plusMinutes(pauseMin.toLong())
            // Évite le passage à minuit (LocalTime boucle).
            if (!suivant.isAfter(finSeance) && pauseMin > 0) break
            debut = suivant
        }
        return resultat
    }

    /** Retire les créneaux qui chevauchent le précédent (plages qui se recouvrent). */
    private fun sansChevauchement(creneaux: List<Pair<LocalTime, LocalTime>>): List<Pair<LocalTime, LocalTime>> {
        val resultat = mutableListOf<Pair<LocalTime, LocalTime>>()
        for (c in creneaux.sortedBy { it.first }) {
            val dernier = resultat.lastOrNull()
            if (dernier == null || !c.first.isBefore(dernier.second)) resultat += c
        }
        return resultat
    }

    /** Vrai si la plage recouvre une des autres plages. */
    fun chevauche(plage: Plage, autres: List<Plage>): Boolean =
        autres.any { plage.debut.isBefore(it.fin) && it.debut.isBefore(plage.fin) }

    fun semaine(lundi: LocalDate, reglages: Reglages, matieres: List<Matiere>): PlanSemaine {
        val actives = matieres.filter { it.active && it.priorite > 0 }
        val credits = DoubleArray(actives.size)
        val poidsTotal = actives.sumOf { it.priorite }.toDouble()

        val jours = (0L until 7L).map { offset ->
            val date = lundi.plusDays(offset)
            val (type, evenement) = typeDe(date, reglages)
            val creneaux = reglages.plages(type)
                .sortedBy { it.debut }
                .flatMap { creneaux(it, reglages.dureeSeanceMin, reglages.pauseMin) }
                .let(::sansChevauchement)

            val seances = mutableListOf<Seance>()
            val compteJour = IntArray(actives.size)
            var precedente = -1
            creneaux.forEachIndexed { index, (debut, fin) ->
                if (actives.isEmpty()) return@forEachIndexed
                for (i in actives.indices) credits[i] += actives[i].priorite.toDouble()
                val choix = choisir(actives, credits, compteJour, precedente, creneaux.size)
                credits[choix] -= poidsTotal
                compteJour[choix]++
                precedente = choix
                seances += Seance(
                    date = date,
                    debut = debut,
                    fin = fin,
                    matiere = actives[choix],
                    activite = activite(type, index, reglages.niveau),
                )
            }
            JourPlan(date, type, evenement, seances)
        }
        return PlanSemaine(lundi, jours)
    }

    private fun choisir(
        actives: List<Matiere>,
        credits: DoubleArray,
        compteJour: IntArray,
        precedente: Int,
        seancesDuJour: Int,
    ): Int {
        // Au plus ~1/3 des séances d'une journée pour une même matière (minimum 2).
        val maxParJour = maxOf(2, (seancesDuJour + 2) / 3)
        val candidats = actives.indices.filter { i ->
            (i != precedente || actives.size == 1) && compteJour[i] < maxParJour
        }.ifEmpty { actives.indices.filter { it != precedente || actives.size == 1 } }
        return candidats.maxWith(compareBy<Int> { credits[it] }.thenByDescending { it })
    }

    private fun activite(type: TypeJour, index: Int, niveau: Niveau): String = when (type) {
        TypeJour.COURS -> if (index == 0) "Relire les cours du jour" else "Exercices et devoirs"
        TypeJour.MERCREDI -> when (index % 3) {
            0 -> "Relire et faire une fiche de révision"
            1 -> "Exercices et devoirs"
            else -> "Apprendre les leçons"
        }
        TypeJour.LIBRE -> when {
            niveau.examen != null && index % 3 == 1 -> "Sujet type ${niveau.examen}"
            index % 2 == 0 -> "Approfondir le cours, faire des fiches"
            else -> "Exercices d'entraînement"
        }
    }
}
