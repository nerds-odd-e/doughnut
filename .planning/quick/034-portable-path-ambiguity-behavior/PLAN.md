# Portable path ambiguity behavior

**Status:** deferred — not authorized for execution  
**Source:** `.planning/seeds/SEED-009-portable-path-ambiguity-resolution.md`  
**Prerequisite:** Complete `.planning/quick/032-portable-path-domain-model/PLAN.md`

## Goal

Implement the deferred SEED-009 rule:

> A shorthand Portable path resolves only when it identifies one destination
> under the documented resolution scope. Otherwise it is
> unresolved/ambiguous, and Donut asks for a longer path.

This plan owns behavior changes only after the direct Portable-path/Wiki-link
model from plan 032 exists. It must not be executed as part of plan 032.

## Requirements

- Combine display-name and recognized-alias matches in one notebook scope,
  deduplicated by note id. One resolves, zero is unresolved, and more than one
  is ambiguous.
- Notebook qualification changes scope but does not break ambiguity by database
  order.
- Exact path-shaped Portable paths continue to match folder trail plus display
  name. Property validity is checked after note resolution.
- Ambiguous is an explicit non-navigable result, distinct from missing in user
  guidance.
- Donut-authored repair, insertion, rewrite, and pasted-link conversion choose
  the display-name shorthand only when unique; otherwise they use the complete
  normalized path. An exact root note uses `/Title` when `Title` is ambiguous.
- Cross-notebook output qualifies the same shorthand-or-normalized note portion.
- Resolution-dependent indexes and graph consumers follow later title, alias,
  location, deletion/restoration, and notebook changes.
- No compatibility resolution mode, feature flag, or old/new API fields.
- Source-relative and partial-folder-suffix paths remain out of scope.

## Key design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Ambiguity | Explicit `AMBIGUOUS`, navigationally unresolved | The user needs a different repair instruction from a missing destination. |
| Candidate set | Union title and alias matches, dedupe by note id | A title match must not hide an alias collision; duplicate aliases on one note are one destination. |
| Authoring | Backend owns source-scoped Portable-path selection | Uniqueness depends on current notebook data; frontend reconstruction would duplicate the domain rule. |
| Longer path | Full normalized folder path; exact root is `/Title` | This is the smallest already-readable exact form for the otherwise unlengthenable root collision. |
| Mutation consistency | One affected-scope re-resolution operation updates the derived resolved-link index | Cache history must not decide current Portable notebook tree semantics. |
| ADR closeout | Remove plan 032's implementation-gap note only when these behaviors pass | ADR 0001/0004 must remain honest throughout deferred execution. |

## Slices

### 1. Ambiguous shorthand resolves nowhere

**Status:** planned  
**Type:** Behavior

**Precondition:** A shorthand has zero, one, or multiple distinct
display-name/alias candidates in its documented notebook scope.  
**Trigger:** Donut resolves it from body or frontmatter.  
**Postcondition:** Exactly one candidate resolves; zero is unresolved; multiple
candidates are ambiguous and create no resolved-link row or navigation.

Test first:

- Replace the lowest-note-id characterization with title/title ambiguity.
- Cover title/alias and alias/alias collisions, duplicate aliases on one note,
  notebook qualification, and a property selector at controller/resolver
  boundaries.
- Add one E2E scenario showing a duplicate-title shorthand no longer opens the
  first-created note; generic unresolved UI is acceptable until slice 3.

Implementation:

- Add `PortablePathResolution` with resolved, unresolved, and ambiguous states.
- Union and deduplicate candidates before cardinality; remove first-match
  resolution APIs.
- Make cache refresh, unresolved reporting, and rewrite lookup consume the same
  result. Exact paths do not fall back to shorthand.
- Update ADR 0004 candidate semantics only after tests pass; retain the
  implementation-gap note until the complete user interaction ships.

Verification:

- Run backend tests and the focused wiki-link E2E feature.

Stop-safe outcome: database order no longer decides a manually authored
shorthand destination.

### 2. Wiki-link resolution is explicit in the public contract

**Status:** planned  
**Type:** Structure

Extend the direct `WikiLink` contract from plan 032 for the ambiguity guidance
in slice 3, without changing presentation yet.

- Add `resolution: RESOLVED | UNRESOLVED | AMBIGUOUS` and make
  `destinationNoteId` optional.
- Return every authored Wiki link in `NoteRealm.wikiLinks`, not only the
  resolved index subset.
- Regenerate OpenAPI/generated TypeScript through `generate-api-client` and
  update frontend matching by the direct fields. Add no old-field adapters.
- Keep unresolved and ambiguous visually equivalent until slice 3.

Verification:

- Run backend and frontend tests plus generated-client and whitespace checks.

Stop-safe outcome: the UI receives the resolver's domain result directly and
can implement guidance without inference.

### 3. An ambiguous link asks for a longer Portable path

**Status:** planned  
**Type:** Behavior

**Precondition:** A rendered Wiki link has `resolution: AMBIGUOUS`.  
**Trigger:** The user follows it.  
**Postcondition:** Donut explains that several notes match and asks the user to
choose one so it can write a longer Portable path; it neither navigates nor
offers to create another candidate.

Test first:

- Extend the ambiguity E2E through the click and assert the guidance and note
  selection action.
- At a mounted component boundary, distinguish ambiguous from unresolved while
  retaining the existing create-note flow for missing links.

Implementation:

- Add an explicit ambiguous presentation and Portable-path copy.
- Route it to destination selection and suppress create-new-note.
- Do not expose unreadable notebook candidates.

Verification:

- Run frontend tests and the focused wiki-link E2E feature.

Stop-safe outcome: ambiguity is understandable and actionable.

### 4. Selecting a destination writes a longer Portable path

**Status:** planned  
**Type:** Behavior

**Precondition:** The user selects one candidate for an ambiguous shorthand.  
**Trigger:** The user confirms the destination.  
**Postcondition:** Donut replaces the shorthand with the selected note's full
normalized Portable path, preserving display text and property selector.

Test first:

- Extend the E2E to assert `[[Folder/Title|display]]` and successful navigation.
- Cover the root collision as `[[/Title]]` and one preserved property selector.

Implementation:

- Add the backend source-scoped Portable-path authoring operation. It returns
  shorthand only when unique, otherwise the full normalized path.
- Return the Portable path directly to the selection flow; frontend code only
  formats the surrounding Wiki-link spelling.
- Update ADR 0004 to make `/Title` the product-authored exact-root fallback.

Verification:

- Run backend/frontend tests and focused wiki/property E2E features.

Stop-safe outcome: every ambiguity offered for repair has an exact spelling.

### 5. Inserting a Wiki link writes the shortest unambiguous Portable path

**Status:** planned  
**Type:** Behavior

**Precondition:** The user selects a destination while inserting a Wiki link.  
**Trigger:** Donut inserts it.  
**Postcondition:** The stored path is shorthand when unique and otherwise the
full normalized path, notebook-qualified when the source scope differs.

Test first:

- Cover unique same-notebook, title/title, title/alias, root, and
  cross-notebook collisions in `wiki_link.feature`.
- Prove source-aware search returns the final `portablePath`; frontend tests do
  not reproduce uniqueness.
- Cover insertion as a property link.

Implementation:

- Reuse slice 4's authoring operation from literal/semantic/recent-note search
  and recall/overlap link insertion flows.
- Make the frontend formatter require a valid Portable path; remove title,
  notebook, empty, and unknown-source fallbacks.

Verification:

- Run backend/frontend tests and focused wiki/property E2E features.

Stop-safe outcome: newly inserted links cannot introduce known ambiguity.

### 6. Affected-scope re-resolution has one owner

**Status:** planned  
**Type:** Structure

Add one cohesive re-resolution operation to the Wiki-link service for the tree
mutation behavior in slice 7. Do not wire new mutation triggers yet.

- Given an affected Portable notebook tree/scope, re-resolve relevant authored
  links through `PortablePathResolution` and rebuild only resolved index rows.
- Reuse the existing resolved-link index; add no second lookup model, queue, or
  compatibility status.
- Preserve current behavior until slice 7 invokes the operation from mutation
  boundaries.

Verification:

- Run backend tests; existing behavior remains green.

Stop-safe outcome: one tested operation is ready for the immediate mutation
behavior without changing external results.

### 7. Resolution follows title and tree changes

**Status:** planned  
**Type:** Behavior

**Precondition:** A shorthand is currently resolved or ambiguous.  
**Trigger:** Note creation, rename, move, deletion/restoration, or notebook/tree
movement changes candidate cardinality.  
**Postcondition:** rendering, resolved-link index, inbound references, graph,
and focus context reflect the current result without editing the source note.

Test first:

- Start resolved, introduce a display-name collision, then remove/move it and
  assert ambiguous then resolved transitions.
- Assert one canonical index/graph/focus surface excludes ambiguity and
  recovers uniqueness.
- Add one E2E create/rename collision transition.

Implementation:

- Invoke slice 6's affected-scope operation from existing note/folder/notebook
  mutation boundaries.
- Never preserve a resolved row merely because it resolved historically.

Verification:

- Run backend tests and focused wiki-link E2E.

Stop-safe outcome: cache history cannot override the current tree.

### 8. Resolution follows alias changes

**Status:** planned  
**Type:** Behavior

**Precondition:** Alias candidates make a shorthand unique or ambiguous.  
**Trigger:** Authored aliases are added, removed, or changed.  
**Postcondition:** link rendering and graph/index consumers immediately reflect
the new cardinality without editing the source note.

Test first:

- Add/remove a title-colliding alias at the text-content controller boundary.
- Cover alias/alias cardinality at the resolver boundary without duplicating
  slice 7's graph assertions.

Implementation:

- Feed affected old/new alias lookup keys into the same re-resolution owner.
- Keep accidental spelling-match behavior unchanged.

Verification:

- Run backend tests.

Stop-safe outcome: aliases participate in uniqueness over time as well as at
initial resolution.

### 9. Rename and move rewrites remain unambiguous

**Status:** planned  
**Type:** Behavior

**Precondition:** A resolved link points to a note whose title, folder, or
notebook will change.  
**Trigger:** Existing reference-preserving rename or move runs.  
**Postcondition:** The rewrite still identifies that note uniquely, lengthening
or qualifying the Portable path as needed while preserving display text,
property selector, and wiki/path-Markdown spelling.

Test first:

- Extend existing rename/move E2E scenarios with a destination namesake.
- Cover one property and one path-Markdown rewrite at controller boundaries.
- Prove already-ambiguous markup is not guessed and rewritten.

Implementation:

- Use the shared Portable-path authoring operation after the new tree location
  is known rather than rewriting a title string in isolation.

Verification:

- Run backend/frontend tests and the three focused link E2E features.

Stop-safe outcome: Donut maintenance cannot rewrite a good link into an
ambiguous shorthand.

### 10. Pasted SPA links store the same Portable path

**Status:** planned  
**Type:** Behavior

**Precondition:** A user pastes a `noteShow` or `noteProperty` URL whose note
has a unique or colliding display name in the source scope.  
**Trigger:** Donut converts the internal URL to notebook markup.  
**Postcondition:** The stored Wiki link uses the same backend-authored shortest
unambiguous Portable path as insertion, with separate display text and one
encoded property selector.

Test first:

- Extend the mounted paste behavior with same-notebook, root, and
  cross-notebook property collisions.
- Retain one unresolved URL case that leaves ordinary Markdown unchanged.

Implementation:

- Replace frontend note-identity reconstruction with the shared source-aware
  backend authoring result.
- Reconcile ADR 0005's paste wording; preserve Proposed status.

Verification:

- Run frontend tests, lint, and whitespace checks.

Stop-safe outcome: paste, insertion, repair, and rewrite agree on spelling.

## Completion gates

- Remove the implementation-gap note added by plan 032 only after all
  ambiguity behavior is green; leave ADR 0001/0004 stating the live rule.
- Run backend verify, frontend tests, the three focused note-topology E2E
  features, lint, and whitespace checks.
- Retire or update SEED-009 only after the full plan is shipped.

## Slice wrap-up contract

For every executed slice: run its red-to-green cycle, run
`post-change-refactor`, update this plan, run listed verification, then commit
and push. This plan stays deferred until separately authorized.
