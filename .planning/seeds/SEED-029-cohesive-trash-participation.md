---
id: SEED-029
status: dormant
planted: 2026-09-18
planted_during: Learning-session false ambiguity diagnosis and owner review of trash cohesion
trigger_when: selecting internal improvements to trash availability and note collection boundaries
scope: M
---

# SEED-029: Cohesive trash participation across note consumers

## Why This Matters

For Donut maintainers, choosing a collection of notes should make its trash
semantics explicit and reuse the existing domain rule. Notebook owners benefit
from consistent exclusion from learning and search while their recoverable
content and learning history remain preserved.

The owner selected this as an internal structural improvement and requested
first backlog priority. It is not a new user-facing feature or permission to
change trash policy. Evaluation is through code cohesion and preservation of
existing product contracts, rather than a new UI outcome.

The motivating report was rejected as an ambiguous `なにしろ` title. Read-only
development inspection found an available note (4884) and a trashed note (128)
in notebook 1, Japanese learning. LearningSessionService counted both titles.
Commit `4ea717b3de88c233fceab5bafc08f0323ad28f0f` on
`codex/fix-learning-session-ambiguous-note` fixes that regression and has a
passing controller regression test. At refinement, main is `bedb7517be` and
does not contain that fix. Reconcile that branch before later execution;
do not duplicate its repair or assume it is integrated.

## Alternatives and Decision

The user selected improving cohesion around trash. A call-site fix alone
restores submission but leaves an easy-to-misuse collection boundary. Renaming
that boundary is the smallest promising structural improvement; assess its
callers and remaining rule duplication to establish whether naming alone is
sufficient. Do not assume a new service, generic filter framework, or one query
for all purposes is needed.

Reuse established availability semantics. Persisted content and participating
content have different purposes: export and synchronization must include trash,
whereas learning, search, and wiki-link destination matching must exclude it.
Centralization must preserve that distinction.

## Story Decomposition

<a id="story-1"></a>

### 1. Make trash participation rules cohesive across note consumers

#### Goal

Give maintainers explicit, consistently used boundaries for all stored notes
and notes available for participation, with trash semantics owned by the
existing domain model. A review should be able to establish why each affected
consumer includes or excludes trash without reconstructing a special local
rule.

#### Scope

- Clarify collection names and contracts, particularly `findLiveNotes...` and
  `findLiveNoteFolderIds...`, whose queries include trash. Trace affected
  callers and naming through export, Git snapshots, learning, and health.
- Assess whether consumers use the shared availability boundary consistently.
  Consolidate only demonstrated duplicate or bypassed domain knowledge.
- Examine the learning-session branch's split between recognized titles and
  participating candidates. Preserve its regression protection. Determine
  whether the diagnostic distinction needs both collections before choosing
  a structural simplification.
- Assess the representations of root-trash membership in database queries,
  current in-memory ancestry, folder creation, and frontend presentation.
  Document necessary differences and remove unjustified duplication when
  the same domain responsibility can use an existing owner.
- Preserve IDs, learning history, independent tracking preferences, recovery,
  Portable content, and transaction visibility. Do not broaden the work into
  deletion lifecycle redesign, a new API, or schema redesign by default.
- Existing rejection wording is observable behavior. The previous suggestion
  to change a trashed-only report from “No commissioned memory tracker” to
  “Note title not found” is a separate behavior decision, not accepted merely
  by selecting this internal improvement.

#### Key examples and evaluation

- One available commissioned note and a same-title trashed note: report
  submission resolves the available note using shared participation rules.
- Two available same-title notes: the existing ambiguity rejection remains.
- A note beneath a case-insensitive notebook-root `_trash`, including nested
  descendants, is excluded from learning/search/wiki matching. A folder named
  `_trash` beneath an ordinary root is not the reserved trash root.
- Moving a folder into and out of trash updates membership during the current
  transaction; object checks must not become stale database snapshots.
- Export/synchronization still includes trashed files. Recovery preserves note
  identity, learning history, and prior tracking preferences.
- Structural review can map each affected collection and consumer to either
  stored-content or participation semantics; it finds no new local trash
  detector where an appropriate shared solution already exists.

These are preservation examples and review criteria, not an executable test
plan. Reuse adequate existing proof; add only missing observations for changes.

- **Value / learning:** Reduce future wrong-query selection and determine
  which apparent duplicate representations have distinct lifecycle needs.
- **Effort hypothesis:** M (1–2 hours), low confidence until the caller scope
  and diagnostic contract are settled. Cross-layer redesign is not presumed.
- **Depends on:** Reconcile the existing bug-fix branch and its regression
  proof with the execution baseline; no unrelated feature prerequisite.
- **Safe stopping point:** Clear shared boundaries and verified preservation
  of their existing consumers remain useful without later trash redesign.

#### Initial existing-solution findings

| Area | Evidence | Refinement implication |
| --- | --- | --- |
| Query availability | `Note.JPA_AVAILABLE` / `NATIVE_AVAILABLE`, `NoteRepository.findAvailableNotesByNotebookIdOrderByIdAsc`; search, MemoryTrackerRepository, semantic search, alias and property queries reuse them | An existing solution is available; evaluate caller selection before adding another filter. |
| All persisted notes | `NoteRepository.findLiveNotesByNotebookIdOrderByIdAsc` has no availability predicate; NotebookExportRows and NotebookGitStateLoader consume it | The name is misleading after removal of note soft deletion. Preserve inclusion and clarify the boundary. |
| Health occupancy | EmptyFolderHealthRule and ReadmeOnlyFolderHealthRule use `findLiveNoteFolderIdsByNotebookId` | Occupancy and eligibility are different questions; establish their contracts before filtering. |
| Learning-session fix | LearningSessionService in commit `4ea717b3de` recognizes all titles but computes ambiguity and finds trackers from available notes | Inspect whether diagnostic compatibility justifies two collections; do not silently turn an error-message change into refactoring. |
| Membership representations | Folder.isTrashed follows current ancestry; Note delegates to Folder; `trashed_folder` view supplies persisted membership to formulas | Both model the same root rule, but observe different state moments. FolderRepositoryTest already proves query membership and current-transaction ancestry. |
| Root naming and presentation | FolderConstructionService creates `_trash`; frontend `utils/folderTrash.ts` is shared by NoteShow, FolderPage, FolderSettings | Assess shared ownership within each runtime; literal repetition across SQL/Java/TypeScript alone does not prove a new cross-runtime abstraction is warranted. |

This is a bounded initial assessment, not proof that every product consumer is
correct. No additional defect is claimed from a name or search result alone.

#### Architectural constraints

[ADR 0001](../../docs/adrs/0001-ubiquitous-language.md) defines trash as
recoverable movement retaining identity and distinguishes permanent deletion.
[ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
defines root/descendant membership, Portable preservation, learning/search
exclusion, and exclusion from wiki-link ambiguity checks. Both are Accepted.
Their rules remain authoritative; no new ADR or exception is proposed.

## Ordering and Scope Reduction

First in the product backlog by explicit owner instruction. Preserve the
existing near-future direction and other queued work. The internal improvement
supports that direction by keeping recoverable Portable content separate from
learning participation. It remains queued during refinement; no implementation
or slice plan is authorized by this artifact.

## Open Decisions

- Complete the assessment of whether explicit collection contracts and caller
  alignment suffice, or an additional shared responsibility is justified.
- Resolve the learning-session diagnostic compatibility only if its removal
  is proposed; until then preserve the observable behavior of the repaired
  baseline.
- The existing next queue entry references SEED-027, but its seed file is
  absent in this checkout and was not found in available Git history. Its
  potential overlap cannot be verified. Preserve its entry; recover its
  canonical scope before deciding to combine, replace, or execute related work.

## When to Surface

Now: first-priority internal refinement selected by the owner after reviewing
the trash-related learning-session regression.

## Breadcrumbs

- Owner discussion, 2026-09-18: capture first, refine internal trash cohesion.
- [Product backlog](../PRODUCT-BACKLOG.md).
- Bug repair and regression evidence: commit `4ea717b3de88c233fceab5bafc08f0323ad28f0f`.
- Existing preservation proofs: LearningSessionRecordTests,
  FolderRepositoryTest, NoteTrashRecoveryLearningPreferencesTest,
  NotebookGitProposalFolderRelocationTrashRoundTripControllerTest.
