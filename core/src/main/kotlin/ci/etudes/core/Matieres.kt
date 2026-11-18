package ci.etudes.core

/**
 * Matières proposées par défaut selon la classe et la série du système ivoirien.
 * Les priorités (1 à 5) sont des suggestions : l'élève peut les modifier,
 * désactiver une matière ou en ajouter une.
 */
object MatieresParDefaut {

    private fun m(id: String, nom: String, priorite: Int) = Matiere(id, nom, priorite)

    fun pour(niveau: Niveau, serie: Serie?): List<Matiere> = when {
        !niveau.estLycee -> college(niveau)
        niveau == Niveau.SECONDE -> seconde(serie ?: Serie.C)
        else -> cycleTerminal(niveau, serie ?: Serie.D)
    }

    private fun college(niveau: Niveau): List<Matiere> {
        val liste = mutableListOf(
            m("francais", "Français", 5),
            m("maths", "Mathématiques", 5),
            m("anglais", "Anglais", 4),
            m("pc", "Physique-Chimie", 3),
            m("svt", "SVT", 3),
            m("hg", "Histoire-Géographie", 3),
            m("edhc", "EDHC", 1),
        )
        if (niveau >= Niveau.QUATRIEME) {
            liste.add(3, m("lv2", "Espagnol / Allemand", 3))
        }
        if (niveau == Niveau.TROISIEME) {
            // Année du BEPC : on renforce les matières principales.
            liste.replaceAll { if (it.id in setOf("pc", "svt", "hg")) it.copy(priorite = 4) else it }
        }
        return liste
    }

    private fun seconde(serie: Serie): List<Matiere> = when (serie) {
        Serie.A, Serie.D -> listOf(
            m("francais", "Français", 5),
            m("anglais", "Anglais", 4),
            m("lv2", "Espagnol / Allemand", 4),
            m("hg", "Histoire-Géographie", 4),
            m("maths", "Mathématiques", 3),
            m("svt", "SVT", 2),
            m("pc", "Physique-Chimie", 2),
            m("edhc", "EDHC", 1),
        )
        Serie.C -> listOf(
            m("maths", "Mathématiques", 5),
            m("pc", "Physique-Chimie", 5),
            m("svt", "SVT", 3),
            m("francais", "Français", 4),
            m("anglais", "Anglais", 3),
            m("hg", "Histoire-Géographie", 2),
            m("lv2", "Espagnol / Allemand", 2),
            m("edhc", "EDHC", 1),
        )
    }

    private fun cycleTerminal(niveau: Niveau, serie: Serie): List<Matiere> {
        val philo = if (niveau == Niveau.TERMINALE) 5 else 3
        return when (serie) {
            Serie.A -> listOf(
                m("francais", "Français", 5),
                m("philo", "Philosophie", philo),
                m("anglais", "Anglais", 4),
                m("lv2", "Espagnol / Allemand", 4),
                m("hg", "Histoire-Géographie", 4),
                m("maths", "Mathématiques", 3),
                m("svt", "SVT", 1),
                m("edhc", "EDHC", 1),
            )
            Serie.C -> listOf(
                m("maths", "Mathématiques", 5),
                m("pc", "Physique-Chimie", 5),
                m("svt", "SVT", 3),
                m("francais", "Français", 3),
                m("philo", "Philosophie", if (niveau == Niveau.TERMINALE) 3 else 2),
                m("anglais", "Anglais", 3),
                m("hg", "Histoire-Géographie", 2),
                m("lv2", "Espagnol / Allemand", 1),
                m("edhc", "EDHC", 1),
            )
            Serie.D -> listOf(
                m("svt", "SVT", 5),
                m("maths", "Mathématiques", 4),
                m("pc", "Physique-Chimie", 4),
                m("francais", "Français", 3),
                m("philo", "Philosophie", if (niveau == Niveau.TERMINALE) 3 else 2),
                m("anglais", "Anglais", 3),
                m("hg", "Histoire-Géographie", 2),
                m("lv2", "Espagnol / Allemand", 1),
                m("edhc", "EDHC", 1),
            )
        }
    }
}
