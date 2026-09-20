# 0002 — Git-native Portable notebook tree synchronization

**Status:** Accepted

**Date:** 2026-09-04

**Accepted:** 2026-09-19

**Amended:** 2026-09-20 — local rebase responsibility and Portable/learning-data boundary, decided by Terry Yin

**Decision makers:** Terry Yin

## Decision

- **Content authority:** The accepted Git commit is authoritative for the
  Portable notebook tree. MySQL holds its current application projection and
  remains authoritative for private Note/Folder identities and their learning
  data. Markdown notes and guidance, and non-Markdown attachments, belong to
  Portable content; recall history and other private learning data do not.
  The tree format follows [ADR 0004](./0004-okf-compatible-notebook-markdown-accepted.md);
  Markdown guidance uses the same format and behavior as other Markdown.
- **One synchronization model:** Use Git objects, refs, and history for two-way
  synchronization. Preserve submitted commit IDs on acceptance and never rewrite
  accepted history; local unpublished commits may be rebased before submission.
  Do not add a custom revision
  protocol, sync envelope, or merge model. Require no Donut-specific local files or
  database; ordinary Git configuration and credential storage are allowed.
- **One publication boundary:** Every accepted Portable content change,
  including web edits, goes through Git publication. Commit the accepted head,
  final MySQL projection, identity outcomes, and required derived state
  atomically. Reject stale or invalid updates without overwriting accepted work.
  Portable content changes must appear in the published commit.
- **Final-state projection:** Retain original Git history, but validate and
  materialize only the publication tip. Use intermediate commits as identity
  evidence; do not replay them through live application mutations. The
  relationship to ADR 0004's validation rule remains unresolved below.
- **Identity protection:** Preserve existing identities and dependent data for
  soundly identified moves. Refuse ambiguous identity changes. Confirmed
  deletion ends an identity; recreation starts a new one. Restoring Git content
  cannot recover deleted identities or learning data.
- **Repository binding:** Bind a notebook to a repository, accepted ref, and
  root directory. V1 uses one dedicated repository per notebook and an
  append-only, linear `main`. Serialize acceptance per notebook. If web and local
  content changes diverge from a common base, the local side acquires the latest
  accepted history, rebases its unpublished changes onto it, resolves conflicts
  locally, and pushes a fast-forward result. Donut Web never merges or rebases
  either history; it rejects divergent or stale submissions without rewriting
  them. Keep the binding model compatible with a future directory
  inside a project repository.

## Detailed architecture

[Notebook Git synchronization architecture](../notebook-git-synchronization.md)
specifies repository and history restrictions, cutover, publication guarantees,
identity semantics, and future integration boundaries. Consult it when changing
synchronization, Git storage, or Portable-content publication.

## Unresolved policy

Identity admission beyond supported exact correspondence, remaining durability
and receipt guarantees, and the interpretation of
[ADR 0004's durable-write rule](./0004-okf-compatible-notebook-markdown-accepted.md#validation)
for retained intermediate commits remain open. See
[unresolved policy](../notebook-git-synchronization.md#unresolved-policy).
