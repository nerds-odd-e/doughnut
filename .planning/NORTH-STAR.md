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
and the [integration details](../docs/notebook-git-synchronization.md#classification-and-references).

## One accepted-change boundary

Integrate attachments through the existing domain-operation and Git publication
owners. Accept complete content/reference changes atomically and project the
final tree once. Git owns Portable content; private learning data stays
server-side. Local tools rebase unpublished divergence; Donut accepts only
forward linear history. Extend these owners rather than creating a separate
attachment store of truth or synchronization path.
See [ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
and [domain operation ownership](../docs/notebook-git-synchronization.md#domain-operation-ownership).
