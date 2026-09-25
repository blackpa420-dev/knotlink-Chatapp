# Legacy Issues & Architectural Discrepancies

| File / Component | Function / Area | Current Behavior | Why It Is Wrong | Target Behavior | Remediation Phase |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `AuthRepository.kt` | OTP & Auth | Direct Firebase Auth phone sign-in / anonymous sign-in | Bypasses BulkSMSBD and creates permanent auth records prematurely | Secure Cloud Function proxy to BulkSMSBD + explicit temp session | Phase 1 |
| `WelcomeScreen.kt` | UI Nav | Navigates directly to incomplete registration states | Lacks strict temporary session validation | Enforce strict separation between OTP verification and account creation | Phase 2 |
| `MessageDao.kt` | Local Cache | Room acting as primary data source | Room must be strictly an offline cache, not authoritative | Server-first with offline Room read-through cache | Phase 3 |
| `ChatId` generation | Chat Creation | Random or unverified client chat ID generation | Can lead to split or duplicate chat threads between users | Deterministic sorting of participant UIDs (`sort(uidA, uidB).join("_")`) | Phase 4 |
