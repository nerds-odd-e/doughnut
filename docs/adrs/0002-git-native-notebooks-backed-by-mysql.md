# 0002 — Host Git-native notebooks backed by MySQL

**Status:** Proposed  
**Date:** 2026-08-04  
**Decision makers:** Terry Yin  
**Consulted:** (people / teams asked for advice)

## Context

Doughnut stores the live notebook domain in MySQL. Notes, folders, readmes,
wiki links, permissions, subscriptions, memory tracking, and other product
behavior are updated through application services and database transactions.
The current notebook export is a generated ZIP of Markdown files, and CLI sync
uses a private local baseline rather than a Git commit graph.

We want a local notebook copy to be:

- compatible with the
  [Open Knowledge Format (OKF)](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md);
- usable with normal offline Git workflows, diffs, commits, branches, and
  merges;
- able to synchronize in both directions with a remote Doughnut notebook;
- free of Doughnut note IDs, UUIDs, or other opaque stable per-note identity
  in the local Markdown tree; and
- compatible with Doughnut continuing to use MySQL as its main operational
  storage.

OKF represents a concept as a Markdown file and defines its public concept ID
as its path without the `.md` suffix. It recommends a Git repository as a
distribution form, but does not define synchronization behavior.

Git and MySQL cannot participate in one atomic transaction. A plain Git hook
is not a sufficient consistency boundary: `pre-receive` runs before Git
updates refs, while `post-receive` runs after refs are updated and cannot
change the push result. Doughnut must therefore define which system owns each
kind of state and how interrupted updates converge.

The web application also autosaves more frequently than users would normally
create Git commits. Treating every content PATCH as a Git commit would produce
noisy history and couple the web editing experience to repository operations.

## Alternatives considered

### Optional Git with filename and modified-time synchronization

Keep Git optional and infer local changes using paths and filesystem modified
times.

This has a low entry cost but has no trustworthy common ancestor, depends on
unstable timestamps, and cannot reliably distinguish edits, renames, copies,
and delete-and-recreate operations. It would require Doughnut to invent a
weaker version-control protocol while still retaining private sync state.

**Not selected for two-way synchronization.** A non-Git OKF/ZIP export may
remain available as a portability feature.

### Require a local Git repository but keep the remote non-Git-native

Use local commits as the merge baseline while translating push and pull
through bespoke Doughnut APIs.

This improves local conflict handling but does not make Doughnut a Git host:
standard Git clients cannot clone or fetch the remote without Doughnut-aware
tooling. A
[Git remote helper](https://git-scm.com/docs/gitremote-helpers.html) may use
this model as an incremental delivery step, but it is not the target
architecture.

### Make Git authoritative and rebuild MySQL from Git

Store notebook state primarily as Git trees and treat MySQL as a search and
application projection.

This is the purest Git model, but it would require substantial changes to
Doughnut's existing transactional domain behavior. It would also force web
autosave, permissions, note relationships, deletion side effects, and other
database-backed behavior through a Git-first write path.

**Not selected now.** This ADR can be superseded later if operating experience
shows that Git should become the primary notebook store.

### Allow Git and MySQL to update current notebook state independently

Treat both the Git `main` ref and current MySQL rows as writable authorities
and reconcile conflicts after the fact.

**Rejected.** This creates split-brain behavior and leaves ordinary reads
without a deterministic answer when the two stores disagree.

### Host Git repositories while retaining MySQL as the live domain authority

Give Git and MySQL distinct ownership boundaries. Git provides standard
transport and durable version history; MySQL owns the accepted live notebook
state and Doughnut-specific behavior.

**Selected.**

## Decision

### 1. One Git repository per synchronized notebook

Doughnut will work toward exposing each sync-enabled notebook as a Git
repository. The repository's working tree is the canonical OKF representation
of the notebook.

Standard clone and fetch over Git HTTPS are the target. SSH transport, pull
requests, arbitrary server-side refs, Git LFS, and branch-aware web editing are
deferred capabilities, not prerequisites for the initial Git-native host.

ZIP/OKF export may remain available for portability. A local copy that
participates in two-way synchronization must be a Git repository; Doughnut
will not offer filename-and-modified-time synchronization as an equivalent
mode.

### 2. Divide authority by concern

MySQL is authoritative for:

- current notes, folders, readmes, links, and other live notebook state;
- Doughnut permissions and product-domain behavior;
- internal note and folder identities;
- the monotonically increasing accepted notebook revision;
- the Git commit accepted as the notebook's `main`; and
- the private mapping between accepted paths and internal entities.

Git is authoritative for:

- immutable commit objects and their parent graph;
- commit authorship, timestamps, and messages; and
- non-main branch and tag refs that Doughnut chooses to support.

The Git `main` ref must correspond to the commit recorded for the accepted
MySQL notebook revision. If they disagree, the MySQL accepted-revision mapping
determines the live notebook and the Git ref is repaired. Neither system may
silently overwrite divergent state in the other.

### 3. Keep internal identity out of the working tree

The canonical Git tree will not contain a Doughnut note ID, database key,
UUID, or opaque stable identity token in Markdown frontmatter, Markdown body,
filename, or a committed sync manifest.

The OKF path is the public concept identity. Doughnut keeps its existing
internal entity identity and the accepted path-to-entity mapping privately in
MySQL.

For an incoming change:

- an unchanged path addresses the entity previously accepted at that path;
- a new path normally creates a new entity;
- deletion removes the entity at the accepted path;
- an unambiguous rename may preserve the internal entity; and
- an ambiguous delete/add or rename must be rejected for explicit resolution,
  or deliberately applied as delete plus create. Doughnut must not guess.

Filename collisions use human-readable path allocation, such as
`Recipe.md` and `Recipe (2).md`. A collision suffix must not expose the
database key, and existing surviving paths must not be renumbered merely
because another colliding note is removed.

OKF-required or author-owned semantic frontmatter, such as `type: Note`, is
permitted because it describes the concept rather than identifying the
Doughnut database entity. Unknown author frontmatter must survive a round
trip.

### 4. Convert Git changes into Doughnut domain operations

A push to the accepted branch is a proposal to change the live notebook, not
a direct filesystem write to MySQL.

Before accepting it, Doughnut must:

1. authenticate the Git user and authorize the notebook operation;
2. require the proposed history to be based on the currently accepted commit;
3. validate repository, blob, file-count, path, and canonical OKF rules;
4. compare the proposed tree with its accepted parent;
5. map the diff to Doughnut create, edit, rename/move, and delete domain
   operations;
6. execute those operations through the existing application services in a
   MySQL transaction using an expected notebook revision; and
7. reject the whole update if any commit or operation cannot be represented
   safely in the Doughnut domain.

When practical, accepted canonical commits pushed by a user retain their Git
commit IDs. Web-originated changes create server-authored commits.

Non-main branches may exist only in Git until they are merged. They do not
change the live MySQL notebook merely by being pushed.

### 5. Use explicit revision, outbox, and reconciliation state

Every MySQL transaction that changes the canonical Git view records a notebook
revision and a transactional outbox entry. An idempotent projector materializes
that revision as a Git commit and advances `main`.

Inbound pushes use an idempotent receipt/state machine so an interrupted
MySQL-write/Git-ref-update sequence can be resumed without applying the domain
change twice. The design follows the
[transactional outbox pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
rather than assuming a distributed transaction.

A reconciliation process verifies the accepted MySQL revision, canonical
rendered tree, recorded commit, and Git `main` ref. It reports or repairs
incomplete projection work. Repository reads must never advertise an
unaccepted commit as the live notebook state.

### 6. Treat Git commits as durable notebook checkpoints

MySQL may retain the web application's fine-grained autosaves. The Git
projector may coalesce them into a meaningful edit-session checkpoint after an
idle, navigation, or explicit-flush boundary.

Before a Git operation that needs the latest remote state, pending canonical
changes must be projected or the operation must clearly report that projection
is pending. The Git history represents durable notebook checkpoints; it does
not promise one commit per HTTP write.

### 7. Deliver the architecture incrementally

Implementation will be gated in this order:

1. prove a deterministic, reversible MySQL-to-OKF codec on representative
   notebooks;
2. add notebook revisions, transactional outbox, projection status, and
   reconciliation;
3. provide read-only standard Git clone/fetch from a MySQL-backed repository;
4. validate writable import through a Doughnut-aware CLI or Git remote helper;
5. expose standard Git push only after authorization, idempotency, conflict,
   and recovery behavior are proven; and
6. consider richer branch/review hosting separately.

An ADR does not itself prioritize these implementation phases.

## Consequences

- Doughnut becomes a genuine Git knowledge host without replacing the
  existing MySQL-backed product domain.
- A Git clone replaces `.doughnut-sync` baseline content for Git-based local
  workspaces; the Git commit graph supplies the common ancestor.
- MySQL remains optimized for current application behavior, while Git adds
  offline history, attribution, diffs, branches, backups, and interoperability
  with developer and agent tooling.
- The canonical OKF codec becomes a cross-cutting compatibility contract. A
  lossy or nondeterministic export is no longer acceptable.
- The system gains operational responsibility for Git object storage,
  authentication, pack validation, quotas, garbage collection, backup,
  recovery, and abuse prevention.
- Deletion and privacy policies must account for historical Git objects and
  already-created clones. Removing current MySQL rows does not erase content
  from prior commits or users' local repositories.
- Direct pushes may be rejected when the remote web notebook has advanced, an
  edit cannot be represented by Doughnut, or identity-free rename inference is
  ambiguous. Users must fetch and merge/rebase, or resolve the operation
  explicitly.
- Web autosaves and Git commits have different granularity. The UI and API must
  expose projection lag where it is user-visible.

## Pros

- Gives local users standard Git history and merge ancestry instead of a
  timestamp-based sync protocol.
- Aligns notebook distribution with OKF's recommended Git form.
- Keeps internal database identity and implementation metadata out of the
  user's Markdown.
- Preserves existing MySQL transactions, permissions, search, relationships,
  and learning features.
- Allows branches to remain cheap Git-only state until a merge affects the
  live notebook.
- Supports a staged path from read-only hosting to standard two-way Git
  without committing to a GitHub-scale feature set.

## Cons

- Introduces a consistency boundary between MySQL and Git that requires
  idempotency, projection status, and repair tooling.
- Requires an inverse importer and canonical renderer rather than the current
  one-way ZIP exporter.
- Adds repository storage and Git server operational costs.
- Identity-free Markdown makes some rename/copy cases inherently ambiguous.
- Git history complicates retention, erasure, and authorization changes after
  content has been cloned.
- A special CLI/remote-helper bridge may be needed before standard Git push is
  safe.

## Prerequisites / Assumptions

- A representative corpus can round-trip through
  `MySQL -> canonical OKF tree -> import -> MySQL -> canonical OKF tree`
  without unintended semantic or textual drift.
- The canonical codec defines title, folder/readme, link, attachment, reserved
  filename, frontmatter, duplicate-name, and filename-sanitization behavior.
- Every application mutation that affects the canonical tree can record a
  notebook revision and outbox event in the same MySQL transaction.
- Existing Doughnut authorization remains the source of Git read/write
  permissions.
- Initial repositories can constrain refs, blob sizes, file counts, pack sizes,
  and force pushes.
- Attachments may remain remote URLs initially; offline binary storage and Git
  LFS are separate decisions.
- Repository history has an explicit backup, retention, privacy, and erasure
  policy before writable hosting is generally available.

## Related

- Supersedes: (none)
- Superseded by: (none)
- Links:
  - ADR-0000 [Use Architectural Decision Records](./0000-use-adrs-accepted.md)
  - [ADR playbook](./README.md)
  - [Open Knowledge Format v0.2](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md)
  - [Git remote helpers](https://git-scm.com/docs/gitremote-helpers.html)
  - [Git hooks](https://git-scm.com/docs/githooks)
  - [Git receive-pack](https://git-scm.com/docs/git-receive-pack)
  - [Transactional outbox pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)
