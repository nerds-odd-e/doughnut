# CLI: shrink, simplify, idiomatic Ink + React

Informal plan. Delete or shrink when this refactor is done or parked.

## North star

- **Reduce line count and conceptual surface area.** The CLI has **limited features** today; the implementation is **bloated**. Every phase should prefer **fewer files, fewer types, fewer indirections** unless a layer clearly pays for itself.
- **Idiomatic Ink + React** is a main tactic: **one `render()`**, state in **hooks** (`useReducer` / `useState`), **no** `drawBox` + **`shellInstance.rerender`** + closure **`patch` / `patchAndDraw`** in `interactiveTtySession.ts`.
- **UX / UI may change** when that **materially simplifies** the code or aligns with normal Ink patterns. Document notable UX deltas in **`cli.mdc`** and **E2E / Vitest** where behavior is still worth locking.
- **Delete dead code and the tests that exist only to cover it** in the same change. Do **not** keep tests as anchors for removed product behavior.
- **Remove excessive abstraction**: merge thin wrappers, collapse props-drilling layers, inline one-off helpers, fold **`ShellSessionRoot` / `InteractiveShellDisplay` / live panels** where a single tree reads clearer after the React root move.

## Status snapshot (2026-03-27)

- **Phase 0 is done:** trimmed unused public surface (module-private types/helpers where nothing imported them) and removed **`ConfirmLivePanel.tsx`** — it was unreachable from the bundle and referenced non-existent modules (`sessionYesNoInteraction`, `ConfirmDisplay`). **`selectListInteraction.test.ts`** now builds full `SelectListKeyEvent` shapes for `dispatchSelectListKey` (internal type is no longer exported).
- **Phases 1–5 are implemented** (run **`pnpm cli:test`** after Phase 5 layout changes).
- `runInteractiveTtySession` uses a **single Ink mount** and a reducer-driven React root (**`InteractiveApp`** in **`cli/src/ui/interactiveApp.tsx`**).
- The old imperative repaint loop is removed: no adapter-level `drawBox`, `patchAndDraw`, or manual `shellInstance.rerender`.
- **No identity `forceRedraw` in the adapter:** repaints that only existed to re-run **`handleShellRendered`** use **`bumpTtyContractEpoch`** (`ttyContractEpoch` on **`ShellSessionState`**).
- Shared **`runProcessInputTurn`** covers session y/n, MCQ submit, and normal command-line **`processInput`** await/exit paths.
- **`TTYDeps`:** removed **`isNumberedChoiceListActive`**; call sites use **`getNumberedChoiceListChoices() !== null`**.
- **`OutputAdapter`:** **`writeCurrentPrompt` / `beginCurrentPrompt`** left split (TTY vs non-TTY recall paths unchanged in Phase 3).
- `ShellSessionRoot` now owns both transcript rendering and live-column routing; `InteractiveShellDisplay.tsx` is removed.
- **Phase 2:** Fetch-wait changes bump **`ttyContractEpoch`** on `ShellSessionState` so `useLayoutEffect` re-runs **`handleShellRendered`** without identity redraw. Fetch-wait **Esc** relies on `runInteractiveFetchWait` **`finally`** → **`setActiveWaitLine`** → **`onInteractiveFetchWaitChanged`** (no separate **`redrawRef`** tick).

## Current shape (after Phase 5)

- **`cli/src/ttyAdapters/`** — TTY/process I/O only: **`ttyEntry`**, **`interactiveTtySession`**, **`interactiveTtyStdout`**, **`ttyDeps`**.
- **`cli/src/ui/interactiveApp.tsx`** — Ink **composition root** (**`InteractiveApp`**): session reducer, **`ShellSessionRoot`**, and terminal contract hooks (**`interactiveTtyStdout`**, stdin **`setRawMode`** when the live column uses an alternate panel).
- **`runInteractiveTtySession`** mounts Ink **once** and renders **`InteractiveApp`** with **React-owned session state**.
- State transitions are routed through reducer patching + refs so async handlers read current state without closure drift.
- **`ShellSessionRoot`** is a pure function of **`session` + `deps` + `handlers`**, and directly renders **past transcript + live column**.
- **`liveColumnInk`** shares `LiveColumnPromptBlock` across command-line, MCQ, and token-picker modes to reduce repeated stage/prompt composition.
- Repaint-sensitive behavior is now primarily governed by state transitions and effect timing, not explicit draw calls.

### `commandTurn` and repaint discipline

- **`commandTurn`** is **not** read by Ink; it flushes to **`pastMessages`**. Prefer a **ref** or small buffer **outside** the reducer so log spam does not force reconciles — unless we **simplify** by changing how assistant output streams (UX trade).

## External references (Context7)

- **Ink**: **`render(<App />)`** + internal state; **`rerender`** for **root replacement**, not per-update churn.
- **React**: **`useReducer`** + **`dispatch`** from event handlers (including stdin/Ink paths).

## Target direction

1. **Single Ink mount**; **visible shell state** in **React**; **`commandTurn`** (or successor) structured for **minimal reconciles**.
2. **Terminal contract** (OSC, cursor, live-region finalize) in **`useLayoutEffect`** (or equivalent **one** place), not scattered after imperative paints.
3. **Fewer modules and types** as a **success metric** — periodic diffstat / “could this be one file?” check.
4. **`processInput` / `TTYDeps`**: **simplify internals and call sites** when it **net-reduces** code; avoid **gratuitous** public behavior changes **without** simplification payoff (scripts, E2E, MCP still need a stable story).

## Phasing (per `.cursor/rules/planning.mdc`)

Order by **net simplification + safety**. Prefer **one user-visible slice** per phase where possible. Tests: **observable** behavior for **kept** features; **delete** tests tied only to **removed** code.

### Phase 0 — Inventory and cut dead weight

- ✅ **Done (2026-03-27).**
- **Removed dead module:** `cli/src/ui/ConfirmLivePanel.tsx` (no importers; broken imports).
- **Narrowed exports → file-private** where repo-wide grep showed no external use: e.g. `htmlToMarkdown`, `CommandDocCategory` (inlined on `CommandDoc`), `recallStopConfirmInkModel`, several Ink panel prop types, `McqGuidancePhysicalRows` / `CommandInputDraftOptions` / `InteractiveInputReadyOsc`, `PatchedTextInput*` state types, and internal-only types in `selectListInteraction.ts`.
- **Tests:** adjusted `selectListInteraction.test.ts` only to satisfy the non-exported event type; no behavioral test deletions.

### Phase 1 — React root + remove `drawBox`

- ✅ Implemented.
- `runInteractiveTtySession` now performs one Ink `render` and no longer drives UI through imperative redraw helpers.
- Kept `ShellSessionRoot` as-is for this phase to minimize behavior churn and keep Phase 2 isolated.
- Related interactive Vitest coverage was adjusted to assert observable outcomes where repaint-byte timing changed under the new render model.

### Phase 2 — Terminal side effects + external signals

- ✅ Implemented.
- **Checklist:** `ttyContractEpoch` on **`ShellSessionState`**; **`onInteractiveFetchWaitChanged`** flushes command-turn / resets live line as before, then **`patch` increments epoch** (never **`forceRedraw`** there); fetch-wait **Esc** = abort + **`finally`** clears wait line and bumps epoch via the same callback — **`redrawRef`** removed.
- **`handleShellRendered` / `finalizeInteractiveLiveRegionPaint`** run from **`useLayoutEffect` `[session]`** (including when only **`ttyContractEpoch`** changed).

### Phase 3 — Simplify handlers and deps wiring

- ✅ **Done (2026-03-27).**
- Removed **`forceRedraw`** from **`interactiveTtySession.ts`**. **`bumpTtyContractEpoch`** replaces identity spreads where the goal was to re-run **`useLayoutEffect`** / terminal contract.
- **`finishProcessInputTurnAfterAwait`:** after flushing command-turn to past messages, bumps epoch when the turn was empty or session y/n chrome is active.
- **`routeRecallMcqChoicesInkStdin`:** list **`redraw` / default** → epoch bump; **`runProcessInputTurn`** for submit.
- **`handleCommandLineInkInput`:** redundant redraws after state-changing **`patch`** removed; empty submit / MCQ highlight reset use epoch bump; slash-picker submit falls back to **`filtered[0]`** when highlight is out of range (no no-op redraw).
- **`runProcessInputTurn`:** single path for **`processInput` → exit or `finishProcessInputTurnAfterAwait`** (session y/n, MCQ, command line).
- **`TTYDeps`:** dropped **`isNumberedChoiceListActive`**; **`buildTTYDeps`** in **`interactive.ts`** updated.
- **`OutputAdapter`:** evaluated; **not** unified in this phase — split still carries TTY vs console recall behavior.
- **Tests:** `interactiveTtySession`, `interactiveTtySuggestionScroll`, `interactiveTtyMcq`, `interactiveTtyBufferAfterFetchWait` Vitest files green.

### Phase 4 — UI structure and remaining fat

- ✅ **Done (2026-03-27).**
- Removed the thin display wrapper layer by folding transcript rendering into `ShellSessionRoot`; deleted `cli/src/ui/InteractiveShellDisplay.tsx`.
- Reduced repeated stage/prompt JSX in `liveColumnInk.tsx` with shared `LiveColumnPromptBlock` used by command-line, MCQ, and token-picker panels.
- Kept stdin/focus contracts unchanged (`LiveColumnStdinPolicy`, `isAlternateLivePanel`, Ink-only list Esc handling).
- Validation: targeted interactive Vitest, full `pnpm cli:test`, and scoped CLI E2E (`e2e_test/features/cli/**/*.feature`) all green.

### Phase 5 — `ttyAdapters` + `InteractiveApp` in `ui/`

- ✅ **Done (2026-03-27).**
- **Rename:** `cli/src/adapters` → **`cli/src/ttyAdapters`** so the folder name matches **TTY-only** I/O wiring (entry, session, stdout helpers, **`TTYDeps`** type).
- **Move + rename component:** `interactiveTtyInkApp.tsx` → **`cli/src/ui/interactiveApp.tsx`**, export **`InteractiveApp`** (props type **`InteractiveAppProps`**). **`runInteractiveTtySession`** imports it from **`../ui/interactiveApp.js`**.
- **Imports:** **`ShellSessionRoot`** (and any other **`ui/`** that needs **`TTYDeps`**) use **`../ttyAdapters/ttyDeps.js`**; **`InteractiveApp`** imports **`interactiveTtyStdout`** and **`TTYDeps`** from **`ttyAdapters`**. **`interactive.ts`** imports **`runTTY`** from **`./ttyAdapters/ttyEntry.js`**.
- **Docs:** **`.cursor/rules/cli.mdc`** paths updated to **`ttyAdapters`**.
- **Tests:** no import of the old module path expected; full **`pnpm cli:test`** green.

### Phase 6 — Docs and exit

- **`ttyEntry.ts`** approved-bytes list stays accurate.
- Remove or archive this file when done.

## Risks / watch list

- **ESC/input event shape differences** (`key.escape`, `key.name`, raw `\u001b`) surfaced during Phase 1; treat them as one normalized escape signal in interaction handlers.
- **Stale closures** in async **`processInput`** still require ref-backed reads where state freshness matters.
- **OSC timing** still has edge sensitivity; fetch-wait and post-**`processInput`** repaints both route through **`ttyContractEpoch`** + **`useLayoutEffect`** (Phase 2–3).
- **E2E / Gherkin**: delete or rewrite scenarios that **only** encoded removed UI; do **not** preserve steps for deleted features.

## Non-goals

- **Replacing Ink** with another renderer (separate decision).
- **Expanding scope** into **`frontend/`**, **`backend/`**, or **new product features** under this plan — **cli/** (and **minimal** shared test-fixture touch) only.
