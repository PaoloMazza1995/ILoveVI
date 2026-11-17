-- commit_map_edits already refuses anonymous callers from inside the function body (it checks
-- public.is_admin() and raises if false), but PostgREST auto-grants EXECUTE on new public
-- functions to anon/authenticated unless revoked — so an anon caller could still reach the
-- function body and spend a wasted round-trip before being refused. delete_own_account already
-- revokes anon's EXECUTE grant (20261106120001); this does the same for commit_map_edits, for
-- defense in depth and consistency between the two RPCs. Pre-submission audit finding BE-07 /
-- RA-11, 2026-11-17 (docs/RELEASE-AUDIT.md).

revoke execute on function public.commit_map_edits(jsonb) from anon;
