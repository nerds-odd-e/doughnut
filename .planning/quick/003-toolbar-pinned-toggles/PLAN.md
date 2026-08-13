# Pin on-state note-toolbar toggles

**Status:** in progress (Phases 1–2 done)  
**Type:** ad-hoc UX under `.planning/quick/`  
**Goal:** An on-state Audio / Assimilation control stays on the note toolbar (never in More options), with higher priority than other icons as width shrinks. Off-state toggles can still live in More options.

Soft-primary on-state styling is already shipped (`NoteMoreOptionsActions` toolbar buttons). Keep it.

Audio and assimilation share one panel (`activePanel`), so **at most one** toggle is on.

## Learnings (failed first attempt)

Do **not** replace the 600px overflow model, rewrite `NoteToolbar`, or introduce a measured layout engine in the same slice as pinning. That attempt:

- Split `NoteMoreOptionsActions` into several new files and duplicated export/delete shortcuts
- Wrapped items in `class="contents"` (offsetWidth 0) then fought `v-show` vs `exists()` in tests
- Broke `PopButton` refs (`openDialog is not a function` when the ref became an array)
- Left E2E helpers assuming “any visible audio/assimilation means the whole group is inline”

**Keep for this plan:** pin first on the existing 600px split; measure later; hide from the right; never hide the on-toggle; open More options in E2E only if the **requested** action is not already visible.

## Locked decisions

| # | Decision |
|---|----------|
| D-01 | Pin **Audio** and **Assimilation** when on. Off copies stay in More options (narrow) or inline (wide). |
| D-02 | Hide from the **right**. Order: New note → Wiki → Conversation → Edit → Export → Questions → Audio (off) → Assimilation (off) → Delete. On-toggle is never hidden. |
| D-03 | More options (`…`) only when something overflowed. |
| D-04 | On-toggle uses `shrink-0` and stays on the toolbar even if the bar is narrower than that one button. |
| D-05 | Drop overflow **checkmark** (`DropdownMenuActionButton` `checked`) once on-toggles are omitted from the menu. |
| D-06 | No new E2E feature file. Cypress viewport stays 1200. Update [`noteMoreOptionsForm.ts`](e2e_test/start/pageObjects/noteMoreOptionsForm.ts) when visibility rules change. |
| D-07 | Do **not** put New / Wiki / Conversation / Edit in More options until more-options overflow is already measured (Phase 4+). |

## Current code (after Phase 2)

- [`NoteToolbar.vue`](frontend/src/components/notes/core/NoteToolbar.vue) — New / Wiki / Conversation / Edit always inline; more-options via [`NoteToolbarMoreOptions.vue`](frontend/src/components/notes/widgets/NoteToolbarMoreOptions.vue)
- [`useNoteToolbarMoreOptionsInline.ts`](frontend/src/composables/useNoteToolbarMoreOptionsInline.ts) — all more-options inline iff `clientWidth >= 600` (Phase 3 replaces this for the more-options group)
- [`NoteMoreOptionsActions.vue`](frontend/src/components/notes/widgets/NoteMoreOptionsActions.vue) — `layout: "toolbar" | "menu" | "pinned"`; toolbar `omit: NoteMoreOptionsActionId[]` hides items with `v-if` (real boxes, no `contents`)
- E2E: [`noteMoreOptionsForm.ts`](e2e_test/start/pageObjects/noteMoreOptionsForm.ts) `openOverflowMenuIfNeeded(title)` opens `…` only if that title is not already visible

## Learnings (Phase 1)

- Pin by mounting `layout="pinned"` beside `…` when not inline; `NoteMoreOptionsActions` decides which button to show. Do not duplicate “is a toggle on” in the parent.
- E2E helper `noteMoreOptions()` no longer assumes both Audio and Assimilation are visible at once.

## Learnings (Phase 2)

- Parent omit list + `v-if` (not `v-show` / `class="contents"`). Visible items stay real boxes for Phase 3 `offsetWidth`. Default omit is empty so 600px behavior is unchanged.

## Phases

### Phase 1 — Pin on-state toggle on a narrow toolbar (Behavior) — done

On a narrow bar, an on-state Audio/Assimilation button sits beside `…` (soft-primary, pressed, `shrink-0`) and is omitted from the menu. Off peer stays in the menu. Wide still inline. Overflow checkmarks removed. Pin coverage: [`NoteToolbar.pinnedToggles.spec.ts`](frontend/tests/notes/NoteToolbar.pinnedToggles.spec.ts).

### Phase 2 — Independent more-options item visibility (Structure) — done

`NoteToolbarMoreOptions` / `NoteMoreOptionsActions` accept `omit`. Toolbar items not listed stay in the DOM as real boxes. Production still omits nothing extra. Seam test in `NoteToolbar.moreOptions.spec.ts`.

### Phase 3 — Overflow more-options from the right by width (Behavior) — planned

**Observable:** As the note toolbar shrinks below “all more-options fit”, **Delete** leaves the bar first, then off-state Assimilation, off-state Audio, Questions, Export — into More options. An **on-toggle never leaves** the bar. New / Wiki / Conversation / Edit stay on the toolbar.

**Do:** Replace `NOTE_TOOLBAR_MORE_OPTIONS_INLINE_MIN_PX` for this group with ResizeObserver + child `offsetWidth`, including `…` width when anything is hidden. A small **pure** `computeNoteToolbarOverflow` (present ids, pinned ids, widths, available width) is the domain-stable contract — unit-test that; drive Vue from `NoteToolbarMoreOptions`, not a new `NoteToolbar` rewrite.

**Tests:** mock `clientWidth` / `offsetWidth` in [`mockNoteToolbarNavWidth.ts`](frontend/tests/helpers/mockNoteToolbarNavWidth.ts); drop 599-vs-600 as the source of truth. Extend `NoteToolbar.moreOptions.spec.ts`.

**Stop-safe:** Pin still works; more-options crowding improves; main actions unchanged.

### Phase 4 — Yield Edit into More options (Behavior) — planned

**Observable:** If the bar is still too tight after Phase 3 (typically with an on-toggle pinned), **Edit** moves into More options. Conversation / Wiki / New stay on the bar.

Menu-styled Edit in overflow. Keep keyboard `m`.

### Phase 5 — Yield Conversation, Wiki, then New note (Behavior) — planned

**Observable:** Same right-to-left rule continues: Conversation, then Wiki, then New note move into More options. Extreme: only the on-toggle and `…` remain (`…` lists everything hidden). If nothing is on, a tiny bar can be `…` only.

One phase because it is the same overflow rule applied to the remaining three actions, not a new concept.

**Stop-safe:** After Phase 4, Edit already yields; this only helps narrower bars.

## Out of scope

- FSRS / ADR 0003 planning files
- Custom CSS beyond DaisyUI/Tailwind classes already in use
- Changing Cypress default viewport
