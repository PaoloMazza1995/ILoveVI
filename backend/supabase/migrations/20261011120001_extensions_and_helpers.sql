-- pgTAP powers the test suite in supabase/tests/database; harmless to have in every environment.
create extension if not exists pgtap with schema extensions;

-- Admin claim convention: a user is an admin when their JWT's app_metadata.role is "admin".
-- app_metadata (unlike user_metadata) can only be set by a service-role call, never by the user
-- themselves, so this is safe to trust from RLS policies.
create or replace function public.is_admin()
returns boolean
language sql
stable
set search_path = ''
as $$
  select coalesce(
    (auth.jwt() -> 'app_metadata' ->> 'role') = 'admin',
    false
  );
$$;

comment on function public.is_admin() is
  'True when the calling JWT carries app_metadata.role = "admin". See backend/README.md for how to grant it.';

-- Shared "bump updated_at on every UPDATE" trigger, attached per table below. INSERT is left to
-- the updated_at column's own DEFAULT now(), so seeding/backfilling with an explicit updated_at
-- still works.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;
