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
it is deleted with its notebook; a folder's files are removed in code before the
folder, never by a database cascade. Filenames are unique within their
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
through the same folder placement. A note moved within the notebook on the web
carries the file its `image:` names as a plain filename in the note's folder
into its new folder, in the same accepted change; when an entry there already
holds that name (ignoring case), the file takes the first free numbered name
(`force (2).png`) and the note's `image:` is rewritten to it. When another note
in the source folder also names the file, the file stays and the moved note gets
a copy reusing the same stored bytes. A note moved to another notebook leaves
the file in the source folder and keeps its `image:` unchanged. Trashing a note
leaves that file where it is. Dissolving a folder, or merging it into a same-named folder within the
notebook, carries its files exactly as its notes:
each file keeps its row and bytes and only its folder changes. File references
are not rewritten: a note outside the dissolved or merged folder whose `image:`
points into it shows a broken picture afterwards. Permanent folder
deletion removes its files. Moving a folder whose subtree contains a file to
another notebook is refused without changing either the accepted tree or its
projection. Notebook health counts a file as occupying its folder, so a folder
holding only files is neither reported nor purged as empty.

### One set of names per folder

On the web, the entries of one folder (or the notebook root) share one set of
names: a note occupies `Title.md`, a folder its name and a file its filename,
compared without letter case. Every web placement — note create, rename, move,
undo and trash; folder create, rename, move, trash, dissolve and merge; picture
upload; Book file naming — asks this one rule over live rows
(`FolderSiblingNameValidation`). A name held by another folder is
`FOLDER_NAME_CONFLICT` (where merging is offered); one held by a note or file
is `RESOURCE_CONFLICT` naming its path. Dissolve and merge check every
destination entry before any change and merge case-variant folders into the
existing one. Trash mirrors the item's folder path under `_trash/` and reuses a
folder there whose name differs only in letter case, keeping its name. Existing
content is not judged again. The case-sensitive
database unique keys stay as the last safety net, and local publish is not
governed by this rule.

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

Whoever can edit the notebook can delete a file from its page after a plain
confirmation ("Delete <filename>?"). The delete removes the row as one accepted
web change ("Delete file: <path>"), so the next pull no longer has the file;
notes, other files and learning history are untouched, and the reader lands on
the containing folder or notebook page. The LFS object stays, since earlier
history still names it, and there is no web Trash for files. A note whose
`image` names the deleted file keeps that value and shows a broken picture, as
after the same delete through local publish.

A picture uploaded with a note's `image` property on the web becomes a file in
the note's folder under its uploaded name, with its original bytes: the bytes are
stored in the notebook's content store first, then the LFS pointer and the
note's `image: <filename>` are accepted together in one web commit, with the
note content prepared like any other content save. The editor saves pending
text before sending the upload and then shows the returned note without saving
it again, so one upload is exactly one commit. A name that a file, note or
folder in that folder already uses (ignoring case, under the one set of names),
or one that is not a plain filename (empty, containing `/`, or starting with
`.`), is refused with a message naming the path; nothing is renamed or
overwritten. A notebook without an LFS binding refuses the upload. Uploading a
replacement picture leaves the previous file in the folder: it stays part of the
notebook and its Git history until someone deletes it.

Pictures uploaded before pictures became notebook files were moved into their
notebooks as files beside their notes; their old table and
`/attachments/images/...` address no longer exist.

A Book's source file (PDF or EPUB) is an ordinary file at the notebook root.
Attaching a Book on the web stores the bytes in the notebook's content store
first, then accepts the file's LFS pointer together with the Book in one web
commit ("Attach book: <name>"). Donut chooses the name: `<book name>.pdf` or
`<book name>.epub`, or `book.<format>` when the book name is not a plain
filename, then the next free numbered name before the extension when a file,
note or folder at the notebook root already uses it, ignoring case
(`Physics Primer (2).pdf`). The Book refers to that root-relative path
(`book.source_file_path`), and Book reading serves the file through the one
attachment reader. The Book size limit (100 MB) applies instead of the 10 MiB
limit for new payloads; it is Spring's `spring.servlet.multipart.max-file-size`
in `backend/src/main/resources/application.yml`, and an oversize upload gets
HTTP 413 with an `ApiError` JSON body. A notebook without a Git binding saves the file row and
the Book without a commit. Removing the Book removes its reading structure and
progress but leaves the file, and the accepted tree, unchanged.

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

Publication also refuses (409) a proposal that deletes, renames or changes the
file a Book reads from, naming the file and the Book: the owner removes the
Book on the web first. A rename counts as removing the old path. Deleting that
file on the web is refused with the same message.

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
`/` (such as `/pictures/example.png`) or carrying a URL scheme are used as-is,
and `image_mask:` is unchanged. Markdown body image embeds are kept on web edits
but not yet rendered from notebook files.
This does not change semantic Wiki-link or property-reference rules. Existing
image ownership, sharing, and references must be established before migration;
preserve image access and note learning identity throughout the transition.
