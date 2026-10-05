create unique index if not exists profiles_username_canonical_unique
on public.profiles (lower(username))
where username is not null and btrim(username) <> '';

alter table public.profiles
  drop constraint if exists profiles_username_canonical_check;

alter table public.profiles
  add constraint profiles_username_canonical_check
  check (
    username = lower(username)
    and username ~ '^@[a-z0-9][a-z0-9_]{2,23}\.link$'
  );

create or replace function public.prevent_username_change()
returns trigger
language plpgsql
as $$
begin
  if tg_op = 'UPDATE' and old.username is distinct from new.username then
    raise exception 'Username cannot be changed once assigned';
  end if;
  return new;
end;
$$;

drop trigger if exists profiles_username_immutable on public.profiles;

create trigger profiles_username_immutable
before update of username on public.profiles
for each row
execute function public.prevent_username_change();
