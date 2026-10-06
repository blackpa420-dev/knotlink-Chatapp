

alter table public.messages
  add column if not exists is_pinned boolean not null default false,
  add column if not exists pinned_by uuid references auth.users(id) on delete set null,
  add column if not exists pinned_at timestamptz;
-- Backend v2 hardening: immutable usernames and server-authorized message state mutations.
-- No OTP/SMS provider is involved here.

create or replace function public.complete_profile(
  p_username text,
  p_full_name text,
  p_avatar_url text,
  p_designation text default null
)
returns public.profiles
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  normalized_username text;
  existing_username text;
  result public.profiles;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  normalized_username := lower(btrim(p_username));
  normalized_username := regexp_replace(normalized_username, '^@+', '@');

  if normalized_username !~ '^@[a-z0-9_][a-z0-9_.-]*$' then
    raise exception 'invalid_username' using errcode = '22023';
  end if;

  if normalized_username !~ '\\.link$' then
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
    raise exception 'username_immutable' using errcode = '22023';
  end if;

  if exists (
    select 1 from public.profiles
    where lower(username) = normalized_username and id <> me
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
$$;

revoke all on function public.complete_profile(text,text,text,text) from public, anon;
grant execute on function public.complete_profile(text,text,text,text) to authenticated;

create or replace function public.mark_message_delivered(p_message_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  changed integer;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  update public.messages
     set status = 'delivered'
   where id = p_message_id
     and recipient_id = me
     and status = 'sent';

  get diagnostics changed = row_count;
  return changed > 0;
end;
$$;

create or replace function public.mark_chat_messages_read(p_chat_id uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  changed integer;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  if not exists (
    select 1 from public.chat_participants
    where chat_id = p_chat_id and user_id = me
  ) then
    raise exception 'chat_access_denied' using errcode = '42501';
  end if;

  update public.messages
     set status = 'read'
   where chat_id = p_chat_id
     and recipient_id = me
     and status <> 'read';

  get diagnostics changed = row_count;
  return changed;
end;
$$;

create or replace function public.edit_message(p_message_id uuid, p_body text)
returns public.messages
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  result public.messages;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  if p_body is null or length(btrim(p_body)) = 0 then
    raise exception 'message_body_required' using errcode = '22023';
  end if;

  update public.messages
     set body = p_body,
         edited_at = now()
   where id = p_message_id
     and sender_id = me
     and deleted_at is null
  returning * into result;

  if result.id is null then raise exception 'message_not_found_or_forbidden' using errcode = '42501'; end if;
  return result;
end;
$$;

create or replace function public.set_message_pinned(p_message_id uuid, p_pinned boolean)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  changed integer;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  update public.messages
     set is_pinned = coalesce(p_pinned, false),
         pinned_by = case when p_pinned then me else null end,
         pinned_at = case when p_pinned then now() else null end
   where id = p_message_id
     and exists (
       select 1 from public.chat_participants cp
       where cp.chat_id = messages.chat_id and cp.user_id = me
     );

  get diagnostics changed = row_count;
  return changed > 0;
end;
$$;

create or replace function public.delete_message_for_everyone(p_message_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  changed integer;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;

  update public.messages
     set body = '',
         media_url = null,
         deleted_at = now()
   where id = p_message_id
     and sender_id = me
     and deleted_at is null;

  get diagnostics changed = row_count;
  return changed > 0;
end;
$$;

revoke all on function public.mark_message_delivered(uuid) from public, anon;
revoke all on function public.mark_chat_messages_read(uuid) from public, anon;
revoke all on function public.edit_message(uuid,text) from public, anon;
revoke all on function public.set_message_pinned(uuid,boolean) from public, anon;
revoke all on function public.delete_message_for_everyone(uuid) from public, anon;

grant execute on function public.mark_message_delivered(uuid) to authenticated;
grant execute on function public.mark_chat_messages_read(uuid) to authenticated;
grant execute on function public.edit_message(uuid,text) to authenticated;
grant execute on function public.set_message_pinned(uuid,boolean) to authenticated;
grant execute on function public.delete_message_for_everyone(uuid) to authenticated;

-- Direct client PATCH of message rows is no longer the authority for delivery/read/edit/delete.
drop policy if exists messages_update_participant on public.messages;
