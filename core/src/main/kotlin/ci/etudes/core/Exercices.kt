package ci.etudes.core

import kotlin.random.Random

/** Une question à choix multiple. [bonne] est l'indice de la bonne réponse dans [choix]. */
data class Question(
    val enonce: String,
    val choix: List<String>,
    val bonne: Int,
    val explication: String,
) {
    init {
        require(choix.size >= 2) { "Au moins deux choix : $enonce" }
        require(bonne in choix.indices) { "Bonne réponse hors des choix : $enonce" }
        require(choix.toSet().size == choix.size) { "Choix en double : $enonce" }
    }

    /** Même question avec les choix dans un ordre aléatoire. */
    fun melangee(random: Random): Question {
        val ordre = choix.indices.shuffled(random)
        return copy(choix = ordre.map { choix[it] }, bonne = ordre.indexOf(bonne))
    }
}

/** Cycles d'enseignement, pour adapter la difficulté des exercices. */
enum class Cycle(val libelle: String) {
    DEBUT_COLLEGE("6e – 5e"),
    FIN_COLLEGE("4e – 3e"),
    LYCEE("2nde – 1ère"),
    TERMINALE("Terminale");

    companion object {
        fun de(niveau: Niveau): Cycle = when (niveau) {
            Niveau.SIXIEME, Niveau.CINQUIEME -> DEBUT_COLLEGE
            Niveau.QUATRIEME, Niveau.TROISIEME -> FIN_COLLEGE
            Niveau.SECONDE, Niveau.PREMIERE -> LYCEE
            Niveau.TERMINALE -> TERMINALE
        }
    }
}

/** Matières qui ont des exercices (les identifiants sont ceux des matières du planning). */
enum class MatiereExercice(val id: String, val nom: String) {
    FRANCAIS("francais", "Français"),
    MATHS("maths", "Mathématiques"),
    PC("pc", "Physique-Chimie"),
    SVT("svt", "SVT"),
    ANGLAIS("anglais", "Anglais");

    companion object {
        fun deId(id: String): MatiereExercice? = entries.firstOrNull { it.id == id }
    }
}

object Exercices {

    const val QUESTIONS_PAR_SERIE = 10

    /** Une série d'exercices : questions de la banque et calculs générés, dans le désordre. */
    fun serie(
        matiere: MatiereExercice,
        niveau: Niveau,
        random: Random = Random.Default,
        taille: Int = QUESTIONS_PAR_SERIE,
    ): List<Question> {
        val cycle = Cycle.de(niveau)
        val fixes = Banque.questions(matiere, cycle).shuffled(random)
        val liste = if (matiere == MatiereExercice.MATHS) {
            // Moitié banque, moitié calculs générés (toujours nouveaux).
            val generees = List(taille - taille / 2) { CalculsMaths.generer(cycle, random) }
            (fixes.take(taille / 2) + generees).shuffled(random)
        } else {
            fixes.take(taille)
        }
        return liste.map { it.melangee(random) }
    }
}

/** Calculs de mathématiques générés au hasard, adaptés au cycle. */
object CalculsMaths {

    fun generer(cycle: Cycle, random: Random): Question {
        val generateurs: List<(Random) -> Question> = when (cycle) {
            Cycle.DEBUT_COLLEGE -> listOf(::multiplication, ::pourcentage, ::perimetre, ::simplification)
            Cycle.FIN_COLLEGE -> listOf(::equation, ::pythagore, ::puissances, ::simplification)
            Cycle.LYCEE -> listOf(::equation, ::fonctionAffine, ::racinesTrinome, ::pythagore)
            Cycle.TERMINALE -> listOf(::derivee, ::suiteArithmetique, ::racinesTrinome, ::logarithme)
        }
        return generateurs[random.nextInt(generateurs.size)](random)
    }

    /** Trois mauvaises réponses proches de la bonne, toutes différentes. */
    private fun distracteurs(bonne: Int, random: Random): List<Int> {
        val res = mutableSetOf<Int>()
        val ecarts = listOf(-10, -5, -3, -2, -1, 1, 2, 3, 5, 10).shuffled(random)
        for (e in ecarts) {
            if (res.size == 3) break
            res += bonne + e
        }
        return res.toList()
    }

    private fun qcmEntier(enonce: String, bonne: Int, explication: String, random: Random): Question {
        val choix = listOf(bonne) + distracteurs(bonne, random)
        return Question(enonce, choix.map { it.toString() }, 0, explication)
    }

    private fun multiplication(r: Random): Question {
        val a = r.nextInt(12, 99)
        val b = r.nextInt(3, 13)
        return qcmEntier("Calcule $a × $b.", a * b, "$a × $b = ${a * b}.", r)
    }

    private fun pourcentage(r: Random): Question {
        val p = listOf(10, 20, 25, 50, 75).random(r)
        val base = r.nextInt(1, 40) * 20
        val res = base * p / 100
        return qcmEntier("Combien font $p % de $base ?", res, "$p % de $base = $base × $p ÷ 100 = $res.", r)
    }

    private fun perimetre(r: Random): Question {
        val l = r.nextInt(3, 30)
        val w = r.nextInt(2, l)
        val p = 2 * (l + w)
        return qcmEntier(
            "Quel est le périmètre d'un rectangle de longueur $l cm et de largeur $w cm ? (en cm)",
            p, "P = 2 × (L + l) = 2 × ($l + $w) = $p cm.", r,
        )
    }

    private fun pgcd(a: Int, b: Int): Int = if (b == 0) a else pgcd(b, a % b)

    private fun simplification(r: Random): Question {
        val k = r.nextInt(2, 10)
        var n = r.nextInt(1, 10)
        var d = r.nextInt(2, 12)
        while (d == n || pgcd(n, d) != 1) {
            n = r.nextInt(1, 10)
            d = r.nextInt(2, 12)
        }
        val bonne = "$n/$d"
        val fausses = linkedSetOf("${n * k}/$d", "$n/${d * k}", "${n + 1}/$d", "$n/${d + 1}", "${d}/$n")
        fausses.remove(bonne)
        return Question(
            "Simplifie la fraction ${n * k}/${d * k}.",
            listOf(bonne) + fausses.take(3),
            0,
            "On divise le numérateur et le dénominateur par $k : ${n * k}/${d * k} = $bonne.",
        )
    }

    private fun equation(r: Random): Question {
        val a = r.nextInt(2, 10)
        val x = r.nextInt(-9, 13)
        val b = r.nextInt(-20, 21)
        val c = a * x + b
        val signe = if (b >= 0) "+ $b" else "− ${-b}"
        return qcmEntier(
            "Résous : ${a}x $signe = $c. Que vaut x ?",
            x,
            "${a}x = $c ${if (b >= 0) "− $b" else "+ ${-b}"} = ${c - b}, donc x = ${c - b} ÷ $a = $x.",
            r,
        )
    }

    private fun pythagore(r: Random): Question {
        val (a, b, c) = listOf(Triple(3, 4, 5), Triple(5, 12, 13), Triple(8, 15, 17), Triple(6, 8, 10)).random(r)
        val k = r.nextInt(1, 4)
        return qcmEntier(
            "Un triangle est rectangle. Ses côtés de l'angle droit mesurent ${a * k} cm et ${b * k} cm. " +
                "Combien mesure l'hypoténuse ? (en cm)",
            c * k,
            "Pythagore : ${a * k}² + ${b * k}² = ${a * a * k * k + b * b * k * k} = ${c * k}², donc ${c * k} cm.",
            r,
        )
    }

    private fun puissances(r: Random): Question {
        val base = listOf(2, 3, 5, 10).random(r)
        val m = r.nextInt(2, 7)
        val n = r.nextInt(2, 7)
        val bonne = "$base^${m + n}"
        val fausses = listOf("$base^${m * n}", "${base * 2}^${m + n}", "$base^${m + n + 1}")
            .filter { it != bonne }.distinct()
        return Question(
            "Écris $base^$m × $base^$n sous la forme d'une seule puissance.",
            listOf(bonne) + fausses,
            0,
            "aᵐ × aⁿ = aᵐ⁺ⁿ, donc $base^$m × $base^$n = $bonne.",
        )
    }

    private fun fonctionAffine(r: Random): Question {
        val a = r.nextInt(-5, 6).let { if (it == 0) 2 else it }
        val b = r.nextInt(-10, 11)
        val x = r.nextInt(-5, 6)
        val y = a * x + b
        val bTxt = if (b >= 0) "+ $b" else "− ${-b}"
        return qcmEntier(
            "Soit f(x) = ${a}x $bTxt. Calcule f($x).",
            y,
            "f($x) = $a × ($x) $bTxt = $y.",
            r,
        )
    }

    private fun racinesTrinome(r: Random): Question {
        var x1 = r.nextInt(-6, 7)
        var x2 = r.nextInt(-6, 7)
        if (x1 == x2) x2 = x1 + 1
        if (x1 > x2) x1 = x2.also { x2 = x1 }
        val s = x1 + x2
        val p = x1 * x2
        val bTxt = when {
            s == 0 -> ""
            s > 0 -> " − ${s}x"
            else -> " + ${-s}x"
        }
        val cTxt = when {
            p == 0 -> ""
            p > 0 -> " + $p"
            else -> " − ${-p}"
        }
        val bonne = "$x1 et $x2"
        val fausses = linkedSetOf("${-x2} et ${-x1}", "${x1 - 1} et ${x2 + 1}", "${x1 + 1} et ${x2 + 1}", "$x1 et ${x2 + 2}")
        fausses.remove(bonne)
        return Question(
            "Quelles sont les solutions de x²$bTxt$cTxt = 0 ?",
            listOf(bonne) + fausses.take(3),
            0,
            "Δ = ${s * s - 4 * p} ; les racines sont $x1 et $x2 (leur somme vaut $s et leur produit $p).",
        )
    }

    private fun derivee(r: Random): Question {
        val a = r.nextInt(2, 9)
        val n = r.nextInt(2, 6)
        val coef = a * n
        val exposant = if (n - 1 == 1) "x" else "x^${n - 1}"
        val bonne = "$coef$exposant"
        val fausses = linkedSetOf("$a" + "x^${n - 1}", "$coef" + "x^$n", "${a + n}$exposant", "$n$exposant")
        fausses.remove(bonne)
        return Question(
            "Quelle est la dérivée de f(x) = ${a}x^$n ?",
            listOf(bonne) + fausses.take(3),
            0,
            "(xⁿ)' = n·xⁿ⁻¹, donc f'(x) = $a × $n × $exposant = $bonne.",
        )
    }

    private fun suiteArithmetique(r: Random): Question {
        val u0 = r.nextInt(-10, 21)
        val raison = r.nextInt(-5, 8).let { if (it == 0) 3 else it }
        val n = r.nextInt(5, 21)
        val un = u0 + n * raison
        return qcmEntier(
            "(uₙ) est arithmétique de premier terme u₀ = $u0 et de raison $raison. Que vaut u$n ?",
            un,
            "uₙ = u₀ + n × r = $u0 + $n × ($raison) = $un.",
            r,
        )
    }

    private fun logarithme(r: Random): Question {
        val a = r.nextInt(2, 9)
        val b = r.nextInt(2, 9)
        return qcmEntier(
            "Simplifie ln(e^$a × e^$b).",
            a + b,
            "e^$a × e^$b = e^${a + b} et ln(e^k) = k, donc le résultat est ${a + b}.",
            r,
        )
    }
}
