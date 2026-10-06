-- Keep message status mutations recoverable after Realtime disconnects.
-- BulkSMSBD is not used.

alter table public.messages
  add column if not exists updated_at timestamptz not null default now();

create index if not exists messages_chat_updated_idx
  on public.messages(chat_id, updated_at desc, id desc);

create index if not exists messages_recipient_updated_idx
  on public.messages(recipient_id, updated_at desc, id desc);

drop trigger if exists messages_touch_updated_at on public.messages;
create trigger messages_touch_updated_at
before update on public.messages
for each row execute function private.touch_updated_at();

update public.messages set updated_at = created_at where updated_at < created_at;
