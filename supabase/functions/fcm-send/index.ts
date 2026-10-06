import { withSupabase } from "npm:@supabase/server@1";

export default withSupabase({ auth: "user" }, async () =>
  Response.json({ error: "Legacy fcm-send endpoint retired. Use fcm-send-v2." }, { status: 410 })
);
