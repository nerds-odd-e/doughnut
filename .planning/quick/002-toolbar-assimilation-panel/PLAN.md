# Toolbar assimilation panel — PLAN

Status: planned  
Context: [CONTEXT.md](./CONTEXT.md)

## Intent

One exclusive under-toolbar panel slot with shared chrome; audio and assimilation as peer more-options toggles (Mic + CircleCheck); assimilation natural height — no duplicated state or shell.

## Design decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Panel exclusivity | Shared active id `none \| audio \| assimilation` | Mutual hide by construction |
| Panel chrome | One shared under-toolbar shell (from audio’s container/animation/surface) | Confirmed A — cohesive restyle |
| Toggle placement | Both in more-options (inline + overflow), same pressed/checked/`aria-pressed` pattern | Confirmed B |
| Icons | Mic = audio; CircleCheck = assimilation | Same pattern; CircleCheck kept for assimilation |
| Assimilation targeting | `useAssimilationView` for note id + pending property; visibility via shared slot | Confirmed E |
| Height | No half-page/`40vh` cage; natural height in slot | Confirmed C |
| Conversation | Not in exclusivity set | Confirmed D |
| Tests | Unit at toolbar/more-options/note-show; fix E2E page objects for Audio tools via more-options reachability | Capability names only |

## Phase sizing notes

- Target ~5 minutes wall-clock per phase (implement + targeted tests).
- Structure only when it enables the immediate next Behavior.

## Phases

### Phase 1 — Structure: Shared exclusive panel state + shared shell (audio only) — **done**

**Type:** Structure  
**Enables:** Phase 2–3 (second panel + peer toggles without a second open model or second shell).

**Shipped:** `useNoteToolbarPanel` (`none | audio`), `NoteToolbarPanelShell` with `data-testid="note-toolbar-panel-shell"`, audio wired through composable. Mic still on primary toolbar; assimilation still page-bottom.

**Learnings:** Shell uses capability name `note-toolbar-panel-shell` (not legacy `audio-tools-container`); `close`/`activePanel` exported for Phase 2 assimilation slot.

---

### Phase 2 — Behavior: Assimilation opens in the shared under-toolbar panel — **planned**

**Type:** Behavior  

**Pre:** Note show; shared slot may be none or audio.  
**Trigger:** Toggle Assimilation settings, or `openForNote` / go-to-next.  
**Post:**

- Assimilation content renders inside the **shared shell** under the toolbar (restyled — not page-bottom card/footer).
- Slot exclusivity: assimilation hides audio and vice versa.
- Natural height: no `max-h-[min(40vh,22rem)]` (or equivalent) cage; content grows in the slot.
- Assimilation control pressed state matches audio (`daisy-btn-active` + `aria-pressed` when open for this note; menu checked when open).

**Change:**

- Extend shared panel id with `assimilation`; wire `useAssimilationView` open/toggle/dismiss to select/clear the slot.
- Mount `AssimilationPanel` (or its settings content) in the same under-toolbar mount as audio, inside the shared shell; remove `NoteShowPage` bottom mount.
- Strip footer/card/half-page scroll chrome from assimilation settings so it fits the shared shell.
- Preserve assimilate / revive / refine / pending-property / reload behavior.

**Verify (targeted):**

- Unit: assimilation under toolbar in shared shell; toggle off removes it.
- Unit: audio ↔ assimilation exclusivity.
- Unit: no half-page max-height cage.
- Placement / assimilate-on/off note-show specs updated and green.
- More-options assimilation toggle specs still green.

**Stop-safe:** Assimilation UX matches audio panel model; Mic may still sit on the primary toolbar until Phase 3.

---

### Phase 3 — Behavior: Peer more-options toggles for audio and assimilation — **planned**

**Type:** Behavior  

**Pre:** Note show with more-options (inline or overflow).  
**Trigger:** User opens/toggles Audio tools or Assimilation settings from more-options.  
**Post:**

- Mic and CircleCheck are **peers** in more-options (same inline vs overflow rules as today’s more-options actions).
- Identical toggle affordances: active + `aria-pressed` when inline; checked when in the overflow menu.
- Mic is **not** a separate primary-toolbar-only control.
- Opening either still drives the shared exclusive panel slot.

**Change:**

- Move Audio tools control into `NoteMoreOptionsActions` / titles alongside assimilation (shared action pattern — no one-off Mic block on `NoteToolbar`).
- Ensure overflow menu closes on toggle the same way assimilation already does.
- Update unit tests that clicked primary `button[title="Audio tools"]`; update E2E `toolbarButton('Audio tools')` to the same reachability path as assimilation (`makeSureNoteMoreOptionsFormIsOpen` or shared helper).

**Verify:** Audio and assimilation toggle specs cover both layouts as peers; existing audio + assimilation E2E entry points still open their panels.

**Stop-safe:** Full cohesion of toggles + panels.

---

### Phase 4 — Structure: Dead chrome / naming cleanup (only if needed) — **planned**

**Type:** Structure  

**Change:** Remove leftover page-bottom assimilation helpers, obsolete footer placement assertions, duplicate shell CSS, and transitional names.

**Verify:** Related frontend unit tests green; no behavior change.

**Skip** if Phase 2–3 already leave no dead code.

## Testing strategy

| Layer | Role |
|-------|------|
| Unit | Shared slot, shell, exclusivity, natural height, peer more-options toggles (inline + menu) |
| E2E | Existing audio + assimilation features; page objects use one more-options reachability path for both toggles |

## Done when

- Shared exclusive under-toolbar panel + shared shell for audio and assimilation.
- Peer more-options toggles (Mic + CircleCheck) with the same pressed/checked pattern.
- No half-page height cage on assimilation.
- No duplicated open-state or shell solution.
- CONTEXT A–E match what shipped.
