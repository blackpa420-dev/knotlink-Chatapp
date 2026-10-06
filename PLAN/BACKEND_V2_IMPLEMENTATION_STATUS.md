# Backend V2 implementation status

Branch: `backend-v2-foundation`

## Completed

- New Supabase project is the application backend.
- UUID is the canonical identity for profiles, chats, participants and messages.
- Direct chats use one unordered UUID pair.
- Message creation uses the server-side RPC with a per-sender client message ID for idempotency.
- Profile completion is server-controlled and username is immutable after first completion.
- Client update privileges are restricted to allowed columns.
- R2 media uploads use the authenticated `r2-media-url` Edge Function and short-lived presigned PUT URLs.
- Firebase HTTP v1 sending uses the authenticated `fcm-send` Edge Function; Firebase service-account private keys are not shipped in Android.
- Android Supabase configuration now points to the new project.

## Required manual secret configuration

In Supabase Dashboard -> Edge Functions -> Secrets, add:

### r2-media-url
- `R2_ACCOUNT_ID`
- `R2_BUCKET` = `knotlink-media`
- `R2_ACCESS_KEY_ID` = Access Key ID from the new `knotlink-token2`
- `R2_SECRET_ACCESS_KEY` = Secret Access Key from `knotlink-token2`

### fcm-send
- `FIREBASE_PROJECT_ID` = `knotlink-1`
- `FIREBASE_CLIENT_EMAIL` = Firebase service-account client email
- `FIREBASE_PRIVATE_KEY` = the service-account private key, preserving newline escapes as required by the Supabase secret editor

Never commit any of these values to GitHub or put them in the APK.

## Credential rotation required

The previous Android source contained long-lived R2 and Firebase service-account credentials. They must be revoked/rotated because source history should be treated as compromised.

- Revoke/delete the old R2 token after `knotlink-token2` is confirmed working.
- In Google Cloud/Firebase, disable/delete the old exposed service-account key and create a replacement key if required.
- Store only the replacement Firebase key in Supabase Edge Function Secrets.

## Media note

The current upload path returns the bucket's existing managed r2.dev URL after upload. This keeps the current Android media model compatible. The Edge Function also supports signed GET URLs for the later private-media hardening step. Cloudflare recommends presigned URLs for direct mobile uploads/downloads without exposing R2 credentials.
