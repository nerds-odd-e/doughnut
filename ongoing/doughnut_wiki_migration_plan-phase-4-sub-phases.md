# Doughnut Wiki Migration Plan - Phase 4 Sub-Phases

## Purpose

This document decomposes **Phase 4 - Introduce Note Properties** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 4 Target

After Phase 4:

- notes persist properties as leading YAML frontmatter in Markdown details
- markdown editing shows and saves the persisted Markdown details directly, including frontmatter
- rich editing shows the same properties above the Quill body as editable key-value rows
- users can insert, remove, and edit properties in rich mode
- saving and reloading from either editing surface keeps frontmatter and body content consistent
- API consumers continue to use the existing note details representation

## Key Decisions

- Do not add a backend properties column or API field in Phase 4. The persisted `details` text is the source of truth and may include leading YAML frontmatter.
- The backend should preserve frontmatter as opaque Markdown details. Frontend rich editing derives property rows from the leading frontmatter and writes changes back into that same Markdown text.
- Support simple scalar frontmatter values in the rich property UI for Phase 4. Nested maps, arrays, duplicate keys, and ambiguous YAML should be rejected or surfaced before rich-mode property editing silently misrepresents them.
- Do not introduce a separate note-properties endpoint. Properties belong to the same note save experience as title/details editing through the existing details update path.
- Extend the existing capability-owned note editing E2E coverage, for example `e2e_test/features/note_creation_and_update/note_edit.feature`; do not create phase-named feature files.
- Keep page object additions capability-named, such as property editing helpers on `notePage`, not phase-specific helpers.
- Run targeted specs only, normally `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`.
- Sub-phases **4.11–4.14** do **not** add new E2E scenarios; cover removal, validation edge cases, and export frontmatter with **high-level unit tests** (frontend compose/UI and backend export assertions). **4.15** includes an **E2E dedup/simplify** pass on existing note-edit scenarios so the suite stays minimal relative to `ongoing/doughnut_wiki_architecture_north_star.md`.

## Status

All sub-phases are **planned**.

## Sub-Phases

### 4.1 Add the Markdown Properties E2E Scenario

**Type:** Behavior

Define the markdown-mode user behavior before implementation.

**Commit includes:**

- add an `@wip` scenario to the note editing feature for editing note properties in markdown mode
- scenario shape: open an existing note, switch to markdown, add a YAML frontmatter block with at least two scalar properties, save/reload, and assert the same properties appear when editing as markdown again
- assert the rendered rich body still shows the note content without rendering the frontmatter as body text
- add only the minimal page object steps needed for the scenario wording
- no production behavior change

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
- confirm the scenario fails for missing property/frontmatter behavior, not a typo or selector issue

### 4.2 Preserve Frontmatter Through the Existing Details Contract

**Type:** Structure

Confirm the backend already persists frontmatter as opaque note details, without adding a separate property contract.

**Commit includes:**

- no database migration
- no new note read/update DTO property field
- controller-level or high-level backend test proves updating note details with leading YAML frontmatter reads back the same Markdown text
- existing title/details behavior remains unchanged

**Verification:**

- targeted backend note update/read tests

### 4.3 Add Frontmatter Parsing and Serialization

**Type:** Structure

Introduce the shared frontend conversion rules used by rich editing without wiring them into the UI yet.

**Commit includes:**

- a cohesive helper that parses Markdown details into `{ properties, body }` and composes `{ properties, body }` back into Markdown details with leading frontmatter
- unit tests for no frontmatter, simple scalar frontmatter, body beginning after frontmatter, empty frontmatter, duplicate keys, non-scalar values, and malformed YAML
- invalid or unsupported frontmatter returns a validation-shaped failure for the rich editing surface rather than silently dropping data
- no production UI behavior change

**Verification:**

- targeted frontend unit tests for the conversion helper

### 4.4 Wire Rich Rendering to Body-Only Markdown

**Type:** Behavior

The markdown E2E scenario from 4.1 passes for persisted frontmatter and rich body rendering.

**Commit includes:**

- markdown mode continues to display and save the full persisted details, including frontmatter
- rich mode renders only the parsed body content, not the leading frontmatter
- saving rich body edits preserves existing frontmatter by composing it back with the edited body before calling the existing update API
- remove `@wip` from the markdown properties E2E scenario once it passes
- keep the first implementation scoped to scalar string values for rich property parsing

**Verification:**

- targeted frontend tests for body/frontmatter composition if needed
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`

### 4.5 Show Read-Only Rich Properties Above the Body

**Type:** Behavior

Users can see existing frontmatter properties in rich mode before editing them there.

**Commit includes:**

- render a `Properties` section above the rich markdown editor when the note details have supported frontmatter properties
- display key and value text for each property
- add or update a frontend component test around note details editing/rendering
- no add/edit/remove controls yet

**Verification:**

- targeted frontend component test

### 4.6 Add the Rich Insert Property E2E Scenario

**Type:** Behavior

Define the rich-mode behavior for adding a new property row.

**Commit includes:**

- add an `@wip` scenario to the note editing feature for inserting a property in rich mode
- scenario shape: open a note in rich mode, add a property key/value above the editor, save/reload, assert the property row is visible in rich mode, then switch to markdown and assert matching frontmatter
- add only the page object helpers needed for adding and asserting property rows
- no production behavior change

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
- confirm the scenario fails because the rich properties UI does not exist yet

### 4.7 Prepare Rich Property Editing State

**Type:** Structure

Prepare the property row state used by insert/edit/remove controls without adding controls yet.

**Commit includes:**

- keep the rich details component's editable property rows derived from parsed frontmatter
- compose property row changes back into the Markdown details draft with the existing body content
- add focused frontend tests for composing inserted, renamed, and removed rows into frontmatter
- no new visible controls yet

**Verification:**

- targeted frontend component tests

### 4.8 Add Rich-Mode Property Insertion

**Type:** Behavior

The rich insert E2E scenario from 4.6 passes.

**Commit includes:**

- add an inline control for adding a property row in rich mode
- new row edits update the frontmatter in the same note details save path
- save/reload shows the inserted property in rich mode and frontmatter in markdown mode
- remove `@wip` from the rich insert E2E scenario once it passes

**Verification:**

- targeted frontend component test for adding a property
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`

### 4.9 Add the Rich Edit Property E2E Scenario

**Type:** Behavior

Define the rich-mode behavior for editing an existing property key and value.

**Commit includes:**

- add an `@wip` scenario to the note editing feature for editing a property in rich mode
- scenario shape: start with a note that has a property, edit the key and value in rich mode, save/reload, assert the old key is gone and the new key/value appears, then assert markdown frontmatter matches
- no production behavior change

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
- confirm the scenario fails because rich rows are not editable yet

### 4.10 Add Rich-Mode Property Editing

**Type:** Behavior

The rich edit E2E scenario from 4.9 passes.

**Commit includes:**

- make rich property keys and values editable inline
- changing a key renames the property without leaving the old key behind
- validation prevents empty keys and duplicate keys through the visible save path
- remove `@wip` from the rich edit E2E scenario once it passes

**Verification:**

- targeted frontend component test for editing key/value and duplicate-key validation
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`

### 4.11 Specify Rich Property Removal Behavior (High-Level UT)

**Type:** Behavior

Define removal behavior without adding a new E2E scenario. Earlier phases already exercise save/reload and markdown parity via `note_edit.feature`; removal is specified and locked with **high-level unit tests** (frontend: compose/remove paths, empty-properties UI state, frontmatter after removing one of several keys).

**Commit includes:**

- high-level frontend tests: starting from parsed multi-property frontmatter, removing a row yields composed Markdown that excludes the removed key and retains the rest; empty row list matches empty or omitted frontmatter per product rules
- no new `@wip` scenario and no new scenario rows in `note_edit.feature`
- no production behavior change beyond what tests require to compile against public APIs

**Verification:**

- targeted frontend unit or focused component tests for removal composition (same layer as 4.3 / 4.7 helpers)

### 4.12 Add Rich-Mode Property Removal

**Type:** Behavior

Implement removal to satisfy the behavior captured in 4.11’s tests.

**Commit includes:**

- add a remove control for each rich property row
- removing a row updates the frontmatter in the same note details save path
- remove the `Properties` section, or show an empty add-only state, when no properties remain
- extend high-level UT from 4.11 so they pass against the wired UI (still no new dedicated E2E for removal)

**Verification:**

- targeted frontend component test or high-level UT for the remove control and composed save payload
- regression: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature` (existing scenarios only; confirms no regressions)

### 4.13 Close Property Validation Gaps

**Type:** Behavior

Users get clear failures for property shapes this phase cannot safely edit in rich mode.

**Commit includes:**

- frontend tests for unsupported YAML values, duplicate keys, empty keys, and malformed frontmatter
- frontend displays save errors near the markdown editor or properties list without corrupting existing note content
- no new property value types beyond scalar strings
- no new E2E scenarios; validation stays covered by unit/integration-style frontend tests

**Verification:**

- targeted frontend validation tests when UI error handling changes

### 4.14 Include Properties in Obsidian Export

**Type:** Behavior

Exported markdown preserves the same persisted frontmatter from note details.

**Commit includes:**

- extend or verify the existing notebook export behavior so note details frontmatter is included in exported Markdown
- keep existing export metadata that is still needed without introducing a separate property map
- assert export output with a **high-level backend export test** and/or a focused export-format unit test (Markdown string includes leading YAML from persisted details)—do **not** add a new notebook export E2E scenario for frontmatter; rely on UT unless an existing E2E already asserts export shape and only needs a narrow assertion tweak

**Verification:**

- targeted backend or frontend export test (UT-level), not an expanded Cypress surface for this sub-phase

### 4.15 Phase 4 Cleanup, E2E Dedup, and Consistency Pass

**Type:** Structure

Remove interim duplication, simplify overlapping E2E, and verify the whole property capability is commit-ready.

**Commit includes:**

- delete dead helper code and any temporary compatibility paths not needed after 4.14
- ensure markdown/rich property labels and page object names reflect the product capability, not this migration phase
- **E2E cleanup:** dedupe or merge overlapping scenarios in `e2e_test/features/note_creation_and_update/note_edit.feature` (and related steps/page objects) where insert, edit, remove, and markdown frontmatter flows repeat the same setup—prefer fewer scenarios that still prove persistence and parity between markdown and rich surfaces; drop redundant assertions bundled into one stronger scenario where safe
- update `ongoing/doughnut_wiki_migration_plan.md` and this plan with final Phase 4 status and any discoveries that affect Phase 5
- no intentional product behavior change beyond test and harness simplification

**Verification:**

- targeted frontend tests touched by property components
- targeted backend tests touched by property persistence/export
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature` after dedup (and `--spec` any other features trimmed in the same pass)
