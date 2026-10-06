import { withSupabase } from "npm:@supabase/server@1";
import { createClient } from "npm:@supabase/supabase-js@2";
import { GoogleAuth } from "npm:google-auth-library@9";
const UUID_RE=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const json=(data:unknown,status=200)=>Response.json(data,{status,headers:{"Cache-Control":"no-store"}});
export default withSupabase({auth:"user"},async(req,ctx)=>{
 if(req.method!=="POST")return json({error:"POST required"},405);
 let body:Record<string,string|undefined>;try{body=await req.json()}catch{return json({error:"Invalid JSON"},400)}
 const callerId=String(ctx.userClaims?.sub??"");if(!UUID_RE.test(callerId))return json({error:"Unauthenticated"},401);
 const targetUserId=body.targetUserId?.trim()??"";if(!UUID_RE.test(targetUserId))return json({error:"targetUserId must be a canonical UUID"},400);if(targetUserId===callerId)return json({sent:0,reason:"self_target"});
 const type=(body.type??body.knotlinkMessageType??"message").trim().slice(0,32),chatId=(body.chatId??"").trim(),senderId=(body.senderId??callerId).trim();
 if(senderId!==callerId)return json({error:"senderId must match authenticated user"},403);if(!UUID_RE.test(chatId))return json({error:"chatId must be a canonical UUID"},400);
 const{data:membership,error:membershipError}=await ctx.supabase.from("chat_participants").select("chat_id").eq("chat_id",chatId).eq("user_id",callerId).maybeSingle();if(membershipError)return json({error:"Chat authorization check failed"},500);if(!membership)return json({error:"Caller is not a member of chat"},403);
 const{data:recipientMembership,error:recipientMembershipError}=await ctx.supabase.from("chat_participants").select("chat_id").eq("chat_id",chatId).eq("user_id",targetUserId).maybeSingle();if(recipientMembershipError)return json({error:"Recipient authorization check failed"},500);if(!recipientMembership)return json({error:"Recipient is not a member of chat"},403);
 const serviceKey=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")??"",supabaseUrl=Deno.env.get("SUPABASE_URL")??"";if(!serviceKey||!supabaseUrl)return json({error:"Supabase server credentials are not configured"},503);
 const admin=createClient(supabaseUrl,serviceKey,{auth:{autoRefreshToken:false,persistSession:false}});const{data:tokens,error:tokenError}=await admin.from("push_tokens").select("fcm_token").eq("user_id",targetUserId);if(tokenError)return json({error:"Failed to resolve recipient push tokens"},500);if(!tokens?.length)return json({sent:0,reason:"no_registered_devices"});
 const projectId=Deno.env.get("FIREBASE_PROJECT_ID")??"",clientEmail=Deno.env.get("FIREBASE_CLIENT_EMAIL")??"",privateKey=Deno.env.get("FIREBASE_PRIVATE_KEY")??"";if(!projectId||!clientEmail||!privateKey)return json({error:"Firebase server credentials are not configured"},503);
 const auth=new GoogleAuth({credentials:{client_email:clientEmail,private_key:privateKey.replaceAll("\\n",String.fromCharCode(10))},scopes:["https://www.googleapis.com/auth/firebase.messaging"]}),client=await auth.getClient(),tokenResponse=await client.getAccessToken(),oauthToken=typeof tokenResponse==="string"?tokenResponse:tokenResponse?.token;if(!oauthToken)return json({error:"Unable to obtain Firebase access token"},503);
 const title=(body.title??"KnotLink").trim().slice(0,120),messageBody=(body.messageBody??body.body??"").trim().slice(0,240),senderName=(body.senderName??body.callerName??"").trim().slice(0,120),senderAvatar=(body.senderAvatar??"").trim().slice(0,1000),callType=(body.callType??"").trim().slice(0,32),serverMessageId=(body.serverMessageId??"").trim(),messageType=(body.messageType??"").trim().slice(0,32);
 const dataPayload={knotlink_message_type:type,title,body:messageBody,chat_id:chatId,sender_id:callerId,sender_name:senderName,sender_avatar:senderAvatar,call_type:callType,message_type:messageType,server_message_id:serverMessageId};
 let sent=0,failed=0;const isCall=["call","incoming_call","call_ended","call_declined","call_cancelled"].includes(type);
 for(const row of tokens){const fcmToken=String(row.fcm_token??"").trim();if(!fcmToken)continue;const response=await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,{method:"POST",headers:{"Authorization":`Bearer ${oauthToken}`,"Content-Type":"application/json"},body:JSON.stringify({message:{token:fcmToken,data:dataPayload,android:{priority:"HIGH",ttl:isCall?"60s":"3600s"}}})});if(response.ok)sent++;else{failed++;console.warn("FCM provider rejected token",response.status,(await response.text()).slice(0,500))}}
 return json({sent,failed,targetUserId});
});