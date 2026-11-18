package ci.etudes.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmploiScolaireTest {

    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    /** Construit des mots placés dans une grille : colonnes = jours, lignes = heures. */
    private fun grille(transposee: Boolean = false): List<BlocTexte> {
        val blocs = mutableListOf<BlocTexte>()
        fun mot(texte: String, x: Float, y: Float) {
            val b = BlocTexte(texte, x - 40, y - 10, x + 40, y + 10)
            blocs += if (transposee) b.transpose() else b
        }
        val jours = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi")
        jours.forEachIndexed { i, j -> mot(j, 200f + i * 150f, 50f) }
        val heures = listOf("7h30-8h25", "8h25-9h20", "9h35-10h30", "10h30-11h25", "11h25-12h20", "15h00-15h55", "15h55-16h50")
        heures.forEachIndexed { i, h -> mot(h, 60f, 100f + i * 60f) }
        // Lundi
        mot("FRANCAIS", 200f, 100f); mot("FRANCAIS", 200f, 160f); mot("M. Kouassi", 200f, 172f)
        mot("MATHS", 200f, 220f); mot("HIST-GEO", 200f, 280f)
        // Mardi
        mot("Anglais", 350f, 100f); mot("SVT", 350f, 160f); mot("EPS", 350f, 400f); mot("EPS", 350f, 460f)
        // Mercredi
        mot("Physique-Chimie", 500f, 100f); mot("Maths", 500f, 160f); mot("EDHC", 500f, 340f)
        // Jeudi : salle et rien d'autre
        mot("Salle 12", 650f, 100f)
        // Vendredi
        mot("PC", 800f, 220f); mot("ESP", 800f, 460f)
        return blocs
    }

    @Test
    fun `reconnaissance des matieres et des heures`() {
        assertEquals("hg", AnalyseurEmploi.reconnaitreMatiere("Hist-Géo")?.first)
        assertEquals("pc", AnalyseurEmploi.reconnaitreMatiere("PHYSIQUE-CHIMIE")?.first)
        assertEquals("francais", AnalyseurEmploi.reconnaitreMatiere("Français")?.first)
        assertEquals(null, AnalyseurEmploi.reconnaitreMatiere("Kouassi"))
        assertEquals(listOf(t(7, 30), t(8, 25)), AnalyseurEmploi.heures("7h30-8h25"))
        assertEquals(listOf(t(15)), AnalyseurEmploi.heures("15H"))
        assertEquals(listOf(t(8), t(9)), AnalyseurEmploi.heures("08:00 - 09:00"))
    }

    private fun verifierGrille(cours: List<CoursScolaire>) {
        val lundi = cours.filter { it.jour == DayOfWeek.MONDAY }
        assertEquals(listOf("francais", "maths", "hg"), lundi.map { it.matiereId })
        assertEquals(t(7, 30) to t(9, 20), lundi[0].debut to lundi[0].fin) // deux heures fusionnées
        val mardi = cours.filter { it.jour == DayOfWeek.TUESDAY }
        assertEquals(listOf("Anglais", "SVT", "EPS"), mardi.map { it.libelle })
        assertEquals(t(15) to t(16, 50), mardi[2].debut to mardi[2].fin)
        assertTrue(cours.none { it.jour == DayOfWeek.THURSDAY })
        assertEquals(listOf("pc", "lv2"), cours.filter { it.jour == DayOfWeek.FRIDAY }.map { it.matiereId })
    }

    @Test
    fun `analyse d'une grille jours en colonnes`() = verifierGrille(AnalyseurEmploi.analyser(grille()))

    @Test
    fun `analyse d'une grille jours en lignes`() = verifierGrille(AnalyseurEmploi.analyser(grille(transposee = true)))

    @Test
    fun `plages libres apres les cours`() {
        val c = listOf(CoursScolaire(DayOfWeek.MONDAY, t(7, 30), t(17, 30), "maths", "Maths"))
        assertEquals(listOf(Plage(t(18), t(19, 30)), Plage(t(20, 15), t(22))), EmploiScolaire.plagesLibres(c, 30))
        val mercredi = listOf(CoursScolaire(DayOfWeek.WEDNESDAY, t(7, 30), t(12, 20), "maths", "Maths"))
        assertEquals(
            listOf(Plage(t(15), t(17, 30)), Plage(t(18), t(19, 30)), Plage(t(20, 15), t(22))),
            EmploiScolaire.plagesLibres(mercredi, 30),
        )
        val tot = listOf(CoursScolaire(DayOfWeek.TUESDAY, t(7, 30), t(16, 50), "maths", "Maths"))
        assertEquals(listOf(Plage(t(18), t(19, 30)), Plage(t(20, 15), t(22))), EmploiScolaire.plagesLibres(tot, 30))
    }

    @Test
    fun `le planning suit l'emploi du temps de l'ecole`() {
        val cours = AnalyseurEmploi.analyser(grille())
        val r = Reglages(niveau = Niveau.QUATRIEME, emploiScolaire = cours, utiliserEmploiScolaire = true)
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), r, MatieresParDefaut.pour(Niveau.QUATRIEME, null))
        val lundi = plan.jours[0]
        // Lundi, les cours finissent à 12h20 : séances dès 15h.
        assertEquals(t(15), lundi.seances.first().debut)
        assertTrue(lundi.seances.any { it.activite == "Relire le cours du jour et faire les exercices" })
        assertTrue(lundi.seances.any { it.activite == "Préparer le cours de demain" })
        // Jeudi sans cours détecté : plages par défaut d'un jour de cours.
        assertEquals(t(18), plan.jours[3].seances.first().debut)
    }
}
