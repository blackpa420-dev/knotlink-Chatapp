import { withSupabase } from "npm:@supabase/server@1";
import { S3Client, GetObjectCommand, PutObjectCommand } from "npm:@aws-sdk/client-s3";
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner";

const ACCOUNT_ID = Deno.env.get("R2_ACCOUNT_ID") ?? "";
const BUCKET = Deno.env.get("R2_BUCKET") ?? "";
const ACCESS_KEY_ID = Deno.env.get("R2_ACCESS_KEY_ID") ?? "";
const SECRET_ACCESS_KEY = Deno.env.get("R2_SECRET_ACCESS_KEY") ?? "";

function json(data: unknown, status = 200) {
  return Response.json(data, { status, headers: { "Cache-Control": "no-store" } });
}

function validKey(key: string) {
  return key.length > 0 && key.length <= 512 &&
    !key.startsWith("/") && !key.includes("..") && !key.includes("\\");
}

function parseKey(key: string) {
  const parts = key.split("/");
  if (parts[0] === "users" && parts.length >= 3) return { kind: "user", userId: parts[1] };
  if (parts[0] === "chats" && parts.length >= 3) return { kind: "chat", chatId: parts[1] };
  return { kind: "invalid" };
}

export default withSupabase({ auth: "user" }, async (req, ctx) => {
  if (req.method !== "POST") return json({ error: "POST required" }, 405);

  let body: { action?: string; objectKey?: string; contentType?: string; expiresIn?: number };
  try { body = await req.json(); } catch { return json({ error: "Invalid JSON" }, 400); }

  const action = body.action;
  const objectKey = body.objectKey?.trim() ?? "";
  const contentType = body.contentType?.trim() || "application/octet-stream";
  const expiresIn = Math.max(60, Math.min(3600, Number(body.expiresIn) || 900));
  const userId = String(ctx.userClaims?.sub ?? "");

  if (!userId) return json({ error: "Unauthenticated" }, 401);
  if (action !== "upload" && action !== "download") return json({ error: "action must be upload or download" }, 400);
  if (!validKey(objectKey)) return json({ error: "Invalid object key" }, 400);

  const parsed = parseKey(objectKey);
  if (parsed.kind === "invalid") {
    return json({ error: "Object key must start with users/<user_uuid>/ or chats/<chat_uuid>/" }, 400);
  }

  if (parsed.kind === "user" && action === "upload" && parsed.userId !== userId) {
    return json({ error: "User object upload denied" }, 403);
  }

  if (parsed.kind === "chat") {
    const { data: membership, error } = await ctx.supabase
      .from("chat_participants")
      .select("chat_id")
      .eq("chat_id", parsed.chatId)
      .eq("user_id", userId)
      .maybeSingle();

    if (error) return json({ error: "Membership check failed" }, 500);
    if (!membership) return json({ error: "Chat access denied" }, 403);
  }

  if (!ACCOUNT_ID || !BUCKET || !ACCESS_KEY_ID || !SECRET_ACCESS_KEY) {
    return json({ error: "R2 credentials are not configured on the server" }, 503);
  }

  const s3 = new S3Client({
    region: "auto",
    endpoint: `https://${ACCOUNT_ID}.r2.cloudflarestorage.com`,
    credentials: { accessKeyId: ACCESS_KEY_ID, secretAccessKey: SECRET_ACCESS_KEY },
  });

  const command = action === "upload"
    ? new PutObjectCommand({ Bucket: BUCKET, Key: objectKey, ContentType: contentType })
    : new GetObjectCommand({ Bucket: BUCKET, Key: objectKey });

  const url = await getSignedUrl(s3, command, { expiresIn });
  return json({ url, expiresIn, objectKey, method: action === "upload" ? "PUT" : "GET" });
});
