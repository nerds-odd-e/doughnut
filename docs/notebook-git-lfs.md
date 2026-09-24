# Notebook Git LFS attachment storage

This document details the accepted attachment storage decision under
[ADR 0002](./adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and the file representation in
[ADR 0004](./adrs/0004-okf-compatible-notebook-markdown-accepted.md).
Accepted by Terry Yin on 2026-09-23. It is an architectural contract;
implementation proceeds incrementally through the North Star and stories.

## Context

Attachment payloads currently occupy MySQL projections and Git objects. Moving
only their database storage cannot make Git bundles smaller. We need ordinary
local files with compact Git history and one attachment model, including images.

## Decision

- **Representation:** Use standard Git LFS v1 pointers (SHA-256 and byte size)
  for non-Markdown attachments. Git selects the exact immutable content version;
  private GCS objects hold its bytes. MySQL stores metadata and references, not
  attachment payloads, including retained legacy payloads. Never commit storage
  URLs or credentials. Markdown remains ordinary Git content; `.gitattributes`
  is Git metadata, and structural markers retain their existing role. Follow
  the standard empty-file representation.
- **Clients:** Require standard Git LFS for supported local attachment workflows.
  Donut coordinates existing Git-bundle transport with standard LFS transfers;
  it does not implement another local cache, pointer format, or sync protocol.
  Working files and authored references retain their original paths and bytes.
  A Git bundle alone contains pointers, not a complete attachment backup.
- **Acceptance:** Upload immutable objects and verify actual size and digest
  before atomically accepting the Git head and application projection. Validate
  attachment representation and object availability across newly admitted
  history, not only its tip. One exception: a new oversized payload that
  appears only in unpublished intermediate commits, and not at a valid tip,
  may be absent; it is never stored, and fetching it later reports the object
  unavailable (see [Recovery](#recovering-a-published-attachment-version)).
  Within-limit, previously accepted, and tip objects stay required. Missing
  objects or raw attachment payloads reject publication without rewriting
  submitted commits. Failed publications may leave unreferenced staged
  objects, never partially accepted content.
- **Access and lifetime:** Object access follows notebook authorization; knowing
  a hash grants no access. Keep objects reachable from retained accepted history,
  even after removing a current file. Backups include Git, referenced objects,
  and private application data. Cleanup must preserve accepted and in-flight work.
- **Compatibility:** Preserve accepted commit IDs, linear history, note/learning
  identities, and the shared publication boundary. Existing notebooks transition
  by forward commits; legacy history stays readable. Forward conversion stops
  binary-driven bundle growth but cannot remove old binaries from full-history
  bundles. Rewriting or resetting accepted history needs a separate decision.

## Consequences

Local attachment work requires Git LFS and object access; already hydrated files
remain usable offline. Pointer-only history stays small as binary versions grow,
although acquiring a file still transfers its bytes. GCS durability and retention
become part of content preservation. Delivery is incremental, under the
[North Star](../.planning/NORTH-STAR.md#attachment-storage-transition).

## Recovering a published attachment version

Ordinary clone and `donut notebook pull` hydrate only the current tip. Previously published attachment
bytes stay reachable from retained accepted history. Recover them with the
standard Git LFS client (verified with Git LFS 3.7.1 against Donut's notebook
endpoint). There is no Donut history UI or Donut-specific history command.

Configure the notebook endpoint and bearer token in local Git config (clone
already does this; transfers use `lfs.url`, not the placeholder `origin` URL):

```bash
git config lfs.url http://127.0.0.1:<port>/api/notebooks/<notebook-id>/lfs
git config http.extraHeader "Authorization: Bearer <token>"
```

Clear the local object cache when you need a fresh download (Git LFS 3.7.1 has
`push --object-id` but not `fetch --object-id`):

```bash
rm -rf .git/lfs/objects
git lfs fetch origin <published-commit>
```

The requested digest's exact bytes land under `.git/lfs/objects/<aa>/<bb>/<oid>`.
Removing the current attachment row never deletes those retained objects; fetch
of a historical commit that still references them continues to succeed.

An oversized intermediate object that was omitted at publication (corrective tip
only) is not stored. Fetching that intermediate commit reports the object
unavailable (standard batch/object-not-found failure) and does not begin storing
the omitted payload.

## Related

- [Notebook Git synchronization](./notebook-git-synchronization.md) owns
  accepted history, publication, and identity protection.
- [Attachment integration](./notebook-git-attachments.md) owns placement,
  references, and the current implementation boundary.
- [Git LFS pointer/filter specification](https://github.com/git-lfs/git-lfs/blob/main/docs/spec.md),
  [Batch API](https://github.com/git-lfs/git-lfs/blob/main/docs/api/batch.md),
  [Basic transfers](https://github.com/git-lfs/git-lfs/blob/main/docs/api/basic-transfers.md).
- Feature proof of these commands:
  `e2e_test/features/cli/cli_notebook_lfs.feature` (explicit historical fetch
  after replacement/deletion and omitted oversized intermediate).
