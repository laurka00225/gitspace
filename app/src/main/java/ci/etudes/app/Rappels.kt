package ci.etudes.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ci.etudes.core.MatieresParDefaut
import ci.etudes.core.Planificateur
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Un rappel à afficher à une date et heure précises. */
data class Rappel(val quand: LocalDateTime, val titre: String, val texte: String)

/**
 * Rappels et alarmes. On programme toujours un seul réveil : le prochain rappel.
 * Quand il sonne, on affiche la notification puis on programme le suivant.
 */
object Rappels {

    private const val CANAL_RAPPELS = "rappels"
    private const val CANAL_ALARMES = "alarmes"
    private const val CANAL_MINUTEUR = "minuteur"
    private const val EXTRA_TITRE = "titre"
    private const val EXTRA_TEXTE = "texte"
    private const val EXTRA_MINUTEUR = "minuteur"
    private const val CODE_RAPPEL = 1
    private const val CODE_MINUTEUR = 2

    val HEURE_RAPPEL_COUCHER: LocalTime = LocalTime.of(21, 50)
    val HEURE_RAPPEL_VEILLE: LocalTime = LocalTime.of(19, 0)

    fun creerCanaux(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_RAPPELS, "Rappels de séances", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avant chaque séance d'études, au coucher et la veille des devoirs"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ALARMES, "Alarmes de séances", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rappels avec la sonnerie d'alarme"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_MINUTEUR, "Minuteur de séance", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Fin d'une séance chronométrée"
            },
        )
    }

    fun peutNotifier(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun peutAlarmePrecise(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Liste des rappels à venir (sur 8 jours) à partir de [depuis]. */
    fun aVenir(context: Context, depuis: LocalDateTime, limite: Int = 50): List<Rappel> {
        val stockage = Stockage(context)
        if (!stockage.estConfigure) return emptyList()
        val r = stockage.lireRappels()
        if (!r.actif) return emptyList()
        val reglages = stockage.lireReglages()
        val matieres = stockage.lireMatieres() ?: MatieresParDefaut.pour(reglages.niveau, reglages.serie)
        val parId = matieres.associateBy { it.id }
        val changements = stockage.lireChangements()
        val evaluations = stockage.lireEvaluations()
        val nomMatiere = { id: String -> parId[id]?.nom ?: "une matière" }

        val liste = mutableListOf<Rappel>()
        val debut = depuis.toLocalDate()
        var lundi = Planificateur.lundiDe(debut)
        while (!lundi.isAfter(debut.plusDays(8))) {
            val plan = Planificateur.semaine(lundi, reglages, matieres, evaluations)
            for (jour in plan.jours) {
                if (jour.date.isBefore(debut) || jour.date.isAfter(debut.plusDays(8))) continue
                for (s in jour.seances) {
                    val matiere = changements[s.cle]?.let { parId[it] } ?: s.matiere
                    val quand = LocalDateTime.of(s.date, s.debut).minusMinutes(r.minutesAvant.toLong())
                    val dans = if (r.minutesAvant == 0) "maintenant" else "dans ${r.minutesAvant} min"
                    liste += Rappel(
                        quand,
                        "${matiere.nom} $dans",
                        "Séance de ${s.debut.enTexte()} à ${s.fin.enTexte()} : ${s.activite}",
                    )
                }
                if (r.coucher && jour.seances.isNotEmpty()) {
                    liste += Rappel(
                        LocalDateTime.of(jour.date, HEURE_RAPPEL_COUCHER),
                        "Bientôt 22h",
                        "Range tes affaires et prépare ton sac pour demain. Bonne nuit !",
                    )
                }
            }
            lundi = lundi.plusWeeks(1)
        }
        if (r.veilleDevoirs) {
            evaluations.filter { !it.date.isBefore(debut) }.forEach { e ->
                liste += Rappel(
                    LocalDateTime.of(e.date.minusDays(1), HEURE_RAPPEL_VEILLE),
                    "Demain : ${e.type.libelle.lowercase()} de ${nomMatiere(e.matiereId)}",
                    if (e.note.isNotBlank()) e.note else "Révise bien ce soir et prépare ton matériel.",
                )
            }
        }
        return liste.filter { it.quand.isAfter(depuis) }.sortedBy { it.quand }.take(limite)
    }

    /** Programme le prochain rappel (ou annule s'il n'y en a pas). */
    fun planifierProchain(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = intentRappel(context, null)
        am.cancel(pi)
        val prochain = aVenir(context, LocalDateTime.now(), limite = 1).firstOrNull() ?: return
        programmer(context, am, millis(prochain.quand), intentRappel(context, prochain))
    }

    fun demarrerMinuteur(context: Context, finMillis: Long, titre: String) {
        val stockage = Stockage(context)
        stockage.finMinuteur = finMillis
        stockage.titreMinuteur = titre
        val am = context.getSystemService(AlarmManager::class.java)
        programmer(context, am, finMillis, intentMinuteur(context))
    }

    fun arreterMinuteur(context: Context) {
        Stockage(context).finMinuteur = 0L
        context.getSystemService(AlarmManager::class.java).cancel(intentMinuteur(context))
    }

    private fun programmer(context: Context, am: AlarmManager, quand: Long, pi: PendingIntent) {
        if (peutAlarmePrecise(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quand, pi)
        }
    }

    private fun millis(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun intentRappel(context: Context, rappel: Rappel?): PendingIntent {
        val intent = Intent(context, RecepteurRappel::class.java)
        if (rappel != null) {
            intent.putExtra(EXTRA_TITRE, rappel.titre).putExtra(EXTRA_TEXTE, rappel.texte)
        }
        return PendingIntent.getBroadcast(
            context, CODE_RAPPEL, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun intentMinuteur(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, CODE_MINUTEUR,
            Intent(context, RecepteurRappel::class.java).putExtra(EXTRA_MINUTEUR, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun surReveil(context: Context, intent: Intent) {
        creerCanaux(context)
        if (intent.getBooleanExtra(EXTRA_MINUTEUR, false)) {
            val stockage = Stockage(context)
            val titre = stockage.titreMinuteur.ifBlank { "Séance" }
            stockage.finMinuteur = 0L
            afficher(context, CANAL_MINUTEUR, 1002, "Séance terminée !", "$titre : bravo. Fais une petite pause avant la suite.")
            return
        }
        val titre = intent.getStringExtra(EXTRA_TITRE)
        val texte = intent.getStringExtra(EXTRA_TEXTE)
        if (titre != null && texte != null) {
            val canal = if (Stockage(context).lireRappels().alarme) CANAL_ALARMES else CANAL_RAPPELS
            afficher(context, canal, 1001, titre, texte)
        }
        planifierProchain(context)
    }

    private fun afficher(context: Context, canal: String, id: Int, titre: String, texte: String) {
        if (!peutNotifier(context)) return
        val ouvrir = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titre)
            .setContentText(texte)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texte))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (canal == CANAL_ALARMES) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(ouvrir)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notif)
        } catch (_: SecurityException) {
            // Permission retirée entre-temps : rien à faire.
        }
    }

    private fun LocalTime.enTexte(): String = "%02dh%02d".format(hour, minute)
}

/** Reçoit les réveils (rappel, minuteur) et le redémarrage du téléphone. */
class RecepteurRappel : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> Rappels.planifierProchain(context)
            else -> Rappels.surReveil(context, intent)
        }
    }
}
