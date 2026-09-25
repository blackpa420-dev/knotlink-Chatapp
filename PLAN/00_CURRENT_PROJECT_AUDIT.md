# Current Project Audit

## Overview
This document outlines the inspection of the existing Android project state in `/app` prior to executing Phase 1 of the BitChat architecture rebuild.

## A. Current Authentication & Registration Flow
- **Current State**: Contains experimental UI screens (`WelcomeScreen.kt`, `PhoneAuthScreen.kt`, `OtpVerificationScreen.kt`) and placeholder repository layers (`AuthRepository`, `BulkSmsService`).
- **Conflict**: Previous implementations mixed OTP verification directly with account creation and client-side database calls.
- **Resolution**: All legacy auth flows are marked for complete replacement in Phase 1 and Phase 2 adhering to the new separation contract.

## B. Current Firebase & Data Flow
- **Firebase Initialization**: Configured via `google-services.json`.
- **Firestore / RTDB**: Present in dependencies but lacks proper atomic separation of phone identity indexing and user profiles.
- **Room Cache**: Basic structure exists but requires alignment with the new offline-first read/write cache pattern.

## C. Current Messaging & Chat Flow
- **Current State**: Mock implementations and UI stubs.
- **Resolution**: To be implemented cleanly in Phases 4 and 5 according to deterministic `chatId` generation and clientMessageId idempotency rules.
