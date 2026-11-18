package ci.etudes.app

import android.content.Context
import ci.etudes.core.JourLibrePerso
import ci.etudes.core.Matiere
import ci.etudes.core.Niveau
import ci.etudes.core.Plage
import ci.etudes.core.Reglages
import ci.etudes.core.Serie
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

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
    }
}
