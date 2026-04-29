# Doughnut Wiki Migration Plan - Phase 4 Sub-Phases

## Purpose

This document decomposes **Phase 4 - Introduce Note Properties** from `ongoing/doughnut_wiki_migration_plan.md` into small, closed sub-phases.

Each sub-phase should be small enough to complete in about 5 minutes and commit independently. Do not start the next sub-phase until the current one is green, cleaned up, and commit-ready.

## Phase 4 Target

After Phase 4:

- notes have an editable property bag persisted with the note
- markdown editing shows properties as leading YAML frontmatter and saves frontmatter changes back to the property bag
- rich editing shows the same properties above the Quill body as editable key-value rows
- users can insert, remove, and edit properties in rich mode
- saving and reloading from either editing surface keeps properties and body content consistent
- API consumers see one consistent property representation

## Key Decisions

- Store properties as a `Map<String, String>`-shaped value on `Note` and expose the same shape through the existing note read/update contracts. This phase does not attempt full YAML type fidelity.
- Support simple scalar frontmatter values in Phase 4. Nested maps, arrays, duplicate keys, and ambiguous YAML should be rejected or surfaced through the public save contract rather than silently misrepresented.
- Keep `details` as the markdown body content without frontmatter in the persisted note body. The markdown editor composes frontmatter plus body for editing and splits it on save. This keeps the new property concept separate from body content while still giving markdown users an Obsidian-like source view.
- Do not introduce a separate note-properties endpoint unless the existing note update path makes a closed slice impossible. Properties belong to the same note save experience as title/details editing.
- Extend the existing capability-owned note editing E2E coverage, for example `e2e_test/features/note_creation_and_update/note_edit.feature`; do not create phase-named feature files.
- Keep page object additions capability-named, such as property editing helpers on `notePage`, not phase-specific helpers.
- Run targeted specs only, normally `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`.

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

### 4.2 Persist Empty Note Properties Through the Backend Contract

**Type:** Structure

Add the backend field and API shape without changing visible editor behavior yet.

**Commit includes:**

- database migration adding a note properties column with an empty-object default or equivalent
- `Note` exposes a non-null empty property map for existing notes
- note read/update DTOs include properties without changing existing details/title behavior
- controller-level or high-level backend test proves existing notes read back with empty properties and updates preserve them
- regenerate the frontend TypeScript client

**Verification:**

- targeted backend controller/entity tests
- `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`

### 4.3 Save Properties Through the Note Update Path

**Type:** Behavior

API consumers can write and read note properties through the same logical note update path.

**Commit includes:**

- extend the update-details contract, or the smallest existing note update contract, to accept the property map
- backend test updates a note with properties and reads the same values back through the public note response
- preserve existing details editing behavior when properties are omitted
- no frontend UI changes yet

**Verification:**

- targeted backend note update/read tests

### 4.4 Add Frontmatter Parsing and Serialization

**Type:** Structure

Introduce the shared conversion rules used by the markdown editor without wiring them into the UI yet.

**Commit includes:**

- a cohesive helper that composes `{ properties, body }` into markdown source with frontmatter and splits markdown source back into `{ properties, body }`
- unit tests for no frontmatter, simple scalar frontmatter, body beginning after frontmatter, empty frontmatter, duplicate keys, non-scalar values, and malformed YAML
- invalid or unsupported frontmatter returns a public validation-shaped failure rather than silently dropping data
- no production UI behavior change

**Verification:**

- targeted backend or frontend unit tests for the conversion helper, depending on where the helper lives

### 4.5 Wire Markdown Editing to Properties

**Type:** Behavior

The markdown E2E scenario from 4.1 passes.

**Commit includes:**

- markdown mode displays frontmatter composed from the note property map above the existing body
- saving markdown splits frontmatter into properties and body before calling the update API
- rich rendering continues to show only body content
- remove `@wip` from the markdown properties E2E scenario once it passes
- keep the first implementation scoped to scalar string values

**Verification:**

- targeted frontend tests for markdown composition/splitting if needed
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`

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

### 4.7 Show Read-Only Rich Properties Above the Body

**Type:** Behavior

Users can see existing properties in rich mode before editing them there.

**Commit includes:**

- render a `Properties` section above the rich markdown editor when the note has properties
- display key and value text for each property
- add or update a frontend component test around note details editing/rendering
- no add/edit/remove controls yet

**Verification:**

- targeted frontend component test

### 4.8 Add Rich-Mode Property Insertion

**Type:** Behavior

The rich insert E2E scenario from 4.6 passes.

**Commit includes:**

- add an inline control for adding a property row in rich mode
- new row edits update the same property map sent through the note save path
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

### 4.11 Add the Rich Remove Property E2E Scenario

**Type:** Behavior

Define the rich-mode behavior for removing a property row.

**Commit includes:**

- add an `@wip` scenario to the note editing feature for removing a property in rich mode
- scenario shape: start with at least two properties, remove one in rich mode, save/reload, assert only the remaining property appears, then assert markdown frontmatter excludes the removed property
- no production behavior change

**Verification:**

- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
- confirm the scenario fails because removal is not implemented yet

### 4.12 Add Rich-Mode Property Removal

**Type:** Behavior

The rich remove E2E scenario from 4.11 passes.

**Commit includes:**

- add a remove control for each rich property row
- removing a row updates the note property map and save path
- remove the `Properties` section, or show an empty add-only state, when no properties remain
- remove `@wip` from the rich remove E2E scenario once it passes

**Verification:**

- targeted frontend component test for removing a property
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`

### 4.13 Close Property Validation Gaps

**Type:** Behavior

Users get clear failures for property shapes this phase cannot safely persist.

**Commit includes:**

- public contract tests for unsupported YAML values, duplicate keys, empty keys, and malformed frontmatter
- frontend displays save errors near the markdown editor or properties list without corrupting existing note content
- no new property value types beyond scalar strings

**Verification:**

- targeted backend validation tests
- targeted frontend validation test if UI error handling changes

### 4.14 Include Properties in Obsidian Export

**Type:** Behavior

Exported markdown includes the same persisted property values in frontmatter.

**Commit includes:**

- extend the existing notebook export behavior so note properties are included in exported frontmatter
- keep existing export metadata that is still needed, unless a current test proves it should move into the property map
- update the capability-owned notebook export E2E or backend export test to assert one persisted property appears in the exported markdown

**Verification:**

- targeted notebook export test or `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/notebooks/notebook_export.feature`

### 4.15 Phase 4 Cleanup and Consistency Pass

**Type:** Structure

Remove interim duplication and verify the whole property capability is commit-ready.

**Commit includes:**

- delete dead helper code and any temporary compatibility paths not needed after 4.14
- ensure markdown/rich property labels and page object names reflect the product capability, not this migration phase
- update `ongoing/doughnut_wiki_migration_plan.md` and this plan with final Phase 4 status and any discoveries that affect Phase 5
- no observable behavior change

**Verification:**

- targeted frontend tests touched by property components
- targeted backend tests touched by property persistence/conversion
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_edit.feature`
