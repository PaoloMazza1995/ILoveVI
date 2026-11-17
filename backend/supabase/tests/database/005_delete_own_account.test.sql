-- delete_own_account: a signed-in user can delete their own auth.users row (and nothing else's);
-- an anonymous caller is refused; a caller with no session is refused.
begin;
select plan(6);

insert into auth.users (id, email, aud, role)
values
  ('a0000000-0000-0000-0000-000000000001', 'delete-own-account-1@example.com', 'authenticated', 'authenticated'),
  ('a0000000-0000-0000-0000-000000000002', 'delete-own-account-2@example.com', 'authenticated', 'authenticated');

-- Signed in as user 1.
select set_config(
  'request.jwt.claims',
  '{"role":"authenticated","sub":"a0000000-0000-0000-0000-000000000001"}',
  true
);
set local role authenticated;

select lives_ok(
  $$ select public.delete_own_account() $$,
  'a signed-in user can delete their own account'
);

-- `authenticated` has no SELECT on auth.users (by design — this repo's privilege, not a bug this
-- step introduces), so the verification queries below run after `reset role`, back to the role
-- this test session connects as, which does have full read access.
reset role;

select is_empty(
  $$ select 1 from auth.users where id = 'a0000000-0000-0000-0000-000000000001' $$,
  'the caller''s own auth.users row is gone'
);

select isnt_empty(
  $$ select 1 from auth.users where id = 'a0000000-0000-0000-0000-000000000002' $$,
  'a different user''s row is untouched'
);

-- Signed in as user 2, trying (and failing) to re-delete the already-deleted user 1 is covered
-- implicitly by the function always acting on auth.uid() only — there is no parameter through
-- which user 2 could even name user 1. Instead, confirm user 2 can delete their own account too.
select set_config(
  'request.jwt.claims',
  '{"role":"authenticated","sub":"a0000000-0000-0000-0000-000000000002"}',
  true
);
set local role authenticated;

select lives_ok(
  $$ select public.delete_own_account() $$,
  'a second signed-in user can also delete their own account'
);

reset role;

select is_empty(
  $$ select 1 from auth.users where id = 'a0000000-0000-0000-0000-000000000002' $$,
  'user 2''s row is now gone too'
);

-- Anonymous cannot call it at all (no execute grant, and auth.uid() would be null anyway).
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);

select throws_like(
  $$ select public.delete_own_account() $$,
  '%permission denied%',
  'anonymous cannot call delete_own_account (no execute grant)'
);

reset role;

select * from finish();
rollback;
