# Scénario ARTEMIS — M3 : DailyPickerEngine & Écran « Aujourd'hui »

## Objectifs
- Valider le tirage déterministe pseudo-aléatoire du membre du jour selon la date active et le fuseau horaire.
- Vérifier que chaque membre est tiré exactement 1 fois par manche (fair rotation).
- Valider le compte à rebours avant la date limite.
- Valider l'action « Passer mon tour » et le DebugClock.

## Séquence de test
1. **Écran Aujourd'hui** :
   - Depuis les détails du groupe, tap sur `open_today_card`.
   - Vérifier l'affichage du membre désigné du jour (`today_assigned_pseudo`).
   - Vérifier le compte à rebours dynamique heures/minutes restantes (`countdown_chip`).
2. **Différenciation de rôle** :
   - Si Alice est désignée : affichage de la carte `today_assignment_card` avec bouton `btn_start_record`.
   - Si Bob ouvre l'application ce même jour : affichage de la carte `today_waiting_card` ("En attente de @Alice").
3. **Passer son tour (Skip)** :
   - Sous le compte désigné, tap sur `btn_skip_turn`.
   - Confirmation du dialogue d'abandon.
   - Vérifier la réassignation immédiate au membre suivant de la manche.
4. **DebugClock & Bascule de jour** :
   - Tap sur `+1 Jour (Bascule)` dans la section debug.
   - Vérifier la mise à jour de la manche et la désignation du membre suivant.
   - Tap sur `Reset Heure` -> retour à l'heure système réelle.
