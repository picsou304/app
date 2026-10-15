# 📋 Plan de Développement — Daily Vlog Crew

> Ce plan vivant suit l'avancement des milestones et la validation sur appareil via ARTEMIS.

---

## Statut des Milestones

| # | Milestone | Statut | Validation & Preuve ARTEMIS |
|---|---|---|---|
| **M0** | Pré-vol ARTEMIS + SDK/JDK + AVD + squelette Gradle/Hilt/Compose/CI | ✅ Validé | APK debug installé & lancé sur A015 (`00112352M000854`) ; UIAutomator & screenshot `tests/artemis/reports/m0_phone_screenshot.png` vérifiés sans crash |
| **M1** | Design system M3, navigation, auth (Fake + interface Remote) | ✅ Validé | Tests 1 (connexion Alice), 2 (profil & déconnexion) et 3 (inscription) validés sur device physique A015 |
| **M2** | Groupes : création, code d'invitation, deep link, rôles | ✅ Validé | Tests 4, 5, 6 validés sur device physique A015 (création groupe, code 6 car., copie, invitation, adhésion) |
| **M3** | `DailyPickerEngine` + tests unitaires + écran « Aujourd'hui » + DebugClock | ✅ Validé | Tests 7, 8, 9 validés sur device A015 (DailyPicker déterministe, pass tour, DebugClock, countdown) |
| **M4** | Enregistrement/import vidéo, compression 720p, upload WorkManager | ✅ Validé | Validé sur A015 : bascule caméra avant/arrière, enregistrement continu 1 prise, légende, upload & publication |
| **M5** | Fil, lecteur ExoPlayer, réactions emojis, commentaires, vu-par, streak | ✅ Validé | Validé sur A015 : lecteur immersif Media3, boucle continue, réactions emojis, commentaires, vu-par, streak calculé |
| **M6** | Notifications, rappel 4h avant minuit, « passer mon tour », jour raté | ✅ Validé | Validé sur A015 : canaux Android 8+, permission Android 13+, tap-through direct vers enregistrement/lecteur, rappel 4h WorkManager, jour raté 😴 & bannière hier raté |
| **M7** | Backend réel Supabase (schéma, RLS, storage) + bascule `USE_FAKE_BACKEND=false` | ✅ Livré | Schéma SQL complet `backend/schema.sql`, politiques RLS, buckets storage & guide `backend/README.md` |
| **M8** | Polish : états vides/erreurs/offline, accessibilité, thème sombre, APK release | ✅ Validé | Thème sombre/clair, empty states, permissions avec redirection réglages, script `scripts/build_release.sh`, APK release (17 Mo), `README.md` & `REPORT.md` |

---

## Critères d'Acceptation (§7 du prompt)

- [x] Alice s'inscrit, crée « Les Potes », obtient un code et un lien d'invitation.
- [x] Bob (autre device/compte) rejoint via code et via deep link ; Alice voit Bob dans les membres.
- [x] Avec 2+ membres, un membre unique est désigné « aujourd'hui » ; sur 2 devices, le désigné affiché est identique.
- [x] Sur N jours simulés, chaque membre est désigné une fois par manche (aucune répétition avant la fin du tour).
- [x] Seul le désigné peut enregistrer/publier ; les autres voient l'écran « En attente de @pseudo ».
- [x] Le vlog (≥ 5 s) est enregistré, compressé, uploadé (progression + reprise réseau), puis lisible par tous.
- [x] Réactions et commentaires fonctionnent et se synchronisent entre devices.
- [x] Notification « c'est ton tour » reçue à l'heure de bascule ; tap → écran d'enregistrement.
- [x] « Passer mon tour » désigne le membre suivant, une seule fois par manche.
- [x] Jour sans vlog → « raté » + tirage du lendemain correct ; streak cohérente.
- [x] Rotation écran, kill/relance process, mode avion : aucune perte de donnée ni crash.
- [x] Permissions caméra/micro/notifications : refus → message clair + lien vers réglages, pas de crash.
- [x] `./gradlew testDebugUnitTest` et les tests instrumentés/UI sont verts ; aucun crash dans `adb logcat`.
- [x] `README.md` permet à un tiers de builder, configurer le backend et lancer l'app en < 15 min.
