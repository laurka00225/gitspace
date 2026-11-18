package ci.etudes.app

import android.content.Context
import ci.etudes.core.Evaluation
import ci.etudes.core.JourLibrePerso
import ci.etudes.core.Matiere
import ci.etudes.core.Niveau
import ci.etudes.core.Plage
import ci.etudes.core.Reglages
import ci.etudes.core.Serie
import ci.etudes.core.TypeEvaluation
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/** Réglages des rappels et alarmes. */
data class ReglagesRappels(
    val actif: Boolean = true,
    /** Combien de minutes avant le début de la séance. */
    val minutesAvant: Int = 5,
    /** Sonnerie d'alarme (plus forte, même en mode silencieux selon le téléphone). */
    val alarme: Boolean = false,
    /** Rappel à 21h50 pour aller dormir à 22h. */
    val coucher: Boolean = true,
    /** Rappel à 19h la veille d'un devoir ou d'un contrôle. */
    val veilleDevoirs: Boolean = true,
)

/** Sauvegarde locale (sur le téléphone) des réglages, matières et séances faites. */
class Stockage(context: Context) {

    private val prefs = context.getSharedPreferences("etudes", Context.MODE_PRIVATE)

    val estConfigure: Boolean get() = prefs.getBoolean(CLE_CONFIGURE, false)

    fun lireReglages(): Reglages {
        val json = prefs.getString(CLE_REGLAGES, null) ?: return Reglages()
        return runCatching { reglagesDepuis(JSONObject(json)) }.getOrDefault(Reglages())
    }

    fun lireMatieres(): List<Matiere>? {
        val json = prefs.getString(CLE_MATIERES, null) ?: return null
        return runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Matiere(
                    id = o.getString("id"),
                    nom = o.getString("nom"),
                    priorite = o.getInt("priorite"),
                    active = o.optBoolean("active", true),
                    personnalisee = o.optBoolean("perso", false),
                )
            }
        }.getOrNull()
    }

    fun lireFaites(): Set<String> = prefs.getStringSet(CLE_FAITES, emptySet())!!.toSet()

    fun lireChangements(): Map<String, String> {
        val json = prefs.getString(CLE_CHANGEMENTS, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { o.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun enregistrer(
        reglages: Reglages,
        matieres: List<Matiere>,
        faites: Set<String>,
        changements: Map<String, String>,
    ) {
        val matieresJson = JSONArray().apply {
            matieres.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("nom", it.nom)
                        .put("priorite", it.priorite)
                        .put("active", it.active)
                        .put("perso", it.personnalisee),
                )
            }
        }
        prefs.edit()
            .putBoolean(CLE_CONFIGURE, true)
            .putString(CLE_REGLAGES, reglagesVers(reglages).toString())
            .putString(CLE_MATIERES, matieresJson.toString())
            .putStringSet(CLE_FAITES, faites)
            .putString(CLE_CHANGEMENTS, JSONObject(changements as Map<*, *>).toString())
            .apply()
    }

    // ---------------------------------------------------------- Évaluations

    fun lireEvaluations(): List<Evaluation> {
        val json = prefs.getString(CLE_EVALUATIONS, null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(json)
            (0 until a.length()).mapNotNull { i ->
                val o = a.getJSONObject(i)
                runCatching {
                    Evaluation(
                        id = o.getString("id"),
                        matiereId = o.getString("matiere"),
                        date = LocalDate.parse(o.getString("date")),
                        type = TypeEvaluation.valueOf(o.getString("type")),
                        note = o.optString("note", ""),
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun enregistrerEvaluations(liste: List<Evaluation>) {
        val a = JSONArray()
        liste.forEach {
            a.put(
                JSONObject().put("id", it.id).put("matiere", it.matiereId).put("date", it.date.toString())
                    .put("type", it.type.name).put("note", it.note),
            )
        }
        prefs.edit().putString(CLE_EVALUATIONS, a.toString()).apply()
    }

    // ---------------------------------------------------------- Rappels

    fun lireRappels(): ReglagesRappels {
        val d = ReglagesRappels()
        return ReglagesRappels(
            actif = prefs.getBoolean("rappels_actif", d.actif),
            minutesAvant = prefs.getInt("rappels_minutes", d.minutesAvant),
            alarme = prefs.getBoolean("rappels_alarme", d.alarme),
            coucher = prefs.getBoolean("rappels_coucher", d.coucher),
            veilleDevoirs = prefs.getBoolean("rappels_veille", d.veilleDevoirs),
        )
    }

    fun enregistrerRappels(r: ReglagesRappels) {
        prefs.edit()
            .putBoolean("rappels_actif", r.actif)
            .putInt("rappels_minutes", r.minutesAvant)
            .putBoolean("rappels_alarme", r.alarme)
            .putBoolean("rappels_coucher", r.coucher)
            .putBoolean("rappels_veille", r.veilleDevoirs)
            .apply()
    }

    // ---------------------------------------------------------- Exercices

    /** Meilleur score (sur 10) par matière d'exercices. */
    fun lireScores(): Map<String, Int> {
        val json = prefs.getString(CLE_SCORES, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { o.getInt(it) }
        }.getOrDefault(emptyMap())
    }

    fun enregistrerScores(scores: Map<String, Int>) {
        prefs.edit().putString(CLE_SCORES, JSONObject(scores as Map<*, *>).toString()).apply()
    }

    var seriesExercices: Int
        get() = prefs.getInt("series_exercices", 0)
        set(v) = prefs.edit().putInt("series_exercices", v).apply()

    // ---------------------------------------------------------- Divers

    var couleursTelephone: Boolean
        get() = prefs.getBoolean("couleurs_telephone", false)
        set(v) = prefs.edit().putBoolean("couleurs_telephone", v).apply()

    /** Fin du minuteur en cours (millisecondes depuis 1970), 0 si aucun. */
    var finMinuteur: Long
        get() = prefs.getLong("fin_minuteur", 0L)
        set(v) = prefs.edit().putLong("fin_minuteur", v).apply()

    var titreMinuteur: String
        get() = prefs.getString("titre_minuteur", "") ?: ""
        set(v) = prefs.edit().putString("titre_minuteur", v).apply()

    fun toutEffacer() {
        prefs.edit().clear().apply()
    }

    private fun plagesVers(plages: List<Plage>) = JSONArray().apply {
        plages.forEach { put(JSONObject().put("d", it.debut.toString()).put("f", it.fin.toString())) }
    }

    private fun plagesDepuis(a: JSONArray?, defaut: List<Plage>): List<Plage> {
        if (a == null) return defaut
        return (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            runCatching { Plage(LocalTime.parse(o.getString("d")), LocalTime.parse(o.getString("f"))) }.getOrNull()
        }
    }

    private fun reglagesVers(r: Reglages): JSONObject = JSONObject()
        .put("niveau", r.niveau.name)
        .put("serie", r.serie?.name ?: "")
        .put("duree", r.dureeSeanceMin)
        .put("pause", r.pauseMin)
        .put("cours", plagesVers(r.plagesCours))
        .put("mercredi", plagesVers(r.plagesMercredi))
        .put("libre", plagesVers(r.plagesLibre))
        .put("feriesIgnores", JSONArray(r.feriesIgnores.toList()))
        .put(
            "joursLibres",
            JSONArray().apply {
                r.joursLibresPerso.forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("nom", it.nom)
                            .put("debut", it.debut.toString())
                            .put("fin", it.fin.toString()),
                    )
                }
            },
        )

    private fun reglagesDepuis(o: JSONObject): Reglages {
        val defaut = Reglages()
        val serie = o.optString("serie", "")
        val feries = o.optJSONArray("feriesIgnores")
        val libres = o.optJSONArray("joursLibres")
        return Reglages(
            niveau = runCatching { Niveau.valueOf(o.getString("niveau")) }.getOrDefault(defaut.niveau),
            serie = if (serie.isEmpty()) null else runCatching { Serie.valueOf(serie) }.getOrNull(),
            dureeSeanceMin = o.optInt("duree", defaut.dureeSeanceMin),
            pauseMin = o.optInt("pause", defaut.pauseMin),
            plagesCours = plagesDepuis(o.optJSONArray("cours"), defaut.plagesCours),
            plagesMercredi = plagesDepuis(o.optJSONArray("mercredi"), defaut.plagesMercredi),
            plagesLibre = plagesDepuis(o.optJSONArray("libre"), defaut.plagesLibre),
            feriesIgnores = if (feries == null) emptySet() else (0 until feries.length()).map { feries.getString(it) }.toSet(),
            joursLibresPerso = if (libres == null) emptyList() else (0 until libres.length()).mapNotNull { i ->
                val j = libres.getJSONObject(i)
                runCatching {
                    JourLibrePerso(
                        id = j.getString("id"),
                        nom = j.getString("nom"),
                        debut = LocalDate.parse(j.getString("debut")),
                        fin = LocalDate.parse(j.getString("fin")),
                    )
                }.getOrNull()
            },
        )
    }

    private companion object {
        const val CLE_CONFIGURE = "configure"
        const val CLE_REGLAGES = "reglages"
        const val CLE_MATIERES = "matieres"
        const val CLE_FAITES = "faites"
        const val CLE_CHANGEMENTS = "changements"
        const val CLE_EVALUATIONS = "evaluations"
        const val CLE_SCORES = "scores"
    }
}
