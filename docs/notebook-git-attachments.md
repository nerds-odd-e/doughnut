# Notebook Git attachment synchronization

This document details the attachment-specific part of the
[Notebook Git synchronization architecture](./notebook-git-synchronization.md#attachments-in-the-portable-tree).
[ADR 0001](./adrs/0001-ubiquitous-language.md#notebook--note-structure) defines
the domain vocabulary; [ADR 0004](./adrs/0004-okf-compatible-notebook-markdown-accepted.md#validation)
owns representation and validation. Attachment placement in notebook folders is
implemented, as **Attachments today** below records; the rest of this document is
an architectural contract rather than a claim that those parts are already
implemented.

[Git LFS attachment storage](./notebook-git-lfs.md) defines the accepted target
for external immutable payloads and their Git pointers. The database-byte
projection below describes the current implementation awaiting that transition.

## Attachments today

A non-Markdown file at the notebook root or in a folder is an Attachment: it
carries a complete filename, extension included, and exact bytes, and it has no
note identity, title or learning history.

`notebook_attachment` holds each file's notebook, nullable folder, filename and
bytes. It is a projection of accepted Git content, not a second authority, and
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

Publication admits each newly introduced raw-Git attachment blob against an
inclusive 10 MiB (10,485,760 byte) limit across the contiguous first-parent
proposal range, not only the tip. Classification uses the ordinary attachment
rule at each historical path, so a later rename to Markdown does not hide an
earlier oversized attachment. Payloads already accepted as attachments in the
same notebook's retained history remain reusable by content identity. Removing
or shrinking the tip is not enough while an oversized unpublished blob remains
in that range; rewrite only unpublished commits. Story 13's Git LFS transition
is intended to replace this strict intermediate raw-blob refusal with the
owner-approved omission of new oversized unpublished intermediate LFS payloads
when the tip is valid; that exception is not delivered here.

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
by this direction. Skipping common AI guidance folders during assimilation is a
separate backlog outcome whose folder rules and ignore behavior await refinement.

Before image delivery, settle attachment-reference spelling so the same authored
destination works locally and in web presentation without private server IDs.
This does not change semantic Wiki-link or property-reference rules. Existing
image ownership, sharing, and references must be established before migration;
preserve image access and note learning identity throughout the transition.
