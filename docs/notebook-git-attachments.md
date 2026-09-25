# Notebook Git attachment synchronization

This document details the attachment-specific part of the
[Notebook Git synchronization architecture](./notebook-git-synchronization.md#attachments-in-the-portable-tree).
[ADR 0001](./adrs/0001-ubiquitous-language.md#notebook--note-structure) defines
the domain vocabulary; [ADR 0004](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
owns representation and validation. Attachment placement in notebook folders is
implemented, as **Attachments today** below records; the rest of this document is
an architectural contract rather than a claim that those parts are already
implemented.

[Git LFS attachment storage](./notebook-git-lfs.md) defines how attachment
bytes are stored outside Git and referenced by Git pointers.

## Attachments today

A non-Markdown file at the notebook root or in a folder is an Attachment: it
carries a complete filename, extension included, and exact bytes, and it has no
note identity, title or learning history.

`notebook_attachment` holds each file's notebook, nullable folder, filename and
accepted Git content: the file's Git LFS pointer, or empty content for an empty
file. It is a projection of accepted Git content, not a second authority, and
it is deleted with its notebook or folder. Filenames are unique within their
notebook-root or folder location under a binary collation, so paths differing
only in case remain distinct files exactly as Git treats them.

Acceptance applies a proposal tip's whole attachment set by full path in one
rule covering addition, edit, rename and removal: no commit is replayed, and the
set is projected before the post-mutation tree comparison, inside the
transaction that persists the accepted head. Missing folder ancestry is created
before that comparison. An initial publication may therefore be file-only, with
no note, container README or `.keep`; its folder ancestry remains visible in the
web projection.

Web rename, move within the notebook, trash and recovery carry a folder's files
through the same folder placement. Permanent folder deletion deletes its files.
As an interim safety boundary, dissolving or merging a folder whose subtree
contains a file, or moving that folder to another notebook, is refused without
changing either the accepted tree or its projection.

Every consumer of a notebook's live Portable content — ZIP export, Git cutover
and history reset, accepted web changes, and projection-drift detection — reads
one assembled tree, so files survive ordinary note and folder work and appear in
export and reset without per-consumer handling. Within a directory the canonical
order is README, then notes, then attachments by filename, then subdirectories;
the Git side re-sorts by path.

A root `.keep` is an ordinary attachment. The empty-folder marker is a `.keep`
inside a folder, which keeps its structural role.

Web Donut shows every attachment row to whoever can read the notebook,
including Bazaar readers. The sidebar lists files at their folder or the root,
sorted with notes by filename. A file's page (`/notebooks/:notebookId/attachments/:attachmentId`)
shows its filename and size and downloads its exact bytes under its exact
filename, always as `Content-Disposition: attachment` with
`application/octet-stream` and `nosniff`, so an SVG or HTML file never renders
in Donut's origin. The download serves the stored object named by the row's
pointer; [Git LFS attachment storage](./notebook-git-lfs.md#consequences) gives
the rules for a missing object and an empty file. `.gitattributes`
and nested `.keep` markers are not rows, so, like `.git`, they are not shown on
the web.

A picture uploaded with a note's `image` property on the web becomes a file in
the note's folder under its uploaded name, with its original bytes: the bytes are
stored in the notebook's content store first, then the LFS pointer and the
note's `image: <filename>` are accepted together in one web commit, with the
note content prepared like any other content save. The editor saves pending
text before sending the upload and then shows the returned note without saving
it again, so one upload is exactly one commit. A name that a file, note or
folder in the notebook's accepted tree already uses in that folder, or one that
is not a plain filename (empty, containing `/`, or starting with `.`), is
refused with a message naming the path; nothing is renamed or overwritten. A notebook without an LFS binding refuses the
upload.

Legacy uploaded pictures (the `image` table, addressed as
`/attachments/images/{id}/{fileName}`) are moved into their notebooks at
application startup, after Flyway's migration. For each notebook with a note
whose content mentions that address, a note whose frontmatter `image:` names an
upload owned by a note in the same notebook gets the picture's bytes stored
first and then a file beside the note, and `image:` names that file; the
`image_mask:`, body and last-updated time are unchanged. The file uses the
stored name's last segment (`picture` is prefixed to a hidden or empty one) and,
when the folder already uses it or another moved picture chose it, the next
free numbered name before the extension (`example (2).png`). Each note referring
to the same upload gets its own copy. All of a notebook's moved pictures are one
Donut System commit. Each notebook moves in its own transaction: a notebook
whose move fails is logged and left unchanged, the others still move, and the
next startup retries it. Running the move again changes nothing. A reference to
another notebook's upload or to an upload without a note is left unchanged and
counted in a startup warning; a reference to a missing upload is left
unchanged. Trashed notes move like any other note, and recall and memory-tracker
state are untouched. Moved pictures are existing content, so the 10 MiB limit
for new payloads does not apply to them. Another notebook's upload is never
copied, because that would place one notebook's possibly private bytes in
another notebook's history. Legacy orphan cleanup skips a note-relative
`image:` value, so a moved note's row stays as the backup. The legacy rows stay, still served inline under the notebook read
rule (owners, subscribers, and Bazaar readers, even logged out; another user is
refused, and an anonymous request for a private notebook's image must log in),
until they are retired. No new picture is stored there.

Publication checks every attachment in the contiguous first-parent proposal
range, not only the tip, by the
[Git LFS acceptance rule](./notebook-git-lfs.md#decision), including its
object-availability exception for oversized intermediate-only payloads: each
one must be a Git LFS pointer or an empty file, and a new payload may be at most
10 MiB (10,485,760 bytes, inclusive). Classification uses the ordinary attachment
rule at each historical path, so a later rename to Markdown does not hide an
earlier oversized attachment. Payloads already accepted as attachments in the
same notebook's retained history remain reusable by content identity. To fix a
refused proposal, rewrite only unpublished commits.

## Placement and ownership

Notes and attachments use the same notebook folder structure and access boundary.
The notebook root is also a content location; no additional root Folder entity
is required. Each file path has one role and one content value, so a note and
attachment cannot occupy the same path. An attachment-only folder remains
represented and visible, and an attachment counts as content for `.keep`.
Container Readme and structural markers retain their distinct roles.

An attachment belongs to its notebook independently of any referring note.
Removing a note or reference does not itself delete the attachment. Deleting an
attachment does not delete referring notes or their learning data. The user
experience for remaining references after file removal still needs refinement.
Images share attachment placement, ownership, publication, and deletion behavior;
rendering does not introduce another file-management model. A PDF attachment
does not by itself create a Book or reading record.

Attachments cover non-Markdown files. These domain distinctions prescribe
neither a class hierarchy nor a universal file entity, database schema, or new
attachment identity service. Attachment paths retain the complete filename,
including its extension; they are not note/property Portable paths or stable
identities across moves.

## Classification and references

Use one classification and codec contract across import, export, publication,
and lint. Web browsing consumes the resulting domain projection rather than
reclassifying files or maintaining another inventory. All Markdown, including
`AGENTS.md`, `SKILL.md`, and tool documentation, retains ordinary note/Readme
behavior. Purpose, IDE name, folder location, and whether the file is new do not
create an alternative Markdown admission path. Existing reserved-name and
container `README.md` rules remain unchanged; unknown valid types remain valid.

At local publication, every Markdown file must satisfy the existing format:
valid UTF-8 and YAML frontmatter with a nonblank type, plus the applicable
concept/path rules. Invalid Markdown rejects the whole proposal without changing
accepted content or learning identities. The CLI reports the error; the owner
corrects the local file and republishes. Publication does not repair it or
preserve it as an attachment. Existing web save normalization also stays intact.

Non-Markdown supporting files use the attachment model with their original
bytes preserved. Standard Git metadata such as `.gitattributes` remains metadata;
it is not an attachment or learning concept. The hydrated Portable tree contains
the actual attachment files; their Git representation follows the
[LFS storage contract](./notebook-git-lfs.md).
Accept files and authored references through the existing publication boundary,
with no separate attachment history or mutable content authority. ADR 0002
excludes Donut-specific local classification manifests; standard Git metadata
and LFS configuration are allowed. Check the resulting hydrated mixed-tree
profile against OKF before claiming compatibility for the entire tree.

AI guidance uses ordinary note refinement, with no refinement changes required
by this direction.

A note's frontmatter `image:` may name a notebook file by a path relative to the
note's own folder (`force-diagram.png`, `images/force.png`), so the same authored
value works locally and on the web without private server IDs. Web Donut serves
it at `/api/notes/{note}/attachment-image?path=<image value>` under the notebook
read rule: the file whose path equals the note's folder prefix plus the value,
taken literally with no normalization, so `..`, `./` or a leading `/` never
match and the picture stays visibly broken (404). Only PNG, JPEG, GIF and WebP,
chosen by filename extension and never sniffed, are served inline with
`nosniff`; any other type, including SVG, is refused (415). Values starting with
`/` (such as `/attachments/images/...`) or carrying a URL scheme are used as-is,
and `image_mask:` is unchanged. Markdown body image embeds are kept on web edits
but not yet rendered from notebook files.
This does not change semantic Wiki-link or property-reference rules. Existing
image ownership, sharing, and references must be established before migration;
preserve image access and note learning identity throughout the transition.
