# Frontmatter Aliases — Post‑plan Cleanup

Follow‑up to `frontmatter-aliases.md` (Phases 1–12 done). This plan removes dead code,
fixes one schema/query mismatch, and tidies naming/duplication found in review.

All phases are **Structure/cleanup**: no externally observable behavior changes. Each is
verified by the **existing** recall, cloze, wiki‑resolution, and alias tests staying green,
and each is independently shippable (stop‑safe). Ordered low‑risk → higher‑risk.

Targeted test commands (run only what each phase touches):

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.algorithms.NoteTitleTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.algorithms.ClozeDescriptionTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.controllers.NoteControllerTests
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.WikiLinkResolverYamlAndBodyIntegrationTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.NoteAliasIndexServiceTest
CURSOR_DEV=true nix develop -c pnpm backend:test_only --tests com.odde.doughnut.services.WikiTitleCacheServiceTest
```

For the migration phases, run the slower full verify (Flyway):

```bash
CURSOR_DEV=true nix develop -c pnpm backend:verify
```

---

## Phase 1 — Remove dead title plain‑alias matching

Status: done

Type: Structure (dead‑code removal).

- Precondition: Phase 12.1 moved recall to `NoteTitle.matchesForRecall`; `Note.matchAnswer`
  no longer calls `NoteTitle.matches`.
- Postcondition: the obsolete plain‑title‑alias matching path is gone; recall/cloze behavior
  unchanged.

Scope:
- Delete `NoteTitle.matches(String)` and `NoteTitle.getTitleAliases()`
  (`backend/.../algorithms/NoteTitle.java`). Both are production‑dead (only reachable from
  the dead `matches()` and tests).
- Keep `NoteTitle.getAliasSegmentsInOrder()` (still used by the recall parser) and
  `NoteTitle.getQualifier()` (confirm callers before touching — out of scope here).
- Remove obsolete `NoteTitleTest` cases that only exercised `getTitleAliases()`/`matches()`
  (`with_aliases`, `ascii_slash_does_not_separate_aliases`, `with_qualifier` if it asserts
  via `getTitleAliases`, `replacing`, the `getTitleAliases` whitespace case, and the
  `matches("color")` assertion in `matchesForRecall_...`). Keep the recall‑fragment and
  qualifier behavior assertions.

Tests: `NoteTitleTest`, `ClozeDescriptionTest`, `NoteControllerTests` stay green.

## Phase 2 — Rename and slim the title‑segment parser

Status: done

Type: Structure (naming + dead‑field removal).

- Precondition: migration code was removed in Phase 12.3; the parser now serves only recall
  title‑segment parsing.
- Postcondition: the class is named by capability, and its result exposes only what production
  uses; behavior unchanged.

Scope:
- Rename `TitleAliasMigrationPlan` → a capability name (e.g. `ParsedNoteTitle` or
  `RecallTitleSegments`) and rename `TitleAliasMigrationPlanTest` to match. "Migration" is
  development‑history naming and violates the repo naming rule.
- Drop the now‑unused `Result.plainAliases()` and `Result.qualifier()` components (production
  uses only `primary()` and `retainedSuffixFragments()`); simplify `from(...)` accordingly.
- Update the only production caller, `NoteTitle.getRecallTitleFragments()`.
- Trim the renamed test to the components the parser still exposes.

Tests: renamed parser test + `NoteTitleTest` + `ClozeDescriptionTest` stay green.

## Phase 3 — Drop the orphaned admin migration table

Status: skipped

Type: Structure (schema cleanup).

Deferred by developer.

- Precondition: Phase 12.3 deleted the `AdminDataMigrationProgress` entity/repository, but
  `V300000226__create_admin_data_migration_progress.sql` still creates the table on every
  environment (migrations are immutable).
- Postcondition: the unused `admin_data_migration_progress` table is dropped; nothing
  references it.

Scope:
- Add `V300000227__drop_admin_data_migration_progress.sql` (drop FK then table).
- Regenerate `docs/database-erd.md` (use the `database-erd` skill) in the same commit.

Tests: `backend:verify` (Flyway migrate clean) passes.

## Phase 4 — Consolidate alias normalization helpers

Status: done

Type: Structure (de‑duplication).

- Precondition: NFKC+lowercase normalization is implemented in
  `FrontmatterAliases.normalizedLookupKey`, but `WikiLinkResolver.dedupePreserveOrder`
  re‑implements it inline; `FrontmatterAliases.anyMatches` trims answers with `String.strip()`
  while stored aliases were trimmed via `DisplayNamePathSeparators.trimSurroundingWhitespace`.
- Postcondition: one normalization helper is reused; alias matching trims consistently;
  behavior unchanged.

Scope:
- Reuse `FrontmatterAliases.normalizedLookupKey` inside
  `WikiLinkResolver.dedupePreserveOrder`; remove the now‑unneeded `Normalizer`/`Locale`
  imports.
- Use a single trimming helper in `FrontmatterAliases.anyMatches` to match how stored aliases
  are trimmed.

Tests: `WikiLinkResolverYamlAndBodyIntegrationTest`, `NoteControllerTests` (alias cases),
`FrontmatterAliasesTest`, `NoteControllerTests` spelling cases stay green.

## Phase 5 — Align alias index schema with its query (drop write‑only `notebook_id`)

Status: planned

Type: Structure (schema/query alignment).

**Decision (settled):** Option **(b)** — keep scoping alias lookups by the note's *live*
notebook (matching the existing exact‑title resolver pattern), remove the denormalized,
write‑only `note_alias_index.notebook_id`, and add an index that matches the actual query.
Rationale: one representation (note → notebook), consistency with title resolution,
robustness if a future path changes a note's notebook, and a real index seek on
`alias_lookup_key`. (Option (a) — query by `notebook_id` using the existing composite index —
was rejected to avoid keeping a denormalization and a name→id lookup.)

- Precondition: the resolver query
  `NoteAliasIndexRepository.findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc` filters by
  `i.aliasLookupKey` and joins to the note's live notebook by name, but the table has no index
  leading with `alias_lookup_key`; `notebook_id` is written in `refreshForNote` and never read.
- Postcondition: alias lookups seek by `alias_lookup_key`; `notebook_id` column/FK/index are
  gone; resolution behavior unchanged.

Scope:
- New migration `V300000228__note_alias_index_lookup_index.sql`: drop FK
  `fk_note_alias_index_notebook`, drop index `idx_note_alias_index_notebook_lookup`, drop
  column `notebook_id`; add `KEY idx_note_alias_index_lookup (alias_lookup_key)`. Regenerate
  `docs/database-erd.md` in the same commit.
- `NoteAliasIndex`: remove the `notebook` association/getter/setter.
- `NoteAliasIndexService.refreshForNote`: drop `notebookRef`/`setNotebook`; keep the
  `notebook == null` guard only if still meaningful after the field is gone.
- `NoteAliasIndexServiceTest`: drop the `getNotebook()` assertion.
- Repository query is unchanged (already scopes by `n.notebook` name), now backed by the new
  index.

Tests: `NoteAliasIndexServiceTest`, `WikiLinkResolverYamlAndBodyIntegrationTest`,
`WikiTitleCacheServiceTest`, `NoteControllerTests` (alias cases) stay green; `backend:verify`
passes.

---

## Out of scope (reviewed, intentionally not changed)

- Backend/frontend alias‑validation duplication (regex, `[[`/`]]`, message string). Phase 11
  was deliberately skipped; cross‑language duplication is acceptable. Revisit only if more
  constrained frontmatter keys appear.
- `NoteAliasIndexRepository.findByNote_IdOrderByIdAsc` is test‑only, but consistent with the
  sibling property‑index repository; leave as is.

## Phase completion checklist (per phase)

- Existing targeted tests pass; no new behavior, so no new E2E needed.
- Remove dead code; update this plan's status.
- Commit, push, let CD deploy before the next phase.
