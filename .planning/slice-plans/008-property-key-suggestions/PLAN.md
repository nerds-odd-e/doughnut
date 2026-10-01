# Property key suggestions use meaningful base keys

**Identity:** SEED-063#story-3
**Source:** [refined story](../../seeds/SEED-063-track-property-values-separately.md#story-3).
The owner accepted the occupied-preset presentation rules and requested slice planning on 2026-10-01.
This instruction authorizes planning only.

## Goal and scope

Notebook authors select a meaningful property key and add another value under it without Donut suggesting
numbered alternatives. Add property keeps occupied list-capable base presets available; scalar-only and singleton
slots are omitted when occupied. Stored-row suggestions omit another row's exact base key and exclude their own row
from occupancy checks. Preserve current duplicate-key validation, legacy recognition, preset context and filtering.
Remove obsolete numbered-key generation functions and exports together with their retired expectations.

Deferred: existing-content/tracker migration, merging rows or histories, new key normalization, forbidding manually
authored numbered keys, draft-row Add/Cancel design, and cleanup of spent Java migration code.

## Workspace and coordination

Continue in the established preparation workspace
`/Users/terryyin/git/doughnut/.worktrees/property-key-suggestions-stop-creating-numbered`, branch
`codex/property-key-suggestions-stop-creating-numbered`, starting revision `9c1b40d7561b85cbbe253f7bb7d574f420355eb7`.
Preparation assignment: Mihiro-chan, SEED-063#story-3. Publication target is `origin/main`; integration checkout is
`/Users/terryyin/git/doughnut`. This plan does not Take the story or publish the preparation draft.

SEED-064#story-6 is Taken on another worktree and changes the draft's confirmation and selectors. It does not own
preset availability. Before execution, inspect its current delivered state and adapt the test/page-object confirmation
to the current surface; do not copy its draft redesign or require its completion. Today's form confirms on value blur;
its planned replacement confirms with Add/Enter. Preserve this story's preset-selection and saved-content assertions
through reconciliation. A materially changed append or rename rule returns to story review.

## Existing solutions and design

PFE search across frontend, backend, CLI and MCP found one rich-mode preset policy:
`noteContentPropertyKeyPresets.ts` → `RichFrontmatterPropertyKeyPresets.vue` → shared
`RichFrontmatterPropertyKeyField.vue`. Both insertion and stored-row editing use it; stored rows already supply
`excludeRowIndex`. Change this existing policy rather than add a second dropdown or a new property representation.

Reuse `isListCapablePropertyKey`, `keysInPresetFamily` and `findPropertyRowIndexByExactKey` for their existing domain
purposes. Singleton/structural slot occupancy uses existing family recognition, including legacy aliases/suffixes;
ordinary list-key occupancy uses exact authored keys. These are different existing product rules, not per-preset
special handlers. `tryCommitInsert` and `propertyRowsAfterAppendingValueToExactKey` already own append; `commitRow`
and `validatePropertyRowsForRichEdit` already own duplicate rename rejection. Neither operation needs new semantics.
The backend reduce flow already appends through `NoteContentMarkdown.addPropertyValueToLeadingFrontmatter`; it has
no rich-mode suggestion responsibility. CLI/MCP do not consume the frontend generator.

The cumulative availability rule is: offer canonical presets applicable to the editor context, omit occupied
singleton/structural slots, and for stored-row editing also omit another row's exact base key. In Add property,
list-capable non-singleton keys remain available even when occupied. Keep this rule in the existing policy module;
no framework, registry, or new normalization layer is needed.

[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md) requires valid authored frontmatter
and preserving author-owned keys. Reuse the existing Markdown compose/save path; suggestion changes do not rewrite
legacy keys. [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) governs isolated test resources.
The North Star's One format boundary is respected by preserving the codec; no new architectural direction is needed.

## Decisive premises and observations

Observed on product revision `9c1b40d756` on 2026-10-01; preparation prose changes do not alter the product baseline.

| Premise | Consumed by | Literal observation and inspected boundary | Result |
| --- | --- | --- | --- |
| Selecting an occupied suggestion creates a numbered key instead of appending | slice 1 | Temporary mounted-editor case in `RichMarkdownEditor.propertyEntry.spec.ts`: mount `example of: "[[A]]"`, open Add property, assert literal options including `example of 2`, select it, enter `[[B]]`, blur; inspect emitted Markdown through the real parser. Command: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts` | 18/18 passed including two temporary probes; the base stayed scalar and a second scalar key was created. Probe edits were removed afterward. This reproduces the symptom, not the desired remedy. |
| Exact-key append and duplicate rename already work through the editor | slices 1, 3 | `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/noteContentPropertyKeyPresets.spec.ts tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts`; inspect entry's `appends to exact list-capable keys without folding legacy suffixes` and row editing's `rejects duplicate keys before emitting valid renamed keys and values` | 35/35 passed. Append preserves legacy content; duplicate rename emits no change and restores the row. These tests do not prove occupied-preset selection. |
| Readme context and legacy structural alias reach the preset policy | slice 2 | Second temporary case in the same 18/18 run: mount `titlePattern: "Topic *"` with `isReadmeContext: true`, open Add property, assert literal options | Options contained `title_pattern 2`, without note-only presets. New omission proof must replace this old behavior. |
| Existing list append preserves order | slice 1 | Read `appendValueToPropertyRow` in `noteContentPropertyRows.ts` and its caller `propertyRowsAfterAppendingValueToExactKey` consumed by `tryCommitInsert` | Existing list spreads its items then appends the trimmed new value; scalar promotion keeps the original value first. Slice 1 will observe existing-list append through preset selection. |
| One shared policy serves both surfaces and already carries current-row exclusion | slices 1–3 | `rg -n 'richModeKeyDropdownPresetKeysForPropertyRows|exclude-row-index' frontend/src`; read `RichFrontmatterPropertyKeyPresets.vue`, `RichFrontmatterPropertyKeyField.vue`, `RichFrontmatterPropertyRow.vue` and insert form | The shared component calls the policy; stored rows pass their index, insertion passes none. Preserve that distinction in the draft replacement. |
| Generator removal leaves recognition callers supported | slice 2 | `rg -n 'nextAvailablePropertyKey|keysInPresetFamily|propertyKeyBaseAndSuffix' frontend/src frontend/tests`; product-wide `rg -n 'nextAvailablePropertyKey|richModeKeyDropdownPresetKeys' --glob '!*.md' .` | `ForPreset` is consumed by the policy and re-exported by `noteContentFrontmatter.ts`; `ForBase` has only utility-test callers. Private generation helpers serve those two exports. The suffix parser also serves structural recognition and `authoredNoteLevelValidation`; keep it and its recognition tests. |
| The existing persistence journey and owning command are usable in this worktree | slice 1 | Read `note_edit.feature`, `note_editing.ts`, `noteRichPropertyMethods.ts`; run `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature` | 12/12 passed, including rich edits after reload and Markdown source checks. Runner selected worktree-specific database and origin. Existing add page object types keys; new proof must click the preset, not use that typing shortcut. |

The temporary probes used only local headless component state and were restored. No retained product/test change
or production observation occurred during planning. No paid or credentialed premise remains for an early probe slice.

## Outside-in proof ownership

| Source promise | Owner | Observable proof |
| --- | --- | --- |
| Occupied `example of` scalar becomes one ordered list through preset selection | slice 1 | Mounted editor: click literal `example of`, confirm `[[past tense]]`, parse emitted Markdown; one base list and no generated numbered key. Extend `note_edit.feature` with that preset journey, save/reload and source check. |
| Occupied `url` list gains a third value in order | slice 1 | Mounted editor initialized with two URLs → select literal `url` → confirm third → emitted list retains all three. |
| Authored legacy numbered content remains independently readable/editable and is not folded into the base | slice 1 | Preserve existing legacy append case; add a mounted case with only `example of 2` → select available `example of` → saved content retains legacy value and adds the base separately. Tracker mutation paths are unchanged, not a new tracker-migration claim. |
| Occupied structural/singleton presets omitted, recognized aliases/suffixes still occupy slots, context unchanged | slice 2 | Mounted editor options for `image`, `wikidataId`, `question_generation_instruction` and existing note-only singleton slots; readme `titlePattern` case. Unoccupied canonical suggestions remain. Keep literal expected options independent of production generators. |
| Structural values keep existing scalar/control behavior | slice 2 | Retained classification/recognition tests and mounted editor's existing image and Wikidata value cases; no change to structural controls or `isListCapablePropertyKey`. |
| Obsolete generation functions and exports removed, useful recognition retained | slice 2 | Search for generator symbols after deletion, typecheck, retained recognition tests and component cases. Remove only tests of the retired generation contract. |
| Stored-row suggestions omit another row's occupied key and retain their own available preset | slice 3 | Mounted editor with `topic` and `url`: focus `topic` → literal options exclude `url`; focus `url` → literal options include `url`. Retain option narrowing and focus behavior. |
| Manual duplicate rename still rejects and saves nothing | slice 3 | Existing mounted row-editing duplicate test, extended to the story's list-key case if necessary; error and unchanged emitted/saved content, no row merge. |

## Ordered slices

All slices are Behavior and are independently safe to stop after. Target about 5 minutes including focused proof;
>5 minutes scrutinize, >10 minutes finer-decompose unless the focused-test/external-wait exception below applies
(AGENTS.md). Estimates include implementation and slice-local cleanup. E2E service startup and the fixed selected
feature run are a stated test-wait exception, not grounds to bundle more implementation. Record elapsed time and
new evidence if a slice overruns; revise remaining work in this plan rather than retrying the same oversized leaf.

### 1. Adding another value selects the same list-capable base key
Type: Behavior
Status: planned
Proof: new occupied-preset mounted cases red then green in `RichMarkdownEditor.propertyEntry.spec.ts`; persistence
journey in `note_edit.feature`; commands below.

Behavior: an occupied `example of` scalar or `url` list → select that canonical preset in Add property and confirm
another value → one ordered base-key list, with no newly generated numbered key. A legacy suffixed-only row stays
separate when the available base is selected.

Change the existing availability policy for list-capable non-singleton presets; keep current singleton omission
and, temporarily, structural generation until slice 2. Reuse existing exact-key append with no normalization or merge.
Replace affected utility expectations and helper-derived component expectations with literal promised options.
Extend the existing E2E authoring feature with a thin preset-selection page-object action that clicks the preset,
sets the value, confirms using the current surface and flushes the pending save. Reload and inspect saved source/list
values; setup must start with a scalar and must not pre-create the promised list. Keep temporary remaining structural
suffix expectations only until slice 2.

Size hypothesis: about 5 minutes active work plus fixed E2E startup/run; medium confidence because the scenario
needs a small page-object action. Safe stop: list authoring stops producing numbered keys, while structural presets
retain their old behavior until the next slice.

### 2. Occupied scalar and singleton slots have no numbered substitute
Type: Behavior
Status: planned
Proof: mounted occupied-slot and readme-context cases red then green in `RichMarkdownEditor.propertyEntry.spec.ts`;
retained recognition and value-control tests plus typecheck.

Behavior: an occupied `image`, `wikidata_id`, instruction or readme title-pattern slot → open suggestions → that
canonical slot is absent, with no numbered substitute. Existing note-only singleton omissions remain. An unoccupied
slot still offers its canonical key; excluding the current row preserves its own slot.

Complete the common policy by using existing family occupancy for scalar/singleton slots in both key surfaces.
Include recognized aliases/suffixes in fixtures to prove reuse. Delete both `nextAvailablePropertyKeyFor*` exports,
private helpers used only by generation, the frontmatter facade's generator export, and their obsolete tests once
no production caller needs them. Retain suffix parsing, structural recognizers and their live callers. Update all
remaining numbered-option expectations in the same slice; no green tests should endorse a generated numbered preset.
Do not build a new structural-value editor or change list capability.

Size hypothesis: about 5 minutes; medium confidence. Safe stop: neither key surface generates numbered presets;
occupied list-capable keys may still be visible when renaming and rejected by the existing duplicate rule until slice 3.

### 3. Stored-row suggestions respect other rows without hiding their own key
Type: Behavior
Status: planned
Proof: mounted occupied-list-key and current-row cases red then green, retained narrowing/focus and duplicate rename
cases; final frontend suite and typecheck.

Behavior: `topic` and `url` rows → open `topic`'s key suggestions → no occupied `url`; open `url`'s suggestions → `url`
is available. Manual `topic` → `url` rename still reports the current duplicate-key error and changes no saved content.

Use current-row exclusion to distinguish stored-row availability from insertion in the existing policy. For list keys,
check exact base-key occupancy among other rows. Structural/singleton family rules remain those from slice 2.
Keep Add property offering occupied list-capable base keys. Do not introduce row merging or tracker changes.
Recheck the delivered SEED-064#story-6 surface so its draft uses insertion availability and stored rows keep exclusion.

Size hypothesis: about 5 minutes; high confidence because the index is already passed. Safe stop: the full story's
suggestion policy is delivered while legacy authored keys continue to work.

## Verification and delivery

Focused frontend proof for each slice (add `listProperties` when its rendering boundary changes):

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyEntry.spec.ts tests/components/form/RichMarkdownEditor.propertyRowEditing.spec.ts tests/utils/noteContentPropertyKeyPresets.spec.ts tests/utils/noteContentPropertyKeys.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
```

Slice 1 persistence proof:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_edit.feature
```

At slice 3, run `CURSOR_DEV=true nix develop -c pnpm frontend:test` once for the completed shared frontend policy and
recognition/export cleanup, following frontend skill's suite preference. Reuse matching typecheck evidence. No API
signature change or generated client update is expected. Hosted CI checks remain owned after publication; they are
not a blanket local gate. A new failure or changed boundary determines any additional focused proof.

Execution uses the required AGENTS.md/dough-execute-plan wrap-up: Jidoka; fresh dough-post-change-refactor agent;
API generation only if triggered; coordinator runs `./scripts/run.sh pnpm format:changed` once; update this plan without
a second routine formatting pass; commit with the independent check-only lint hook; managed publication to the
recorded target with asynchronous CI repair. Implementers/refactorers run neither formatting nor standalone
`lint:changed`. The execution workflow owns authorization, Take, assignment disposition, observer and publication;
this planning request performs none of those actions.

## Current decisions

- Owner accepted the two occupied-preset rules on 2026-10-01; no scope decision remains open.
- Reuse existing append, duplicate rejection, recognition, codec and save responsibilities.
- Canonical suggestions do not imply migrating authored legacy keys or rejecting manually entered numbered keys.
- Generator cleanup belongs with the slice that removes its final production caller, not a separate technical leaf.
- Preserve source examples through sibling draft-row delivery; adapt selectors/confirmation, not expected behavior.
- No new ADR, North Star topic, backend contract or migration is required.

## Learnings

None beyond the preparation observations above. Execution has not started.
