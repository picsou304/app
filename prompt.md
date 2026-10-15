# 🎬 Prompt — App Android « Daily Vlog Crew »

> À coller dans Antigravity (workspace : `/home/picsou/Projects/app`). L'agent a accès aux outils MCP **ARTEMIS** (`mobile_diagnose`, `mobile_get_device_state`, `mobile_run_task`, `mobile_manage_task`, `mobile_inspect_trace`).

---

## 0. Rôle & objectif

Tu es un ingénieur Android senior + QA mobile. Tu dois **construire, compiler, installer et valider sur appareil/émulateur** une application Android **100 % fonctionnelle** (pas une maquette) :

> Un utilisateur crée un **groupe**, **invite ses potes**, et **chaque jour, 1 membre du groupe est désigné** pour poster un **petit vlog** (vidéo courte). Les autres membres le regardent, réagissent et commentent.

Tu travailles en autonomie jusqu'à ce que **tous les critères d'acceptation (§7) soient prouvés par des tests exécutés sur device via ARTEMIS**. Ne t'arrête pas à « ça compile ».

---

## 1. Règles de travail (impératives)

1. **Planifie d'abord** : écris `PLAN.md` (milestones + critères de validation) avant de coder. Mets-le à jour au fil de l'eau.
2. **Livre du code qui tourne** : chaque milestone se termine par `./gradlew assembleDebug` OK + installation + vérification sur device.
3. **Ne suppose jamais un comportement UI** : vérifie-le avec ARTEMIS (§5) avant d'écrire un test.
4. **Pas de TODO / placeholder / données factices dans le code livré** (hors `FakeBackend` explicitement prévu en §3).
5. **Secrets** : jamais en dur ni commités. Clés dans `local.properties` (+ `local.properties.example` commité).
6. Commits git atomiques par milestone (`git init` si besoin), messages clairs.
7. Si une décision bloquante dépend de moi (ex. clés backend, plusieurs devices connectés), **pose-moi la question** au lieu de deviner.
8. Langue de l'UI : **français** (strings dans `strings.xml`, prêt pour i18n). Code et commentaires en anglais.
9. **Skills UI (workspace `.agents/skills/`) — à activer (lis leur `SKILL.md`) avant de toucher à l'UI** : `material-design-3-ui` (tokens, composants, états, accessibilité, layouts adaptatifs), `compose-expert` (API Compose, recomposition, navigation, motion) et `compose-kotlin-agent-skills` (architecture MVI/MVVM, anti-patterns, edge-to-edge, perf). Valide ensuite le rendu réel via ARTEMIS (thème clair/sombre, grandes polices, petit écran).

---

## 2. Spécifications fonctionnelles

### 2.1 Authentification
- Inscription / connexion par **email + mot de passe** (+ option « Continuer avec Google » si faisable).
- Profil : pseudo, avatar (photo galerie/caméra ou initiales par défaut).
- Session persistante, déconnexion.

### 2.2 Groupes
- **Créer un groupe** : nom, emoji/couleur, heure de bascule quotidienne (défaut `09:00`, fuseau du créateur), durée max du vlog (défaut 60 s).
- **Inviter des potes** :
  - Code d'invitation à 6 caractères (ex. `K7QX2M`) + **lien partageable** via `Intent.ACTION_SEND` (deep link `dailyvlog://join/<code>`).
  - Rejoindre via saisie du code ou ouverture du lien.
  - Le créateur est **admin** : peut retirer un membre, régénérer le code, supprimer le groupe. Un membre peut quitter.
  - Min. 2 membres pour activer le tirage quotidien ; max 20 membres.
- Un utilisateur peut appartenir à **plusieurs groupes**.

### 2.3 Tirage quotidien (cœur de l'app)
- **Chaque jour, 1 membre est désigné** par groupe.
- Algorithme **équitable et déterministe** (identique chez tous les clients, sans serveur dédié nécessaire) :
  - Cycle par « manches » : chaque membre est tiré **exactement une fois** avant qu'un second tour commence.
  - Ordre de la manche = mélange pseudo-aléatoire seedé avec `hash(groupId + numéroDeManche)`.
  - Un membre qui rejoint en cours de manche est ajouté à la manche courante (non encore tiré).
  - Un membre qui quitte est exclu des tirages futurs sans casser l'historique.
  - Le jour est calculé via `LocalDate` du fuseau défini dans le groupe (pas l'heure device brute).
- Implémente l'algorithme dans une **classe pure Kotlin** (`DailyPickerEngine`) couverte par **tests unitaires** : équité sur 100 jours, ajout/départ de membre, déterminisme, changement de fuseau/DST, groupe de 2.
- Le désigné reçoit une **notification push/locale** à l'heure de bascule : « C'est ton tour de vlogger aujourd'hui 🎥 ». Rappel si pas de vlog posté 4 h avant minuit.
- Option « **Passer mon tour** » (1 fois par manche) → le tirage choisit le prochain membre restant de la manche.

### 2.4 Le vlog
- Le désigné ouvre l'écran **Enregistrer** : caméra (CameraX), bascule avant/arrière, flash, compte à rebours, durée max visible, pause interdite (1 prise continue) **ou** import depuis la galerie (≤ durée max).
- Aperçu avant publication : rejouer / refaire / ajouter une légende (≤ 140 car.) / publier.
- Compression avant upload (H.264, ≤ 720p) ; upload avec **progression**, reprise sur échec réseau (WorkManager).
- Un seul vlog publié par jour et par groupe ; modifiable/supprimable par son auteur jusqu'à la fin de la journée.
- Les autres membres ne peuvent **pas** poster de vlog ce jour-là (UI + règle serveur).

### 2.5 Fil & interactions
- **Écran Groupe** : bandeau « Aujourd'hui : @pseudo » + état (⏳ en attente / ✅ posté), vidéo du jour, membres avec avatar, historique des vlogs (liste par date, grille).
- Lecteur vidéo (Media3/ExoPlayer) plein écran, lecture auto en boucle optionnelle.
- **Réactions emoji** (6 emojis) et **commentaires** texte sur chaque vlog.
- Indicateur « vu par » (avatars).
- **Streak du groupe** : nombre de jours consécutifs où le vlog a été posté.
- Si le désigné ne poste pas : le jour est marqué « raté 😴 » à minuit ; le tirage continue normalement.

### 2.6 Notifications
- FCM (ou locales + WorkManager si backend sans push) : « c'est ton tour », « nouveau vlog de @pseudo », « nouveau commentaire/réaction », « @pote a rejoint ton groupe ».
- Canaux de notification Android 8+ configurables ; permission `POST_NOTIFICATIONS` demandée avec rationale (Android 13+).

---

## 3. Stack technique imposée

| Domaine | Choix |
|---|---|
| Langage / UI | **Kotlin**, **Jetpack Compose** + Material 3 (thème clair/sombre, dynamic color) |
| Architecture | MVVM + Clean (data/domain/ui), **Hilt**, Coroutines/Flow, Navigation-Compose |
| Min / Target SDK | `minSdk 26`, `targetSdk` = dernière stable |
| Caméra / vidéo | CameraX (`Recorder`/`VideoCapture`), Media3 ExoPlayer, Media3 Transformer (compression) |
| Local | Room + DataStore ; WorkManager pour uploads/rappels |
| Backend | **Supabase** (Auth + Postgres + Storage + Realtime, RLS activée) — *alternative acceptée : Firebase (Auth/Firestore/Storage/FCM)*. Demande-moi mon choix si je n'ai pas de projet prêt. |
| Tests | JUnit5 + Turbine (unit), Compose UI tests, **ARTEMIS** (E2E device) |
| Build | Gradle Kotlin DSL + version catalog (`libs.versions.toml`) |

**Pattern obligatoire :** toute la couche données passe par des interfaces de repository (`AuthRepository`, `GroupRepository`, `VlogRepository`…) avec **deux implémentations** :
- `RemoteBackend` (Supabase/Firebase), activée si les clés sont présentes dans `local.properties` ;
- `FakeBackend` (Room/in-memory persistant, multi-utilisateur simulé) pour **tout tester immédiatement sur émulateur sans compte cloud**.

Mets un flag `BuildConfig.USE_FAKE_BACKEND` (défaut `true` tant que les clés manquent). Fournis `backend/` avec : schéma SQL, politiques **RLS** (un membre ne lit/écrit que les données de ses groupes ; seul le désigné du jour peut insérer un vlog), buckets Storage, et un `README` de mise en route backend en 5 étapes.

---

## 4. Structure attendue du dépôt

```
/home/picsou/Projects/app
├── prompt.md
├── PLAN.md                 # plan vivant + statut des milestones
├── README.md               # build, config clés, lancement, tests
├── app/ ...                # module Android
├── backend/                # schema.sql, policies.sql, storage.md, README
├── tests/artemis/          # scénarios ARTEMIS (task_desc) + rapports
│   ├── 01_auth.md ... 
│   └── reports/
├── scripts/                # build_install.sh, run_unit_tests.sh
└── local.properties.example
```

---

## 5. 🔧 Intégration ARTEMIS (protocole obligatoire)

### 5.1 Pré-vol (avant toute ligne de code Android)
1. Appelle **`mobile_diagnose`** (puis `attempt_fix=true` si besoin) et suis ses `next_steps` **dans l'ordre**. Répète jusqu'à `verdict = ready|degraded`.
2. ⚠️ `adb` peut être absent de l'hôte : si `adb: command not found`, installe les **Android platform-tools** / SDK (demande confirmation avant `apt`/téléchargement) et exporte `ANDROID_HOME`/`PATH`.
3. Si aucun device : `mobile_diagnose(launch_avd="<nom>")` (jamais `emulator -avd` dans le shell), attends ~60 s puis re-diagnostique. Crée un AVD Pixel (API 34, Google APIs, caméra *virtual scene*) si aucun n'existe.
4. Utilise `adb devices -l` pour lister les serials. **Si plusieurs devices : demande-moi lequel utiliser.**
5. Pour tester l'invitation inter-utilisateurs, prévois **2 devices/émulateurs** (Alice = admin, Bob = invité). Les tâches sur devices distincts tournent **en parallèle**.

### 5.2 Boucle de développement par milestone
```
coder → ./gradlew assembleDebug testDebugUnitTest
      → mobile_run_task(app_path=<apk>, ...) pour explorer/valider
      → mobile_inspect_trace + lecture stderr_log / notes_dir
      → corriger → répéter jusqu'à vert
```

### 5.3 Choix Flash vs Pro
- **Flash** : parcours simples et déterministes (login, créer groupe, saisir un code, ouvrir un écran, réagir avec un emoji).
- **Pro** (`verification_level="checkpoints"`, `expected_output_desc` renseigné) : parcours complexes ou multi-branches — enregistrement vidéo + upload, tirage quotidien/changement de jour, permissions système, notifications, diagnostic `adb logcat`. Utilise `strict` pour les parcours critiques (publication de vlog).
- Renseigne toujours `locked_app_package` (`com.dailyvlog.app`), `conversation_id` (wakeup) et `device_serial` si plusieurs devices.

### 5.4 Règles d'appel `mobile_run_task`
- `task_desc` **autonome** : état de départ, comptes de test (`alice@test.dev` / `bob@test.dev`, mot de passe `Test1234!`), actions UI exactes, **condition d'arrêt** et ce qu'il faut rapporter (screenshot/état attendu).
- **Suivi obligatoire** : pose un timer et appelle `mobile_manage_task(status)` **au moins toutes les minutes** (les tâches peuvent caler sans bruit). Ne te fie pas qu'au wakeup.
- Après chaque tâche : lis `stderr_log`, `mobile_inspect_trace`, et pour Pro le `notes_dir`/`output.md`. Un run « terminé » ≠ réussi : **vérifie le résultat réel**.
- Échec ARTEMIS (tâche rejetée, pas de device, erreur) → **`mobile_diagnose` d'abord**, pas de relance à l'aveugle.
- **Latence modèle** : Flash ≈ 5 s/étape → pour attendre 30 s, demande ~25 s ; Pro ≈ 30 s/tour → pas d'attente explicite en plus. Les durées exactes (ex. vlog de 60 s) sont indicatives : un vlog de 5–10 s suffit pour valider le flux.
- Sur émulateur, pour simuler le changement de jour, **ne change pas l'heure à la main sur un test critique** : expose un `DebugClock` (injecté via Hilt, activable en build `debug` uniquement) piloté par un écran/intent debug (`adb shell am broadcast -a com.dailyvlog.DEBUG_SET_DATE --es date 2026-10-16`).

### 5.5 Du test exploratoire au test déterministe
Une fois un parcours validé par ARTEMIS, **génère le test exécutable** correspondant (Compose UI test / UiAutomator) :
- **Dynamic-first, coordinate-fallback** : locators par `testTag`/`contentDescription`/texte/resource-id (ajoute des `Modifier.testTag` systématiquement) ; coordonnées vérifiées par ARTEMIS uniquement en repli (`try` dynamique → `catch` coordonnées).
- Attentes **explicites** (`waitUntil`, `IdlingResource`), jamais de `sleep` aveugle sauf pour la caméra/lecture vidéo (borné et commenté).
- Sauvegarde les scénarios ARTEMIS (`task_desc`) dans `tests/artemis/*.md` et les rapports dans `tests/artemis/reports/`.

---

## 6. Milestones

| # | Milestone | Validation |
|---|---|---|
| M0 | Pré-vol ARTEMIS + SDK + AVD(s) + squelette Gradle/Hilt/Compose/CI scripts | `mobile_diagnose = ready`, app « Hello » installée & lancée via ARTEMIS |
| M1 | Design system, navigation, auth (Fake puis Remote) | Flash : inscription + connexion + déconnexion |
| M2 | Groupes : création, code d'invitation, deep link, rejoindre, rôles | 2 devices : Alice crée, Bob rejoint par code **et** par lien |
| M3 | `DailyPickerEngine` + tests unitaires + écran « Aujourd'hui » | Tests JUnit verts ; Pro : le désigné affiché = celui attendu, change au jour suivant (DebugClock) |
| M4 | Enregistrement/import vidéo, compression, upload WorkManager, publication | Pro `strict` : le désigné publie, un non-désigné ne peut pas |
| M5 | Fil, lecteur, réactions, commentaires, vu-par, historique, streak | Bob voit le vlog d'Alice, réagit, commente ; Alice le voit en temps réel |
| M6 | Notifications (tour, nouveau vlog), rappel, « passer mon tour », jour raté | Pro : notification reçue, tap → bon écran |
| M7 | Backend réel (schéma, RLS, storage) + bascule `USE_FAKE_BACKEND=false` | Mêmes scénarios rejoués contre le backend réel |
| M8 | Polish : états vides/erreurs/offline, accessibilité, thème sombre, perf, icône | Checklist §7 complète, APK release signable |

À chaque milestone : mets à jour `PLAN.md`, commit, et résume en 5 lignes max ce qui est validé **et comment (trace_id ARTEMIS)**.

---

## 7. Critères d'acceptation (tous à prouver sur device)

- [ ] Alice s'inscrit, crée « Les Potes », obtient un code et un lien d'invitation.
- [ ] Bob (autre device/compte) rejoint via code **et** via deep link ; Alice voit Bob dans les membres.
- [ ] Avec 2+ membres, un membre unique est désigné « aujourd'hui » ; **sur 2 devices, le désigné affiché est identique**.
- [ ] Sur N jours simulés, chaque membre est désigné une fois par manche (aucune répétition avant la fin du tour).
- [ ] Seul le désigné peut enregistrer/publier ; les autres voient l'écran « En attente de @pseudo ».
- [ ] Le vlog (≥ 5 s) est enregistré, compressé, uploadé (progression + reprise réseau coupé/rétabli), puis lisible par tous.
- [ ] Réactions et commentaires fonctionnent et se synchronisent entre devices.
- [ ] Notification « c'est ton tour » reçue à l'heure de bascule ; tap → écran d'enregistrement.
- [ ] « Passer mon tour » désigne le membre suivant, une seule fois par manche.
- [ ] Jour sans vlog → « raté » + tirage du lendemain correct ; streak cohérente.
- [ ] Rotation écran, kill/relance process, mode avion : aucune perte de donnée ni crash.
- [ ] Permissions caméra/micro/notifications : refus → message clair + lien vers réglages, pas de crash.
- [ ] `./gradlew testDebugUnitTest` et les tests instrumentés/UI sont verts ; **aucun crash** dans `adb logcat` pendant les parcours.
- [ ] `README.md` permet à un tiers de builder, configurer le backend et lancer l'app en < 15 min.

---

## 8. Livrables finaux

1. APK debug installé et fonctionnel + chemin de l'APK release.
2. Code source complet, tests verts, scripts `scripts/*.sh`.
3. `backend/` complet (SQL + RLS + guide) et `local.properties.example`.
4. `tests/artemis/` : scénarios + rapports (avec `trace_id`).
5. Rapport final `REPORT.md` : état de chaque critère §7 (✅/❌ + preuve), limites connues, prochaines étapes.

**Commence maintenant par M0 : `mobile_diagnose`, puis `PLAN.md`.**
