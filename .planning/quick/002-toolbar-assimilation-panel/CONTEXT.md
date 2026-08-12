# Toolbar assimilation panel — CONTEXT

## Requirement (developer)

Make assimilation settings behave like the audio tools panel:

- Toolbar button toggles the panel; same pressed/active button treatment.
- Only one panel under the toolbar at a time (opening assimilation hides audio, and vice versa).
- Remove the half-page height constraint/container from assimilation settings.
- Solution must be cohesive — one panel concept, no duplicated toggle/slot wiring.

Do not execute yet; plan only.

## Current state

| Concern | Audio tools | Assimilation settings |
|---------|-------------|------------------------|
| Toggle control | Mic on primary `NoteToolbar` | CircleCheck in more-options (`NoteMoreOptionsActions`) |
| Active affordance | `daisy-btn-active` + `aria-pressed` | `daisy-btn-active` / menu `checked` only (no `aria-pressed`) |
| Open state | Local `audioToolsOpen` in `NoteToolbar` | Module singleton `useAssimilationView` (also opened by `useGoToNextAssimilation`) |
| Panel mount | Sibling under toolbar nav in `NoteToolbar` | Bottom of `NoteShowPage` → `AssimilationPanel` → `AssimilationSettings` |
| Chrome | `.audio-tools-container` dark dropdown + drop animation | `<footer>` + daisy-card; scroll body `max-h-[min(40vh,22rem)]` |

## Confirmed decisions (A–E)

| # | Decision |
|---|----------|
| **A** | **Restyle** assimilation into the same under-toolbar panel chrome as audio (shared shell — not a separate card/footer look). |
| **B** | **Peer controls in more-options.** Both audio and assimilation live in the more-options set (inline toolbar buttons when wide, overflow menu when narrow) and use the **same** toggle / active / `aria-pressed` / menu-checked pattern. Icons: keep **Mic** for audio; keep **CircleCheck** for assimilation (good fit — same *pattern*, not the same glyph). Mic leaves the primary toolbar row. |
| **C** | After removing max-height, tall settings **grow** and shrink the note body — no inner half-page scroll cage. |
| **D** | Conversation is **not** in the exclusive toolbar-panel set. |
| **E** | `goToNextAssimilation` / pending property still open this same under-toolbar assimilation panel via `openForNote`. |

## Intent

1. **One under-toolbar panel slot** — exclusive `none | audio | assimilation`.
2. **One panel chrome** — shared shell (today’s audio drop-down container / animation / surface); both contents render inside it.
3. **Peer more-options toggles** — Mic + CircleCheck with identical interaction in inline and menu layouts.
4. **Natural height** for assimilation content (no `max-h-[min(40vh,22rem)]` cage).
5. **Cohesion** — no parallel open flags, no duplicated shell CSS, no one-off “close the other” branches.

## Cohesion target

- Shared composable for exclusive panel id.
- Shared presentational wrapper for under-toolbar panels (extracted from current audio container).
- Shared more-options action pattern for both panel toggles (toolbar button + menu item with checked/pressed).
- `useAssimilationView` keeps note id + pending property; “panel showing” is the shared slot.

## Out of scope

- Changing assimilate / revive / refine domain flows (only chrome/placement/toggles).
- Putting Export / Questions / Delete into the panel slot.
- Conversation exclusivity.
- Full E2E suite; update page objects that click “Audio tools” / assimilation so both go through the same more-options reachability helper.
