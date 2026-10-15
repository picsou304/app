# Scénario ARTEMIS — M0 : Vérification du lancement de l'application (« Hello »)

## Contexte
- **Application** : `com.dailyvlog.app`
- **Activité de départ** : `com.dailyvlog.app/.MainActivity`
- **Device cible** : `00112352M000854` (Téléphone physique A015)
- **Modèle ARTEMIS** : Flash

## Task Description
```markdown
Ouvre l'application Daily Vlog Crew (package: com.dailyvlog.app).
Vérifie que l'écran d'accueil s'affiche correctement :
1. Le logo caméra (testTag "app_logo") est visible.
2. Le titre "Bienvenue sur Daily Vlog Crew" (testTag "welcome_title") est présent.
3. Le sous-titre "Chaque jour, un membre poste son vlog !" (testTag "welcome_subtitle") est présent.
4. Les boutons "Connexion" (testTag "btn_login") et "Inscription" (testTag "btn_register") sont visibles et cliquables.

Prends une capture d'écran de confirmation et rapporte l'état visuel et les éléments détectés.
Arrête la tâche après avoir confirmé la présence de ces éléments.
```

## Résultat attendu
- L'application se lance sans crash.
- Les éléments Compose s'affichent avec le thème Material 3.
- Aucun crash dans `adb logcat`.
