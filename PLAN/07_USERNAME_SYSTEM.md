# Username System

## 1. Rules & Specifications
- **Format**: 3 to 20 characters, lowercase alphanumeric and underscores (`^[a-z0-9_]{3,20}$`).
- **Uniqueness**: Globally enforced via `/usernames/{username}` collection lookup and Firestore transaction check.
- **Atomic Claiming**: Username reservation and user profile creation happen in a single Firestore transaction to prevent race conditions.
- **Change Rules**: Users may change their username once every 30 days. The old username index document is deleted, and the new one is claimed atomically.
