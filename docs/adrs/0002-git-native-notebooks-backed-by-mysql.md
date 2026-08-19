# 0002 — Host Git-native notebooks backed by MySQL

**Status:** Approved
**Date:** 2026-08-04  
**Decision makers:** Terry Yin  
**Consulted:** None

## Context

Doughnut stores notes, folders, and readmes as mutable MySQL rows. Local
export is a ZIP of Markdown. Path-keyed CLI sync and private `.doughnut-sync`
baselines are retired (not part of the product).

We want Git revisions stored by Doughnut to be authoritative for portable
notebook content (notes, folders, readmes). Relational note/folder *content*
becomes a rebuildable projection of the accepted Git head. Permissions,
subscriptions, memory trackers, questions, and other Doughnut-specific
behavior stay authoritative relational data.

Local copies should be OKF-compatible Markdown trees (see
[ADR 0004](./0004-okf-compatible-notebook-markdown.md)), real Git
repositories, free of Doughnut note IDs in the working tree, and syncable
with Doughnut. Doughnut keeps stable internal identities privately so
learning data stays attached across renames.

Git objects and refs live in MySQL so accepting a ref, recording identity
lineage, and updating the projection share one transactional boundary.

## Decision

### Level 1 — MVP

Authority and representation:

1. **Git is authoritative for portable content.** Store blobs, trees, commits,
   and the accepted `main` ref in MySQL. Projected note/folder rows carry the
   commit they represent and must be rebuildable from that commit plus identity
   lineage. Do not mutate projected content outside an accepted revision.
2. **OKF working tree, no internal IDs.** Canonical tree follows
   [ADR 0004](./0004-okf-compatible-notebook-markdown.md). No note ID, UUID,
   or sync manifest identity in the tree.
3. **Private identity lineage.** Same path → same identity; unambiguous
   rename/folder-move → preserve; delete → tombstone (keep DB-only history);
   ambiguous delete/add/rename/copy → reject for explicit resolution (CLI asks
   the user). Collision paths use human suffixes like `Recipe (2).md`, never
   DB keys. Projection rebuild must not reallocate surviving note IDs.
4. **One acceptance boundary.** Every durable content change (CLI push or web
   edit) must: authorize → require parent = current `main` → validate OKF →
   resolve lineage → atomically advance `main`, lineage, and projection — or
   reject entirely. Force push is forbidden. Web autosave may be draft until a
   commit is accepted. Existing notebooks migrate as a single initial commit.
5. **MVP transport and concurrency.** Accept only `main`. The local OKF tree is a
   real Git repo; Git fetch/push with the server go through the Doughnut CLI (not
   yet a Git remote). While a push is accepted, the server notebook is locked;
   the server never merges — merge/rebase and conflict resolution are always
   local. ZIP/OKF export may remain for one-way portability, not as equivalent
   two-way sync.

### Level 2 — Later

Does not change Level 1 authority. Deferred until needed:

- Doughnut as a standard Git remote (HTTPS clone/fetch/push); CLI ceases to be
  the only sync transport
- Unlock server during push; server-side conflict resolution while web editing
- External remotes (e.g. GitHub) with auto-sync from the server
- Non-`main` refs affecting the live notebook; branch-aware web editing
- Near-full Git hosting (SSH, PRs, LFS, …)
- Images and other binaries in the Git tree

## Consequences

- Retired `.doughnut-sync` baselines are replaced by the Git commit graph as merge-base.
- Hardest Level 1 work: identity-free rename lineage, and web writes → commits.
- History outlives soft-deleted MySQL rows and local clones; retention/erasure
  must account for that.
- Level 2 Git HTTP can reuse the same accept path; transport is separable from
  authority.

## Pros

- Real local Git history instead of timestamp sync; OKF-aligned distribution.
- Learning/DB identity preserved without polluting Markdown.
- MVP avoids merge engines, smart HTTP, and multi-remote authority.

## Cons

- New object store, lineage model, lossless codec, and commit-shaped writes.
- Ambiguous renames need user resolution.

## Assumptions

- Exact Git object IDs are stored and retained for accepted history.
- Accept of ref + lineage + coherent projection is transactional.
- Codec round-trip is lossless for the canonical profile.
- Doughnut auth gates all accept operations; quotas apply to objects/packs.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR 0004 — OKF-compatible notebook Markdown](./0004-okf-compatible-notebook-markdown.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Git rename detection](https://git-scm.com/docs/git-status.html)
