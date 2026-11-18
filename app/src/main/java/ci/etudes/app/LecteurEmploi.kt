package ci.etudes.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import ci.etudes.core.AnalyseurEmploi
import ci.etudes.core.BlocTexte
import ci.etudes.core.CoursScolaire
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Résultat de la lecture d'un emploi du temps. */
data class LectureEmploi(val cours: List<CoursScolaire>, val texteLu: String)

/**
 * Lit un emploi du temps (photo JPEG/PNG ou PDF) sur le téléphone, sans Internet :
 * reconnaissance du texte, puis reconstitution du tableau.
 */
object LecteurEmploi {

    /** Taille maximale du côté le plus long, pour garder une bonne lecture sans saturer la mémoire. */
    private const val COTE_MAX = 2600

    suspend fun lire(context: Context, uri: Uri): LectureEmploi = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri) ?: ""
        val images = if (type == "application/pdf" || uri.toString().lowercase().endsWith(".pdf")) {
            pagesPdf(context, uri)
        } else {
            listOfNotNull(image(context, uri))
        }
        require(images.isNotEmpty()) { "Impossible d'ouvrir ce fichier." }

        var meilleur = LectureEmploi(emptyList(), "")
        for (bitmap in images) {
            val blocs = reconnaitre(bitmap)
            val cours = AnalyseurEmploi.analyser(blocs)
            val lecture = LectureEmploi(cours, blocs.joinToString(" ") { it.texte })
            if (cours.size > meilleur.cours.size || meilleur.texteLu.isEmpty()) meilleur = lecture
            bitmap.recycle()
        }
        meilleur
    }

    private fun image(context: Context, uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        var echantillon = 1
        while (maxOf(options.outWidth, options.outHeight) / echantillon > COTE_MAX * 2) echantillon *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
        } ?: return null
        // Remet la photo à l'endroit selon l'orientation de l'appareil photo.
        val rotation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val redim = redimensionner(bitmap)
        if (rotation == 0f) return redim
        val m = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(redim, 0, 0, redim.width, redim.height, m, true).also {
            if (it !== redim) redim.recycle()
        }
    }

    private fun redimensionner(b: Bitmap): Bitmap {
        val cote = maxOf(b.width, b.height)
        if (cote <= COTE_MAX) return b
        val f = COTE_MAX.toFloat() / cote
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt(), (b.height * f).toInt(), true).also {
            if (it !== b) b.recycle()
        }
    }

    /** Les 3 premières pages du PDF, rendues en images. */
    private fun pagesPdf(context: Context, uri: Uri): List<Bitmap> {
        val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        return pfd.use { fd ->
            val renderer = PdfRenderer(fd)
            try {
                (0 until minOf(renderer.pageCount, 3)).map { i ->
                    val page = renderer.openPage(i)
                    try {
                        val f = COTE_MAX.toFloat() / maxOf(page.width, page.height)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * f).toInt().coerceAtLeast(1),
                            (page.height * f).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    } finally {
                        page.close()
                    }
                }
            } finally {
                renderer.close()
            }
        }
    }

    private suspend fun reconnaitre(bitmap: Bitmap): List<BlocTexte> {
        val reconnaisseur = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val texte = suspendCancellableCoroutine { cont ->
                reconnaisseur.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            // On garde les lignes (« 7h30 - 8h25 ») et les mots (« LUNDI », « MATHS »).
            val res = mutableListOf<BlocTexte>()
            for (bloc in texte.textBlocks) for (ligne in bloc.lines) {
                // Une ligne de plusieurs mots peut déborder sur deux cases : on ne la garde
                // entière que si c'est une plage horaire ou un seul mot.
                if (ligne.elements.size <= 1 || AnalyseurEmploi.heures(ligne.text).isNotEmpty()) {
                    ligne.boundingBox?.let { r ->
                        res += BlocTexte(ligne.text, r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
                    }
                }
                if (ligne.elements.size > 1) {
                    for (mot in ligne.elements) {
                        mot.boundingBox?.let { r ->
                            res += BlocTexte(mot.text, r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
                        }
                    }
                }
            }
            return res
        } finally {
            reconnaisseur.close()
        }
    }
}
