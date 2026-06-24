# Frontmatter Aliases (Obsidian-compatible)

## Status

Phases 1–4.2 done. Remaining phases not yet implemented.

## Goal

Migrate note aliases from the legacy title-segment form (`note.title` split on `／` / U+FF0F)
to an Obsidian-compatible frontmatter `aliases:` list, make aliases drive wiki-link resolution
with Obsidian-faithful ambiguity behavior, and migrate existing notes and their references.

## Settled decisions

- **Storage:** plain aliases (non-`~` `／` segments after the first) move to frontmatter `aliases:`.
  The primary name, the qualifier `(...)`, and `~`/`〜`/`～` cloze-suffix fragments **stay in the title**.
- **Alias YAML shape:** semantically valid `aliases` are a one-level YAML list of scalar strings,
  matching Obsidian. Early behavior phases read only valid list items and do not yet reject invalid
  shapes; later phases add explicit type/character constraints.
- **Alias normalization:** use the same matching spirit as existing title-alias behavior: trim
  surrounding whitespace, compare case-insensitively, and use the existing wiki-link normalized key
  convention where an index/dedupe key is needed (NFKC + lower case).
- **Alias characters:** aliases should follow stricter Doughnut entry rules than Obsidian's warning
  allows. Problematic wiki-link characters (`|`, `#`, `^`, `:`, `[[`, `]]`, newlines, and path
  separators) are invalid for authored aliases; early semantic readers should ignore invalid items
  rather than making them link targets.
- **What frontmatter `aliases` drives:**
  - Wiki-link resolution (new Obsidian-like behavior).
  - Spelling-recall answer matching (preserved; relocated source).
  - Cloze masking in recall content (preserved; relocated source).
  - Not quick-switcher.
  - Link insertion suggestions are a larger authoring feature and are left as a late fuzzy phase.
- **Resolution scope:** within the focus notebook by default; cross-notebook still uses the
  `Notebook:` prefix. Folder-path disambiguation is desirable but optional and intentionally late.
- **Ambiguity (Obsidian-faithful):**
  - In a future alias-aware insertion UI, picking an alias stores `[[Canonical Title|alias]]` —
    never a bare alias as the target.
  - Bare titles may later auto-qualify with the hierarchical path (`Notebook:parent/child`) on
    collision, but this requires a separate folder-path wiki target grammar.
  - A manually-typed bare ambiguous link resolves to a **deterministic first match (lowest id)**
    (Obsidian's own order is undefined; we make it deterministic).
- **Reference migration:** existing `[[colour／color]]` links are rewritten to the new primary
  title `[[colour]]` (visible text follows the primary title).
- **Migration vehicle:** additive schema via Flyway SQL; the data transform via the existing
  **admin-triggered batch migration framework** (reuse the dormant `AdminDataMigration*` pieces),
  because the transform must run live, tested domain code (title parsing, frontmatter compose,
  wiki-link rewrite) and needs dry-run, collision reporting, batching, resumability, and
  decoupling from app boot. A Flyway *Java* migration is rejected for the transform (frozen-code
  coupling, boot-blocking, non-resumable, no dry-run).
- **Collision policy (migration-only):** when stripping aliases collapses two notes to the same
  primary title within a notebook+folder, disambiguate by qualifier: lowest id keeps the bare
  title; others get `(1)`, `(2)`, …; if a qualifier already exists, extend it with a numbered
  suffix (`(animal)` → `(animal 1)`). This code lives only in the migration step and is removed
  after; the resulting `(N)` titles persist.
- **Reversibility:** no rollback machinery and no reliance on DB backup. Guarantee correctness via
  thorough tests + a dry-run/preview before the batch commits.

## Defaulted decisions (override anytime)

- **Alias editing UX:** reuse the existing frontmatter property editor for `aliases`; redirect the
  Wikidata "Append" action to write `aliases` instead of mutating the title. No dedicated field.
- **Mixed `word／~suffix` titles:** extract only non-`~` segments to frontmatter; `~` fragments stay
  joined to the primary in the title.
- **New-title `／` input:** the title editor no longer creates plain `／` aliases; `／` before a `~`
  fragment stays allowed. Enforced in the cleanup phase.
- **Type-constraint delivery:** first ship alias behavior with semantic readers/writers, then add
  alias-specific constraints, then generalize that constraint mechanism into a cohesive frontmatter
  property schema instead of prematurely building a generic type system.

## Discoveries (key code)

- Title parsing: `backend/.../algorithms/NoteTitle.java` (`getTitleAliases`, `getQualifier`,
  `matches`), `TitleFragment.java` (`~` suffix marker, cloze regexes).
- Recall: `Note.matchAnswer` → `NoteTitle.matches`; used by `NoteController` (spelling) and
  `MemoryTrackerService`.
- Cloze: `Note.createMaskedContentForRecall().hide(getNoteTitle())` → `ClozedString` /
  `ClozeReplacement.maskAliasesAndQualifier`; consumed by `RecallPrompt.getSpellingQuestion`.
  Pronunciation `/.../` masking in `maskPronunciationsAndTitles` is unrelated content masking — keep.
- Wiki resolution: `WikiLinkResolver`, `NoteRepository.findByNotebookNameAndNoteTitleOrderByIdAsc`
  (exact full-title match today), `WikiTitleCacheService.refreshForNote`, cache table
  `note_wiki_title_cache`. Rewrite: `WikiLinkRewriteService`, `WikiLinkMarkdown`.
- Frontmatter: `Frontmatter.java`, `NoteContentMarkdown` (parse/split). `aliases` is currently a
  passthrough key in `PropertyKeyNaming.isPassthroughPropertyKey` (parsed/round-tripped, not
  semantic). Frontend: `noteContentFrontmatterParse.ts`, `noteContentFrontmatter.ts`.
- Current frontmatter parsing already supports one-level YAML lists on both backend and frontend,
  but there is no domain-specific property schema or per-key type constraint yet.
- Wikidata append-to-title: `frontend/src/utils/noteTitleAliasJoiner.ts`, `wikidataTitleActions.ts`,
  `SuggestTitle.vue`.
- Title validation/normalization: `DisplayNamePathSeparators.java` (forbids `\ / :`, fullwidth
  conversion).
- Current wiki-link targets support `Notebook:Title`, not `Notebook:folder/title`; slash and colon
  handling currently belongs to path-separator validation/sanitization. Folder-path targets are
  therefore a separate grammar change, not part of the core alias migration.
- Admin migration framework (dormant, reusable; previously hosted a wiki-reference migration):
  `AdminDataMigrationController` (`/api/admin/data-migration/status`, `/run-batch`),
  `AdminDataMigrationService` (`orderedAdminDataMigrationSteps` currently empty),
  `AdminDataMigrationBatchWorker`, `AdminDataMigrationProgressPopulator`,
  `WikiReferenceMigrationStepStatus`, `AdminDataMigrationStatusDTO`; frontend
  `DataMigrationPanel.vue`, `AdminDashboardPage.vue`; e2e `adminDashboardPage.ts`.
- Java Flyway migration precedent (additive backfills calling domain code):
  `backend/src/main/java/db/migration/V300000219__*.java`, `V300000220__*.java`.
- Phase 1: `FrontmatterAliases` reads YAML-list `aliases` via `Frontmatter.getSequenceItemsIgnoreCase`;
  `Note.matchAnswer` combines title matching with `FrontmatterAliases.matchesFromNoteContent`.
- Phase 2: `Note.createMaskedContentForRecall` passes `FrontmatterAliases.fromNoteContent` into
  `ClozedString.hideAliases`; `ClozeReplacement` masks extra aliases alongside title aliases;
  qualifier and `~` fragments still come from `NoteTitle` only. `TitleFragment.mergeSortedLongestFirst`
  centralizes longest-first alias ordering for cloze.
- Phase 3: `note_alias_index` table stores `alias_display` + `alias_lookup_key` (NFKC + lower case)
  per note and notebook; `NoteAliasIndexService.refreshForNote` rebuilds on
  `WikiTitleCacheService.refreshForNote`; `FrontmatterAliases.normalizedLookupKey` shared with dedupe.
- Phase 4.1: `WikiLinkResolver` falls back to `NoteAliasIndexRepository` after exact title match;
  resolves only when exactly one note owns the alias in the notebook; qualified `Notebook:` links
  preserved.
- Phase 4.2: exact title matches win over alias matches; ambiguous alias matches resolve to the
  lowest readable note id via existing `firstReadableNotebookMatch` over alias candidates ordered by
  note id.

## Dependency / ordering rationale

The read paths (recall, cloze) must accept frontmatter aliases **before** any data moves there,
or migrated notes would lose recall/cloze. So: dual-read plumbing → alias resolution → migrate
existing data → remove legacy title-alias reading. Each behavior phase adds working capability for
newly authored frontmatter aliases before existing data is touched, so stopping early wastes nothing.

Transitional gap (accepted): between resolution shipping and the migration running, an unmigrated
note titled `colour／color` still resolves via `[[colour／color]]` (full title) but not yet via
`[[colour]]`; the migration closes this.

---

## Phase 1 — Recall accepts frontmatter aliases

Status: done

Type: Behavior.

- Precondition: a note has plain aliases in frontmatter `aliases:`.
- Trigger: the user submits a spelling answer equal to a frontmatter alias.
- Postcondition: the answer is accepted, in addition to today's title + title-alias matching.

Scope:
- Add a small backend alias reader that returns valid scalar items from a YAML-list `aliases`
  property only. Scalar `aliases: Foo`, nested lists/maps, blank values, and invalid link-target
  characters are ignored for alias behavior in this phase.
- `Note.matchAnswer` combines title-derived matching (unchanged) with frontmatter aliases.
- No change to title parsing.

Tests:
- Extend `backend/.../controllers/NoteControllerTests.java` (spelling) and
  `e2e_test/features/assimilation/assimilate_with_remembering_spelling.feature` with a note whose
  aliases are in frontmatter.

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.controllers.NoteControllerTests
```

## Phase 2 — Cloze masks frontmatter aliases

Status: done

Type: Behavior.

- Precondition: a note has frontmatter aliases mentioned in its recall content.
- Trigger: a spelling recall question is generated.
- Postcondition: frontmatter aliases are masked like title aliases are today; title qualifier and
  `~` fragments still come from the title.

Scope:
- `createMaskedContentForRecall` supplies frontmatter aliases into `ClozedString`/`ClozeReplacement`
  alongside the title's qualifier and `~` fragments.
- Reuse the same backend alias reader as Phase 1 so recall and cloze never diverge on which aliases
  count.

Tests:
- Extend `backend/.../algorithms/ClozeDescriptionTest.java` and
  `backend/.../controllers/RecallPromptControllerTests.java`.

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.algorithms.ClozeDescriptionTest
```

## Phase 3 — Alias index (structure, populated but unused)

Status: done

Type: Structure.

- Precondition: notes may carry frontmatter `aliases`.
- Trigger: a note is saved / its content changes.
- Postcondition: a reverse alias index (`alias → note` within a notebook) is maintained, with no
  observable resolution change yet (resolver still matches full title only).

Scope:
- One commit: Flyway SQL + entity/repository/service wiring + refresh hook.
- Store both display alias and normalized lookup key (trim + NFKC + lower case), scoped by notebook
  and note.
- Ignore blank/invalid/duplicate aliases for the same note; do not change resolution yet.
- Regenerate `docs/database-erd.md` in the same commit as the schema change.

Tests:
- Existing wiki-resolution and cache tests stay green; add a focused test that the index is
  populated from frontmatter aliases without changing resolution.

## Phase 4.1 — Unambiguous `[[alias]]` resolves within the notebook

Status: done

Type: Behavior.

- Precondition: note A has frontmatter alias `X`; note B links `[[X]]`.
- Trigger: wiki-link resolution.
- Postcondition: `[[X]]` resolves to A in the focus notebook and is cached.

Scope:
- Extend `WikiLinkResolver` / repository lookup to consult the alias index after exact title match,
  but cover only the single-target alias case here.
- Preserve cross-notebook `Notebook:` prefix behavior.

Tests:
- Extend `WikiLinkResolver` integration tests and a controller-level link-resolution test for a
  single alias match.

## Phase 4.2 — Title precedence and ambiguous alias resolution

Status: done

Type: Behavior.

- Precondition: aliases collide with an exact title or with each other inside a notebook.
- Trigger: wiki-link resolution.
- Postcondition: exact title matches win over alias matches; ambiguous alias matches resolve to the
  lowest readable note id.

Scope:
- Add deterministic tie-break ordering to alias lookup.
- Keep authorization filtering consistent with existing wiki-link resolution.

Tests:
- Extend resolver/controller tests for title-wins, same-notebook ambiguity, and unreadable lowest-id
  candidate skipped in favor of the first readable target.

## Phase 5.1 — Wikidata Append writes one alias list

Type: Behavior.

- Precondition: the user associates a Wikidata entity and chooses to add a label as an alias on a
  note without an existing `aliases` property.
- Trigger: the "Append" action.
- Postcondition: the label is written to frontmatter as a YAML `aliases` list, not appended to the
  title.

Scope:
- Redirect `SuggestTitle.vue` / `wikidataTitleActions.ts` Append path to frontmatter `aliases`.
- Do not remove `noteTitleAliasJoiner.ts` until no remaining caller uses it.

Tests:
- Update `frontend/tests/notes/WikidataAssociationDialog.spec.ts` for the no-existing-alias case.

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/WikidataAssociationDialog.spec.ts
```

## Phase 5.2 — Wikidata Append merges with existing aliases

Type: Behavior.

- Precondition: the user appends a Wikidata label to a note that already has a valid alias list.
- Trigger: the "Append" action.
- Postcondition: the label is added without duplicating an existing alias by normalization key.

Scope:
- Write `aliases` as a YAML list, preserving existing valid aliases and deduping by alias
  normalization key.
- `noteTitleAliasJoiner.ts` becomes unused after this phase, but removal waits for cleanup.

Tests:
- Add/adjust frontend tests for merge and dedupe.

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/WikidataAssociationDialog.spec.ts
```

## Phase 6 — Register the admin migration step

Type: Behavior.

- Precondition: the admin migration framework is dormant (`orderedAdminDataMigrationSteps` empty).
- Trigger: an admin opens the data-migration panel / requests status.
- Postcondition: a capability-named step (e.g. `title_alias_to_frontmatter`) is registered with
  accurate status/progress reporting; running a batch is still a safe no-op (no transform yet).

Scope:
- Repopulate `orderedAdminDataMigrationSteps`; wire `AdminDataMigrationService` /
  `AdminDataMigrationBatchWorker` / progress populator and step status.

Tests:
- Extend `AdminDataMigrationServiceTest`, `AdminDataMigrationControllerTest`,
  `frontend/tests/components/admin/DataMigrationPanel.spec.ts`.

## Phase 7.1 — Parse legacy title-alias migration candidates

Type: Structure.

- Precondition: legacy titles may include primary text, plain aliases, `~` suffix fragments, and a
  qualifier.
- Trigger: migration planning code inspects a note title.
- Postcondition: a migration-focused parser/result exposes primary title, plain aliases after the
  first segment, retained `~` fragments, and qualifier, without mutating data.

Scope:
- Reuse `NoteTitle` concepts where they match target semantics, but do not use
  `NoteTitle.getTitleAliases()` blindly because it includes the primary segment.
- Cover escaped fullwidth slash (`／／`), qualifiers, mixed plain/`~` segments, whitespace
  normalization, and already-primary-only titles.

Tests:
- Focused pure tests for the migration parser/result.

## Phase 7.2 — Dry-run previews note title/content changes

Type: Behavior.

- Precondition: notes still hold plain title aliases.
- Trigger: an admin requests a dry run.
- Postcondition: the admin sees, without mutation, each affected note's planned new title and
  frontmatter `aliases`.

Scope:
- Wire dry-run output through the admin migration service/controller for note title/content changes
  only.
- Include "no changes" status for notes without migratable aliases.
- Do not include reference rewrites or collision disambiguation yet.

Tests:
- Service/controller tests asserting preview output and that no note rows are mutated.

## Phase 7.3 — Dry-run reports migration-only title collisions

Type: Behavior.

- Precondition: stripping aliases would collapse two notes to the same primary title within a
  notebook+folder.
- Trigger: an admin requests a dry run.
- Postcondition: the dry-run shows the full collision list and planned `(N)` qualifier
  disambiguation without mutation.

Scope:
- Collision resolution per the migration-only policy.
- Cover existing qualifiers (`(animal)` → `(animal 1)`) and existing numbered qualifiers.

Tests:
- Service/controller tests asserting collision report and planned title resolution; no rows mutated.

## Phase 8.1 — Execute simple alias-to-frontmatter batches

Type: Behavior.

- Precondition: a confirmed dry run has non-colliding notes to migrate.
- Trigger: the admin runs batches.
- Postcondition: notes whose migrated primary title has no collision are updated in batches: plain
  aliases move to frontmatter, title becomes primary, qualifier + `~` fragments are retained.

Scope:
- Batch worker mutates `title` and `content` for non-colliding notes.
- Write frontmatter `aliases` as a YAML list and merge with any existing valid alias list, preserving
  existing frontmatter order where practical.
- Alias index (Phase 3) refreshed for migrated notes.
- Skip colliding notes for now and report them as pending collision handling.

Tests:
- Service tests for simple batch boundaries, existing alias merge, and fresh/empty DB no-op.

## Phase 8.2 — Execute collision disambiguation batches

Type: Behavior.

- Precondition: a confirmed dry run includes title collisions after alias stripping.
- Trigger: the admin runs batches.
- Postcondition: colliding migrated notes receive stable `(N)` qualifier disambiguation and are
  migrated to frontmatter aliases.

Scope:
- Apply the migration-only collision policy per notebook+folder.
- Lowest id keeps the bare title; later ids receive `(1)`, `(2)`, etc.
- Refresh alias index for changed notes.

Tests:
- Service tests for collision suffixes, existing qualifiers, deterministic ordering, and idempotent
  re-run after partial completion.

## Phase 8.3 — Migration progress and resumability hardening

Type: Behavior.

- Precondition: the transform may be interrupted between batches.
- Trigger: an admin checks status or reruns a batch after interruption.
- Postcondition: progress accurately reports completed/pending notes, and rerunning batches does not
  duplicate aliases or change already-migrated titles.

Scope:
- Tighten idempotency guards and status calculation for simple and collision paths.
- Keep the admin UI display minimal; richer UX belongs in a later admin-console pass if needed.

Tests:
- Service/controller tests for partial batches, status after interruption, and repeated run-batch
  calls.

## Phase 9.1 — Preview inbound reference rewrites

Type: Behavior.

- Precondition: notes have been migrated to primary titles.
- Trigger: an admin requests migration status/dry-run after the alias transform.
- Postcondition: the admin sees planned inbound `[[old full title]]` link rewrites, including
  `Notebook:`-qualified links, without mutation.

Scope:
- Reuse `WikiLinkMarkdown` parsing/replacement rules.
- Include whether the visible text will change for bare links; piped links keep explicit display.

Tests:
- Service/controller tests for bare, qualified, and piped old full-title links; no rows mutated.

## Phase 9.2 — Execute inbound reference rewrites and rebuild caches

Type: Behavior.

- Precondition: notes have been migrated to primary titles and reference rewrites are pending.
- Trigger: the reference-rewrite batch runs.
- Postcondition: inbound `[[old full title]]` links are rewritten to the new primary title;
  `note_wiki_title_cache` and the alias index are rebuilt; `[[alias]]` resolves to migrated notes.

Scope:
- Reuse `WikiLinkRewriteService` / `WikiLinkMarkdown`; preserve display per the rewrite-to-title rule.
  This deliberately changes visible text for bare old full-title links to the new primary title;
  piped links keep their explicit display text.
- Cover `Notebook:`-qualified links.
- Idempotent and resumable.

Tests:
- Service/controller and an e2e covering a migrated note: full-title links updated, `[[alias]]`
  resolves, recall/cloze still work.

## Phase 10.1 — Backend rejects invalid authored aliases

Type: Behavior.

- Precondition: a user saves note content containing an `aliases` property.
- Trigger: backend content save receives an invalid `aliases` value or invalid alias item.
- Postcondition: Doughnut rejects the edit with a domain-specific message: `aliases` must be a
  one-level YAML list of nonblank strings that can safely be used as wiki-link text.

Scope:
- Add alias-specific validation to backend content save paths only.
- Keep migration readers tolerant: historical invalid aliases remain ignored until edited, not made
  link targets.
- Keep the validation local and explicit; do not generalize all frontmatter property typing.
- Invalid authored aliases with `|`, `#`, `^`, `:`, `[[`, `]]`, newlines, or path separators are
  rejected instead of silently ignored.

Tests:
- Backend controller tests for saving invalid alias shapes/items.

## Phase 10.2 — Frontend editor explains alias constraints

Type: Behavior.

- Precondition: a user edits `aliases` in the existing frontmatter property editor.
- Trigger: the property value is scalar, nested, blank, duplicated, or contains invalid wiki-link
  characters.
- Postcondition: the editor blocks save or surfaces the backend error with alias-specific copy.

Scope:
- Add frontend property-row validation/copy for `aliases`.
- Keep the existing generic property editor; no dedicated alias field.

Tests:
- Frontend property editor tests for invalid alias list/scalar feedback and a valid list save.

## Phase 11 — Cohesive frontmatter property constraints

Type: Structure.

- Precondition: `aliases` has explicit local validation and other frontmatter keys still rely on
  scattered ad hoc rules.
- Trigger: refactoring validation/parsing code.
- Postcondition: the alias constraint is represented through a small cohesive frontmatter property
  schema/constraint mechanism, with no behavior change from Phase 10 and no broadened enforcement.

Scope:
- Extract a backend/frontend concept for per-property value shape and item validation, starting with
  `aliases` as the first constrained list property.
- Preserve existing structural scalar-only behavior for keys such as `image`, `wikidata_id`, and
  `title_pattern`; do not broaden enforcement beyond already-observed behavior unless paired with a
  later behavior phase.
- This phase is justified only by the existing alias behavior and upcoming cleanup: it removes
  duplicate alias-specific validation before legacy title-alias code is deleted. If the local
  alias-specific implementation from Phase 10 is already cohesive enough, skip this phase.

Tests:
- Existing alias constraint tests still pass; add focused pure tests for the property-constraint
  mapping if useful.

## Phase 12.1 — Cleanup: recall and cloze stop reading plain title aliases

Type: Behavior.

- Precondition: the migration has completed in all environments.
- Trigger: spelling recall answer matching or recall prompt generation.
- Postcondition: recall/cloze read aliases solely from frontmatter; `NoteTitle` still preserves
  qualifier and `~` suffix-fragment behavior.

Scope:
- Remove plain title-alias matching from `Note.matchAnswer` and cloze masking.
- Keep qualifier and `~` tests green.

Tests:
- Update `NoteTitleTest`, `ClozeDescriptionTest`, `NoteControllerTests`, and recall prompt tests.

## Phase 12.2 — Cleanup: title authoring no longer creates plain aliases

Type: Behavior.

- Precondition: the user edits or creates a note title.
- Trigger: the title contains plain fullwidth-slash alias syntax.
- Postcondition: Doughnut no longer treats plain `／` as authoring an alias; allowed `／` usage is
  limited to retained `~` suffix-fragment syntax or literal title text per final validation rules.

Scope:
- Remove frontend title-alias joiner usage and delete `noteTitleAliasJoiner.ts` when unused.
- Adjust title validation/normalization for the final `／` rules.
- Update fixtures/e2e that used `／` titles as aliases.

Tests:
- Frontend title/Wikidata tests and backend title validation tests.

## Phase 12.3 — Cleanup: remove temporary migration machinery

Type: Behavior.

- Precondition: all environments have completed the migration and the cleanup behavior phases are
  deployed.
- Trigger: an admin opens the data-migration panel / requests status after cleanup.
- Postcondition: the completed alias migration step is no longer offered; collision-only migration
  code is gone, and no dead legacy alias code remains.

Scope:
- Drain `orderedAdminDataMigrationSteps`; remove the step + collision code.
- Remove dead helper code and tests that only supported the migration.

Tests:
- Existing admin dashboard/data-migration tests reflect no pending step.

## Phase 13 — Fuzzy: alias-aware link insertion suggestions

Type: Behavior (placeholder; refine before execution).

- Precondition: alias resolution is live and aliases are validated.
- Trigger: the user searches/selects an alias when inserting or editing a wiki link.
- Postcondition: Doughnut offers alias matches in the link insertion UI and stores
  `[[Canonical Title|alias]]`; a selected alias is never stored as a bare target.

Notes:
- This is intentionally late and fuzzy because Doughnut does not currently have Obsidian-style
  inline `[[...]]` autocomplete. Before implementation, split this into concrete scenarios around
  the existing search dialog, selected text/dead-link relinking, and possible future inline
  suggestions.
- Quick-switcher behavior remains out of scope unless explicitly re-planned.
- This placeholder is not ready for `execute-plan`; refine it into stop-safe phases first.

Tests:
- Frontend wiki-link insert/edit tests; rich/markdown round-trip.

## Phase 14 — Fuzzy: folder-path disambiguation for colliding targets

Type: Behavior (placeholder; refine before execution).

- Precondition: two notes share a bare title or alias within the resolution scope.
- Trigger: inserting a link to the colliding title, or a new collision appearing.
- Postcondition: stored links use the shortest hierarchical path that disambiguates
  (`Notebook:parent/child`), mirroring Obsidian's "shortest path when possible".

Notes:
- This requires a new folder-path wiki target grammar and resolution rule. It is optional for the
  core alias migration and should be decomposed as its own plan before execution.
- Until this exists, ambiguous manually typed bare aliases/titles resolve deterministically to the
  lowest readable note id after exact-title precedence.
- This placeholder is not ready for `execute-plan`; refine it into stop-safe phases first.

Tests:
- Resolution/rewrite tests for collision qualification and existing-link auto-update.

## Phase completion checklist (per phase)

- Tests added/updated before or alongside implementation; targeted runs only.
- No failing tests at the boundary; `@wip` only for not-yet-passing e2e.
- Remove dead code; update this plan with discoveries.
- Commit, push, let CD deploy before the next phase.
