# KnotLink Backend V2 Contract

## Source of truth

- Supabase is the transactional source of truth for Auth, profiles, usernames, chats, participants, messages, realtime state, call sessions, and push-token registration.
- Firebase is used for FCM delivery, Crashlytics, Analytics, Performance Monitoring, App Check, and Remote Config.
- Cloudflare R2 remains the media/object store.
- Room is the Android local cache and UI read layer.

## Identity

- Supabase Auth UUID is the only canonical internal user identity.
- public.profiles.id equals auth.users.id and is UUID.
- Usernames are display/search identifiers only.
- Stored usernames are canonical single-@ form and end in .link.
- No message, participant, call, or permission relationship may use username, email, legacy public_id, hash IDs, or local IDs.
- QR payloads must resolve to the canonical UUID.

## Profile completion

- Auth verification alone does not create a public profile.
- A public profile is created only through complete_profile after avatar, full name, and username are all present.
- Username uniqueness is enforced by the database.
- Profile completion must not depend on local availability state.

## Direct chat

- There is exactly one direct chat per unordered pair of UUIDs.
- get_or_create_direct_chat() is the canonical creation path.
- direct_user_low/direct_user_high are sorted UUIDs.
- chat_participants contains both members.

## Messages

- send_direct_message() is the canonical direct-message write path.
- Every message has a server UUID and a client_message_id for idempotency.
- Messages are read only by chat members.
- Postgres Realtime is the live transport to Room.
- FCM is a notification transport, not a message database.
- Do not add a second normal-message transport or periodic polling as a primary path.

## Caching / egress

- Android reads chat history from Room first.
- Supabase is queried only for initial sync, missing ranges, pagination, or explicit refresh.
- Realtime inserts/updates are written into Room and rendered from Room.
- Profile data is cached and refreshed only when stale or explicitly requested.
- Never fetch all profiles just to resolve one user.

## Security

- RLS is enabled on all exposed public tables.
- Service-role/secret credentials never enter the APK.
- Security-definer RPCs must validate auth.uid(), use an empty search_path, and have explicit EXECUTE grants.
- Client code must use only the Supabase publishable/anon key.
