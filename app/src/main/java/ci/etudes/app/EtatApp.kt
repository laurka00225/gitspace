package ci.etudes.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ci.etudes.core.JourPlan
import ci.etudes.core.Matiere
import ci.etudes.core.MatieresParDefaut
import ci.etudes.core.Niveau
import ci.etudes.core.PlanSemaine
import ci.etudes.core.Planificateur
import ci.etudes.core.Reglages
import ci.etudes.core.Serie
import java.time.LocalDate

/** État de l'application, observé par l'interface et sauvegardé à chaque changement. */
class EtatApp(private val stockage: Stockage) {

    var configure by mutableStateOf(stockage.estConfigure)
        private set
    var reglages by mutableStateOf(stockage.lireReglages())
        private set
    var matieres by mutableStateOf(
        stockage.lireMatieres() ?: MatieresParDefaut.pour(reglages.niveau, reglages.serie),
    )
        private set

    /** Clés des séances cochées comme faites. */
    var faites by mutableStateOf(stockage.lireFaites())
        private set

    /** Matière choisie à la main pour une séance (clé de séance -> id de matière). */
    var changements by mutableStateOf(stockage.lireChangements())
        private set

    private fun sauver() {
        stockage.enregistrer(reglages, matieres, faites, changements)
        configure = true
    }

    fun demarrer(niveau: Niveau, serie: Serie?) {
        reglages = reglages.copy(niveau = niveau, serie = serie)
        matieres = MatieresParDefaut.pour(niveau, serie)
        sauver()
    }

    /** Change de classe ou de série : les matières de la nouvelle classe remplacent les anciennes. */
    fun changerClasse(niveau: Niveau, serie: Serie?) {
        val perso = matieres.filter { it.personnalisee }
        reglages = reglages.copy(niveau = niveau, serie = serie)
        matieres = MatieresParDefaut.pour(niveau, serie) + perso
        changements = emptyMap()
        sauver()
    }

    fun modifierReglages(transformation: (Reglages) -> Reglages) {
        reglages = transformation(reglages)
        sauver()
    }

    fun modifierMatiere(m: Matiere) {
        matieres = matieres.map { if (it.id == m.id) m else it }
        sauver()
    }

    fun ajouterMatiere(nom: String, priorite: Int) {
        val id = "perso_" + System.currentTimeMillis()
        matieres = matieres + Matiere(id, nom.trim(), priorite, active = true, personnalisee = true)
        sauver()
    }

    fun supprimerMatiere(m: Matiere) {
        matieres = matieres.filterNot { it.id == m.id }
        changements = changements.filterValues { it != m.id }
        sauver()
    }

    fun retablirMatieres() {
        matieres = MatieresParDefaut.pour(reglages.niveau, reglages.serie)
        changements = emptyMap()
        sauver()
    }

    fun basculerFaite(cle: String) {
        faites = if (cle in faites) faites - cle else faites + cle
        sauver()
    }

    fun changerMatiereSeance(cle: String, matiereId: String?) {
        changements = if (matiereId == null) changements - cle else changements + (cle to matiereId)
        sauver()
    }

    fun toutReinitialiser() {
        stockage.toutEffacer()
        reglages = Reglages()
        matieres = MatieresParDefaut.pour(reglages.niveau, reglages.serie)
        faites = emptySet()
        changements = emptyMap()
        configure = false
    }

    fun plan(lundi: LocalDate): PlanSemaine {
        val brut = Planificateur.semaine(lundi, reglages, matieres)
        val parId = matieres.associateBy { it.id }
        return brut.copy(
            jours = brut.jours.map { jour ->
                jour.copy(
                    seances = jour.seances.map { s ->
                        val choisie = changements[s.cle]?.let { parId[it] }
                        if (choisie != null) s.copy(matiere = choisie) else s
                    },
                )
            },
        )
    }

    fun jour(date: LocalDate): JourPlan =
        plan(Planificateur.lundiDe(date)).jours.first { it.date == date }
}
