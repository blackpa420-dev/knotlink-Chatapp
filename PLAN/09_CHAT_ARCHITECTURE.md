# Chat Architecture

## 1. Deterministic Chat ID Generation
- To prevent duplicate or split chat threads between two users, `chatId` is generated deterministically by sorting the participant UIDs alphabetically and combining them with an underscore separator:
  $$\text{chatId} = \text{sort}(uid_1, uid_2).\text{join("\_")}$$
- This guarantees that User A and User B always write to and read from the exact same Firestore chat document.
