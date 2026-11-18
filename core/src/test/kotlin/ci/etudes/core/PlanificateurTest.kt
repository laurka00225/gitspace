package ci.etudes.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PlanificateurTest {

    private val reglages = Reglages(niveau = Niveau.TROISIEME)
    private val matieres = MatieresParDefaut.pour(Niveau.TROISIEME, null)

    @Test
    fun `types de journees de la semaine`() {
        // Semaine du lundi 23 novembre 2026, sans jour férié.
        val lundi = LocalDate.of(2026, 11, 23)
        val types = (0L until 7L).map { Planificateur.typeDe(lundi.plusDays(it), reglages).first }
        assertEquals(
            listOf(
                TypeJour.COURS, TypeJour.COURS, TypeJour.MERCREDI, TypeJour.COURS,
                TypeJour.COURS, TypeJour.LIBRE, TypeJour.LIBRE,
            ),
            types,
        )
    }

    @Test
    fun `un jour ferie en semaine devient une journee libre`() {
        // 7 août 2026 est un vendredi : fête de l'Indépendance.
        val (type, nom) = Planificateur.typeDe(LocalDate.of(2026, 8, 7), reglages)
        assertEquals(TypeJour.LIBRE, type)
        assertEquals("Fête de l'Indépendance", nom)
    }

    @Test
    fun `un ferie desactive redevient un jour normal`() {
        val r = reglages.copy(feriesIgnores = setOf("2026-08-07"))
        assertEquals(TypeJour.COURS, Planificateur.typeDe(LocalDate.of(2026, 8, 7), r).first)
    }

    @Test
    fun `jours libres perso`() {
        val r = reglages.copy(
            joursLibresPerso = listOf(
                JourLibrePerso("1", "Congés de Noël", LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 3)),
            ),
        )
        val (type, nom) = Planificateur.typeDe(LocalDate.of(2026, 12, 23), r)
        assertEquals(TypeJour.LIBRE, type)
        assertEquals("Congés de Noël", nom)
    }

    @Test
    fun `paques`() {
        assertEquals(LocalDate.of(2026, 4, 5), JoursFeries.paques(2026))
        assertEquals(LocalDate.of(2027, 3, 28), JoursFeries.paques(2027))
        assertEquals(LocalDate.of(2025, 4, 20), JoursFeries.paques(2025))
    }

    @Test
    fun `aucune seance apres 22h ni avant 17h30 un jour de cours`() {
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), reglages, matieres)
        for (jour in plan.jours) {
            for (s in jour.seances) {
                assertFalse(s.fin.isAfter(LocalTime.of(22, 0)), "$s finit après 22h")
                if (jour.type == TypeJour.COURS) {
                    assertFalse(s.debut.isBefore(LocalTime.of(17, 30)), "$s commence pendant les cours")
                }
                if (jour.type == TypeJour.MERCREDI) {
                    assertFalse(s.debut.isBefore(LocalTime.of(12, 30)), "$s commence le mercredi matin")
                }
            }
        }
    }

    @Test
    fun `pas deux fois de suite la meme matiere dans une journee`() {
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), reglages, matieres)
        for (jour in plan.jours) {
            jour.seances.zipWithNext().forEach { (a, b) -> assertNotEquals(a.matiere.id, b.matiere.id) }
        }
    }

    @Test
    fun `les matieres prioritaires ont plus de temps`() {
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), reglages, matieres)
        val minutes = plan.minutesParMatiere().mapKeys { it.key.id }
        assertTrue(minutes.getValue("maths") > minutes.getValue("edhc"))
        assertTrue(minutes.getValue("francais") > minutes.getValue("edhc"))
        // Toutes les matières actives apparaissent au moins une fois dans la semaine.
        assertEquals(matieres.map { it.id }.toSet(), minutes.keys)
    }

    @Test
    fun `decoupage d'une plage`() {
        val c = Planificateur.creneaux(
            Plage(LocalTime.of(18, 0), LocalTime.of(19, 30)), dureeMin = 45, pauseMin = 10,
        )
        // 18:00-18:45, 18:55-19:30 (35 min restantes)
        assertEquals(
            listOf(
                LocalTime.of(18, 0) to LocalTime.of(18, 45),
                LocalTime.of(18, 55) to LocalTime.of(19, 30),
            ),
            c,
        )
    }

    @Test
    fun `plage tronquee a l'heure du coucher`() {
        val c = Planificateur.creneaux(
            Plage(LocalTime.of(21, 0), LocalTime.of(23, 0)), dureeMin = 45, pauseMin = 10,
        )
        assertTrue(c.all { !it.second.isAfter(LocalTime.of(22, 0)) })
    }

    @Test
    fun `examen en classe d'examen le week-end`() {
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), reglages, matieres)
        val samedi = plan.jours.first { it.date.dayOfWeek == DayOfWeek.SATURDAY }
        assertTrue(samedi.seances.any { it.activite.contains("BEPC") })
    }

    @Test
    fun `toutes les classes et series ont des matieres`() {
        for (n in Niveau.entries) {
            val series = n.series.ifEmpty { listOf(null) }
            for (s in series) assertTrue(MatieresParDefaut.pour(n, s).size >= 7, "$n $s")
        }
    }
}

class ChevauchementEtFeriesTest {

    @Test
    fun `des plages qui se recouvrent ne donnent pas de seances en double`() {
        val r = Reglages(
            plagesCours = listOf(
                Plage(LocalTime.of(18, 0), LocalTime.of(20, 0)),
                Plage(LocalTime.of(19, 0), LocalTime.of(21, 0)),
            ),
        )
        val plan = Planificateur.semaine(LocalDate.of(2026, 11, 23), r, MatieresParDefaut.pour(Niveau.SIXIEME, null))
        val lundi = plan.jours.first()
        lundi.seances.zipWithNext().forEach { (a, b) -> assertFalse(b.debut.isBefore(a.fin), "$a / $b") }
        assertTrue(Planificateur.chevauche(r.plagesCours[1], listOf(r.plagesCours[0])))
        assertFalse(
            Planificateur.chevauche(
                Plage(LocalTime.of(20, 0), LocalTime.of(21, 0)),
                listOf(Plage(LocalTime.of(18, 0), LocalTime.of(20, 0))),
            ),
        )
    }

    @Test
    fun `le calcul hegirien est proche des dates connues`() {
        for (annee in 2026..2028) {
            val calculees = JoursFeries.fetesMusulmanesCalculees(annee).associateBy { it.nom }
            val connues = JoursFeries.pour(annee).filter { it.estime }
            assertEquals(4, connues.size)
            for (f in connues) {
                val c = calculees.getValue(f.nom)
                val ecart = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(f.date, c.date))
                assertTrue(ecart <= 2, "${f.nom} $annee : ${f.date} vs ${c.date}")
            }
        }
    }

    @Test
    fun `fetes musulmanes apres 2028`() {
        for (annee in 2029..2040) {
            val f = JoursFeries.pour(annee).filter { it.estime }.map { it.nom }.toSet()
            assertTrue("Tabaski" in f && "Fête de fin du Ramadan" in f, "$annee : $f")
        }
    }
}
