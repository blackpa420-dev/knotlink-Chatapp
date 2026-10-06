import { createClient } from "npm:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") ?? "";
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "POST required" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  const accessToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  if (!accessToken) return json({ error: "Missing bearer token" }, 401);

  if (!SUPABASE_URL || !SUPABASE_ANON_KEY || !SERVICE_ROLE_KEY) {
    return json({ error: "Supabase server configuration missing" }, 503);
  }

  const authClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: { user }, error: authError } = await authClient.auth.getUser(accessToken);
  if (authError || !user) return json({ error: "Invalid session" }, 401);

  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return json({ error: "Invalid JSON" }, 400); }

  const targetUserId = String(body.targetUserId ?? "").trim();
  const type = String(body.type ?? "message").trim().slice(0, 32);
  const title = String(body.title ?? "KnotLink").trim().slice(0, 120);
  const messageBody = String(body.messageBody ?? "").trim().slice(0, 240);
  const chatId = String(body.chatId ?? "").trim().slice(0, 128);
  const senderName = String(body.callerName ?? "").trim().slice(0, 120);
  const callType = String(body.callType ?? "").trim().slice(0, 32);
  const senderAvatar = String(body.senderAvatar ?? "").trim().slice(0, 1000);
  const senderId = String(body.senderId ?? user.id).trim();
  const serverMessageId = String(body.serverMessageId ?? "").trim();
  const messageType = String(body.messageType ?? "").trim().slice(0, 32);

  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(targetUserId)) {
    return json({ error: "targetUserId must be a canonical UUID" }, 400);
  }
  if (targetUserId === user.id) return json({ sent: 0, reason: "self_target" });

  const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data: tokens, error: tokenError } = await admin
    .from("push_tokens")
    .select("fcm_token")
    .eq("user_id", targetUserId);

  if (tokenError) return json({ error: "Failed to resolve recipient push tokens" }, 500);
  if (!tokens?.length) return json({ sent: 0, reason: "no_registered_devices" });

  const projectId = Deno.env.get("FIREBASE_PROJECT_ID") ?? "";
  const clientEmail = Deno.env.get("FIREBASE_CLIENT_EMAIL") ?? "";
  const privateKey = (Deno.env.get("FIREBASE_PRIVATE_KEY") ?? "").replace(/\\n/g, "\n");
  if (!projectId || !clientEmail || !privateKey) {
    return json({ error: "Firebase server credentials are not configured" }, 503);
  }

  const auth = new GoogleAuth({
    credentials: { client_email: clientEmail, private_key: privateKey },
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const client = await auth.getClient();
  const tokenResponse = await client.getAccessToken();
  const oauthToken = typeof tokenResponse === "string" ? tokenResponse : tokenResponse?.token;
  if (!oauthToken) return json({ error: "Unable to obtain Firebase access token" }, 503);

  const dataPayload: Record<string, string> = {
    knotlink_message_type: type,
    title,
    body: messageBody,
    chat_id: chatId,
    sender_id: senderId,
    sender_name: senderName,
    sender_avatar: senderAvatar,
    call_type: callType,
    message_type: messageType,
    server_message_id: serverMessageId,
  };

  let sent = 0;
  let failed = 0;

  for (const row of tokens) {
    const fcmToken = String(row.fcm_token ?? "").trim();
    if (!fcmToken) continue;

    const fcmResponse = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${oauthToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: fcmToken,
            data: dataPayload,
            android: {
              priority: "HIGH",
              ttl: "3600s",
            },
          },
        }),
      },
    );

    if (fcmResponse.ok) {
      sent++;
    } else {
      failed++;
      const errorText = await fcmResponse.text();
      console.warn("FCM delivery failed", fcmResponse.status, errorText.slice(0, 500));
    }
  }

  return json({ sent, failed, targetUserId });
});
