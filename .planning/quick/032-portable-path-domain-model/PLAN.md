# Portable path domain vocabulary alignment

**Status:** planned

## Goal

Make the implementation, persistence model, OpenAPI contract, generated
client, frontend, tests, and ADR wording use **Portable path** and **Wiki link**
directly, without compatibility DTOs, deprecated fields, or adapters translating
from `targetToken`, `WikiTitle`, “concept path,” or wiki-title cache concepts.

This plan is behavior-preserving. It does not implement SEED-009. Current link
resolution, authoring, rendering, repair, rewrite, and cache-refresh behavior
must remain externally unchanged while the model and names are aligned.

## Scope boundary

The following are explicitly excluded and live in the separate deferred plan
`.planning/quick/034-portable-path-ambiguity-behavior/PLAN.md`:

- rejecting a shorthand when several title/alias candidates match;
- distinguishing ambiguity from the current generic unresolved/dead state;
- asking the user for a longer Portable path;
- authoring a shortest unambiguous path for insert, repair, rewrite, or paste;
- re-resolving links when later title, alias, or tree mutations change candidate
  cardinality;
- changing the current title-before-alias and repository-order resolution rules.

Do not opportunistically implement any of those behaviors while executing this
plan.

## Requirements

- `PortablePath` is the one code value for a notebook-link destination. It owns
  the optional notebook qualifier, shorthand/path-shaped note portion, and
  optional encoded `#prop:` selector.
- `WikiLink` owns an authored link, its Portable path, display text, and the
  resolved destination note id currently exposed to the frontend.
- The existing resolved-only API semantics remain: `NoteRealm` continues to
  return resolved links; unresolved markup is still detected by the existing
  frontend leftover-markup flow.
- Wiki and path Markdown remain authored spellings of the same Portable path.
  No stored-spelling conversion is introduced.
- Existing link navigation still compiles the resolved destination note id and
  optional decoded property key to ADR 0005 named SPA locations. A Portable
  path never becomes a browser `href` directly.
- The database remains one derived resolved-link index for both spellings. Its
  schema and Java names change in place; its refresh semantics do not.
- Old and new names never coexist in production code or the API. The Flyway
  migration may mention historical schema names solely as the source of an
  in-place rename.
- Relationship-note `source` and `target` remain valid relationship roles. The
  cleanup is scoped to link-destination translations, not every occurrence of
  the English word `target`.
- ADR status remains human-owned. This work changes wording for accuracy but
  does not accept, reject, or supersede an ADR.

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Path model | Merge `WikiLinkAuthoredTarget` and `WikiLinkTargetReference` into `PortablePath` | Qualifier, note path, and property selector are parts of the single ADR 0001 value. |
| Public link model | `WikiLink { authoredLink, portablePath, displayText, destinationNoteId }` | It directly names the current resolved-only payload without adding SEED-009 resolution states. |
| API cutover | Rename Java JSON and generated TypeScript in one compile-safe slice | Accepting both field sets would create the translation layer the user rejected. |
| Persistence | Rename to a `resolved_wiki_link` index with source, destination, and authored-link columns | This describes what the rows already mean; it does not redesign cache behavior. |
| ADR honesty | Retain the chosen ambiguity rule but label its enforcement as an implementation gap tracked by SEED-009 | The ADR remains the desired domain decision without falsely claiming the deferred behavior is live. |
| Root path documentation | Document that the reader already accepts leading `/` in path-shaped wiki destinations; product authoring remains unchanged | This reconciles ADR 0004 with current parsing without adopting the future longer-path behavior. |

## Discoveries affecting execution

- The backend represents one Portable path with two types:
  `WikiLinkAuthoredTarget` owns the property suffix, while
  `WikiLinkTargetReference` owns notebook qualification and the note portion.
- The frontend duplicates the property part as `wikiLinkAuthoredTarget.ts` and
  calls the destination `targetToken`.
- `WikiTitle` is not a title. It is the resolved Wiki-link payload sent in
  `NoteRealm.wikiTitles`.
- The resolved-link persistence subsystem is named
  `NoteWikiTitleCache`/`WikiTitleCacheService` and physically stores
  `(note_id, target_note_id, link_text)`.
- The parser already accepts `/Title` and `/Folder/Title` as path-shaped wiki
  destinations, although ADR 0004 currently says wiki bundle-root paths have no
  leading slash.
- ADR 0001 still describes Property identity with “concept path” and shows
  `[[target]]`; these are vocabulary errors independent of SEED-009.
- ADR 0004 currently states the intended ambiguity rule as though it is live.
  The current resolver instead prefers title matches over aliases and selects
  the first repository-ordered readable candidate. This plan must disclose the
  gap, not change that behavior or endorse it as the domain rule.

## Slices

### 1. One backend Portable path value

**Status:** planned  
**Type:** Structure

Replace the two overlapping backend path types with one `PortablePath` value
without changing parsing, resolution, or rewrite results. This structure is
verified by the existing-behavior slice immediately following it.

- Merge `WikiLinkAuthoredTarget` and `WikiLinkTargetReference` into
  `PortablePath` under `backend/.../algorithms/`.
- Make it own notebook qualification, shorthand/path-shaped note spelling,
  optional `.md`, the encoded property selector, property encoding/decoding,
  and existing rename/folder/notebook rewrite operations.
- Change `WikiLinkMarkdown` split results and link-specific callers from
  `target`/`targetToken`/`noteTarget` to `portablePath` and `displayText`.
- Move the existing pure contract examples into `PortablePathTest`; delete the
  two retired types and tests in the same slice. Add no forwarding methods.
- Preserve the current resolver's title-before-alias and repository-order
  behavior. Do not introduce a resolution-status type.
- Correct ADR 0001's Wiki-link examples from `[[target]]` to
  `[[portable-path]]` and Property identity from “concept path” to Portable
  path. These are vocabulary corrections, not behavior claims.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.

Stop-safe outcome: backend code has one Portable-path value and all existing
behavior remains green.

### 2. Existing Portable path spellings still resolve and rewrite

**Status:** planned  
**Type:** Behavior

**Precondition:** A note contains a currently supported shorthand, qualified,
folder-path, root-path, path-Markdown, or property Portable path.  
**Trigger:** Donut resolves, renders, or performs an existing rename/move
rewrite.  
**Postcondition:** It reaches and rewrites the same note/property as before the
type merge and preserves the authored wiki/path-Markdown spelling.

Test work:

- Keep existing high-level controller and E2E coverage for shorthand,
  qualification, folder paths, path Markdown, properties, rename, and move.
- Add one missing regression for the already-supported `[[/Title]]` reader
  form before documenting it. Do not make product insertion author that form.
- Retain the existing lowest-id/title-before-alias characterization until the
  deferred plan replaces it; do not add broader assertions that further
  entrench the behavior.

ADR reconciliation:

- Update ADR 0004 to say the reader accepts a leading `/` on path-shaped wiki
  destinations as bundle-root spelling while Donut-authored wiki currently
  omits it.
- Add a concise implementation-status note beside the ambiguity decision:
  current enforcement is deferred under SEED-009, and the live resolver still
  applies its existing deterministic first-match behavior. Do not recast that
  implementation gap as the desired domain rule.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
- Run the focused wiki-link, path-Markdown-link, and property-wiki-link E2E
  features.

Stop-safe outcome: the renamed path model and ADR spelling description are
backed by unchanged external behavior.

### 3. One resolved Wiki-link contract from backend to frontend

**Status:** planned  
**Type:** Structure

Cut the public/rendering vocabulary over in one compile-safe change. The
following slice verifies that the existing resolved/dead rendering behavior did
not change.

- Replace backend `WikiTitle` with `WikiLink` and expose
  `NoteRealm.wikiLinks`.
- Rename its fields to `authoredLink`, `portablePath`, `displayText`, and
  `destinationNoteId`. Keep all required and keep the list resolved-only.
- Regenerate OpenAPI and `packages/generated/donut-backend-api` with the
  `generate-api-client` skill; never hand-edit generated artifacts.
- Rename frontend `WikiTitle`, `wikiTitles`, `targetToken`, and concept-path
  helpers/props/fixtures/DOM attributes to Wiki-link and Portable-path nouns.
- Replace `wikiLinkAuthoredTarget.ts` with `portablePath.ts`, preserving the
  exact existing property encoding and parsing behavior.
- Do not add `resolution`, return unresolved/ambiguous entries, change dead-link
  copy, or alter repair/insertion path selection.
- Reconcile ADR 0005 terminology if a live API/UI name is described there;
  preserve its Proposed status and routing behavior.

Verification:

- Run the `generate-api-client` workflow with
  `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- Run `scripts/check_diff_whitespace.sh`.

Stop-safe outcome: Java JSON, generated TypeScript, frontend code, DOM data,
and tests use the domain vocabulary directly, with no old/new adapter.

### 4. Existing live and dead Wiki links behave unchanged

**Status:** planned  
**Type:** Behavior

**Precondition:** Rich content contains a currently resolved or unresolved wiki
or path-Markdown link.  
**Trigger:** The user renders, follows, creates from, or points the link at an
existing note.  
**Postcondition:** Resolved links navigate through the same named SPA location;
unresolved links retain the same dead/pending UI and repair/create behavior.

Test work:

- Run and, only where the vocabulary cutover exposes a coverage gap, extend the
  existing `wiki_link.feature`, `path_markdown_link.feature`, and
  `property_wiki_link.feature` scenarios.
- At mounted frontend boundaries, assert `portablePath` replaces the old DOM
  data/API field while user-visible rendering and actions remain identical.
- Do not add an ambiguous state or longer-path instruction.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- Run the three focused note-topology E2E features.

Stop-safe outcome: the direct public model is a pure vocabulary cutover from a
user's perspective.

### 5. The resolved Wiki-link index uses direct domain names

**Status:** planned  
**Type:** Structure

Rename the derived persistence/index subsystem in place without changing which
rows are stored or when they refresh. The next slice verifies all current index
consumers.

- Add one Flyway migration after `V300000305` that renames
  `note_wiki_title_cache` to `resolved_wiki_link` and its columns/constraints
  to `source_note_id`, `destination_note_id`, and `authored_link`.
- Rename the entity and repository to `ResolvedWikiLink` and
  `ResolvedWikiLinkRepository`; rename `WikiTitleCacheService` and refresh
  collaborators to cohesive Wiki-link service names.
- Update graph, focus-context, rewrite, construction, deletion, controller,
  testability, and test consumers mechanically in the same cutover.
- Keep the same resolved-only rows, uniqueness, authorization checks,
  property-staleness checks, ordering, and refresh triggers.
- Add no compatibility view, legacy entity/bean, or second cache.
- Update ADR 0004's physical tuple to
  `(source_note, destination_note, authored_link)` and continue to describe one
  derived resolved-link index for both spellings.
- Run the `database-erd` skill after migration; do not hand-edit the Mermaid
  ERD.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
- Run the `database-erd` workflow with
  `CURSOR_DEV=true nix develop -c pnpm export:database-erd`.
- Run `scripts/check_diff_whitespace.sh`.

Stop-safe outcome: schema and Java persistence names describe the domain
directly while preserving existing rows and behavior.

### 6. Existing resolved-link consumers survive the index rename

**Status:** planned  
**Type:** Behavior

**Precondition:** A note has currently resolved outbound Wiki links and inbound
referrers, including a property link.  
**Trigger:** Donut builds its note realm, focus context, graph neighborhood, or
performs an existing reference-preserving rewrite.  
**Postcondition:** The same destinations, references, and rewritten authored
links are returned after the schema/service cutover.

Test work:

- Use existing controller/service stable boundaries to cover one canonical
  outbound link, deduplicated inbound references, one focus-context traversal,
  and stale-property exclusion.
- Run existing rename/move rewrite coverage without adding namesake/ambiguity
  preconditions.
- Verify the migration preserves pre-existing resolved-link rows; do not test
  any new re-resolution trigger.

Verification:

- Run `CURSOR_DEV=true nix develop -c pnpm backend:verify`.
- Run the focused wiki-link and property-wiki-link E2E features.
- Run `CURSOR_DEV=true nix develop -c pnpm lint:all`.
- Run `scripts/check_diff_whitespace.sh`.

Stop-safe outcome: all current consumers use the directly named index with no
observable change.

## Completion gates

- Re-read Accepted ADR 0001 and ADR 0004 and Proposed ADR 0002/0005. Confirm
  the documents distinguish the adopted Portable-path model from the deferred
  SEED-009 enforcement gap and make no new behavior claim.
- Search production code, tests, generated API, and the four ADRs for
  `WikiTitle`, `wikiTitles`, `targetToken`, `WikiLinkTargetReference`,
  `WikiLinkAuthoredTarget`, `NoteWikiTitleCache`, and “concept path.” Expect no
  live implementation occurrences. Historical schema names are allowed only
  in the Flyway rename; the ADR implementation-gap note may name SEED-009.
- Review remaining `target` names manually. Keep relationship source/target and
  genuine framework roles; remove only Wiki-link destination translations.
- Confirm SEED-009 and plan 034 remain deferred and untouched by execution of
  this plan.
- Confirm OpenAPI and `docs/database-erd.md` were generated, not hand-edited.

## Slice wrap-up contract

For every executed slice: keep existing behavior green, run
`post-change-refactor`, update this plan with concise learnings, run the listed
verification, then commit and push before the next slice. Do not start plan 034
as part of this execution. After the final slice, clean up spent planning
history according to `planning.mdc`.
