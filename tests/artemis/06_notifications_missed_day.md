# Scénario ARTEMIS — M6 : Notifications, Rappels & Jour Raté

## Objectifs
- Valider la création des canaux de notification Android 8+ (`daily_turn_channel`, `vlog_reminders_channel`, `group_updates_channel`).
- Valider la demande de permission `POST_NOTIFICATIONS` sous Android 13+.
- Valider le routage tap-through des notifications (clic -> ouverture directe de l'enregistrement ou du lecteur).
- Valider le rappel 4 heures avant la date limite via WorkManager (`DailyReminderWorker`).
- Valider la gestion du jour raté sans vlog (bannière hier raté et carte jour raté avec rupture de streak).

## Séquence de test
1. **Permission & Canaux de Notification** :
   - Lancement de l'application : acceptation de la boîte de dialogue de notification Android 13+.
   - Inspection système : vérifier la présence des 3 canaux avec importances configurées.
2. **Notification « C'est ton tour » & Tap-Through** :
   - Tap sur `btn_test_turn_notification` dans la section debug de l'écran Aujourd'hui.
   - Notification système reçue : "C'est ton tour aujourd'hui ! 🎬".
   - Tap sur la notification dans la barre d'état -> navigation automatique vers l'écran d'enregistrement CameraX.
3. **Notification « Nouveau vlog » & Tap-Through** :
   - Tap sur `btn_test_new_vlog_notification`.
   - Notification système reçue : "Nouveau vlog publié ! 🎉".
   - Tap sur la notification -> ouverture directe du lecteur vidéo Media3.
4. **Jour raté & Rupture de Streak** :
   - Option A : Tap sur `+1 Jour (Bascule)` sans avoir posté de vlog -> affichage de la bannière d'alerte `yesterday_vlog_missed_card` ("Hier : Jour raté ! 😴 Aucun vlog n'a été publié hier. La streak du groupe est brisée.").
   - Option B : Tap sur `btn_simulate_missed_day` -> affichage immédiat de la carte `today_vlog_missed_card` ("Jour raté ! 😴 @pseudo n'a pas publié son vlog à temps.").
