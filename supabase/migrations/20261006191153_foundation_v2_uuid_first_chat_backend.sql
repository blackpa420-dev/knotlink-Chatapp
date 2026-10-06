create extension if not exists pgcrypto;

create schema if not exists private;

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text not null,
  full_name text not null,
  avatar_url text not null,
  designation text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profiles_username_format check (username ~ '^@[A-Za-z0-9_][A-Za-z0-9_.-]*\\.link$'),
  constraint profiles_full_name_nonempty check (length(btrim(full_name)) between 1 and 120),
  constraint profiles_avatar_nonempty check (length(btrim(avatar_url)) > 0)
);

create unique index profiles_username_unique on public.profiles (lower(username));

create table public.chats (
  id uuid primary key default gen_random_uuid(),
  chat_type text not null default 'direct' check (chat_type in ('direct','group')),
  direct_user_low uuid references auth.users(id) on delete cascade,
  direct_user_high uuid references auth.users(id) on delete cascade,
  title text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint chats_direct_pair_check check (
    (chat_type = 'direct' and direct_user_low is not null and direct_user_high is not null and direct_user_low < direct_user_high)
    or
    (chat_type = 'group' and direct_user_low is null and direct_user_high is null)
  )
);

create unique index chats_direct_pair_unique
  on public.chats (direct_user_low, direct_user_high)
  where chat_type = 'direct';

create index chats_direct_user_high_idx on public.chats(direct_user_high);

create table public.chat_participants (
  chat_id uuid not null references public.chats(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  joined_at timestamptz not null default now(),
  role text not null default 'member' check (role in ('member','admin','owner')),
  primary key (chat_id, user_id)
);

create index chat_participants_user_id_idx on public.chat_participants(user_id);

create table public.messages (
  id uuid primary key default gen_random_uuid(),
  chat_id uuid not null references public.chats(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  recipient_id uuid references auth.users(id) on delete set null,
  client_message_id uuid not null default gen_random_uuid(),
  message_type text not null default 'text'
    check (message_type in ('text','image','video','audio','file','voice','location','contact','poll','system')),
  body text,
  media_url text,
  reply_to_message_id uuid references public.messages(id) on delete set null,
  status text not null default 'sent'
    check (status in ('sent','delivered','read','failed')),
  created_at timestamptz not null default now(),
  edited_at timestamptz,
  deleted_at timestamptz,
  constraint messages_content_check check (
    body is not null or media_url is not null or message_type = 'system'
  )
);

create unique index messages_sender_client_id_unique
  on public.messages(sender_id, client_message_id);

create index messages_chat_created_idx
  on public.messages(chat_id, created_at desc, id desc);

create index messages_recipient_created_idx
  on public.messages(recipient_id, created_at desc);

create index messages_reply_to_message_id_idx on public.messages(reply_to_message_id);

create table public.typing_status (
  chat_id uuid not null references public.chats(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  is_typing boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (chat_id, user_id)
);

create index typing_status_user_id_idx on public.typing_status(user_id);

create table public.presence (
  user_id uuid primary key references auth.users(id) on delete cascade,
  is_online boolean not null default false,
  last_seen_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.call_sessions (
  id uuid primary key default gen_random_uuid(),
  caller_id uuid not null references auth.users(id) on delete cascade,
  callee_id uuid not null references auth.users(id) on delete cascade,
  chat_id uuid references public.chats(id) on delete set null,
  call_type text not null check (call_type in ('audio','video')),
  status text not null default 'ringing'
    check (status in ('ringing','accepted','declined','missed','ended','failed')),
  created_at timestamptz not null default now(),
  answered_at timestamptz,
  ended_at timestamptz,
  constraint call_sessions_distinct_users check (caller_id <> callee_id)
);

create index call_sessions_chat_id_idx on public.call_sessions(chat_id);
create index call_sessions_callee_status_idx on public.call_sessions(callee_id, status, created_at desc);
create index call_sessions_caller_created_idx on public.call_sessions(caller_id, created_at desc);

create table public.push_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null,
  platform text not null default 'android' check (platform in ('android')),
  device_id text,
  app_version text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, fcm_token)
);

create index push_tokens_user_id_idx on public.push_tokens(user_id);

create or replace function private.touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_touch_updated_at
before update on public.profiles for each row execute function private.touch_updated_at();

create trigger chats_touch_updated_at
before update on public.chats for each row execute function private.touch_updated_at();

create trigger typing_touch_updated_at
before update on public.typing_status for each row execute function private.touch_updated_at();

create trigger presence_touch_updated_at
before update on public.presence for each row execute function private.touch_updated_at();

create trigger push_tokens_touch_updated_at
before update on public.push_tokens for each row execute function private.touch_updated_at();

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
  if other_user_id is null or other_user_id = me then raise exception 'invalid_recipient' using errcode = '22023'; end if;

  if not exists (select 1 from public.profiles where id = me)
     or not exists (select 1 from public.profiles where id = other_user_id) then
    raise exception 'profile_not_found' using errcode = 'P0002';
  end if;

  if me < other_user_id then low_id := me; high_id := other_user_id;
  else low_id := other_user_id; high_id := me; end if;

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

revoke all on function public.get_or_create_direct_chat(uuid) from public, anon;
grant execute on function public.get_or_create_direct_chat(uuid) to authenticated;

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

  if length(btrim(p_avatar_url)) = 0 then raise exception 'avatar_required' using errcode = '22023'; end if;

  if exists (
    select 1 from public.profiles
    where lower(username) = normalized_username and id <> me
  ) then
    raise exception 'username_taken' using errcode = '23505';
  end if;

  insert into public.profiles(id, username, full_name, avatar_url, designation)
  values (me, normalized_username, btrim(p_full_name), btrim(p_avatar_url), nullif(btrim(p_designation),''))
  on conflict (id) do update
    set username = excluded.username,
        full_name = excluded.full_name,
        avatar_url = excluded.avatar_url,
        designation = excluded.designation,
        updated_at = now()
  returning * into result;

  return result;
end;
$$;

revoke all on function public.complete_profile(text,text,text,text) from public, anon;
grant execute on function public.complete_profile(text,text,text,text) to authenticated;

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
  if p_recipient_id is null or p_recipient_id = me then raise exception 'invalid_recipient' using errcode = '22023'; end if;

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

revoke all on function public.send_direct_message(uuid,text,text,text,uuid,uuid) from public, anon;
grant execute on function public.send_direct_message(uuid,text,text,text,uuid,uuid) to authenticated;

alter table public.profiles enable row level security;
alter table public.chats enable row level security;
alter table public.chat_participants enable row level security;
alter table public.messages enable row level security;
alter table public.typing_status enable row level security;
alter table public.presence enable row level security;
alter table public.call_sessions enable row level security;
alter table public.push_tokens enable row level security;

create policy profiles_select_authenticated on public.profiles
for select to authenticated using (true);

create policy profiles_update_self on public.profiles
for update to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

create policy chats_select_member on public.chats
for select to authenticated
using (exists (
  select 1 from public.chat_participants cp
  where cp.chat_id = chats.id and cp.user_id = (select auth.uid())
));

create policy chat_participants_select_member on public.chat_participants
for select to authenticated
using (exists (
  select 1 from public.chat_participants me
  where me.chat_id = chat_participants.chat_id and me.user_id = (select auth.uid())
));

create policy messages_select_member on public.messages
for select to authenticated
using (exists (
  select 1 from public.chat_participants cp
  where cp.chat_id = messages.chat_id and cp.user_id = (select auth.uid())
));

create policy messages_update_participant on public.messages
for update to authenticated
using (sender_id = (select auth.uid()) or recipient_id = (select auth.uid()))
with check (sender_id = (select auth.uid()) or recipient_id = (select auth.uid()));

create policy typing_select_member on public.typing_status
for select to authenticated
using (exists (
  select 1 from public.chat_participants cp
  where cp.chat_id = typing_status.chat_id and cp.user_id = (select auth.uid())
));

create policy typing_insert_self on public.typing_status
for insert to authenticated
with check (
  user_id = (select auth.uid()) and exists (
    select 1 from public.chat_participants cp
    where cp.chat_id = typing_status.chat_id and cp.user_id = (select auth.uid())
  )
);

create policy typing_update_self on public.typing_status
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

create policy presence_select_authenticated on public.presence
for select to authenticated using (true);

create policy presence_insert_self on public.presence
for insert to authenticated
with check (user_id = (select auth.uid()));

create policy presence_update_self on public.presence
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

create policy call_sessions_select_participant on public.call_sessions
for select to authenticated
using (caller_id = (select auth.uid()) or callee_id = (select auth.uid()));

create policy call_sessions_update_participant on public.call_sessions
for update to authenticated
using (caller_id = (select auth.uid()) or callee_id = (select auth.uid()))
with check (caller_id = (select auth.uid()) or callee_id = (select auth.uid()));

create policy push_tokens_select_self on public.push_tokens
for select to authenticated using (user_id = (select auth.uid()));

create policy push_tokens_insert_self on public.push_tokens
for insert to authenticated
with check (user_id = (select auth.uid()));

create policy push_tokens_update_self on public.push_tokens
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

create policy push_tokens_delete_self on public.push_tokens
for delete to authenticated
using (user_id = (select auth.uid()));

alter publication supabase_realtime add table public.messages;
alter publication supabase_realtime add table public.typing_status;
alter publication supabase_realtime add table public.presence;
alter publication supabase_realtime add table public.call_sessions;
