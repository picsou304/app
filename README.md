# 🎬 Daily Vlog Crew — Android App

Application mobile native Android développée en **Kotlin**, **Jetpack Compose (Material 3)**, **Hilt**, **CameraX**, **Media3 / ExoPlayer**, **WorkManager** et architecture Clean MVVM avec double backend (**FakeBackend** autonome hors-ligne et **RemoteBackend** Supabase).

---

## 🚀 Démarrage Rapide (< 15 minutes)

### Prérequis
- **JDK 21** (`openjdk-21`)
- **Android SDK** (API 35, Build-Tools 35.0.0, platform-tools avec `adb`)
- Un appareil physique Android (avec débogage USB activé) ou un émulateur Pixel API 34+

### 1. Cloner et préparer l'environnement
```bash
git clone <url_du_repo>
cd app

# Rendre les scripts exécutables
chmod +x gradlew scripts/*.sh
```

### 2. Lancer les tests unitaires
```bash
./scripts/run_unit_tests.sh
```
> Tous les tests unitaires (moteur déterministe `DailyPickerEngine`, ViewModels, Turbine Flow tests, Fake Repositories) s'exécutent et doivent être 100% verts.

### 3. Installer et lancer l'application (Debug)
Connectez votre téléphone via USB ou lancez un émulateur, puis :
```bash
./scripts/build_install.sh
```
L'APK debug est compilé, installé et lancé automatiquement sur votre appareil !

### 4. Générer l'APK Release signé
```bash
./scripts/build_release.sh
```
L'APK release signé est produit à l'emplacement :
`app/build/outputs/apk/release/app-release.apk` (17 Mo).

---

## 🌟 Fonctionnalités Clés

- **Authentification & Profils** : Inscription, connexion, mise à jour du pseudo et avatar, persistance sécurisée.
- **Groupes & Invitations** : Création de groupes avec emoji, couleur, heure de bascule et durée maximale (10s-180s). Code d'invitation unique à 6 caractères et deep links (`dailyvlog://join/<code>`).
- **Tirage Déterministe (`DailyPickerEngine`)** : Rotation équitable garantie (chaque membre désigné exactement 1 fois par manche).
- **Enregistrement 1-Prise (CameraX)** : Bascule caméra avant/arrière, compte à rebours, enregistrement continu sans pause, arrêt automatique à la durée limite.
- **Aperçu & Publication** : Relecture immédiate, ajout de légende (≤ 140 car.), compression 720p H.264 et upload avec WorkManager.
- **Lecteur Immersif (Media3 ExoPlayer)** : Lecture plein écran en boucle, play/pause, sourdine.
- **Interactions Sociales** : 6 réactions emojis avec compteurs dynamiques, bottom sheet de commentaires texte, indicateur « vu par ».
- **Streak & Jour Raté** : Compteur de jours consécutifs avec vlog. Si le vlog n'est pas posté avant la deadline, la journée est marquée comme ratée 😴 et la streak est brisée avec bannière d'alerte.
- **Notifications & Canaux Android 8+** : Canaux dédiés ("C'est ton tour", "Rappels 4h", "Nouveaux vlogs"), permission Android 13+ et tap-through direct vers l'écran d'enregistrement ou de visionnage.
- **DebugClock** : Horloge de simulation de temps intégrée pour tester facilement les changements de jour et les fins de délais sans modifier l'heure système.

---

## 🏗️ Architecture & Structure du Code

L'application suit scrupuleusement les principes de **Clean Architecture** et **MVVM** :

```
app/src/main/java/com/dailyvlog/app/
├── core/
│   ├── notification/   # NotificationHelper (canaux, intents, permissions) & DailyReminderWorker
│   └── time/           # DebugClock (avance/reset temporel simulé)
├── domain/
│   ├── engine/         # DailyPickerEngine (rotation équitable déterministe)
│   ├── model/          # Modèles métier immutables (User, Group, DailyAssignment, Vlog, etc.)
│   └── repository/     # Interfaces de dépôts (Auth, Group, DailyAssignment, Vlog)
├── data/
│   └── fake/           # FakeBackend (in-memory, réactif via Kotlin StateFlow)
├── ui/
│   ├── auth/           # Écrans Connexion, Inscription, Bienvenue
│   ├── group/          # Création de groupe, adhésion, détails & membres
│   ├── home/           # Accueil, liste des groupes, profil utilisateur
│   ├── navigation/     # Navigation Compose type-safe (routes sérialisables)
│   ├── record/         # Enregistrement CameraX, aperçu vidéo, légende
│   ├── theme/          # Design System Material 3 (thème clair, sombre, dynamic color)
│   ├── today/          # Écran Aujourd'hui, compte à rebours, jour raté, outils debug
│   └── watch/          # Lecteur Media3 ExoPlayer, réactions, commentaires, spectateurs
└── MainActivity.kt     # Point d'entrée, gestion deep links et notification routing
```

---

## 🗄️ Backend Supabase (Production)

L'application supporte nativement **Supabase** en production :
1. Créez un projet sur [supabase.com](https://supabase.com).
2. Exécutez le script SQL complet [`backend/schema.sql`](backend/schema.sql) dans l'éditeur SQL Supabase (tables, contraintes, RLS policies, buckets de stockage).
3. Renseignez vos clés dans `local.properties` :
   ```properties
   supabase.url=https://votre-projet.supabase.co
   supabase.anon.key=votre-cle-anon-publique
   ```
4. Consultez le guide détaillé dans [`backend/README.md`](backend/README.md).

---

## 🧪 Tests & Qualité

- **Tests Unitaires JUnit 4 + Turbine** :
  - `DailyPickerEngineTest` : Vérification du tirage équitable sur N jours et manches multiples.
  - `AuthViewModelTest`, `GroupViewModelTest`, `TodayViewModelTest` : Validation exhaustive des états d'UI et des effets de navigation.
  - Exécution : `./scripts/run_unit_tests.sh` ou `./gradlew testDebugUnitTest`.
- **Scénarios de test ARTEMIS** :
  - Répertoire [`tests/artemis/`](tests/artemis/) avec description détaillée de chaque flux testé sur appareil physique.

---

## 📄 Licence
Projet réalisé dans le cadre du défi **Daily Vlog Crew**.
# app
