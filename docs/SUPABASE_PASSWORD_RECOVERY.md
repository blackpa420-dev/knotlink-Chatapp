# Supabase password recovery

KnotLink's recovery flow sends a code with Supabase Auth's `POST /recover`
endpoint, verifies it with the `recovery` OTP type, and only then uses the
short-lived authenticated recovery session to update the password.

## Required Supabase dashboard setting

The **Recovery** email template must include `{{ .Token }}` so the user
receives the six-digit code shown by the app. For example:

```html
<p>Your KnotLink password-reset code is: {{ .Token }}</p>
```

Supabase's default recovery template uses `{{ .ConfirmationURL }}`, which
sends a clickable recovery link instead. That default link is valid, but it
cannot be entered into KnotLink's code UI. If the template remains link-based,
the reset request succeeds but the user will not receive a code to enter.

No service-role key is used by the Android app. Unknown emails receive the same
success response as known emails, and neither passwords nor OTPs are logged.
