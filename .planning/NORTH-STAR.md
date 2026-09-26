# Short-term architectural direction

## One notebook tree

Model notebook content directly: folders contain notes, attachments, and child
folders; the notebook root can contain the same content. Notes retain their
learning identities. Attachments own named file content independently of note
references; images add presentation to that same attachment model, and a Book
adds private reading structure to it. Share folder placement without forcing
these concepts into a universal file entity.
See the [domain vocabulary](../docs/adrs/0001-ubiquitous-language.md#notebook--note-structure).

## One set of names per folder

Each folder, and the notebook root, has one set of entry names: a note's
`Title.md`, a subfolder's name and a file's filename, compared without letter
case so a checkout on a case-insensitive file system matches the web. One owner
answers whether a name is taken, from live rows, for every web placement: a
name the user chose is refused when taken, a name Donut chooses is the first
free one. Operations that rehome several entries check every destination
before changing anything. Folder contents are removed or moved by code, never
by a database cascade, so every removal passes the accepted-change capture and
a forgotten one fails loudly (owner decisions 2026-09-26). Web placements
follow it ([one set of names per folder](../docs/notebook-git-attachments.md#one-set-of-names-per-folder));
remaining work: cross-notebook moves (SEED-035 story 10).

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

## One attachment content model

This governs every attachment story: LFS conversion, web picture upload,
picture migration, Books, legacy retirement, and the later browsing, deletion
and folder operations. [Git LFS attachment storage](../docs/notebook-git-lfs.md)
holds the accepted details under ADRs 0002 and 0004; this section decides how
the remaining work grows. Acceptance of the architecture does not mean the
capabilities are delivered.

### One store for file bytes

Every non-Markdown file of a notebook is one Attachment: a row placed in a
folder whose bytes are one immutable Git LFS object in the notebook's private
GCS content store, keyed by SHA-256 and verified for size and digest when
stored. Git holds only the pointer; accepted history selects exact versions.
This holds whether the file was published locally, uploaded on the web, or is
a Book's source. No other byte store exists.

Roles refer to an Attachment and never own bytes:

- A note's picture is the authored `image:` value, a path relative to the
  note's folder; `image_mask:` stays presentation on the note.
- A Book is private reading structure (layout, blocks, reading progress) over
  one source Attachment. Like learning history, it stays server-side.
- Attachment references are not semantic Wiki links
  ([ADR 0005](../docs/adrs/0005-web-routes-accepted.md)).

### Every notebook uses LFS

There is one representation, so no code asks how a notebook stores its files.
Notebooks created before LFS reach it through one forward conversion commit
that adds `.gitattributes` and replaces each current file with its pointer.
After all notebooks are converted, the per-notebook representation marker and
the raw-Git paths are removed. Bytes already in raw accepted history stay
readable where they are, in the accepted Git object store; they are neither
moved nor rewritten. No rollout resets history or changes accepted commit IDs,
so existing local checkouts keep working with `donut notebook pull`.

### One way in

- Every addition of file bytes — local publish, web upload, Book attach,
  conversion, migration — stores the verified object first, then accepts the
  pointer together with the references that use it in one accepted change,
  through the existing domain-operation and publication owners.
- A web action that adds a file for a role writes the file and its reference
  (for example `image:` or the Book's source) in the same accepted change.
  It never leaves an unreferenced intermediate commit.
- Changes Donut makes on its own (conversion, migration) are ordinary forward
  commits by the Donut System identity. Owners receive them with
  `donut notebook pull`, which rebases their unpublished local commits.
- When Donut chooses a filename itself (a move or migration), it takes a free
  name in the target folder rather than overwriting or refusing, because it
  writes the only reference at the same moment. A web upload keeps the name
  the user gave and, like operations that move files the user placed, refuses
  when that name is taken in the folder.
- New payloads are limited to 10 MiB (10,485,760 bytes), inclusive. A Book's
  source file is the one exception and keeps the Book upload limit (currently
  100 MB). Content already accepted is never judged again.
- Web saves that change only notes neither load unchanged file bytes nor
  contact GCS.

### One way out

One reader (`NotebookAttachmentFile`) serves the file download, note pictures,
and Book reading, under the notebook read rule. Clone and pull fill in every
current attachment. "Available in a local checkout" is therefore a property of
the model, and each story proves it with a pull rather than building it.

### Standard Git LFS end to end

Require the standard client for local attachment work and reuse its filters,
object cache, and transfer protocol. Keep the Donut CLI and Git-bundle
transport: publish uploads required LFS objects before submitting commits,
because bundle creation does not run Git's pre-push hook. Acquisition configures
the authenticated notebook LFS endpoint before filling in the checkout; a
failure is reported as incomplete acquisition with a retry path, never as
success with a pointer in place of the file. Transfers stream through the
backend's authorization into private GCS under notebook-scoped keys; no
cross-notebook deduplication. `.gitattributes` is reserved Git metadata,
preserved across web changes; server validation owns admission even when a
client's filters are missing or overridden.

### Moving and retiring

- Moves run per notebook, resume after interruption, and skip content already
  moved, so running them again is safe.
- Never delete an object because its current attachment row disappeared.
  Automatic object garbage collection stays deferred until retained history
  and pending uploads are understood.

### Order

The [product backlog](PRODUCT-BACKLOG.md) owns global order. Browsing, deletion and folder operations consume the same model; a PDF file
does not become a Book by itself. Conversion stops old notebooks' bundles from
growing with new files but cannot shrink what their history already holds;
shrinking that would need a separate decision about rewriting history.
