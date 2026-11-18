package ci.etudes.core

import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NouveautesTest {

    @Test
    fun `chaque matiere et chaque cycle ont des exercices valides`() {
        for (m in MatiereExercice.entries) for (n in Niveau.entries) {
            for (graine in 0 until 50) {
                val serie = Exercices.serie(m, n, Random(graine))
                assertTrue(serie.size >= 6, "$m $n : ${serie.size}")
                serie.forEach { q ->
                    assertTrue(q.choix.size >= 3, "${q.enonce} : ${q.choix}")
                    assertTrue(q.explication.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `les calculs generes ont la bonne reponse parmi les choix`() {
        for (c in Cycle.entries) for (g in 0 until 500) {
            val q = CalculsMaths.generer(c, Random(g))
            assertEquals(q.choix.size, q.choix.toSet().size, q.enonce)
        }
    }

    @Test
    fun `le melange garde la bonne reponse`() {
        val q = Question("1 + 1 ?", listOf("2", "3", "4"), 0, "")
        repeat(20) {
            val m = q.melangee(Random(it))
            assertEquals("2", m.choix[m.bonne])
        }
    }

    @Test
    fun `une evaluation proche renforce sa matiere`() {
        val lundi = LocalDate.of(2026, 11, 23)
        val matieres = MatieresParDefaut.pour(Niveau.SIXIEME, null)
        val sans = Planificateur.semaine(lundi, Reglages(), matieres)
        val eval = Evaluation("e1", "edhc", lundi.plusDays(4), TypeEvaluation.DEVOIR)
        val avec = Planificateur.semaine(lundi, Reglages(), matieres, listOf(eval))
        fun compte(p: PlanSemaine) = p.jours.take(4).flatMap { it.seances }.count { it.matiere.id == "edhc" }
        assertTrue(compte(avec) > compte(sans), "${compte(avec)} vs ${compte(sans)}")
        val prep = avec.jours.flatMap { it.seances }.first { it.matiere.id == "edhc" && it.date.isBefore(eval.date) }
        assertTrue(prep.activite.startsWith("Préparer"), prep.activite)
        // Aucune préparation après l'évaluation.
        assertTrue(avec.jours.filter { !it.date.isBefore(eval.date) }.flatMap { it.seances }.none { it.activite.startsWith("Préparer : devoir") })
    }

    @Test
    fun `series de jours`() {
        val auj = LocalDate.of(2026, 11, 18)
        val faites = setOf("2026-11-18 18:00", "2026-11-17 18:00", "2026-11-16 20:15", "2026-11-14 08:30", "2026-11-13 08:30")
        assertEquals(3, Progres.serieEnCours(faites, auj))
        assertEquals(2, Progres.serieEnCours(faites - "2026-11-18 18:00", auj))
        assertEquals(0, Progres.serieEnCours(faites, auj.plusDays(3)))
        assertEquals(3, Progres.meilleureSerie(faites))
    }
}
