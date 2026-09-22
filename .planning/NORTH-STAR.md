# Short-term architectural direction

## One notebook tree

Model notebook content directly: folders contain notes, attachments, and child
folders; the notebook root can contain the same content. Notes retain their
learning identities. Attachments own named file content independently of note
references; images add presentation to that same attachment model. Share folder
placement without forcing these concepts into a universal file entity.
See the [domain vocabulary](../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure).

## One format boundary

One classification and codec owner maps the complete tree to domain concepts.
Import, export, publication, and lint share that contract; browsing uses its
projection. Every Markdown file uses the existing note/Readme rules, including
AI guidance; invalid Markdown is rejected, never treated as an attachment.
Preserve non-Markdown attachment bytes. Follow
[ADR 0004](../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
and the [integration details](../docs/notebook-git-attachments.md#classification-and-references).

## One accepted-change boundary

Integrate attachments through the existing domain-operation and Git publication
owners. Accept complete content/reference changes atomically and project the
final tree once. Git owns Portable content; private learning data stays
server-side. Local tools rebase unpublished divergence; Donut accepts only
forward linear history. Extend these owners rather than creating a separate
attachment store of truth or synchronization path.
See [ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and [domain operation ownership](../docs/notebook-git-synchronization.md#domain-operation-ownership).

## Attachment storage transition

[Git LFS attachment storage](../docs/notebook-git-lfs.md) details the accepted
contract under ADRs 0002 and 0004. The direction below stages its implementation;
acceptance of the architecture does not imply that these capabilities are delivered.

### Use standard Git LFS end to end

Require the standard client for local attachment work. Reuse its filters,
object cache, and transfer protocol rather than writing Donut equivalents.
Keep the existing Donut CLI and Git-bundle transport: bundle creation does not
run Git's pre-push hook, so publish must explicitly upload required LFS objects
before submitting commits. Acquisition configures an authenticated notebook LFS
endpoint in local Git configuration before hydrating the selected checkout;
pull and local rebase use the same LFS configuration. A hydration failure is
reported as incomplete acquisition with a retry path, never success with a
pointer masquerading as an image. Direct Git hosting is not a prerequisite.

Expose the standard LFS Batch/Basic API through Donut's existing authorization.
Start with authenticated transfers streamed through the backend into private
GCS, checking byte counts and SHA-256 on upload. This keeps verification and
access in one place; signed direct transfers can follow measured need. Use
notebook-scoped object keys containing the digest, avoiding cross-notebook
deduplication and its authorization/lifetime bookkeeping initially. Web reads,
LFS transfers, and accepted projections refer to the same immutable content.
Reuse the existing GCS integration where suitable; Book ownership and deletion
remain distinct from attachment history retention.

Treat `.gitattributes` as reserved Git metadata and preserve it across web
changes. It tracks non-Markdown attachments through LFS, exempts Markdown and
Git metadata, and preserves structural markers and standard empty-file behavior.
Server validation owns admission even if a client's filters are absent or its
attributes are overridden. Do not trust a small pointer's declared size without
checking the referenced bytes, or silently transform raw blobs on acceptance.
Endpoint and credential configuration stay outside authored content.

### Deliver usable increments

1. Story 12 applies the attachment size boundary through current ingress paths.
   The initial limit is 10 MiB (10,485,760 bytes), inclusive. Apply it to new
   payloads throughout submitted history; retain already accepted oversized
   content without allowing it to bypass the rule for new content. Books keep
   their separate limits. Aggregate quotas remain outside this increment.
2. Story 13 supplies the full local publication/acquisition and web-preservation
   loop for new notebooks using LFS. Prove bundle growth against incompressible
   binary versions and measure object traffic separately. Note-only web saves
   must neither load unchanged payloads nor contact GCS to rewrite them.
3. Story 14 transitions existing notebooks without rewriting their accepted
   history or losing file access. Keep the legacy representation readable during
   rollout; once converted, require pointers for newly introduced attachments.
   Move legacy Git attachment payload storage to GCS while retaining their
   original Git object IDs and bytes for historical bundle reconstruction.
   Remove database payload copies only after verification and durable switch.
4. Existing browsing, image-authoring/migration, and deletion stories consume
   this common attachment content. Their user outcomes remain separate. Old
   uploaded images join it through story 5; a PDF file does not become a Book.

Each transition keeps one authoritative accepted version. Legacy current-tree
writes end at conversion; historical raw blobs remain readable for as long as
their commits are retained. There are no permanent duplicate payload writes.
Never delete an object merely because its current attachment row disappeared.
Defer automatic object garbage collection initially; retained-history references
and pending uploads must be understood before reclamation is enabled.

Converting the current tree cannot shrink an existing full-history bundle's
legacy binary portion. Moving those bytes to GCS reduces MySQL storage only.
New LFS history avoids adding that cost; already-large full-clone downloads need
a separate owner decision about history rewriting or reduced-history transport.
No rollout step silently resets a notebook or changes accepted commit IDs.
