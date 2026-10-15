# Scénario ARTEMIS — M5 : Lecteur Media3, Réactions, Commentaires, Vu-par & Streak

## Objectifs
- Valider le lecteur vidéo immersif plein écran (Media3 / ExoPlayer).
- Valider la lecture auto en boucle et le contrôle du volume / play-pause.
- Valider les réactions emojis (6 emojis) et les commentaires texte.
- Valider le tracking « vu par » (avatars des membres ayant regardé).
- Valider le calcul de la streak du groupe (jours consécutifs avec vlog).

## Séquence de test
1. **Accès au Vlog** :
   - Depuis l'écran Aujourd'hui ou Groupe, tap sur `btn_watch_vlog`.
   - Vérifier le lancement immédiat du lecteur vidéo en boucle.
   - Tap au centre pour mettre en pause / relancer.
2. **Réactions Emojis** :
   - Tap sur les boutons d'émojis (`btn_reaction_❤️`, `btn_reaction_🔥`, etc.).
   - Vérifier l'incrémentation du compteur et la surbrillance de l'émoji actif.
3. **Commentaires** :
   - Tap sur le bouton commentaires (`btn_open_comments`) -> ouverture du ModalBottomSheet.
   - Saisir un texte dans `input_comment` ("Génial ce vlog !").
   - Tap sur `btn_send_comment` -> affichage immédiat du commentaire avec auteur `@pseudo` et heure.
   - Fermer la bottom sheet.
4. **Indicateur « Vu par » & Streak** :
   - Vérifier le badge de streak `badge_streak` (ex: "🔥 1j").
   - Tap sur l'icône de vue (`btn_open_viewers`) -> affichage de la liste des membres ayant visionné la vidéo.
