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
| **M4** | Enregistrement/import vidéo, compression 720p, upload WorkManager | 🔄 En cours | CameraX VideoCapture 1 prise, aperçu ExoPlayer, légende, WorkManager upload |
| **M5** | Fil, lecteur ExoPlayer, réactions emojis, commentaires, vu-par, streak | ⏳ En attente | Bob réagit & commente ; synchronisation temps réel vérifiée |
| **M6** | Notifications, rappel 4h avant minuit, « passer mon tour », jour raté | ⏳ En attente | Pro : notification reçue, tap-through direct vers enregistrement |
| **M7** | Backend réel Supabase (schéma, RLS, storage) + bascule `USE_FAKE_BACKEND=false` | ⏳ En attente | Scénarios rejoués contre Supabase |
| **M8** | Polish : états vides/erreurs/offline, accessibilité, thème sombre, APK release | ⏳ En attente | Checklist §7 100 % validée, rapport `REPORT.md` |

---

## Critères d'Acceptation (§7 du prompt)

- [ ] Alice s'inscrit, crée « Les Potes », obtient un code et un lien d'invitation.
- [ ] Bob (autre device/compte) rejoint via code et via deep link ; Alice voit Bob dans les membres.
- [ ] Avec 2+ membres, un membre unique est désigné « aujourd'hui » ; sur 2 devices, le désigné affiché est identique.
- [ ] Sur N jours simulés, chaque membre est désigné une fois par manche (aucune répétition avant la fin du tour).
- [ ] Seul le désigné peut enregistrer/publier ; les autres voient l'écran « En attente de @pseudo ».
- [ ] Le vlog (≥ 5 s) est enregistré, compressé, uploadé (progression + reprise réseau), puis lisible par tous.
- [ ] Réactions et commentaires fonctionnent et se synchronisent entre devices.
- [ ] Notification « c'est ton tour » reçue à l'heure de bascule ; tap → écran d'enregistrement.
- [ ] « Passer mon tour » désigne le membre suivant, une seule fois par manche.
- [ ] Jour sans vlog → « raté » + tirage du lendemain correct ; streak cohérente.
- [ ] Rotation écran, kill/relance process, mode avion : aucune perte de donnée ni crash.
- [ ] Permissions caméra/micro/notifications : refus → message clair + lien vers réglages, pas de crash.
- [ ] `./gradlew testDebugUnitTest` et les tests instrumentés/UI sont verts ; aucun crash dans `adb logcat`.
- [ ] `README.md` permet à un tiers de builder, configurer le backend et lancer l'app en < 15 min.
