Remove the "remove point from note" AI feature from backend.

Scope:
- Delete the `/api/ai/remove-point-from-note/{note}` endpoint and its service/tool chain.
- Remove the backend unit tests that exercised this endpoint.
- Regenerate OpenAPI and TypeScript client code after the removal.

Out of scope:
- `ignoredChecklistTopics` in the note assimilation payload stays for now.
