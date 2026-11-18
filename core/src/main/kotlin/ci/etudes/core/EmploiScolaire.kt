package ci.etudes.core

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.abs

/** Un cours de l'emploi du temps de l'école. [matiereId] est nul pour EPS, arts, etc. */
data class CoursScolaire(
    val jour: DayOfWeek,
    val debut: LocalTime,
    val fin: LocalTime,
    val matiereId: String?,
    val libelle: String,
)

/** Un mot lu sur l'image, avec sa position (en pixels). */
data class BlocTexte(
    val texte: String,
    val gauche: Float,
    val haut: Float,
    val droite: Float,
    val bas: Float,
) {
    val cx: Float get() = (gauche + droite) / 2
    val cy: Float get() = (haut + bas) / 2
    val hauteur: Float get() = bas - haut

    /** Échange lignes et colonnes (tableaux où les jours sont à gauche). */
    fun transpose() = BlocTexte(texte, haut, gauche, bas, droite)
}

/** Calculs liés à l'emploi du temps de l'école. */
object EmploiScolaire {

    /** Pauses où l'on n'étudie pas : déjeuner et repos, retour à la maison, dîner. */
    private val PAUSES = listOf(
        LocalTime.of(12, 0) to LocalTime.of(15, 0),
        LocalTime.of(17, 30) to LocalTime.of(18, 0),
        LocalTime.of(19, 30) to LocalTime.of(20, 15),
    )

    /**
     * Plages d'études d'une journée avec des cours : de la fin du dernier cours (plus le trajet)
     * jusqu'à 22h, sans les pauses repas.
     */
    fun plagesLibres(cours: List<CoursScolaire>, trajetMin: Int): List<Plage> {
        if (cours.isEmpty()) return emptyList()
        val finCours = cours.maxOf { it.fin }
        val debut = finCours.plusMinutes(trajetMin.toLong()).let { if (it.isBefore(finCours)) Planificateur.HEURE_COUCHER else it }
        var morceaux = listOf(debut to Planificateur.HEURE_COUCHER)
        for ((pd, pf) in PAUSES) {
            morceaux = morceaux.flatMap { (d, f) ->
                if (!pf.isAfter(d) || !pd.isBefore(f)) {
                    listOf(d to f)
                } else {
                    listOfNotNull(
                        if (pd.isAfter(d)) d to pd else null,
                        if (pf.isBefore(f)) pf to f else null,
                    )
                }
            }
        }
        return morceaux
            .filter { java.time.Duration.between(it.first, it.second).toMinutes() >= 20 }
            .map { Plage(it.first, it.second) }
    }

    fun coursDu(jour: DayOfWeek, emploi: List<CoursScolaire>) =
        emploi.filter { it.jour == jour }.sortedBy { it.debut }
}

/**
 * Retrouve l'emploi du temps à partir des mots lus sur une photo ou un PDF :
 * les jours en en-tête, les heures sur le côté, les matières dans les cases.
 */
object AnalyseurEmploi {

    private val JOURS = mapOf(
        "LUNDI" to DayOfWeek.MONDAY, "LUN" to DayOfWeek.MONDAY,
        "MARDI" to DayOfWeek.TUESDAY, "MAR" to DayOfWeek.TUESDAY,
        "MERCREDI" to DayOfWeek.WEDNESDAY, "MER" to DayOfWeek.WEDNESDAY,
        "JEUDI" to DayOfWeek.THURSDAY, "JEU" to DayOfWeek.THURSDAY,
        "VENDREDI" to DayOfWeek.FRIDAY, "VEN" to DayOfWeek.FRIDAY,
        "SAMEDI" to DayOfWeek.SATURDAY, "SAM" to DayOfWeek.SATURDAY,
    )

    /** Mots-clés (sans accents, en majuscules) → identifiant de matière et libellé. */
    private val MOTS_CLES: List<Triple<Set<String>, String?, String>> = listOf(
        Triple(setOf("FRANCAIS", "FRANC", "FR", "FRA", "FRS", "FRANCAI", "ORTHOGRAPHE", "GRAMMAIRE", "DICTEE", "LECTURE"), "francais", "Français"),
        Triple(setOf("MATHS", "MATH", "MATHEMATIQUES", "MATHEMATIQUE", "MATHS", "MTH", "MATHEM"), "maths", "Mathématiques"),
        Triple(setOf("ANGLAIS", "ANG", "ANGL", "ENGLISH"), "anglais", "Anglais"),
        Triple(setOf("ESPAGNOL", "ESP", "ALLEMAND", "ALL", "LV2"), "lv2", "Espagnol / Allemand"),
        Triple(setOf("PC", "PHYSIQUE", "PHYSIQUES", "CHIMIE", "SP", "PHYS", "PHYSIQUECHIMIE", "PCT"), "pc", "Physique-Chimie"),
        Triple(setOf("SVT", "BIOLOGIE", "SVTE"), "svt", "SVT"),
        Triple(setOf("HG", "HISTOIRE", "GEOGRAPHIE", "HISTGEO", "HIST", "GEO", "HISTOGEO", "HGEO"), "hg", "Histoire-Géographie"),
        Triple(setOf("EDHC", "ECM", "CIVISME"), "edhc", "EDHC"),
        Triple(setOf("PHILO", "PHILOSOPHIE"), "philo", "Philosophie"),
        Triple(setOf("EPS", "SPORT"), null, "EPS"),
        Triple(setOf("ARTS", "AP", "MUSIQUE", "EM", "DESSIN"), null, "Arts / Musique"),
        Triple(setOf("INFO", "INFORMATIQUE", "TICE"), null, "Informatique"),
    )

    private val HEURE = Regex("""(\d{1,2})\s*[hH:.]\s*(\d{2})?""")

    fun normaliser(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")

    /** Matière reconnue dans un mot, ou null. */
    fun reconnaitreMatiere(texte: String): Pair<String?, String>? {
        val parties = (listOf(texte) + texte.split('-', '/', '.', ' ', '_')).map(::normaliser).filter { it.isNotEmpty() }
        for (p in parties) {
            MOTS_CLES.firstOrNull { p in it.first }?.let { return it.second to it.third }
        }
        return null
    }

    /** Heures trouvées dans un texte ("7h30-8h25" → 07:30, 08:25). */
    fun heures(texte: String): List<LocalTime> =
        HEURE.findAll(texte).mapNotNull { m ->
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].ifEmpty { "0" }.toInt()
            if (h in 6..20 && min in 0..59) LocalTime.of(h, min) else null
        }.toList()

    fun analyser(blocs: List<BlocTexte>): List<CoursScolaire> {
        val direct = analyserOrientation(blocs)
        val transpose = analyserOrientation(blocs.map { it.transpose() })
        return if (transpose.size > direct.size) transpose else direct
    }

    private data class Ligne(val cy: Float, val debut: LocalTime, val fin: LocalTime?)

    private fun analyserOrientation(blocs: List<BlocTexte>): List<CoursScolaire> {
        // 1. En-têtes des jours (première occurrence la plus haute de chaque jour).
        val entetes = blocs
            .mapNotNull { b -> JOURS[normaliser(b.texte)]?.let { it to b } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, l) -> l.minBy { it.cy } }
        if (entetes.size < 2) return emptyList()
        val xs = entetes.values.map { it.cx }
        val ys = entetes.values.map { it.cy }
        // Les jours doivent être alignés horizontalement dans cette orientation.
        if ((ys.max() - ys.min()) > (xs.max() - xs.min()) / 2) return emptyList()
        val hauteurType = blocs.map { it.hauteur }.sorted().let { it[it.size / 2] }.coerceAtLeast(1f)
        val basEntete = entetes.values.maxOf { it.bas }

        val colonnes = entetes.entries.sortedBy { it.value.cx }
        val ecartColonnes = colonnes.zipWithNext { a, b -> b.value.cx - a.value.cx }.let { e ->
            if (e.isEmpty()) Float.MAX_VALUE else e.sorted()[e.size / 2]
        }

        // 2. Lignes horaires : on regroupe les heures qui sont à la même hauteur.
        val blocsHeures = blocs.filter { it.cy > basEntete && heures(it.texte).isNotEmpty() }.sortedBy { it.cy }
        val groupes = mutableListOf<MutableList<BlocTexte>>()
        for (b in blocsHeures) {
            val g = groupes.lastOrNull()
            if (g != null && abs(g.map { it.cy }.average().toFloat() - b.cy) < hauteurType * 0.9f) g += b else groupes += mutableListOf(b)
        }
        val lignes = groupes.mapNotNull { g ->
            val h = g.sortedBy { it.cx }.flatMap { heures(it.texte) }.distinct()
            if (h.isEmpty()) null else Ligne(g.map { it.cy }.average().toFloat(), h[0], h.getOrNull(1)?.takeIf { it.isAfter(h[0]) })
        }.distinctBy { it.debut }.sortedBy { it.cy }
        if (lignes.size < 2) return emptyList()

        // 3. Matières dans les cases.
        val cases = mutableMapOf<Pair<DayOfWeek, Int>, Pair<String?, String>>()
        for (b in blocs) {
            if (b.cy <= basEntete) continue
            val matiere = reconnaitreMatiere(b.texte) ?: continue
            val colonne = colonnes.minBy { abs(it.value.cx - b.cx) }
            if (abs(colonne.value.cx - b.cx) > ecartColonnes * 0.75f) continue
            val iLigne = lignes.indices.minBy { abs(lignes[it].cy - b.cy) }
            cases.putIfAbsent(colonne.key to iLigne, matiere)
        }

        // 4. Cours, puis fusion des cours qui se suivent dans la même matière.
        val cours = cases.map { (cle, m) ->
            val (jour, i) = cle
            val l = lignes[i]
            val suivante = lignes.getOrNull(i + 1)?.debut
            val fin = l.fin
                ?: listOfNotNull(suivante, l.debut.plusMinutes(60)).filter { it.isAfter(l.debut) }.minOrNull()
                ?: l.debut.plusMinutes(60)
            CoursScolaire(jour, l.debut, fin, m.first, m.second)
        }.sortedWith(compareBy({ it.jour }, { it.debut }))

        val fusion = mutableListOf<CoursScolaire>()
        for (c in cours) {
            val p = fusion.lastOrNull()
            if (p != null && p.jour == c.jour && p.libelle == c.libelle &&
                !c.debut.isAfter(p.fin.plusMinutes(10))
            ) {
                fusion[fusion.lastIndex] = p.copy(fin = maxOf(p.fin, c.fin))
            } else {
                fusion += c
            }
        }
        return fusion
    }
}
