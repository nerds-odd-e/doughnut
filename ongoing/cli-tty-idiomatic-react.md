# CLI: shrink, simplify, idiomatic Ink + React

Informal plan. Delete or shrink when this refactor is done or parked.

## North star

- **Reduce line count and conceptual surface area.** The CLI has **limited features** today; the implementation is **bloated**. Every phase should prefer **fewer files, fewer types, fewer indirections** unless a layer clearly pays for itself.
- **Idiomatic Ink + React** is a main tactic: **one `render()`**, state in **hooks** (`useReducer` / `useState`), **no** `drawBox` + **`shellInstance.rerender`** + closure **`patch` / `patchAndDraw`** in `interactiveTtySession.ts`.
- **UX / UI may change** when that **materially simplifies** the code or aligns with normal Ink patterns. Document notable UX deltas in **`cli.mdc`** and **E2E / Vitest** where behavior is still worth locking.
- **Delete dead code and the tests that exist only to cover it** in the same change. Do **not** keep tests as anchors for removed product behavior.
- **Remove excessive abstraction**: merge thin wrappers, collapse props-drilling layers, inline one-off helpers, fold **`ShellSessionRoot` / `InteractiveShellDisplay` / live panels** where a single tree reads clearer after the React root move.

## Status snapshot (2026-03-27)

- **Phase 1 is implemented and green in `cli` tests.**
- `runInteractiveTtySession` now uses a **single Ink mount** and a reducer-driven React root (`InteractiveTtyInkApp` inside the adapter file).
- The old imperative repaint loop is removed: no adapter-level `drawBox`, `patchAndDraw`, or manual `shellInstance.rerender`.
- `ShellSessionRoot` remains as the view boundary for now (not inlined yet).
- No Phase 2 architecture move yet: terminal side-effect contract still runs via adapter-owned logic, even though render lifecycle is now React-driven.

## Current shape (after Phase 1)

- **`runInteractiveTtySession`** mounts Ink **once** and renders an internal app component with **React-owned session state**.
- State transitions are routed through reducer patching + refs so async handlers read current state without closure drift.
- **`ShellSessionRoot`** continues to be a pure function of **`session` + `deps` + `handlers`**.
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

### Phase 0 — Inventory and cut dead weight (optional first slice)

- Find **unused exports, unreachable branches, duplicate helpers**, and tests that **only** exist for them → **delete together**.
- Quick wins before structural moves reduce merge pain.

### Phase 1 — React root + remove `drawBox`

- ✅ Implemented.
- `runInteractiveTtySession` now performs one Ink `render` and no longer drives UI through imperative redraw helpers.
- Kept `ShellSessionRoot` as-is for this phase to minimize behavior churn and keep Phase 2 isolated.
- Related interactive Vitest coverage was adjusted to assert observable outcomes where repaint-byte timing changed under the new render model.

### Phase 2 — Terminal side effects + external signals

- **`handleShellRendered` / `finalizeInteractiveLiveRegionPaint`** → **`useLayoutEffect`**; **`onInteractiveFetchWaitChanged`** → **dispatch** or tick that triggers the same effect — **no** one-off repaint escape hatch.

### Phase 3 — Simplify handlers and deps wiring

- **Fewer** `useCallback` layers if **dispatch** + **thin actions** suffice; **merge** `TTYDeps` facets only where it drops boilerplate without hiding domains.
- Revisit **`OutputAdapter`** shape: if **`writeCurrentPrompt` / `beginCurrentPrompt`** split exists only for historical TTY quirks, **unify** when UX allows.

### Phase 4 — UI structure and remaining fat

- **`InteractiveShellDisplay` vs `liveColumnInk` vs panels**: **merge or flatten** until the tree matches **one mental model** (past transcript + live column).
- **UX tweaks** allowed: fewer bands, simpler prompts, Ink-native components — **update** `cli.mdc` and **trim** E2E steps that asserted obsolete chrome.

### Phase 5 — Docs and exit

- **`ttyEntry.ts`** approved-bytes list stays accurate.
- Remove or archive this file when done.

## Risks / watch list

- **ESC/input event shape differences** (`key.escape`, `key.name`, raw `\u001b`) surfaced during Phase 1; treat them as one normalized escape signal in interaction handlers.
- **Stale closures** in async **`processInput`** still require ref-backed reads where state freshness matters.
- **OSC timing** still has edge sensitivity; Phase 2 should consolidate side effects in one render-tied place.
- **E2E / Gherkin**: delete or rewrite scenarios that **only** encoded removed UI; do **not** preserve steps for deleted features.

## Non-goals

- **Replacing Ink** with another renderer (separate decision).
- **Expanding scope** into **`frontend/`**, **`backend/`**, or **new product features** under this plan — **cli/** (and **minimal** shared test-fixture touch) only.
