-- ==============================================================================
-- Daily Vlog Crew — Production Database Schema (Supabase / PostgreSQL)
-- Includes: Tables, Constraints, Indexes, Functions, Triggers, RLS & Storage Buckets
-- ==============================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. ENUMS
DO $$ BEGIN
    CREATE TYPE member_role AS ENUM ('ADMIN', 'MEMBER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE assignment_status AS ENUM ('PENDING', 'POSTED', 'MISSED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 3. PROFILES TABLE (Mirrors auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    pseudo VARCHAR(30) NOT NULL UNIQUE,
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- Index for searching users by pseudo
CREATE INDEX IF NOT EXISTS idx_profiles_pseudo ON public.profiles(pseudo);

-- 4. GROUPS TABLE
CREATE TABLE IF NOT EXISTS public.groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL,
    emoji VARCHAR(10) DEFAULT '🎬' NOT NULL,
    color_hex VARCHAR(10) DEFAULT '#FF5252' NOT NULL,
    admin_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    invite_code VARCHAR(6) NOT NULL UNIQUE,
    switch_time VARCHAR(5) DEFAULT '09:00' NOT NULL,
    max_duration_seconds INT DEFAULT 60 NOT NULL CHECK (max_duration_seconds BETWEEN 10 AND 180),
    timezone VARCHAR(50) DEFAULT 'Europe/Paris' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_groups_invite_code ON public.groups(invite_code);

-- 5. GROUP MEMBERS TABLE
CREATE TABLE IF NOT EXISTS public.group_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    pseudo VARCHAR(30) NOT NULL,
    avatar_url TEXT,
    role member_role DEFAULT 'MEMBER' NOT NULL,
    joined_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON public.group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON public.group_members(user_id);

-- 6. DAILY ASSIGNMENTS TABLE (Deterministic rotation & history)
CREATE TABLE IF NOT EXISTS public.daily_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    date VARCHAR(10) NOT NULL, -- Format YYYY-MM-DD
    round_number INT NOT NULL DEFAULT 1,
    assigned_user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    assigned_user_pseudo VARCHAR(30) NOT NULL,
    deadline_timestamp BIGINT NOT NULL,
    status assignment_status DEFAULT 'PENDING' NOT NULL,
    vlog_id UUID,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(group_id, date)
);

CREATE INDEX IF NOT EXISTS idx_daily_assignments_group_date ON public.daily_assignments(group_id, date);

-- 7. VLOGS TABLE
CREATE TABLE IF NOT EXISTS public.vlogs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE RESTRICT,
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption VARCHAR(140),
    duration_seconds INT NOT NULL CHECK (duration_seconds > 0),
    date VARCHAR(10) NOT NULL, -- Format YYYY-MM-DD
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(group_id, date) -- One vlog per group per day constraint
);

CREATE INDEX IF NOT EXISTS idx_vlogs_group_date ON public.vlogs(group_id, date);

-- 8. VLOG REACTIONS TABLE
CREATE TABLE IF NOT EXISTS public.vlog_reactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vlog_id UUID NOT NULL REFERENCES public.vlogs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(vlog_id, user_id, emoji)
);

CREATE INDEX IF NOT EXISTS idx_vlog_reactions_vlog ON public.vlog_reactions(vlog_id);

-- 9. VLOG COMMENTS TABLE
CREATE TABLE IF NOT EXISTS public.vlog_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vlog_id UUID NOT NULL REFERENCES public.vlogs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    user_pseudo VARCHAR(30) NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vlog_comments_vlog ON public.vlog_comments(vlog_id);

-- 10. VLOG VIEWS TABLE (Seen-by tracking)
CREATE TABLE IF NOT EXISTS public.vlog_views (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vlog_id UUID NOT NULL REFERENCES public.vlogs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    UNIQUE(vlog_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_vlog_views_vlog ON public.vlog_views(vlog_id);

-- ==============================================================================
-- 11. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vlogs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vlog_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vlog_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vlog_views ENABLE ROW LEVEL SECURITY;

-- PROFILES
CREATE POLICY "Public profiles are viewable by authenticated users"
ON public.profiles FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can insert their own profile"
ON public.profiles FOR INSERT TO authenticated
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
ON public.profiles FOR UPDATE TO authenticated
USING (auth.uid() = id);

-- GROUPS
CREATE POLICY "Group members can view their groups"
ON public.groups FOR SELECT TO authenticated
USING (
    id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
    OR invite_code IS NOT NULL -- Allow lookup by invite code
);

CREATE POLICY "Authenticated users can create groups"
ON public.groups FOR INSERT TO authenticated
WITH CHECK (auth.uid() = admin_id);

CREATE POLICY "Admins can update their groups"
ON public.groups FOR UPDATE TO authenticated
USING (auth.uid() = admin_id);

CREATE POLICY "Admins can delete their groups"
ON public.groups FOR DELETE TO authenticated
USING (auth.uid() = admin_id);

-- GROUP MEMBERS
CREATE POLICY "Group members can view other members in their groups"
ON public.group_members FOR SELECT TO authenticated
USING (
    group_id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
);

CREATE POLICY "Users can join groups as members"
ON public.group_members FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Members can leave or admins can remove members"
ON public.group_members FOR DELETE TO authenticated
USING (
    auth.uid() = user_id
    OR auth.uid() IN (SELECT admin_id FROM public.groups WHERE id = group_id)
);

-- DAILY ASSIGNMENTS
CREATE POLICY "Group members can view assignments"
ON public.daily_assignments FOR SELECT TO authenticated
USING (
    group_id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
);

CREATE POLICY "Group members can manage assignments"
ON public.daily_assignments FOR ALL TO authenticated
USING (
    group_id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
);

-- VLOGS
CREATE POLICY "Group members can view vlogs in their group"
ON public.vlogs FOR SELECT TO authenticated
USING (
    group_id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
);

CREATE POLICY "Designated member can publish vlog"
ON public.vlogs FOR INSERT TO authenticated
WITH CHECK (
    auth.uid() = user_id
    AND group_id IN (SELECT group_id FROM public.group_members WHERE user_id = auth.uid())
);

-- REACTIONS
CREATE POLICY "Group members can view reactions"
ON public.vlog_reactions FOR SELECT TO authenticated
USING (
    vlog_id IN (
        SELECT id FROM public.vlogs WHERE group_id IN (
            SELECT group_id FROM public.group_members WHERE user_id = auth.uid()
        )
    )
);

CREATE POLICY "Group members can add reactions"
ON public.vlog_reactions FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can remove their own reactions"
ON public.vlog_reactions FOR DELETE TO authenticated
USING (auth.uid() = user_id);

-- COMMENTS
CREATE POLICY "Group members can view comments"
ON public.vlog_comments FOR SELECT TO authenticated
USING (
    vlog_id IN (
        SELECT id FROM public.vlogs WHERE group_id IN (
            SELECT group_id FROM public.group_members WHERE user_id = auth.uid()
        )
    )
);

CREATE POLICY "Group members can post comments"
ON public.vlog_comments FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

-- VIEWS
CREATE POLICY "Group members can view seen-by list"
ON public.vlog_views FOR SELECT TO authenticated
USING (
    vlog_id IN (
        SELECT id FROM public.vlogs WHERE group_id IN (
            SELECT group_id FROM public.group_members WHERE user_id = auth.uid()
        )
    )
);

CREATE POLICY "Group members can record their view"
ON public.vlog_views FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

-- ==============================================================================
-- 12. STORAGE BUCKETS CONFIGURATION
-- ==============================================================================

-- Create buckets for vlogs and user avatars
INSERT INTO storage.buckets (id, name, public)
VALUES ('vlogs', 'vlogs', true),
       ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

-- Storage policies for Vlogs
CREATE POLICY "Authenticated users can upload vlogs"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'vlogs');

CREATE POLICY "Public read for vlogs"
ON storage.objects FOR SELECT TO authenticated
USING (bucket_id = 'vlogs');

-- Storage policies for Avatars
CREATE POLICY "Authenticated users can upload avatars"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'avatars');

CREATE POLICY "Public read for avatars"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'avatars');
