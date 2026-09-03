# Skip notebook-scope wiki refresh on title save and note create (probe)

**Status:** done  
**Shipped:** title update and note create no longer call `refreshNotebookScope` (keep `refreshForNote` on create). Other callers unchanged.

**Note:** Resolution is live (see `.planning/quick/039-authoritative-authored-note-references/PLAN.md`), so there is no notebook-wide cache to keep coherent going forward.
