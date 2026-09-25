# Error Contract & Standardized Error Codes

- `AUTH_001_INVALID_PHONE`: The provided phone number is not in valid E.164 format.
- `AUTH_002_INVALID_OTP`: The entered OTP code does not match the temporary record.
- `AUTH_003_OTP_EXPIRED`: The OTP code has expired.
- `AUTH_004_TOO_MANY_ATTEMPTS`: Exceeded maximum verification attempts; temporary lockout enforced.
- `AUTH_005_COOLDOWN`: OTP request cooldown period active.
- `AUTH_006_ACCOUNT_NOT_FOUND`: Phone number has no existing user account during login attempt.
- `AUTH_007_REGISTRATION_SESSION_EXPIRED`: Temporary registration session timed out before account completion.
- `USER_001_USERNAME_TAKEN`: Selected username is already claimed.
- `CHAT_001_NOT_AUTHORIZED`: User lacks permission to access or write to chat.
- `MSG_001_SEND_FAILED`: Message transmission failed; queued in local Room cache for retry.
