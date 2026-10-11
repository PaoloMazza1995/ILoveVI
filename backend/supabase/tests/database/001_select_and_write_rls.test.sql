-- RLS: anyone can SELECT; only an admin (app_metadata.role = "admin") can write.
begin;
select plan(3);

insert into public.weapons (id, name, description, sub_type)
values ('test-weapon-rls', 'Test Weapon', 'A test weapon.', 'PISTOL');

-- Anonymous SELECT works.
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);

select isnt_empty(
  $$ select 1 from public.weapons where id = 'test-weapon-rls' $$,
  'anonymous can select weapons'
);

-- Anonymous INSERT is refused.
select throws_like(
  $$ insert into public.weapons (id, name, description, sub_type)
     values ('anon-weapon-rls', 'x', 'y', 'PISTOL') $$,
  '%row-level security%',
  'anonymous insert into weapons is refused'
);

reset role;

-- Admin (authenticated + app_metadata.role = "admin") INSERT works.
select set_config(
  'request.jwt.claims',
  '{"role":"authenticated","app_metadata":{"role":"admin"}}',
  true
);
set local role authenticated;

select lives_ok(
  $$ insert into public.weapons (id, name, description, sub_type)
     values ('admin-weapon-rls', 'Admin Weapon', 'Inserted by an admin.', 'PISTOL') $$,
  'admin insert into weapons succeeds'
);

select * from finish();
rollback;
