create or replace function public.complete_profile(
  p_username text,
  p_full_name text,
  p_avatar_url text,
  p_designation text default null
)
returns public.profiles
language plpgsql
security definer
set search_path to ''
as $function$
declare
  me uuid := auth.uid();
  normalized_username text;
  existing_username text;
  result public.profiles;
begin
  if me is null then
    raise exception 'not_authenticated' using errcode = '28000';
  end if;

  normalized_username := lower(btrim(p_username));
  normalized_username := regexp_replace(normalized_username, '^@+', '@');

  if normalized_username !~ '^@[a-z0-9_][a-z0-9_.-]*$' then
    raise exception 'invalid_username' using errcode = '22023';
  end if;

  if normalized_username !~ '\.link$' then
    normalized_username := normalized_username || '.link';
  end if;

  if length(normalized_username) < 7 or length(normalized_username) > 40 then
    raise exception 'invalid_username_length' using errcode = '22023';
  end if;

  if length(btrim(p_full_name)) = 0 or length(btrim(p_full_name)) > 120 then
    raise exception 'invalid_full_name' using errcode = '22023';
  end if;

  if length(btrim(p_avatar_url)) = 0 then
    raise exception 'avatar_required' using errcode = '22023';
  end if;

  select username into existing_username
  from public.profiles
  where id = me;

  if existing_username is not null and lower(existing_username) <> normalized_username then
    raise exception 'username_locked' using errcode = '42501';
  end if;

  if exists (
    select 1 from public.profiles
    where lower(username) = normalized_username
      and id <> me
  ) then
    raise exception 'username_taken' using errcode = '23505';
  end if;

  insert into public.profiles(id, username, full_name, avatar_url, designation)
  values (me, normalized_username, btrim(p_full_name), btrim(p_avatar_url), nullif(btrim(p_designation),''))
  on conflict (id) do update
    set full_name = excluded.full_name,
        avatar_url = excluded.avatar_url,
        designation = excluded.designation,
        updated_at = now()
  returning * into result;

  return result;
end;
$function$;

revoke update on public.profiles from authenticated;
grant update (full_name, avatar_url, designation) on public.profiles to authenticated;

revoke update on public.messages from authenticated;
grant update (status) on public.messages to authenticated;

revoke update on public.typing_status from authenticated;
grant update (is_typing) on public.typing_status to authenticated;
