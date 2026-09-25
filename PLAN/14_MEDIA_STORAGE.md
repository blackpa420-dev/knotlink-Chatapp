# Media & File Storage

## 1. Attachment Pipeline
- **Types Supported**: Images, Videos, Voice notes, Documents.
- **Workflow**:
  1. Client selects media file.
  2. Thumbnail generated locally (for images/videos).
  3. Secure upload to **Firebase Storage** under `/chats/{chatId}/{mediaId}`.
  4. Upon successful upload, storage download URL and metadata are attached to the Firestore message document.
  5. Cached locally in app cache directory for instant offline playback.
