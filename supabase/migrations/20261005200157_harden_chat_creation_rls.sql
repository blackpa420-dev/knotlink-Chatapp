drop policy if exists "Authenticated users can create chats" on public.chats;
drop policy if exists "Participants can read chats" on public.chats;

create policy "Authenticated users can create chats" on public.chats
  for insert to authenticated
  with check (
    (select auth.uid()) is not null
    and btrim(id) <> ''
    and type in ('DIRECT','GROUP')
  );

create policy "Participants can read chats" on public.chats
  for select to authenticated
  using (private.is_current_user_chat_participant(id));
