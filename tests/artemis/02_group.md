# Scénario ARTEMIS — M2 : Gestion des Groupes & Invitations

## Objectifs
- Valider la création d'un groupe avec emoji, couleur, nom, heure de bascule et durée maximale.
- Valider la génération d'un code à 6 caractères uniques et le partage par deep link (`dailyvlog://join/<code>`).
- Valider l'adhésion d'un second utilisateur (Bob) par saisie directe du code et via deep link.
- Vérifier la gestion des rôles (Admin vs Membre).

## Séquence de test
1. **Création de groupe (Alice)** :
   - Connexion sous Alice (`alice@test.dev`).
   - Tap sur `home_create_group_btn`.
   - Saisir le nom "Les Aventuriers", choisir l'emoji 🏔️, durée 60s, heure de bascule 09:00.
   - Tap sur `btn_submit_create_group`.
   - Vérifier la redirection vers la page de détails du groupe.
2. **Code d'invitation & Deep Link** :
   - Vérifier la présence du tag `invite_code_display` affichant 6 caractères alphanumériques.
   - Tap sur `copy_code_btn` -> Toast "Code copié".
   - Tap sur `share_link_btn` -> Partage Android avec lien `dailyvlog://join/<code>`.
3. **Adhésion au groupe (Bob)** :
   - Déconnexion d'Alice, connexion sous Bob (`bob@test.dev`).
   - Tap sur `home_join_group_btn`.
   - Saisir le code d'invitation à 6 caractères dans `join_code_input`.
   - Tap sur `btn_submit_join`.
   - Vérifier l'accès au groupe et la présence d'Alice et Bob dans la liste des membres (`member_item_<id>`).
4. **Adhésion via Deep Link** :
   - Lancement intent adb : `am start -W -a android.intent.action.VIEW -d "dailyvlog://join/K7QX2M" com.dailyvlog.app`.
   - Vérifier l'adhésion automatique et l'ouverture immédiate du groupe.
