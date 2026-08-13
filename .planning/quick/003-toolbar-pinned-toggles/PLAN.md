# Pin on-state note-toolbar toggles

**Status:** complete  
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

## Current code (after Phase 5 implementation)

- [`NoteToolbar.vue`](frontend/src/components/notes/core/NoteToolbar.vue) — Conversation `v-if` hides when overflowed; Wiki/New stay mounted (hidden) so shortcuts still open the same PopButton
- [`noteToolbarOverflow.ts`](frontend/src/composables/noteToolbarOverflow.ts) — order is `new` → `wiki` → `conversation` → `edit` → …
- [`NoteMoreOptionsYieldedItems.vue`](frontend/src/components/notes/widgets/NoteMoreOptionsYieldedItems.vue) — menu-styled New / Wiki / Conversation / Edit
- [`NoteToolbarMoreOptions.vue`](frontend/src/components/notes/widgets/NoteToolbarMoreOptions.vue) — `availableWidth` is nav `clientWidth` (no preceding siblings); `presentIds` drops New when the sidebar has it
- E2E: [`noteMoreOptionsForm.ts`](e2e_test/start/pageObjects/noteMoreOptionsForm.ts) unchanged (viewport 1200)

## Learnings (Phase 1)

- On-toggle stays on the bar (`shrink-0`, never omitted). E2E helper `noteMoreOptions()` keys off the requested title, not “any audio/assimilation visible”.

## Learnings (Phase 2)

- Parent omit list + `v-if` (not `v-show` / `class="contents"`). Visible items stay real boxes for `offsetWidth`.

## Learnings (Phase 3)

- Overflow is a pure `computeNoteToolbarOverflow`; Vue measures nav `clientWidth` minus preceding siblings (`display: contents` New must sum children). Cache last non-zero widths. Menu `only` lists overflowed ids. Pinned ids are never omitted — no separate `layout="pinned"`. Do not rewrite `NoteToolbar`.

## Learnings (Phase 4)

- Add Edit to the overflow order left of Export. Hide the toolbar Edit control when `"edit"` is overflowed; one menu row; keep `m` on NoteToolbar. Do not count Edit as a preceding sibling.

## Learnings (Phase 5)

- One Wiki PopButton (hide trigger with `hidden`, menu row emits `open-wiki`). Same for New (`n` stays on `NoteNewButton`). Do not mount a second PopButton.
- After New/Wiki/Conversation are overflowable, available width is the full nav `clientWidth`.
- Tests must mock `offsetWidth` before mount; a first real-width measure can cache ~50px and stick after those controls unmount.

## Phases

### Phase 1 — Pin on-state toggle on a narrow toolbar (Behavior) — done

On a narrow bar, an on-state Audio/Assimilation button sits beside `…` (soft-primary, pressed, `shrink-0`) and is omitted from the menu. Off peer stays in the menu. Wide still inline. Overflow checkmarks removed. Pin coverage: [`NoteToolbar.pinnedToggles.spec.ts`](frontend/tests/notes/NoteToolbar.pinnedToggles.spec.ts).

### Phase 2 — Independent more-options item visibility (Structure) — done

`NoteToolbarMoreOptions` / `NoteMoreOptionsActions` accept `omit`. Toolbar items not listed stay in the DOM as real boxes. Production still omits nothing extra. Seam test in `NoteToolbar.moreOptions.spec.ts`.

### Phase 3 — Overflow more-options from the right by width (Behavior) — done

More-options leave the bar from the right (Delete first). On-toggle never leaves. `…` only when something overflowed; menu lists overflowed items only. New / Wiki / Conversation / Edit stay on the toolbar. Coverage: `noteToolbarOverflow.spec.ts`, `NoteToolbar.moreOptionsOverflow.spec.ts`.

### Phase 4 — Yield Edit into More options (Behavior) — done

When the bar is still tight after Export overflows, Edit moves into More options (menu-styled). Conversation / Wiki / New stay on the bar. Keyboard `m` still toggles edit mode.

### Phase 5 — Yield Conversation, Wiki, then New note (Behavior) — done

Conversation, then Wiki, then New yield into More options. Extreme: on-toggle + `…`, or `…` only. Wiki/New keep a single PopButton so shortcuts still work. Coverage: `NoteToolbar.conversationWikiNewOverflow.spec.ts`.

## Out of scope

- FSRS / ADR 0003 planning files
- Custom CSS beyond DaisyUI/Tailwind classes already in use
- Changing Cypress default viewport
