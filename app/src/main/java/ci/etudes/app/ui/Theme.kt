package ci.etudes.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.util.Locale
import ci.etudes.core.Matiere
import ci.etudes.core.TypeJour
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Couleurs inspirées du drapeau ivoirien : orange, blanc, vert.
val Orange = Color(0xFFF77F00)
val OrangeFonce = Color(0xFFE07A10)
val Vert = Color(0xFF009A44)

private val Clair = lightColorScheme(
    primary = OrangeFonce,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0C2),
    onPrimaryContainer = Color(0xFF3A1D00),
    secondary = Vert,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F0D6),
    onSecondaryContainer = Color(0xFF00290F),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
)

private val Sombre = darkColorScheme(
    primary = Color(0xFFFFB877),
    onPrimary = Color(0xFF4A2500),
    primaryContainer = Color(0xFF6A3800),
    onPrimaryContainer = Color(0xFFFFE0C2),
    secondary = Color(0xFF7DDB9C),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF005229),
    onSecondaryContainer = Color(0xFFC8F0D6),
)

@Composable
fun ThemeEtudes(contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Sombre else Clair, content = contenu)
}

private val Palette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFF6D4C41), Color(0xFFD81B60),
    Color(0xFF3949AB), Color(0xFF7CB342), Color(0xFF546E7A), Color(0xFFFDD835),
)

private val CouleursFixes = mapOf(
    "francais" to 1, "maths" to 0, "anglais" to 4, "lv2" to 7, "pc" to 5,
    "svt" to 2, "hg" to 6, "edhc" to 10, "philo" to 8,
)

fun couleurDe(m: Matiere): Color =
    Palette[CouleursFixes[m.id] ?: (Math.floorMod(m.id.hashCode(), Palette.size))]

private val formatJour = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val formatCourt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val formatJourCourt = DateTimeFormatter.ofPattern("EEE d", Locale.FRENCH)
private val formatHeure = DateTimeFormatter.ofPattern("HH'h'mm")

fun LocalDate.enLong(): String = format(formatJour).replaceFirstChar { it.uppercase() }
fun LocalDate.enCourt(): String = format(formatCourt)
fun LocalDate.enJourCourt(): String = format(formatJourCourt).replaceFirstChar { it.uppercase() }
fun LocalTime.enHeure(): String = format(formatHeure)

fun TypeJour.court(): String = when (this) {
    TypeJour.COURS -> "Jour de cours"
    TypeJour.MERCREDI -> "Après-midi libre"
    TypeJour.LIBRE -> "Journée libre"
}
