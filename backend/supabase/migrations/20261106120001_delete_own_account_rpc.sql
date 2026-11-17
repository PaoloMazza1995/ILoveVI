-- delete_own_account: step 13B's in-app account deletion (Google Play / Apple's own "delete your
-- account from within the app" requirement — see docs/auth-notes.md §2). Chosen over an Edge
-- Function: backend/supabase/functions/ exists but has never shipped one (the one earlier attempt,
-- step 12A's fetch-news job, was reverted in step 12D), so this follows the same "one more RPC
-- migration" shape :commit_map_edits already established, with the real advantage that no
-- service-role key is ever needed anywhere — SECURITY DEFINER lets this one statement touch
-- `auth.users` (normally only reachable by the service role / `postgres`) while the RPC call
-- itself still goes through ordinary Postgrest auth (the caller must be `authenticated`, i.e.
-- signed in, to call it at all).
--
-- No arguments on purpose: it always acts on auth.uid(), the calling session's own id, so there is
-- no parameter an attacker could ever pass to delete someone else's account.
--
-- `auth.users` -> `auth.identities`/`auth.sessions`/`auth.refresh_tokens`/`auth.mfa_factors` are
-- all ON DELETE CASCADE in Supabase's own auth schema, so one delete here removes the whole
-- account (every session, every linked identity), not just a profile row. No app content table
-- needs cleanup here — per this step's own scope, "no user data is synchronized in this step."
create or replace function public.delete_own_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'delete_own_account requires a signed-in session';
  end if;

  delete from auth.users where id = auth.uid();
end;
$$;

comment on function public.delete_own_account() is
  'Deletes the calling user''s own auth.users row (cascading to their sessions/identities). Called by :core:data''s AccountServiceImpl.deleteAccount(). See backend/supabase/tests/database/005_delete_own_account.test.sql.';

-- Only signed-in callers may even attempt this (anon has no reason to call it, and auth.uid() is
-- null for them anyway — the explicit grant removes any ambiguity rather than relying only on the
-- in-function check).
revoke all on function public.delete_own_account() from public;
revoke all on function public.delete_own_account() from anon;
grant execute on function public.delete_own_account() to authenticated;
