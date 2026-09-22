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
  history, not only its tip. Missing objects or raw attachment payloads reject
  publication without rewriting submitted commits. Failed publications may
  leave unreferenced staged objects, never partially accepted content.
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

## Related

- [Notebook Git synchronization](./notebook-git-synchronization.md) owns
  accepted history, publication, and identity protection.
- [Attachment integration](./notebook-git-attachments.md) owns placement,
  references, and the current implementation boundary.
- [Git LFS pointer/filter specification](https://github.com/git-lfs/git-lfs/blob/main/docs/spec.md),
  [Batch API](https://github.com/git-lfs/git-lfs/blob/main/docs/api/batch.md),
  [Basic transfers](https://github.com/git-lfs/git-lfs/blob/main/docs/api/basic-transfers.md).
