const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status,
  headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
});

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ACCOUNT_ID = "4114666a87ef501b3d043a8351e5c1f7";
const BUCKET = "knotlink-media";
const R2_HOST = `${ACCOUNT_ID}.r2.cloudflarestorage.com`;

function validKey(key) {
  return key.length > 0 && key.length <= 512 && !key.startsWith("/") && !key.includes("..") && !key.includes("\\");
}

function parseKey(key) {
  const p = key.split("/");
  if (p[0] === "users" && p.length >= 3) return { kind: "user", userId: p[1], scope: p[2] };
  if (p[0] === "chats" && p.length >= 4) return { kind: "chat", chatId: p[1], scope: p[2], ownerId: p[3] };
  return { kind: "invalid" };
}

function encodePath(path) {
  return path.split("/").map(encodeURIComponent).join("/");
}

function encodeQuery(value) {
  return encodeURIComponent(value).replace(/[!'()*]/g, c => "%" + c.charCodeAt(0).toString(16).toUpperCase());
}

function hex(bytes) {
  return [...new Uint8Array(bytes)].map(b => b.toString(16).padStart(2, "0")).join("");
}

async function sha256(value) {
  return crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
}

async function hmac(keyBytes, value) {
  const key = await crypto.subtle.importKey("raw", keyBytes, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return new Uint8Array(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(value)));
}

async function signingKey(secret, date) {
  const kDate = await hmac(new TextEncoder().encode("AWS4" + secret), date);
  const kRegion = await hmac(kDate, "auto");
  const kService = await hmac(kRegion, "s3");
  return hmac(kService, "aws4_request");
}

async function presign(method, key, accessKeyId, secretAccessKey, expiresIn) {
  const now = new Date();
  const iso = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
  const amzDate = iso.slice(0, 15) + "Z";
  const date = amzDate.slice(0, 8);
  const credentialScope = `${date}/auto/s3/aws4_request`;
  const canonicalUri = "/" + encodePath(BUCKET + "/" + key);
  const canonicalHeaders = `host:${R2_HOST}\\n`;
  const signedHeaders = "host";
  const query = [
    ["X-Amz-Algorithm", "AWS4-HMAC-SHA256"],
    ["X-Amz-Credential", `${accessKeyId}/${credentialScope}`],
    ["X-Amz-Date", amzDate],
    ["X-Amz-Expires", String(expiresIn)],
    ["X-Amz-SignedHeaders", signedHeaders],
  ].sort((a, b) => a[0].localeCompare(b[0]));
  const canonicalQuery = query.map(([k, v]) => `${encodeQuery(k)}=${encodeQuery(v)}`).join("&");
  const canonicalRequest = [
    method,
    canonicalUri,
    canonicalQuery,
    canonicalHeaders,
    signedHeaders,
    "UNSIGNED-PAYLOAD",
  ].join("\n");
  const stringToSign = [
    "AWS4-HMAC-SHA256",
    amzDate,
    credentialScope,
    hex(await sha256(canonicalRequest)),
  ].join("\n");
  const signature = hex(await hmac(await signingKey(secretAccessKey, date), stringToSign));
  return `https://${R2_HOST}${canonicalUri}?${canonicalQuery}&X-Amz-Signature=${signature}`;
}

async function getSupabaseUser(request, env) {
  const auth = request.headers.get("authorization") || "";
  if (!auth.startsWith("Bearer ")) return null;
  const response = await fetch(env.SUPABASE_URL + "/auth/v1/user", {
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      authorization: auth,
    },
  });
  if (!response.ok) return null;
  const user = await response.json();
  return UUID_RE.test(String(user?.id || "")) ? user : null;
}

async function isChatMember(chatId, userId, authorization, env) {
  const url = new URL(env.SUPABASE_URL + "/rest/v1/chat_participants");
  url.searchParams.set("select", "chat_id");
  url.searchParams.set("chat_id", `eq.${chatId}`);
  url.searchParams.set("user_id", `eq.${userId}`);
  const response = await fetch(url, {
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      authorization,
    },
  });
  if (!response.ok) return false;
  const rows = await response.json();
  return Array.isArray(rows) && rows.length > 0;
}

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "POST required" }, 405);
    const user = await getSupabaseUser(request, env);
    if (!user) return json({ error: "Unauthenticated" }, 401);

    let body;
    try { body = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }

    const action = String(body?.action || "");
    const key = String(body?.objectKey || "").trim();
    const expiresIn = Math.max(60, Math.min(900, Number(body?.expiresIn) || 900));
    if (action !== "upload" && action !== "download") return json({ error: "Invalid action" }, 400);
    if (!validKey(key)) return json({ error: "Invalid object key" }, 400);

    const parsed = parseKey(key);
    if (parsed.kind === "invalid") return json({ error: "Invalid object key scope" }, 400);

    const authorization = request.headers.get("authorization");
    if (parsed.kind === "user") {
      if (parsed.scope !== "avatar" || parsed.userId !== user.id) return json({ error: "User media access denied" }, 403);
    } else {
      if (parsed.scope !== "media" || !UUID_RE.test(parsed.chatId)) return json({ error: "Chat media access denied" }, 403);
      if (!await isChatMember(parsed.chatId, user.id, authorization, env)) return json({ error: "Chat access denied" }, 403);
      if (action === "upload" && parsed.ownerId !== user.id) return json({ error: "Chat media upload owner mismatch" }, 403);
    }

    if (!env.R2_ACCESS_KEY_ID || !env.R2_SECRET_ACCESS_KEY) return json({ error: "R2 credentials are not configured" }, 503);
    const url = await presign(action === "upload" ? "PUT" : "GET", key, env.R2_ACCESS_KEY_ID, env.R2_SECRET_ACCESS_KEY, expiresIn);
    return json({ url, expiresIn, objectKey: key, method: action === "upload" ? "PUT" : "GET" });
  },
};
