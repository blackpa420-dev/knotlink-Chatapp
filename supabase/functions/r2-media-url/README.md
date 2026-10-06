# KnotLink R2 media URL function

Authenticated Edge Function that issues short-lived Cloudflare R2 presigned URLs.

Production secrets must be stored in Supabase Edge Function Secrets, never in Git.

Required secret names:
- R2_ACCOUNT_ID
- R2_BUCKET
- R2_ACCESS_KEY_ID
- R2_SECRET_ACCESS_KEY

Bucket: knotlink-media

Object key policy:
- users/<user_uuid>/...
- chats/<chat_uuid>/...

The function verifies the caller JWT and checks chat membership before issuing a chat-media URL.
