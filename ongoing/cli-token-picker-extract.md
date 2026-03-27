# CLI: TokenPicker extraction and list-access-token stage rename

Informal plan. **Not executed yet** — implementation follows in a later pass.

## Goal

- **Single place** for access-token **picker UI state** (highlight + open payload: items, command, action) and **key handling** (`dispatchSelectListKey`, submit → `setDefault` / `remove` / `removeAccessTokenCompletely` + past-message patch).
- **`ShellSessionRoot`** no longer receives `tokenSelection`; it **delegates** the token-picker branch and the related **leading snapshot** lines (stage indicator + wrapped `currentPrompt` for `TOKEN_LIST_COMMANDS`) to **TokenPicker**.
- **`InteractiveApp`** does **not** read or pass `tokenSelection` (no leak of picker internal shape).
- Keep **`/list-access-token`** (and remove / remove-completely) behavior; rename the current “list stage” module so the name matches **only** the `/list-access-token`-family command stage, not a generic token list.

## Current state (reference)

| Concern | Where it lives today |
|--------|----------------------|
| `tokenSelection` React state + ref + `applyAccessTokenListNavigation` + `handleTokenPickerKey` / `onTokenPickerGuidanceKey` | `cli/src/ui/accessTokenListStage.tsx` (`useAccessTokenListStage`) |
| `tokenSelection` prop → `computeLiveColumnLeadingSnapshot`, `buildLivePanel` → `AccessTokenPickerLivePanel` | `cli/src/ui/ShellSessionRoot.tsx` |
| Wires hook → `ShellSessionRoot`, `applyAccessTokenListNavigation` → `useInteractiveShellStage` | `cli/src/ui/interactiveApp.tsx`, `cli/src/ui/interactiveShellStage.tsx` |
| Type `TokenSelectionState` | `cli/src/shell/shellSessionState.ts` (not part of `ShellSessionState`, but named alongside it) |

`handleTokenPickerKey` is only used via `onTokenPickerGuidanceKey` (not consumed elsewhere).

## Target architecture

1. **`TokenPicker`** (new module under `cli/src/ui/`, e.g. `TokenPicker.tsx`, optionally a tiny `tokenPickerContext.tsx` if that keeps cohesion)

   - **Owns** picker state: whether the picker is open, `items` / `command` / `action`, and **`highlightIndex`** (all internal; no prop-up to `InteractiveApp`).
   - **Owns** manipulation moved from the current hook: `selectListKeyEventFromInk` → `dispatchSelectListKey` → highlight moves, Esc abort, Enter submit, async remove-completely + `patchSessionAfterTokenListClose`-style session updates (same observable behavior as today).
   - **Surfaces** to the shell only what is needed to render and wire Ink:
     - Either a **React context** (provider mounted once in `InteractiveApp`, consumers in `ShellSessionRoot` + list-stage hook), **or** a ref/imperative handle — **preference: context** so `ShellSessionRoot` can read “is picker active?” and “render live panel slice + leading inputs” without `tokenSelection` props through `InteractiveApp`.
   - **Public API** for the list command hook: e.g. `openPickerFromSlashCommand(...)`, `applyNavigation(...)` (if navigation union is still needed), **without** exposing `TokenSelectionState` to parents. Slash submit result can carry **`{ items, command, action }` only**; **initial highlight** is computed **inside** TokenPicker on open (same rule as today: default label index, fallback `0`).

2. **`ShellSessionRoot`**

   - Drop `tokenSelection` from props.
   - When the picker context says active, delegate **leading snapshot** (`tokenListConfig` from `TOKEN_LIST_COMMANDS`, `greyCurrentStageIndicatorLabel`, wrapped `currentPrompt`) and **live panel** (`AccessTokenPickerLivePanel` wiring: `stageIndicatorLine`, `currentPromptLines`, `items`, `defaultLabel`, `highlightIndex` from context/getter, `onGuidanceListKey` → context handler) to **TokenPicker** (or to context values produced by TokenPicker’s provider).
   - Preserve ordering vs fetch-wait, stop-confirm, recall y/n, MCQ, default command line (no behavior change).

3. **`ListAccessTokenCommandStage`** (rename)

   - Rename file: `accessTokenListStage.tsx` → **`listAccessTokenCommandStage.tsx`**.
   - Rename hook: `useAccessTokenListStage` → **`useListAccessTokenCommandStage`** (and context type names accordingly).
   - **Keep** this module focused on **`/list-access-token` / `/remove-access-token` / `/remove-access-token-completely`** slash submit path: `tryHandleTokenListSlashSubmit`, user line commit / “no tokens” messaging, `rememberCommittedLine`, etc.
   - **Remove** picker state and key-dispatch implementation from this file; **call TokenPicker** (context) to open / close instead of `useState` + `applyAccessTokenListNavigation` updating parent-visible state.
   - Rename exported navigation type if it’s no longer “list stage”–specific (e.g. **`TokenPickerNavigation`**) and define it next to TokenPicker; list-stage hook returns submit results that reference that type.

4. **`InteractiveApp`**

   - Mount **TokenPicker provider** (or equivalent) **once** around the subtree that contains `ShellSessionRoot`.
   - Stop passing `tokenSelection` into `ShellSessionRoot`.
   - Keep passing **callbacks** that don’t expose picker state: e.g. `tryHandleTokenListSlashSubmit` still comes from the renamed hook; **`applyAccessTokenListNavigation`** either disappears (replaced by `tokenPicker.applyX` on context) or becomes a thin forwarder that **does not** surface `TokenSelectionState` through `InteractiveApp`’s own state.

5. **`interactiveShellStage.tsx`**

   - Update imports from renamed module and navigation type.
   - **`applyAccessTokenListNavigation`**: sourced from TokenPicker context or from renamed hook that only forwards to context — **no** `tokenSelection` field on objects flowing through `InteractiveApp`.

6. **Types**

   - **`TokenSelectionState`**: either **moved** into the TokenPicker module (preferred if nothing else needs it) or **shrunk** / replaced by an internal type so `shellSessionState.ts` doesn’t describe UI picker shape. Update imports across `ShellSessionRoot`, tests if any.

## Phases (scenario-first, small slices)

**Phase 1 — Behavior-preserving extraction**

- Introduce TokenPicker (context + provider + internal state + key handling moved from current hook).
- ShellSessionRoot consumes context; remove `tokenSelection` prop; InteractiveApp uses provider and stops passing `tokenSelection`.
- List-stage hook opens picker via context; slash submit still handles “no tokens” and user-line commit locally.
- Run **`CURSOR_DEV=true nix develop -c pnpm cli:test`** (and any existing interactive TTY specs that touch token commands if present).

**Phase 2 — Rename module and public symbols**

- Rename file and hook; re-export or update all imports (`interactiveApp`, `interactiveShellStage`, any others).
- Optionally adjust **cli.mdc** only if it names `accessTokenListStage` explicitly (minimal doc delta).

**Phase 3 — Cleanup**

- Remove dead exports; ensure `TokenSelectionState` lives in one coherent place.
- If mutation tests are used for touched modules, run targeted Stryker per `mutation-testing.mdc` only if the team wants extra confidence on the new boundary.

## Testing notes (planning.mdc)

- **Observable:** prefer existing **`runInteractive`** / TTY tests and **`processInput`** paths over asserting internal picker state.
- **E2E:** if `e2e_test/features/cli/` already covers access-token listing/removal, run the **relevant `--spec`** after implementation; no new E2E required unless coverage is missing for remove/remove-completely.

## Open decisions (resolve during implementation)

- **Context vs ref:** Context is the default so `ShellSessionRoot` and the list-stage hook share one picker instance without prop drilling.
- **Where provider mounts:** `InteractiveApp` immediately inside or outside `ShellSessionRoot` — whichever keeps the fewest redundant re-renders and matches existing `ttyOutput` / `patch` wiring.

## Out of scope (unless requested later)

- Changing copy, `TOKEN_LIST_COMMANDS` text, or Ink focus rules.
- Non-TTY / `processInput` token flows (if any) — only touch if they share the same types and break compile.
