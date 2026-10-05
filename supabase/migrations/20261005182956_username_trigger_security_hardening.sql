create or replace function public.prevent_username_change()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and old.username is distinct from new.username then
    raise exception 'Username cannot be changed once assigned';
  end if;
  return new;
end;
$$;
