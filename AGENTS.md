# AGENTS.md - Critical Rules for AI Coding Agent

## MANDATORY DIRECTIVE: SUPABASE EGRESS & PERFORMANCE PROTECTION

The developer/owner of this app requires strict conservation of Supabase Bandwidth, Egress, and Database API limits. All future feature implementations, bug fixes, and refactoring MUST strictly comply with these rules:

### 1. Zero-Egress / Low-Egress Architecture (MANDATORY)
- **Local-First Caching (Room DB):** ALWAYS query local Room DB or memory state first. NEVER query Supabase REST APIs repeatedly for data that can be cached locally (profiles, chat lists, messages, settings).
- **WebSockets for Realtime Events:** Use Supabase Realtime WebSockets (`postgres_changes` / `broadcast`) for active status, live typing, calling, and new messages instead of REST polling.
- **Minimal Response Bodies:** Always use `Prefer: return=minimal` on HTTP POST/PATCH/DELETE requests to Supabase REST endpoints so Supabase returns HTTP 204 No Content (0 response body bytes).
- **Throttled Network Events:**
  - Typing events MUST be throttled to at least 2,000ms (2 seconds) interval.
  - Heartbeat / Presence updates MUST be throttled to 12,000ms - 15,000ms intervals.
  - Periodic polling loops MUST NEVER run faster than 12 seconds, and bulk queries (e.g. fetching all profiles) MUST NOT run more often than once per 60 seconds.

### 2. High-Egress Danger Warning Directive
If the user requests a feature that naturally consumes significant Supabase Egress (e.g., streaming uncompressed video/audio files directly through Supabase Storage, infinite continuous polling without WebSocket, bulk sync of entire message histories every second, uncompressed raw photo uploads):
- You MUST warn the user clearly before implementing.
- Explain the Egress impact and propose an egress-optimized alternative (e.g. image compression, thumbnail generation, pagination, WebSocket streams, or local Room DB caching).

### 3. Privacy & UI Safety
- These guidelines are agent-only instructions located in root configuration files (`AGENTS.md` / `GEMINI.md`).
- They MUST NOT affect or expose any UI or binary code in the user-facing Android application.


## 2.5 UI PERFORMANCE & CALL-SAFETY GUARDRAILS
- Prefer Room/memory data for UI rendering; do not add REST calls from Composables.
- LazyColumn/LazyRow items MUST use stable keys and contentType where practical.
- Do not perform full-map scans, sorting, date formatting, image decoding, or other O(n)/expensive work inside frequently recomposed list-item bodies when it can be precomputed.
- Do not add continuous/infinite animations to screens or rows unless the element is visibly active; never animate hidden/off-screen content unnecessarily.
- Interactive chat rows and navigation controls should provide lightweight Compose haptic feedback without blocking the main thread.
- NEVER modify WebRTC call state machines, ICE/signaling, FCM call handling, notification actions, or call navigation as a side effect of a UI/performance task.
- NEVER use an automatic "fix" to rewrite call logic merely to silence a compiler/build warning. For call-related errors, make the smallest compile-safe change and preserve existing runtime behavior.
- Do not replace working call behavior with speculative refactors.
