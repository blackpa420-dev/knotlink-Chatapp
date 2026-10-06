-- Backend v2 hardening: immutable usernames, canonical UUID messaging,
-- server-authorized message state mutations, and call signaling fields.
-- No OTP/SMS provider is involved here. BulkSMSBD is not used.

alter table public.call_sessions drop constraint if exists call_sessions_status_check;
alter table public.call_sessions
  add constraint call_sessions_status_check
  check (status in ('ringing','accepted','connected','declined','missed','ended','cancelled','failed'));

alter table public.messages
  add column if not exists is_pinned boolean not null default false,
  add column if not exists pinned_by uuid references auth.users(id) on delete set null,
  add column if not exists pinned_at timestamptz;

alter table public.call_sessions
  add column if not exists caller_name text,
  add column if not exists caller_avatar text,
  add column if not exists sdp_offer text,
  add column if not exists sdp_answer text,
  add column if not exists ice_candidates jsonb not null default '[]'::jsonb;

alter table public.profiles drop constraint if exists profiles_username_format;
alter table public.profiles
  add constraint profiles_username_format
  check (username ~ '^@[A-Za-z0-9_][A-Za-z0-9_.-]*\\.link$');

create index if not exists messages_chat_created_idx
  on public.messages(chat_id, created_at desc, id desc);
create index if not exists messages_recipient_created_idx
  on public.messages(recipient_id, created_at desc, id desc);
create index if not exists messages_client_message_idx
  on public.messages(sender_id, client_message_id);
create index if not exists messages_pinned_by_idx
  on public.messages(pinned_by);
create index if not exists call_sessions_callee_status_idx
  on public.call_sessions(callee_id, status, created_at desc);
create index if not exists call_sessions_caller_created_idx
  on public.call_sessions(caller_id, created_at desc);

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

create or replace function public.get_or_create_direct_chat(other_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  low_id uuid;
  high_id uuid;
  chat_id uuid;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if other_user_id is null or other_user_id = me then
    raise exception 'invalid_recipient' using errcode = '22023';
  end if;

  if not exists (select 1 from public.profiles where id = me)
     or not exists (select 1 from public.profiles where id = other_user_id) then
    raise exception 'profile_not_found' using errcode = 'P0002';
  end if;

  if me < other_user_id then low_id := me; high_id := other_user_id;
  else low_id := other_user_id; high_id := me;
  end if;

  insert into public.chats(chat_type, direct_user_low, direct_user_high)
  values ('direct', low_id, high_id)
  on conflict (direct_user_low, direct_user_high) where chat_type = 'direct'
  do update set updated_at = public.chats.updated_at
  returning id into chat_id;

  insert into public.chat_participants(chat_id, user_id, role)
  values (chat_id, low_id, 'member'), (chat_id, high_id, 'member')
  on conflict (chat_id, user_id) do nothing;

  return chat_id;
end;
$$;

create or replace function public.send_direct_message(
  p_recipient_id uuid,
  p_message_type text,
  p_body text default null,
  p_media_url text default null,
  p_client_message_id uuid default gen_random_uuid(),
  p_reply_to_message_id uuid default null
)
returns public.messages
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := auth.uid();
  chat_id uuid;
  result public.messages;
begin
  if me is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_recipient_id is null or p_recipient_id = me then
    raise exception 'invalid_recipient' using errcode = '22023';
  end if;

  chat_id := public.get_or_create_direct_chat(p_recipient_id);

  insert into public.messages(
    chat_id, sender_id, recipient_id, client_message_id,
    message_type, body, media_url, reply_to_message_id, status
  )
  values (
    chat_id, me, p_recipient_id, p_client_message_id,
    p_message_type, p_body, p_media_url, p_reply_to_message_id, 'sent'
  )
  on conflict (sender_id, client_message_id) do update
    set status = public.messages.status
  returning * into result;

  update public.chats set updated_at = now() where id = chat_id;
  return result;
end;
$$;

create or replace function public.mark_message_delivered(p_message_id uuid)
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

  update public.messages
     set status = case when status = 'sent' then 'delivered' else status end
   where id = p_message_id
     and recipient_id = me
     and status in ('sent','delivered')
  returning * into result;

  if result.id is null then
    select * into result from public.messages where id=p_message_id and recipient_id=me;
  end if;
  if result.id is null then raise exception 'message_not_found' using errcode = 'P0002'; end if;
  return result;
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
    where chat_id=p_chat_id and user_id=me
  ) then
    raise exception 'chat_access_denied' using errcode = '42501';
  end if;

  update public.messages
     set status='read'
   where chat_id=p_chat_id
     and recipient_id=me
     and status in ('sent','delivered');

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
  if p_body is null or length(btrim(p_body))=0 then
    raise exception 'invalid_body' using errcode = '22023';
  end if;

  update public.messages
     set body=p_body, edited_at=now()
   where id=p_message_id and sender_id=me and deleted_at is null
   returning * into result;

  if result.id is null then raise exception 'message_not_found' using errcode = 'P0002'; end if;
  return result;
end;
$$;

create or replace function public.set_message_pinned(p_message_id uuid, p_is_pinned boolean)
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

  update public.messages m
     set is_pinned=p_is_pinned,
         pinned_by=case when p_is_pinned then me else null end,
         pinned_at=case when p_is_pinned then now() else null end
   where m.id=p_message_id
     and exists (
       select 1 from public.chat_participants cp
       where cp.chat_id=m.chat_id and cp.user_id=me
     )
   returning m.* into result;

  if result.id is null then raise exception 'message_not_found' using errcode = 'P0002'; end if;
  return result;
end;
$$;

create or replace function public.delete_message_for_everyone(p_message_id uuid)
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

  update public.messages
     set body=null, media_url=null, deleted_at=now()
   where id=p_message_id and sender_id=me and deleted_at is null
   returning * into result;

  if result.id is null then raise exception 'message_not_found' using errcode = 'P0002'; end if;
  return result;
end;
$$;

revoke all on function public.complete_profile(text,text,text,text) from public, anon;
revoke all on function public.get_or_create_direct_chat(uuid) from public, anon;
revoke all on function public.send_direct_message(uuid,text,text,text,uuid,uuid) from public, anon;
revoke all on function public.mark_message_delivered(uuid) from public, anon;
revoke all on function public.mark_chat_messages_read(uuid) from public, anon;
revoke all on function public.edit_message(uuid,text) from public, anon;
revoke all on function public.set_message_pinned(uuid,boolean) from public, anon;
revoke all on function public.delete_message_for_everyone(uuid) from public, anon;

grant execute on function public.complete_profile(text,text,text,text) to authenticated;
grant execute on function public.get_or_create_direct_chat(uuid) to authenticated;
grant execute on function public.send_direct_message(uuid,text,text,text,uuid,uuid) to authenticated;
grant execute on function public.mark_message_delivered(uuid) to authenticated;
grant execute on function public.mark_chat_messages_read(uuid) to authenticated;
grant execute on function public.edit_message(uuid,text) to authenticated;
grant execute on function public.set_message_pinned(uuid,boolean) to authenticated;
grant execute on function public.delete_message_for_everyone(uuid) to authenticated;

drop policy if exists messages_update_participant on public.messages;
