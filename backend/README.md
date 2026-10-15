# 🚀 Guide de Configuration Supabase — Daily Vlog Crew

Ce document explique comment déployer et configurer le backend distant Supabase en moins de 5 minutes.

---

## 1. Création du Projet Supabase
1. Rendez-vous sur [supabase.com](https://supabase.com) et connectez-vous.
2. Créez un nouveau projet (ex: `daily-vlog-crew`).
3. Choisissez une région proche (ex: `eu-west-3` pour Paris) et un mot de passe fort pour la base PostgreSQL.

---

## 2. Exécution du Schéma SQL
1. Dans le tableau de bord Supabase, allez dans la section **SQL Editor**.
2. Créez une nouvelle requête (**New query**).
3. Copiez l'intégralité du contenu de [`backend/schema.sql`](schema.sql) et collez-le dans l'éditeur.
4. Cliquez sur **Run** (Exécuter).
5. Vérifiez que toutes les tables sont créées dans **Table Editor** :
   - `profiles`
   - `groups`
   - `group_members`
   - `daily_assignments`
   - `vlogs`
   - `vlog_reactions`
   - `vlog_comments`
   - `vlog_views`

---

## 3. Configuration du Stockage (Storage Buckets)
Le script SQL crée automatiquement les buckets :
- `vlogs` (public, vidéos MP4 H.264 et miniatures)
- `avatars` (public, photos de profil des membres)

Si besoin, vérifiez dans **Storage** > **Buckets** que les deux buckets existent et que leurs politiques RLS sont actives.

---

## 4. Liaison avec l'Application Android
1. Dans le tableau de bord Supabase, allez dans **Project Settings** > **API**.
2. Récupérez :
   - **Project URL** (ex: `https://xyzproject.supabase.co`)
   - **anon / public key** (clé API publique)
3. Ouvrez ou créez le fichier `local.properties` à la racine du projet Android :
   ```properties
   supabase.url=https://xyzproject.supabase.co
   supabase.anon.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
4. Rebuildez l'application via `./scripts/build_install.sh`.
   L'application détecte automatiquement la présence des clés pour basculer en mode `RemoteBackend`. En l'absence de ces clés, l'application fonctionne à 100% de manière autonome grâce au `FakeBackend`.
