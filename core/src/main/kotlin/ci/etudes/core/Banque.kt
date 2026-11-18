package ci.etudes.core

/**
 * Banque de questions par matière et par cycle. Dans chaque question, la bonne réponse est
 * écrite en premier ; l'ordre est mélangé à l'affichage.
 */
internal object Banque {

    private fun q(enonce: String, bonne: String, vararg fausses: String, explication: String) =
        Question(enonce, listOf(bonne) + fausses, 0, explication)

    fun questions(matiere: MatiereExercice, cycle: Cycle): List<Question> = when (matiere) {
        MatiereExercice.FRANCAIS -> francais(cycle)
        MatiereExercice.MATHS -> maths(cycle)
        MatiereExercice.PC -> physique(cycle)
        MatiereExercice.SVT -> svt(cycle)
        MatiereExercice.ANGLAIS -> anglais(cycle)
    }

    // ------------------------------------------------------------------ Français

    private fun francais(cycle: Cycle) = when (cycle) {
        Cycle.DEBUT_COLLEGE -> listOf(
            q("Quel est le pluriel de « un cheval » ?", "des chevaux", "des chevals", "des chevaus", "des chevails",
                explication = "Les noms en -al font leur pluriel en -aux (sauf bal, carnaval, festival, chacal, récital, régal)."),
            q("Dans « Les élèves écoutent le professeur », quel est le sujet ?", "Les élèves", "écoutent", "le professeur", "professeur",
                explication = "Le sujet répond à la question « Qui est-ce qui écoute ? » : les élèves."),
            q("Conjugue « finir » au présent avec « nous ».", "nous finissons", "nous finons", "nous finirons", "nous finissions",
                explication = "Les verbes du 2e groupe prennent -issons avec « nous » au présent."),
            q("Quelle phrase est correctement écrite ?", "Ils ont mangé des mangues.", "Ils on mangé des mangues.", "Ils sont mangé des mangues.", "Ils ont mangés des mangues.",
                explication = "« ont » est le verbe avoir (on peut dire « avaient »). Avec avoir, pas d'accord avec le sujet."),
            q("Dans la phrase « Le taxi est rapide », « rapide » est…", "un adjectif", "un nom", "un verbe", "un adverbe",
                explication = "« rapide » qualifie le nom taxi : c'est un adjectif qualificatif (attribut du sujet)."),
            q("Quel est le synonyme de « content » ?", "joyeux", "triste", "fâché", "fatigué",
                explication = "Un synonyme a le même sens : content = joyeux."),
            q("Choisis le bon mot : « Il … parti à l'école. »", "est", "et", "ai", "es",
                explication = "On peut remplacer par « était » : c'est le verbe être, « est »."),
            q("Le contraire de « monter » est…", "descendre", "grimper", "sauter", "lever",
                explication = "Un antonyme a le sens opposé : monter / descendre."),
            q("Quel mot est un verbe ?", "chanter", "chanteur", "chanson", "joyeusement",
                explication = "Un verbe à l'infinitif se termine souvent par -er, -ir, -re ou -oir : chanter."),
            q("« Demain, nous … au marché. » (aller, futur)", "irons", "allons", "allions", "irions",
                explication = "Futur simple de aller : j'irai, tu iras, il ira, nous irons."),
        )
        Cycle.FIN_COLLEGE -> listOf(
            q("Dans « Il faut que tu viennes », le verbe « viennes » est au…", "subjonctif", "indicatif", "conditionnel", "impératif",
                explication = "Après « il faut que », on emploie le subjonctif."),
            q("Dans « Le livre que tu m'as prêté est passionnant », « que tu m'as prêté » est une proposition…", "subordonnée relative", "principale", "subordonnée conjonctive", "indépendante",
                explication = "Elle est introduite par le pronom relatif « que » et complète le nom « livre »."),
            q("Complète : « Si j'avais le temps, je … te voir. »", "viendrais", "viendrai", "viens", "viendra",
                explication = "Si + imparfait → conditionnel présent : je viendrais."),
            q("Quelle figure de style dans « Cet homme est un lion » ?", "une métaphore", "une comparaison", "une personnification", "une hyperbole",
                explication = "On rapproche l'homme et le lion sans outil de comparaison (comme, tel...) : métaphore."),
            q("Complète : « Les lettres que j'ai … sont arrivées. »", "écrites", "écrit", "écrits", "écrite",
                explication = "Avec avoir, le participe s'accorde avec le COD placé avant : « que » = les lettres (féminin pluriel)."),
            q("Un texte argumentatif sert à…", "défendre une thèse", "raconter une histoire", "décrire un lieu", "donner une recette",
                explication = "Il présente des arguments et des exemples pour convaincre ou persuader."),
            q("Passé simple de « prendre » avec « ils » :", "ils prirent", "ils prenaient", "ils prendront", "ils prenèrent",
                explication = "prendre au passé simple : je pris, il prit, ils prirent."),
            q("« Malgré la pluie, nous sommes sortis. » exprime…", "la concession", "la cause", "la conséquence", "le but",
                explication = "« malgré » introduit un fait qui aurait dû empêcher l'action : c'est la concession (opposition)."),
            q("Dans « Il travaille pour réussir », « pour réussir » exprime…", "le but", "la cause", "le temps", "la manière",
                explication = "« pour » + infinitif indique l'objectif : le but."),
            q("Quel est le COD dans « Awa lit un roman » ?", "un roman", "Awa", "lit", "Awa lit",
                explication = "Awa lit quoi ? Un roman : c'est le complément d'objet direct."),
        )
        Cycle.LYCEE -> listOf(
            q("Quel registre cherche à faire rire ?", "comique", "tragique", "lyrique", "épique",
                explication = "Le registre comique provoque le rire ; le lyrique exprime les sentiments."),
            q("« Je meurs de soif » est…", "une hyperbole", "une litote", "une antithèse", "un euphémisme",
                explication = "On exagère pour insister : c'est une hyperbole."),
            q("Un sonnet comporte…", "14 vers", "12 vers", "10 vers", "16 vers",
                explication = "Deux quatrains et deux tercets : 4 + 4 + 3 + 3 = 14 vers."),
            q("Un alexandrin compte…", "12 syllabes", "8 syllabes", "10 syllabes", "14 syllabes",
                explication = "L'alexandrin est le vers de 12 syllabes, souvent coupé en deux hémistiches de 6."),
            q("Dans un résumé de texte, on doit…", "reformuler les idées dans l'ordre du texte", "donner son avis", "recopier les phrases clés", "ajouter des exemples",
                explication = "Le résumé reformule fidèlement, sans avis personnel, en respectant l'ordre et le nombre de mots."),
            q("« Va, je ne te hais point » (Corneille) est un exemple de…", "litote", "hyperbole", "métaphore", "anaphore",
                explication = "On dit moins pour faire entendre plus (« je t'aime ») : c'est une litote."),
            q("Qui a écrit « Les Soleils des indépendances » ?", "Ahmadou Kourouma", "Bernard Dadié", "Camara Laye", "Aimé Césaire",
                explication = "Roman de l'Ivoirien Ahmadou Kourouma, publié en 1968."),
            q("Dans la dissertation, le plan est annoncé dans…", "l'introduction", "la conclusion", "le développement", "la transition",
                explication = "L'introduction comprend l'amorce, la problématique et l'annonce du plan."),
            q("Le point de vue interne, c'est quand le narrateur…", "voit l'histoire à travers les yeux d'un personnage", "sait tout de tous les personnages", "décrit seulement de l'extérieur", "s'adresse au lecteur",
                explication = "Focalisation interne : on perçoit le récit par un personnage."),
        )
        Cycle.TERMINALE -> listOf(
            q("« Climbié » est un roman de…", "Bernard Binlin Dadié", "Ahmadou Kourouma", "Véronique Tadjo", "Amadou Hampâté Bâ",
                explication = "Roman autobiographique de l'Ivoirien Bernard Dadié, publié en 1956."),
            q("Le commentaire de texte demande surtout de…", "analyser comment le texte produit son sens", "résumer le texte", "raconter la vie de l'auteur", "donner son opinion générale",
                explication = "On étudie les procédés d'écriture au service des idées, autour de centres d'intérêt."),
            q("Une anaphore est…", "la répétition d'un mot en début de phrases ou de vers", "une exagération", "une comparaison sans outil", "l'association de deux mots opposés",
                explication = "Ex. : « Paris outragé ! Paris brisé ! Paris martyrisé ! »"),
            q("« Un silence assourdissant » est…", "un oxymore", "une métaphore", "une litote", "une périphrase",
                explication = "Deux termes de sens opposé sont réunis : c'est un oxymore."),
            q("La Négritude a été fondée notamment par…", "Senghor, Césaire et Damas", "Hugo, Zola et Balzac", "Molière, Racine et Corneille", "Rousseau, Voltaire et Diderot",
                explication = "Léopold Sédar Senghor, Aimé Césaire et Léon-Gontran Damas, dans les années 1930."),
            q("Dans une dissertation, la problématique est…", "la question qui guide toute la réflexion", "la conclusion", "une citation", "le résumé du sujet",
                explication = "Elle reformule l'enjeu du sujet sous forme de question."),
            q("« Le berceau de l'humanité » pour désigner l'Afrique est…", "une périphrase", "une hyperbole", "une litote", "une anaphore",
                explication = "On remplace un mot par une expression qui le désigne : périphrase."),
            q("« Il dit qu'il viendra demain » est au…", "discours indirect", "discours direct", "discours indirect libre", "style télégraphique",
                explication = "Les paroles sont rapportées dans une subordonnée introduite par « que », sans guillemets."),
        )
    }

    // ------------------------------------------------------------------ Mathématiques

    private fun maths(cycle: Cycle) = when (cycle) {
        Cycle.DEBUT_COLLEGE -> listOf(
            q("Combien de côtés a un hexagone ?", "6", "5", "7", "8", explication = "Hexa = six."),
            q("La somme des angles d'un triangle vaut…", "180°", "90°", "360°", "270°",
                explication = "Dans tout triangle, la somme des trois angles est 180°."),
            q("Un nombre divisible par 2 et par 3 est divisible par…", "6", "5", "9", "12",
                explication = "2 et 3 n'ont pas de diviseur commun autre que 1, donc il est divisible par 2 × 3 = 6."),
            q("Quelle est l'aire d'un carré de côté 7 cm ?", "49 cm²", "28 cm²", "14 cm²", "21 cm²",
                explication = "Aire = côté × côté = 7 × 7 = 49 cm²."),
            q("0,5 est égal à…", "1/2", "1/5", "5/100", "2/5", explication = "0,5 = 5/10 = 1/2."),
            q("Deux droites perpendiculaires forment un angle de…", "90°", "45°", "180°", "60°",
                explication = "Perpendiculaire = angle droit = 90°."),
        )
        Cycle.FIN_COLLEGE -> listOf(
            q("√49 = ", "7", "24,5", "8", "49", explication = "7 × 7 = 49."),
            q("(a + b)² = ", "a² + 2ab + b²", "a² + b²", "a² − 2ab + b²", "2a + 2b",
                explication = "C'est une identité remarquable."),
            q("Le théorème de Thalès sert à…", "calculer des longueurs avec des droites parallèles", "calculer l'aire d'un cercle", "résoudre une équation du second degré", "calculer un angle droit",
                explication = "Avec deux droites parallèles coupant deux sécantes, les longueurs sont proportionnelles."),
            q("(a − b)(a + b) = ", "a² − b²", "a² + b²", "a² − 2ab + b²", "2a",
                explication = "Identité remarquable : (a − b)(a + b) = a² − b²."),
            q("Le cosinus d'un angle aigu dans un triangle rectangle est…", "côté adjacent ÷ hypoténuse", "côté opposé ÷ hypoténuse", "côté opposé ÷ côté adjacent", "hypoténuse ÷ côté adjacent",
                explication = "SOH-CAH-TOA : Cos = Adjacent / Hypoténuse."),
            q("Quelle est la médiane de la série 3 ; 5 ; 8 ; 10 ; 12 ?", "8", "7,6", "5", "10",
                explication = "La série est rangée et compte 5 valeurs : la médiane est la 3e, soit 8."),
        )
        Cycle.LYCEE -> listOf(
            q("Le discriminant de ax² + bx + c est…", "b² − 4ac", "b² + 4ac", "4ac − b²", "b − 4ac",
                explication = "Δ = b² − 4ac. Si Δ > 0, il y a deux racines réelles."),
            q("La courbe d'une fonction affine est…", "une droite", "une parabole", "un cercle", "une hyperbole",
                explication = "f(x) = ax + b est représentée par une droite de coefficient directeur a."),
            q("cos²x + sin²x = ", "1", "0", "2", "cos 2x", explication = "C'est la relation fondamentale de la trigonométrie."),
            q("Si Δ < 0, l'équation ax² + bx + c = 0…", "n'a pas de solution réelle", "a une solution double", "a deux solutions", "a une infinité de solutions",
                explication = "Un discriminant négatif signifie qu'il n'y a pas de racine réelle."),
            q("On lance un dé équilibré. Probabilité d'obtenir un nombre pair ?", "1/2", "1/3", "1/6", "2/3",
                explication = "3 cas favorables (2, 4, 6) sur 6 possibles : 3/6 = 1/2."),
            q("Le vecteur AB a pour coordonnées…", "(xB − xA ; yB − yA)", "(xA − xB ; yA − yB)", "(xA + xB ; yA + yB)", "(xA × xB ; yA × yB)",
                explication = "On soustrait les coordonnées de l'origine A à celles de l'extrémité B."),
        )
        Cycle.TERMINALE -> listOf(
            q("La dérivée de ln(x) est…", "1/x", "x", "eˣ", "ln(x)/x", explication = "Pour x > 0, (ln x)' = 1/x."),
            q("La dérivée de eˣ est…", "eˣ", "x·eˣ⁻¹", "1/x", "ln x", explication = "La fonction exponentielle est égale à sa dérivée."),
            q("Limite de 1/x quand x tend vers +∞ :", "0", "1", "+∞", "−∞", explication = "Quand x devient très grand, 1/x devient très petit."),
            q("Une primitive de 2x est…", "x²", "2", "2x²", "x", explication = "(x²)' = 2x."),
            q("ln(a × b) = ", "ln a + ln b", "ln a × ln b", "ln a − ln b", "ln(a) / ln(b)",
                explication = "Le logarithme transforme les produits en sommes."),
            q("Une suite géométrique de raison q vérifie…", "uₙ₊₁ = q × uₙ", "uₙ₊₁ = uₙ + q", "uₙ₊₁ = uₙ − q", "uₙ₊₁ = uₙ / q²",
                explication = "On passe d'un terme au suivant en multipliant par la raison q."),
        )
    }

    // ------------------------------------------------------------------ Physique-Chimie

    private fun physique(cycle: Cycle) = when (cycle) {
        Cycle.DEBUT_COLLEGE -> listOf(
            q("Sous la pression normale, l'eau pure bout à…", "100 °C", "0 °C", "50 °C", "200 °C",
                explication = "Sous pression atmosphérique normale, l'eau pure bout à 100 °C et gèle à 0 °C."),
            q("L'unité de la masse est…", "le kilogramme", "le litre", "le mètre", "le newton",
                explication = "La masse se mesure en kilogrammes (kg) avec une balance."),
            q("Les trois états de la matière sont…", "solide, liquide, gazeux", "chaud, froid, tiède", "dur, mou, liquide", "léger, lourd, gazeux",
                explication = "Exemple de l'eau : glace (solide), eau (liquide), vapeur (gaz)."),
            q("Une lampe brille dans un circuit…", "fermé", "ouvert", "coupé", "sans générateur",
                explication = "Le courant ne circule que si la boucle du circuit est fermée."),
            q("La glace qui fond, c'est…", "la fusion", "la solidification", "la vaporisation", "la condensation",
                explication = "Le passage de l'état solide à l'état liquide s'appelle la fusion."),
            q("1 L = …", "1 000 mL", "100 mL", "10 mL", "10 000 mL", explication = "1 litre = 1 000 millilitres = 1 dm³."),
            q("Un matériau qui laisse passer le courant est…", "un conducteur", "un isolant", "un aimant", "un plastique",
                explication = "Les métaux (cuivre, fer...) sont de bons conducteurs ; le plastique est isolant."),
        )
        Cycle.FIN_COLLEGE -> listOf(
            q("Loi d'Ohm : U = …", "R × I", "R / I", "I / R", "R + I", explication = "U en volts, R en ohms, I en ampères."),
            q("L'unité de l'intensité du courant est…", "l'ampère", "le volt", "l'ohm", "le watt",
                explication = "L'intensité se mesure en ampères (A) avec un ampèremètre branché en série."),
            q("La formule chimique de l'eau est…", "H₂O", "CO₂", "O₂", "NaCl",
                explication = "Une molécule d'eau : deux atomes d'hydrogène et un atome d'oxygène."),
            q("Une solution de pH = 3 est…", "acide", "neutre", "basique", "pure",
                explication = "pH < 7 : acide ; pH = 7 : neutre ; pH > 7 : basique."),
            q("Le poids d'un objet de 2 kg (g = 10 N/kg) vaut…", "20 N", "2 N", "0,2 N", "200 N",
                explication = "P = m × g = 2 × 10 = 20 N."),
            q("La combustion complète du carbone produit…", "du dioxyde de carbone", "de l'eau", "de l'hydrogène", "du fer",
                explication = "C + O₂ → CO₂. Le CO₂ trouble l'eau de chaux."),
            q("La vitesse moyenne v = …", "d / t", "d × t", "t / d", "d + t", explication = "Distance parcourue divisée par la durée."),
            q("P = U × I donne une puissance en…", "watts", "joules", "volts", "ampères", explication = "La puissance électrique s'exprime en watts (W)."),
        )
        Cycle.LYCEE -> listOf(
            q("Le noyau d'un atome contient…", "des protons et des neutrons", "des électrons seulement", "des protons et des électrons", "des neutrons et des électrons",
                explication = "Les électrons tournent autour du noyau, formé de nucléons (protons + neutrons)."),
            q("La quantité de matière s'exprime en…", "mole", "gramme", "litre", "kelvin", explication = "Symbole : mol."),
            q("Dans n = m / M, M est…", "la masse molaire", "la masse", "le volume", "la concentration",
                explication = "M en g/mol, m en g, n en mol."),
            q("Si les forces qui s'exercent sur un objet se compensent, il est…", "immobile ou en mouvement rectiligne uniforme", "forcément accéléré", "en chute libre", "en rotation",
                explication = "C'est le principe d'inertie (1re loi de Newton)."),
            q("L'énergie cinétique vaut…", "½·m·v²", "m·g·h", "m·v", "½·m·v", explication = "Ec = ½ m v², en joules."),
            q("0,5 L d'une solution à 0,1 mol/L contient…", "0,05 mol", "0,5 mol", "5 mol", "0,2 mol",
                explication = "n = C × V = 0,1 × 0,5 = 0,05 mol."),
            q("Le numéro atomique Z est le nombre de…", "protons", "neutrons", "nucléons", "molécules",
                explication = "Z = nombre de protons ; A = nombre de nucléons."),
            q("La vitesse de la lumière dans le vide est environ…", "300 000 km/s", "340 m/s", "3 000 km/s", "30 km/s",
                explication = "c ≈ 3 × 10⁸ m/s. 340 m/s est la vitesse du son dans l'air."),
        )
        Cycle.TERMINALE -> listOf(
            q("Deuxième loi de Newton : ΣF = …", "m·a", "m·v", "m/a", "½·m·v²",
                explication = "La somme des forces est égale à la masse multipliée par l'accélération."),
            q("La période T et la fréquence f sont liées par…", "T = 1/f", "T = f", "T = 2f", "T = f²",
                explication = "T en secondes, f en hertz."),
            q("Un ester se forme par réaction entre…", "un acide carboxylique et un alcool", "un acide fort et une base forte", "deux alcools", "un alcane et l'eau",
                explication = "C'est l'estérification, réaction lente et limitée."),
            q("pH d'une solution d'acide fort à 10⁻² mol/L :", "2", "12", "7", "0,01",
                explication = "Pour un acide fort, pH = −log C = −log(10⁻²) = 2."),
            q("La radioactivité α émet…", "un noyau d'hélium", "un électron", "un positon", "un neutron",
                explication = "La particule α est un noyau ⁴₂He."),
            q("Dans un circuit RC, la constante de temps est…", "τ = R·C", "τ = R/C", "τ = C/R", "τ = R + C",
                explication = "τ = RC, en secondes ; le condensateur est chargé à 63 % au bout de τ."),
            q("La force de Lorentz s'exerce sur…", "une charge en mouvement dans un champ magnétique", "une masse au repos", "un neutron immobile", "un objet en chute libre dans le vide",
                explication = "F = q·v ∧ B : il faut une charge et une vitesse."),
            q("L'énergie d'un photon est E = …", "h·ν", "m·c", "½·m·v²", "q·U²",
                explication = "h est la constante de Planck et ν la fréquence de la radiation."),
        )
    }

    // ------------------------------------------------------------------ SVT

    private fun svt(cycle: Cycle) = when (cycle) {
        Cycle.DEBUT_COLLEGE -> listOf(
            q("Quel organe pompe le sang dans le corps ?", "le cœur", "les poumons", "le foie", "les reins",
                explication = "Le cœur est un muscle qui propulse le sang dans les vaisseaux."),
            q("Les plantes vertes fabriquent leur matière grâce à…", "la photosynthèse", "la respiration", "la digestion", "la germination",
                explication = "Avec la lumière, l'eau et le CO₂, la chlorophylle produit de la matière et rejette de l'O₂."),
            q("Un animal qui mange uniquement des végétaux est…", "herbivore", "carnivore", "omnivore", "insectivore",
                explication = "Ex. : le mouton, l'antilope."),
            q("Le paludisme est transmis par…", "le moustique anophèle femelle", "la mouche", "le cafard", "la tique",
                explication = "En piquant, l'anophèle femelle transmet le plasmodium."),
            q("En respirant, nous absorbons…", "du dioxygène", "du dioxyde de carbone", "de l'azote seulement", "de l'hydrogène",
                explication = "Nous absorbons le dioxygène (O₂) et rejetons le dioxyde de carbone (CO₂)."),
            q("La partie de la plante qui absorbe l'eau du sol est…", "la racine", "la feuille", "la tige", "la fleur",
                explication = "Les poils absorbants des racines puisent l'eau et les sels minéraux."),
            q("Pour se protéger du paludisme, il faut…", "dormir sous une moustiquaire imprégnée", "boire beaucoup d'eau", "manger moins", "éviter le soleil",
                explication = "La moustiquaire et l'élimination des eaux stagnantes limitent les piqûres."),
            q("Les êtres vivants sont formés de…", "cellules", "cristaux", "minéraux", "métaux",
                explication = "La cellule est l'unité de base du vivant."),
        )
        Cycle.FIN_COLLEGE -> listOf(
            q("Les globules blancs servent à…", "défendre l'organisme", "transporter le dioxygène", "coaguler le sang", "digérer les aliments",
                explication = "Ce sont les acteurs de l'immunité ; les globules rouges transportent l'O₂."),
            q("Le VIH s'attaque principalement…", "aux lymphocytes T4", "aux globules rouges", "aux os", "aux muscles",
                explication = "En détruisant les LT4, il affaiblit les défenses : c'est le sida."),
            q("La fécondation est la rencontre…", "d'un spermatozoïde et d'un ovule", "de deux ovules", "de deux spermatozoïdes", "d'un ovule et d'une cellule de la peau",
                explication = "Elle forme une cellule-œuf qui donnera l'embryon."),
            q("La digestion de l'amidon commence dans…", "la bouche", "l'estomac", "le gros intestin", "le foie",
                explication = "L'amylase de la salive commence à transformer l'amidon."),
            q("Un vaccin permet…", "de préparer le système immunitaire à reconnaître un microbe", "de guérir immédiatement une maladie", "de tuer les bactéries comme un antibiotique", "de remplacer les globules blancs",
                explication = "Il provoque une mémoire immunitaire sans rendre malade."),
            q("Les échanges gazeux respiratoires se font dans…", "les alvéoles pulmonaires", "les bronches", "la trachée", "le larynx",
                explication = "Leur paroi très fine permet le passage de l'O₂ vers le sang et du CO₂ vers l'air."),
            q("L'érosion des sols est aggravée par…", "la déforestation", "le reboisement", "les haies", "la jachère",
                explication = "Sans arbres, la pluie emporte la terre ; reboiser protège les sols."),
            q("Les chromosomes contiennent…", "l'ADN", "du sucre seulement", "de l'eau seulement", "des graisses",
                explication = "L'ADN porte l'information génétique."),
        )
        Cycle.LYCEE -> listOf(
            q("Une cellule humaine non sexuelle contient…", "46 chromosomes", "23 chromosomes", "44 chromosomes", "48 chromosomes",
                explication = "23 paires, soit 46 chromosomes ; les gamètes n'en ont que 23."),
            q("La méiose produit…", "4 cellules haploïdes", "2 cellules identiques", "1 cellule diploïde", "des cellules musculaires",
                explication = "Elle forme les gamètes, avec un seul chromosome de chaque paire."),
            q("L'unité de base du système nerveux est…", "le neurone", "le néphron", "l'alvéole", "la fibre musculaire",
                explication = "Le néphron est l'unité du rein."),
            q("La mitose permet…", "la multiplication cellulaire en gardant le même nombre de chromosomes", "la formation des gamètes", "la digestion", "la fécondation",
                explication = "Une cellule mère donne deux cellules filles identiques."),
            q("Les roches issues du refroidissement du magma sont…", "magmatiques", "sédimentaires", "métamorphiques", "organiques",
                explication = "Ex. : le granite et le basalte."),
            q("Le muscle utilise directement l'énergie de…", "l'ATP", "l'ADN", "l'eau", "le sel",
                explication = "L'ATP est la molécule énergétique utilisable par la cellule."),
            q("La glycémie normale à jeun est d'environ…", "1 g/L", "0,1 g/L", "10 g/L", "5 g/L",
                explication = "Elle est maintenue autour de 1 g/L de sang."),
            q("L'hormone qui fait baisser la glycémie est…", "l'insuline", "le glucagon", "l'adrénaline", "la testostérone",
                explication = "L'insuline est produite par le pancréas ; le glucagon fait monter la glycémie."),
        )
        Cycle.TERMINALE -> listOf(
            q("Le message nerveux se propage le long de l'axone sous forme de…", "potentiels d'action", "hormones", "globules", "anticorps",
                explication = "C'est un message électrique codé en fréquence de potentiels d'action."),
            q("Au niveau de la synapse, le message est transmis par…", "un neurotransmetteur", "le sang", "une hormone thyroïdienne", "l'ADN",
                explication = "Ex. : l'acétylcholine libérée dans la fente synaptique."),
            q("Les anticorps sont produits par…", "les plasmocytes (issus des lymphocytes B)", "les globules rouges", "les plaquettes", "les neurones",
                explication = "C'est la réponse immunitaire à médiation humorale."),
            q("Chez l'homme, le sexe génétique est déterminé par…", "la paire de chromosomes XX ou XY", "la température", "l'alimentation", "le chromosome 21",
                explication = "XX : sexe féminin ; XY : sexe masculin."),
            q("Le brassage interchromosomique a lieu pendant…", "l'anaphase I de la méiose", "la mitose", "l'interphase", "la cytokinèse de la mitose",
                explication = "Les chromosomes homologues se répartissent au hasard dans les deux cellules."),
            q("En F2 d'un monohybridisme avec dominance complète, on obtient…", "3/4 – 1/4", "1/2 – 1/2", "9/16 – 7/16", "100 % d'un seul phénotype",
                explication = "Le croisement F1 × F1 (Aa × Aa) donne 3/4 [A] et 1/4 [a]."),
            q("La progestérone est surtout sécrétée par…", "le corps jaune", "l'hypophyse", "le pancréas", "la thyroïde",
                explication = "Le corps jaune se forme dans l'ovaire après l'ovulation."),
            q("Les lymphocytes T cytotoxiques détruisent…", "les cellules infectées", "les anticorps", "les globules rouges sains", "les neurotransmetteurs",
                explication = "C'est la réponse immunitaire à médiation cellulaire."),
        )
    }

    // ------------------------------------------------------------------ Anglais

    private fun anglais(cycle: Cycle) = when (cycle) {
        Cycle.DEBUT_COLLEGE -> listOf(
            q("Choose: « She ___ a student. »", "is", "am", "are", "be", explication = "He / she / it → is."),
            q("What is the plural of « child »?", "children", "childs", "childes", "childrens", explication = "Pluriel irrégulier : child → children."),
            q("On dit « Good morning »…", "le matin", "le soir", "la nuit", "pour dire au revoir",
                explication = "Good morning = bonjour (le matin) ; good evening = bonsoir."),
            q("Complete: « I ___ to school every day. »", "go", "goes", "going", "gone", explication = "Présent simple avec I : go (goes avec he/she/it)."),
            q("« Mardi » in English is…", "Tuesday", "Monday", "Thursday", "Wednesday", explication = "Monday, Tuesday, Wednesday, Thursday, Friday."),
            q("Choose: « There ___ three books on the table. »", "are", "is", "am", "be", explication = "There are + pluriel."),
            q("My father's sister is my…", "aunt", "uncle", "cousin", "niece", explication = "Aunt = tante ; uncle = oncle."),
            q("Translate « J'ai douze ans ».", "I am twelve years old", "I have twelve years", "I has twelve", "I am twelve year",
                explication = "En anglais, on utilise « to be » pour l'âge."),
        )
        Cycle.FIN_COLLEGE -> listOf(
            q("Past simple of « go »:", "went", "goed", "gone", "going", explication = "go – went – gone."),
            q("« I have lived here ___ 2020. »", "since", "for", "during", "ago", explication = "since + point de départ ; for + durée."),
            q("Comparative of « big »:", "bigger", "more big", "biger", "the biggest", explication = "Adjectif court : on double la consonne + -er."),
            q("« If it rains, I ___ at home. »", "will stay", "would stay", "stayed", "staying", explication = "If + présent → futur (will)."),
            q("« ___ do you live? » — « In Abidjan. »", "Where", "What", "When", "Who", explication = "Where = où."),
            q("« She is interested ___ music. »", "in", "on", "at", "for", explication = "interested in = intéressé par."),
            q("Passive form of « The teacher corrects the homework »:", "The homework is corrected by the teacher", "The homework corrects the teacher", "The homework was correct by the teacher", "The teacher is corrected by the homework",
                explication = "be + participe passé + by."),
            q("« You ___ wear a uniform at school. » (obligation)", "must", "can", "may", "might", explication = "must exprime l'obligation."),
        )
        Cycle.LYCEE -> listOf(
            q("« I wish I ___ rich. »", "were", "am", "will be", "have been", explication = "I wish + prétérit (were) pour un souhait irréel au présent."),
            q("He said, « I am tired. » → Reported speech:", "He said he was tired", "He said I am tired", "He said he is tiring", "He says he was tired",
                explication = "Au discours indirect, on recule les temps : am → was."),
            q("« By the time we arrived, the film ___ started. »", "had", "has", "have", "was", explication = "Past perfect : action antérieure à une autre au passé."),
            q("« Despite ___ tired, she finished her work. »", "being", "be", "been", "to be", explication = "despite + V-ing."),
            q("Synonym of « huge »:", "enormous", "tiny", "quick", "weak", explication = "huge = enormous = énorme."),
            q("« If I had studied, I ___ the exam. »", "would have passed", "would pass", "will pass", "passed",
                explication = "If + past perfect → would have + participe passé (irréel du passé)."),
            q("« The man ___ car was stolen called the police. »", "whose", "who", "which", "whom", explication = "whose = dont (possession)."),
            q("« He is used to ___ early. »", "getting up", "get up", "got up", "gets up", explication = "be used to + V-ing = avoir l'habitude de."),
        )
        Cycle.TERMINALE -> listOf(
            q("« Hardly ___ arrived when it started to rain. »", "had I", "I had", "I have", "have I", explication = "Après hardly en tête de phrase, on inverse sujet et auxiliaire."),
            q("« It's high time we ___. »", "left", "leave", "will leave", "leaving", explication = "It's high time + prétérit."),
            q("« To look forward to » means…", "avoir hâte de", "regretter", "chercher", "surveiller", explication = "I look forward to hearing from you."),
            q("« Not only ___ late, but he also forgot his book. »", "was he", "he was", "he is", "is he", explication = "Inversion après not only en tête de phrase."),
            q("« She suggested that he ___ a doctor. »", "see", "sees", "saw", "seeing", explication = "suggest that + subjonctif (base verbale) ou should see."),
            q("« Unless you work hard, you ___ succeed. »", "won't", "will", "would", "wouldn't", explication = "unless = if not : si tu ne travailles pas, tu ne réussiras pas."),
            q("Which linking word expresses contrast?", "However", "Moreover", "Therefore", "Furthermore", explication = "However = cependant ; therefore = donc."),
            q("« The more you read, ___ you learn. »", "the more", "more", "the most", "most", explication = "The + comparatif..., the + comparatif."),
        )
    }
}
