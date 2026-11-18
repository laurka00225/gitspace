# Mes Études : emploi du temps d'études (Android)

Application Android pour les élèves de la **6e à la Terminale** du système éducatif ivoirien.
Elle construit automatiquement un emploi du temps d'études à partir des heures libres de l'élève.

## Règles de base

| Journée | Disponibilité par défaut |
|---|---|
| Lundi, mardi, jeudi, vendredi | Cours de 7h30 à 17h30, études de 18h00 à 19h30 puis de 20h15 à 22h00 |
| Mercredi | Pas de cours l'après-midi : 15h00–17h30, 18h00–19h30, 20h15–22h00 |
| Samedi, dimanche, jours fériés, vacances | 8h30–12h00, 15h00–17h30, 20h15–22h00 |
| Tous les jours | Coucher à 22h : aucune séance après |

Toutes les plages se modifient dans **Réglages** (repas, trajet, sieste...).

## Fonctionnalités

- Choix de la classe (6e à Tle) et de la série au lycée (A, C, D) avec les matières correspondantes.
- Priorité de 1 à 5 par matière : les séances de la semaine sont réparties au prorata,
  jamais deux fois de suite la même matière dans une journée.
- Séances de 45 min (30, 60 ou 90 au choix) avec pauses.
- Activités adaptées : relire le cours du jour en semaine, exercices, fiches,
  sujets types **BEPC** (3e) et **BAC** (Tle) le week-end.
- Jours fériés ivoiriens intégrés (dont Pâques, Ascension, Pentecôte calculés,
  fêtes musulmanes estimées et désactivables). Ajout des vacances scolaires.
- Écran « Aujourd'hui » avec la séance en cours, cases « faite » et progression.
- Vue semaine avec bilan du temps par matière ; on peut changer la matière d'une séance.
- **Rappels et alarmes** : notification avant chaque séance (à l'heure, 5, 10, 15 ou 30 min avant),
  sonnerie d'alarme au choix, rappel du coucher à 21h50 et rappel la veille des devoirs à 19h.
  Les rappels survivent au redémarrage du téléphone.
- **Exercices pratiques** en Français, Mathématiques, Physique-Chimie, SVT et Anglais, adaptés au
  cycle (6e–5e, 4e–3e, 2nde–1ère, Tle) : séries de 10 QCM corrigés et expliqués ; en maths,
  des calculs nouveaux sont générés à chaque série. Meilleur score par matière.
- **Devoirs et contrôles** : la matière reçoit plus de séances la semaine qui précède l'épreuve.
- **Minuteur de séance** avec alarme à la fin, même appli fermée.
- **Progrès** : jours d'affilée, record, pourcentage de séances faites dans la semaine.
- Interface Material 3, plein écran, mode sombre, couleurs du téléphone (Android 12+) en option.
- Tout est enregistré sur le téléphone, sans connexion Internet.

## Installer l'application

Chaque envoi de code construit l'APK dans GitHub Actions (onglet **Actions**,
artefact `MesEtudes-apk`). Sur la branche `master`, l'APK est aussi publié dans la
release **derniere-version**. Sur le téléphone : télécharger `MesEtudes.apk`, l'ouvrir
et autoriser l'installation depuis cette source.

## Développement

- `core/` : logique de planification en Kotlin pur, testée (`./gradlew :core:test`).
- `app/` : application Android en Kotlin et Jetpack Compose (Android 8.0 minimum).

```bash
./gradlew :core:test            # tests de la planification
./gradlew :app:assembleRelease  # APK (nécessite le SDK Android)
./gradlew -PcoreOnly :core:test # tests sans SDK Android
```
