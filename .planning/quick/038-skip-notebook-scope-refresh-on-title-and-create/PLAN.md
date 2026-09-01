# Skip notebook-scope wiki refresh on title save and note create (probe)

**Status:** done  
**Shipped:** title update and note create no longer call `refreshNotebookScope` (keep `refreshForNote` on create). Other callers unchanged.

**Diagnosis / follow-up notes:** `.planning/notes/notebook-scope-wiki-refresh-on-title-and-create.md`

**Deploy check:** In a large notebook, time a title rename and a new note. Edit body while a title save is in flight — content PATCH should not lock-timeout. If confirmed, plan the durable cache strategy (async/after-commit or incremental).

### 1. Title save and note create do not rebuild the notebook wiki cache

- **Type:** Behavior
- **Status:** done
