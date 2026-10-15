# Scénario ARTEMIS — M1 : Authentification & Navigation

## Objectifs
- Valider le flux d'inscription, connexion, consultation de profil et déconnexion.
- Vérifier la persistance de session et le bon fonctionnement du thème M3.

## Comptes de test
- Alice : `alice@test.dev` / `Test1234!`
- Bob : `bob@test.dev` / `Test1234!`
- Nouveau compte généré lors du test : `claire@test.dev` / `Secret123!` (pseudo: "Claire")

## Séquence de test (Flash)
1. **Écran d'accueil** : Tap sur le bouton "Connexion" (testTag `btn_login`).
2. **Écran de connexion** :
   - Saisir `alice@test.dev` dans `login_email_input`.
   - Saisir `Test1234!` dans `login_password_input`.
   - Tap sur `login_submit_btn`.
3. **Écran principal (Accueil)** :
   - Vérifier la présence du message de bienvenue `@Alice` (`home_greeting_text`).
   - Vérifier la présence du bouton profil (`home_profile_btn`).
   - Tap sur `home_profile_btn`.
4. **Écran Profil** :
   - Vérifier que l'email `alice@test.dev` est affiché (`profile_email_text`).
   - Modifier le pseudo en `Alice SuperStar` dans `profile_pseudo_input`.
   - Tap sur `profile_save_btn`.
   - Tap sur `profile_logout_btn`.
5. **Retour Accueil Déconnecté** :
   - Vérifier que l'écran d'accueil (`welcome_screen`) est réaffiché.
   - Tap sur "Inscription" (`btn_register`).
6. **Écran d'inscription** :
   - Saisir pseudo `Claire`, email `claire@test.dev`, mdp `Secret123!`, confirmation `Secret123!`.
   - Tap sur `register_submit_btn`.
   - Vérifier l'arrivée sur l'écran d'accueil avec `@Claire`.
