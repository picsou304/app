# Scénario ARTEMIS — M4 : Enregistrement, Caméra, Compression & Upload

## Objectifs
- Valider la capture vidéo via CameraX en 1 seule prise continue (pause interdite).
- Valider la bascule caméra avant/arrière (`btn_flip_camera`).
- Valider l'arrêt automatique à la durée maximale du groupe (ex: 60s).
- Valider l'écran d'aperçu avant publication (rejouer, refaire, légende ≤ 140 car.).
- Valider la compression et la publication directe (ou en tâche de fond WorkManager).

## Séquence de test
1. **Lancement de l'enregistrement** :
   - Depuis l'écran Aujourd'hui (membre désigné), tap sur `btn_start_record`.
   - Si les permissions ne sont pas encore accordées : affichage de `permission_request_view` -> tap sur `btn_grant_permissions` ou `btn_open_app_settings`.
2. **Capture & Bascule Caméra** :
   - Vérifier le flux vidéo de prévisualisation CameraX (`camera_preview`).
   - Tap sur `btn_flip_camera` -> bascule instantanée entre caméra arrière et caméra frontale.
   - Tap sur `btn_record_toggle` -> début de l'enregistrement (jauge rouge active, chronomètre en cours).
   - Tap sur `btn_record_toggle` pour stopper après 5 secondes.
3. **Aperçu & Légende** :
   - Affichage immédiat du lecteur de prévisualisation (`preview_player`).
   - Saisir une légende dans `input_vlog_caption` : "Superbe journée avec le crew ! #vlog".
   - Tap sur `btn_publish_vlog`.
4. **Validation de la publication** :
   - Retour automatique à l'écran Aujourd'hui.
   - Vérification de la carte `vlog_posted_card` ("🎉 Vlog du jour publié !").
