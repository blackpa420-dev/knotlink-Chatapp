-- Tighten direct client writes. Message mutations and profile completion are RPC-owned.
-- Typing updates remain direct but only for chats where the caller is a participant.

drop policy if exists messages_update_sender_or_recipient on public.messages;
drop policy if exists profiles_update_self on public.profiles;

drop policy if exists typing_update_self on public.typing_status;
create policy typing_update_member
on public.typing_status
for update
to authenticated
using (
  user_id = (select auth.uid())
  and exists (
    select 1 from public.chat_participants cp
    where cp.chat_id = typing_status.chat_id
      and cp.user_id = (select auth.uid())
  )
)
with check (
  user_id = (select auth.uid())
  and exists (
    select 1 from public.chat_participants cp
    where cp.chat_id = typing_status.chat_id
      and cp.user_id = (select auth.uid())
  )
);
