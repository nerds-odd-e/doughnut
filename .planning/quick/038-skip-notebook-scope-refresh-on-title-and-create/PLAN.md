# Skip notebook-scope wiki refresh on title save and note create (probe)

**Status:** done  
**Shipped:** title update and note create no longer call `refreshNotebookScope` (keep `refreshForNote` on create). Other callers unchanged.

**Deploy check:** In a large notebook, time a title rename and a new note. Edit body while a title save is in flight — content PATCH should not lock-timeout. Resolution is live (see the live-resolution design in `.planning/quick/039-authoritative-authored-note-references/PLAN.md`), so there is no notebook-wide cache to keep coherent; if a lock-timeout is confirmed, investigate under that design rather than planning a cache strategy.

### 1. Title save and note create do not rebuild the notebook wiki cache

- **Type:** Behavior
- **Status:** done
