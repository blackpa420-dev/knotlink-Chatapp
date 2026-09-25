# Search & User Discovery

## 1. Search Pipeline
1. **User Query**: User types username or phone number in search bar.
2. **Backend Query / Firestore Query**:
   - Username exact or prefix query against `/usernames` index.
   - Phone query against `/users` where privacy settings permit.
3. **Privacy Filtering**: Respects user privacy toggles (e.g., "Allow discovery by phone number").
4. **Result Ranking & Response**: Returns public profile metadata (`uid`, `fullName`, `username`, `profilePhotoUrl`).
5. **Local Cache**: Results cached temporarily in Room for quick re-access.
