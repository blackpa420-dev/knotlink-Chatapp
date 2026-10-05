-- Require a complete profile before a row can exist in public.profiles.
-- Registration writes the profile only after email OTP verification and
-- successful completion of the final profile-setup step.

alter table public.profiles
  drop constraint if exists profiles_full_name_required,
  drop constraint if exists profiles_avatar_required;

alter table public.profiles
  add constraint profiles_full_name_required
  check (btrim(full_name) <> ''),
  add constraint profiles_avatar_required
  check (
    avatar_url is not null
    and btrim(avatar_url) <> ''
  );
