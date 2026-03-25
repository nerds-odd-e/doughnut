# CLI interactive UI — Ink migration (remaining work)

Informal plan; update as work proceeds. **Testing:** observable behavior first — `runInteractive` / E2E; see `.cursor/rules/planning.mdc` and `.cursor/rules/cli.mdc`.

---

## North star (phases 1–8 shipped; 9+ = structural / Ink-native shell)

Interactive TTY = **one Ink `render()`** root: **`Static`** for append-only scrollback + **live subtree** driven by **`useInput`** / **`useFocus`** / **`@inkjs/ui`** where it fits (gate 5). **Business domain** owns the meaning of **chat history** and **command turns** (types + transitions); the shell expresses them as **React state** feeding **`Static` items** and live props — not as opaque mutable blobs inside a fat “adapter.”

**End state (this document, phases 9–14):**

- **No `ttyAdapter` monolith** — replace with a **thin TTY I/O + mount** entry (streams, raw mode, documented bridges only) and **Ink-root state** for shell UI.
- **Piped / `-c` and TTY interactive are separate modules** — **no piped-only code** in the TTY interactive path (see phase 9).
- **`patchConsole: true`** on `render()` once the TTY path no longer relies on raw `console.log` fighting Ink (phase 13).
- **Do not patch, fight, or sidestep Ink** for keys/layout Ink already owns; remaining non-Ink bytes are **listed under [Special cases (approved Ink exceptions)](#special-cases-approved-ink-exceptions)**.

**Current shipped (phases 1–8):** while the shell is active, **one stdin / keyboard owner** for command line, confirm, and lists. **readline** / **`keypress`** — only documented residue (Ctrl+C, fetch-wait Esc, list Esc bridge); see JSDoc on **`stdin.on('keypress')`** in **`ttyAdapter.ts`** (to be relocated with thin TTY entry in phase 11).

**Phase 5 (done):** MCQ and token lists use Ink **`RecallMcqChoicesLivePanel`** / **`AccessTokenPickerLivePanel`** in **`liveSelectionGuidanceInk.tsx`** (`useInput` + **`selectListInteraction`**); readline **`keypress`** handles **Esc** on those lists only (bridge). **Phases 3–6 (done):** stop-confirm + session y/n on Ink (**`RecallInkConfirmPanel`**, shared stdin coalescing in **`inkStdinLogicalKeys.ts`**; **`@inkjs/ui` `StatusMessage`** for invalid keys). **Phase 7 (done):** audited **`ttyAdapter`** — no duplicate handlers for command line, confirm, or list keys; **`readline.createInterface` + `emitKeypressEvents`** kept only to attach this listener; list **Esc** bridge kept (documented on the **`keypress`** handler).

**Non-interactive** (`-c`, piped): **`processInput` + `pipedAdapter` only** — not mixed into the TTY interactive module (phase 9).

**Shell rule (replaces fat “adapter”):** TTY entry file may only wire **mechanism** (`TTYDeps` / streams / `render` options). **Domain branching** stays in **`interactive.ts`** (and siblings); **scrollback and turn state** are named domain concepts surfaced to the Ink root (phases 10–11).

**Layout bridge:** `cli/src/renderer.ts` — **shrink for TTY** (phase 12): keep grapheme-aware width/wrap for **piped** `writeFullRedraw` and any shared string builders still needed for props. TTY live column stays Ink **`Text` / `Box`** wrap (phase 1; gate 4).

**Raw stdout:** Phase 13–14 converge on Ink-managed stdout + **`patchConsole: true`**; **`interactiveTtyStdout`** shrinks to **documented non-Ink bytes** (OSC, exit farewell, cursor) only if still required after migration.

---

## Layering (target after phase 11)

| Layer | Role |
|-------|------|
| **Business** | `interactive.ts`, `recall.ts`, … — orchestration; **chat history** and **command turns** as explicit domain concepts and callbacks. |
| **Shell state (Ink)** | Root React state / reducer (or equivalent) — **`Static` items** + live props; updated from business callbacks, not duplicated ad hoc in a legacy adapter. |
| **Interactive UI** | Presentational Ink components + **`useInput`**; dispatch via props; no product rules. |
| **TTY entry (thin)** | `render` options, stdin/stdout, raw mode, **`patchConsole`** (phase 13), documented **`keypress` residue** only where listed in [Special cases](#special-cases-approved-ink-exceptions). |
| **Piped** | `pipedAdapter` + `processInput` path only — **no imports of piped layout from TTY files** (phase 9). |

---

## Ink vocabulary (use in code/comments)

| Avoid | Prefer |
|-------|--------|
| Modal stack | Conditional subtree / stacked UI state at root |
| Adapter “paints” | Props → `<Box>` / `<Text>` / `Static` |
| History scrollback | **`Static`** items — **append-only** (gate 2); do not rewrite prior history lines |

---

## Decision gates (pause and get sign-off)

1. Single Ink root vs islands / hybrid — **stdin ownership** — **resolved:** one Ink root (phases 1–3 shipped).
2. **`Static`** vs rewriting old history lines — **resolved:** **`Static` only** — append-only history scrollback; no in-place mutation of lines already emitted into history. If a future feature needs a mutating line, treat it as **live** subtree or a **new** gate — not silent rewriting of `Static` items.
3. **`useFocus`** / Tab vs ↑↓ in guidance / selection mode — **resolved:** **do not preserve** the legacy TTY model where **↑↓** globally toggled draft command history vs list selection. **New model:** Ink **`useFocus`** (or equivalent); **Tab** / **Shift+Tab** move focus among **focusable regions** in the live column. **↑↓** (and list-specific keys) apply **only inside** the focused region (draft history in the command area when that area is focused; choice highlight when the list/`Select` region is focused). **Phase 4** ships this for the command line + focus plumbing; **phase 5** attaches MCQ/token/slash **`Select`** as a peer focus target. Update **Vitest + E2E** and any **`.cursor/rules/cli.mdc`** terminology that still describes the old global ↑↓ behavior when phase 4 lands.
4. Ink `Text` wrap vs `renderer.ts` grapheme wrap (**CJK/emoji**) — **resolved:** TTY default live column uses Ink `Text` `wrap` inside `Box width={terminalWidth}` for current prompt + guidance; piped path unchanged (`buildLiveRegionLines`, `buildSuggestionLines`). Subtle wrap differences vs grapheme-aware wrap accepted.
5. **`@inkjs/ui`** vs hand-rolled `useInput` — **resolved:** **complete replacement** toward Ink ecosystem — use **`Select`**, **`ConfirmInput`**, **`TextInput`** from **`@inkjs/ui`** when behavior maps **1:1** (or close enough with thin wrappers). If a primitive does not fit, use Ink **`useInput` inside the live subtree** only — **not** a second handler in the **TTY entry** / legacy **`ttyAdapter`**. Pure policy helpers (e.g. submit-line derivation in **`selectListInteraction`**) may stay **called from** Ink handlers; they are not a duplicate stdin path.
6. Visual parity (stage band, borders) — **declined** for this migration; slimmer Ink look OK
7. **`patchConsole`** / `console.log` vs layout corruption — **resolved (direction):** **`patchConsole: true`** in **phase 13** after TTY path routes user-visible output through Ink / `useStdout().write` / domain hooks — not raw `console.log` in the hot path. **Escape hatch:** if a regression cannot be fixed quickly, revert **`patchConsole`** only for that phase and fix forward (do not leave dual strategies long term).

---

## Completed (no separate phase doc needed)

Ink shell, neutral `TTYDeps`, confirm/MCQ/token/fetch-wait display components, **J1** empty-Enter path **`shellInstance.clear()` before `unmount()`** (avoids stacked boxes — **E2E** `cli_interactive_mode` is the strong check), reverse-video caret in **`CommandLineLivePanel`**, one layout snapshot per `drawBox`, resize → rerender without full-screen clear, **`interactiveTtyStdout`** as single owner of adapter `stdout` writes.

### Phase 1 (done) — default live column: Ink `Box` / `Text` wrap

- **`CommandLineLivePanel`** builds the default live block in Ink (stage band, separators, optional grey current prompt, input box, guidance). No **`buildLiveRegionLinesWithCaret`** on this path; removed unused **`LiveRegionLines.tsx`**.
- **`buildSuggestionLinesForInk`** in **`renderer.ts`**: same rows as **`buildSuggestionLines`** but no per-line **`truncateToWidth`**; **`ttyAdapter`** uses it for **`CommandLineLivePanel`** props. Piped **`writeFullRedraw`** still uses **`buildLiveRegionLines`** + **`buildSuggestionLines`** (grapheme-aware).
- **Ink note:** this Ink version’s **`Text`** has no `width` prop — wrap width comes from a parent **`Box width={terminalWidth}`** per wrappable block. Do **not** put **`width={terminalWidth}`** on the **root** live column **`Box`** (breaks resize: box border stayed at old columns when **`stdout.columns`** changed in Vitest).
- **Verify:** `pnpm cli:test`; **`renderer.test.ts`** covers **`buildSuggestionLinesForInk`**.

---

## Remaining phases (numbered)

**Order (historical):** **2 → 3 → 4 → 5 → 6 → 7 → 8** (done).

**Order (extended track):** **9 → 10 → 11 → 12 → 13 → 14** — **remove `ttyAdapter`**, **separate piped from TTY**, **domain-shaped shell state + `Static` / `useInput`**, **shrink `renderer.ts` for TTY**, **`patchConsole: true`**, **final residue audit**.

**Rationale (planning.mdc):** Phases **9** and **12** are **structure-first** (no new user story); justify each with **full interactive Vitest + targeted E2E** unchanged. Phases **10–11** can be split further if two user-visible slices are clearer (e.g. “history append correctness” vs “command turn flush”) — keep **at most one intentionally failing test** per planning TDD note when driving. **Phase 13** is user-visible only as “no corrupted interleaved logs”; treat **`patchConsole`** flip as **verify-heavy**. **Phase 14** is **audit + documentation** of approved exceptions.

### Phase 2 (done) — `useInput` for main command line (gate 1)

- **`CommandLineLivePanel`:** `useInput` + `onCommandKey` / `onInterrupt` (Ctrl+C). Main line behavior lives in **`handleCommandLineInkInput`** in **`ttyAdapter`**. **Shipped with** legacy global ↑↓ semantics (draft history vs selection shared behavior where applicable).
- **`stdin.on('keypress')`:** returns early when **`!isAlternateLivePanel()`** so readline does not handle command-line keys; returns early for stop-confirm + session y/n (**Ink**, phase 3). **Phase 5:** also returns early for MCQ/token list keys except **Esc** (bridge).
- **Vitest:** command line and MCQ/token lists use **`stdin.push`** (**`pushTTYCommandBytes`**, **`pushTTYCommandEnter`**, **`pushTTYCommandKey`**); synthetic **`pressKey`** where readline must fire (**Esc** on token list, MCQ stop-confirm Esc; fetch-wait cancel).
- **Ink `delete`:** treated like backspace in **`handleCommandLineInkInput`** (TTY DEL).
- **Gate 3 note:** **Phase 4** replaces cross-region keyboard semantics; phase 2 remains the base for command-line **`useInput`** wiring.

### Phase 3 (done) — Confirm / session y/n on Ink input

**`@inkjs/ui` `ConfirmInput`** is **not** used: it only supports instant y/n + Enter default, not draft + hints + session empty-Enter noop. Implemented **`ConfirmLivePanel`** (**`useInput`** + **`dispatchRecallSessionConfirmKey`**, presentational **`ConfirmDisplay`**).

- **Ink details:** `draftRef` so the next key sees an up-to-date line before React commits; split coalesced **`printable+\r`/`\n`** chunks (stdin can merge bytes in one read); **`key`** `confirm-stop-recall` vs `confirm-session-yes-no` so React remounts when switching modes (avoids carrying draft state between stop-confirm and session y/n).
- **`ttyAdapter`:** no confirm/session y/n handling on **`keypress`** (early return); **`dispatchSessionYesNoKey`** removed from **`TTYDeps`**.
- **Verify:** `pnpm cli:test`; Vitest uses **`pushTTYCommandBytes` / `pushTTYCommandEnter`** (and **`pushTTYCommandEscape`** from session) for these panels; **`await tick()`** after typing before Enter where needed for Ink scheduling.

### Phase 4 (done) — Focus-based keyboard model (gate 3)

**User outcome:** **↑↓** for draft history / slash-picker cycling run only while the **command-line** Ink region has focus; **Tab** / **Shift+Tab** cycle Ink focus (second region in phase 5). **No extra chrome** (gate 6).

**Shipped:**

- **`CommandLineLivePanel`:** `useFocus` (`command-line`), `useInput` gated with **`inkFocusEverEstablishedRef`**; **`useLayoutEffect`** refocus after Ink clears focus on **Esc** (slash-picker dismiss) so typing still reaches the command line.
- Recall confirm Ink panel: `useFocus` (`recall-ink-confirm`), same gating; **no** Esc refocus (session y/n and stop-confirm handle Esc in-panel).

**Phase 5 shipped:** MCQ/token on Ink list panels; **`liveFocusPhaseFlags.ts`** removed; confirm panel no longer refocuses on Esc.

**Verify:** `pnpm cli:test`; Vitest `interactiveTtySession` (Esc then type); extend E2E only if needed.

### Phase 5 (done) — MCQ and token list: Ink-owned list input (gates 3 & 5)

- **Shipped:** Hand-rolled Ink in **`liveSelectionGuidanceInk.tsx`** (plan fallback vs **`@inkjs/ui`**). Shared stdin coalescing: **`inkStdinLogicalKeys.ts`** (also used by **`RecallInkConfirmPanel`**). Policy in **`selectListInteraction.ts`** (`dispatchSelectListKey`, `selectListKeyEventFromInk`).
- **`ttyAdapter`:** list arrows / Enter / printable draft / backspace go through Ink; **`keypress`** returns early for those modes. **Esc** on MCQ (stop-confirm) and token list (abort) is handled on **`keypress`** as a small bridge.
- **Focus:** **`LIVE_SELECTION_GUIDANCE_INK_FOCUS_ID`**; command line keeps Esc refocus **`useLayoutEffect`**.
- **Layout:** Exported **`formatMcqChoiceLinesWithIndices`** for grapheme-wrapped MCQ lines + highlight by choice index in the list panel.

### Phase 6 (done) — Simpler recall confirms + `@inkjs/ui` `ConfirmInput`

**User outcome:** Stop-recall and session y/n are **single-key y / n** (Enter alone = **no**); invalid keys show **`StatusMessage`** once. No line editor / typed **`yes`**/**`no`** on TTY.

**Decisions (locked):**

| Topic | Choice |
|-------|--------|
| Typed `yes` / `no` + Enter | **Not supported** on TTY — **y** / **n** only. Piped **`processInput`** uses **`parseRecallPipedYesNo`**. |
| Wrong keys | **`@inkjs/ui` `StatusMessage`** variant **`error`** (e.g. “Please press y or n”). |
| Session + stop: bare Enter | **`defaultChoice="cancel"`** semantics (Enter = **no**); one path for both (replaces session empty-Enter **noop**). |

**Shipped:** **`@inkjs/ui`** (**`StatusMessage`**). **`cli/src/ui/RecallInkConfirmPanel.tsx`** + **`cli/src/interactions/recallYesNo.ts`** (Ink outcomes, stop-confirm model, piped parse). **`dispatched`** guard per stdin callback for **`y`+Enter** coalescing. **`ttyAdapter`:** **`getRecallStopConfirmInkModel`**, snapshot field **`recallStopConfirmInkModel`**.

**Verify:** `pnpm cli:test`; **`pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature`**; `pnpm cli:lint` / format.

### Phase 7 (done) — Complete replacement gate (mandatory)

**Audit:** No second path for command-line keys, recall y/n, or list arrows / Enter / typing — all Ink **`useInput`**. **Removed:** redundant trailing no-ops in the **`keypress`** handler (list keys already fell through). **Intentional residue** (see JSDoc on **`stdin.on('keypress')`** in **`ttyAdapter.ts`**): Ctrl+C; fetch-wait Esc (**`FetchWaitDisplay`** has no `useInput`); MCQ/token **Esc** bridge when stdin ordering is unfriendly to Ink. **`readline.Interface`** output is **`noopOutput`** — no **`line`** event path.

- **Verify:** `pnpm cli:test` interactive suite; **`ttyAdapter`** grep: **`keypress`** / **`readline`** only as above.

### Phase 8 (done) — ink-ui polish

Interactive fetch-wait: **`@inkjs/ui` `Spinner`** (`type="dots"`) in **`FetchWaitDisplay`**; removed adapter **`setInterval`** ellipsis tick + **`INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS`**. Stage-band layout string is static blue label (**`interactiveFetchWaitStageIndicatorLine`**) for **`needsGapBeforeBox`** / piped paths only.

### Phase 9 — Piped vs TTY module boundary

**Goal:** **Zero piped / `writeFullRedraw` / non-interactive layout imports** inside the TTY interactive module (today’s **`ttyAdapter.ts`** and anything that replaces it for **`runTTY`**). Piped stays **`pipedAdapter`** + its call chain only.

- **Outcome:** Clear dependency direction: **TTY shell** → business + Ink; **piped** → **`processInput`** + grapheme **`renderer`** helpers — **no shared file** that branches “if piped” inside TTY code.
- **Verify:** Grep gate (no piped symbols in TTY module); **`pnpm cli:test`** interactive + **`processInput.test.ts`**; spot E2E if imports moved.

### Phase 10 — Domain: chat history + command turns as first-class concepts

**Goal:** **`ChatHistory`** / scrollback entries and **command-turn** buffering are **named types and transitions** in the **business** layer (or a small **`cli/src/shell/`** module owned by business), with stable verbs (append output, commit input line, flush turn, etc.) — **not** ad-hoc arrays only inside a legacy adapter.

- **Ink mapping:** **`Static` `items`** = function of domain history (append-only, gate 2); live region = function of current turn + stage — **one directional flow** from domain updates to props.
- **Verify:** Same observable transcripts as today (Vitest **`runInteractive`**, key E2E); optional **narrow unit** tests on pure transition helpers if they become the stable contract.

### Phase 11 — Remove `ttyAdapter`: thin TTY entry + Ink root state

**Goal:** Delete the **`ttyAdapter` monolith** — replace with **(a)** a **thin TTY session file** (mount Ink, streams, raw mode, **`exitOnCtrlC`**, documented **`keypress` residue** only per [Special cases](#special-cases-approved-ink-exceptions)) and **(b)** **Ink root** (`InteractiveShellDisplay` or successor) holding **React state / reducer** that subscribes to domain callbacks instead of mirroring state in closure variables beside Ink.

- **Principle:** **Rerender** from React state updates; avoid **manual `drawBox`** orchestration that duplicates Ink’s update cycle unless a listed **special case** requires it.
- **Verify:** Full interactive Vitest suite + **`cli_interactive_mode`** / recall E2E as appropriate; **J1** empty-Enter **`clear` before `unmount`** still holds.

### Phase 12 — Greatly shrink `renderer.ts` for TTY

**Goal:** **TTY path** stops depending on large grapheme **live-region line builders** where Ink already wraps (gate 4). **`renderer.ts`** keeps **piped** column-exact **`writeFullRedraw`**, **`renderBox`**, **`truncateToWidth`**, shared tone/ANSI helpers, and any **string props** still passed into Ink (MCQ lines, separators) **without duplicating** a second layout engine.

- **Verify:** **`renderer.test.ts`** for retained piped/shared helpers; interactive tests for TTY wrap unchanged in user-visible terms.

### Phase 13 — `patchConsole: true` + Ink-idiomatic stdout

**Goal:** **`render(..., { patchConsole: true })`**; route intentional logging through **`useStdout().write`** or domain **`OutputAdapter`** hooks that cooperate with Ink — **no** raw **`console.log`** on the interactive TTY hot path fighting the tree. Shrink **`interactiveTtyStdout`** to what Ink cannot own (see phase 14 + special cases).

- **Gate 7:** Closed here per [Decision gates](#decision-gates-pause-and-get-sign-off).
- **Verify:** Interactive Vitest + E2E; **`pnpm cli:lint`**. **Stop if:** persistent corruption — temporarily revert **`patchConsole`** only as a **short** escape hatch while fixing root cause.

### Phase 14 — Residue audit + special cases doc in code

**Goal:** Single checklist of **approved non-Ink** behavior (OSC, exit farewell, cursor, **readline `keypress`** bridges). Remove any **unlisted** sideways hacks. Update **`.cursor/rules/cli.mdc`** if terminology moves from “ttyAdapter” to “TTY session entry” / “Ink shell root.”

- **Verify:** Grep + JSDoc on the thin TTY file; no duplicate stdin handlers beyond the list.

---

## Special cases (approved Ink exceptions)

These are **intentional** places the stack is **not** pure Ink — document **why** next to the code and keep the list minimal.

| Case | Why Ink alone is insufficient (today) |
|------|----------------------------------------|
| **Private OSC** **`INTERACTIVE_INPUT_READY_OSC`** | Invisible integrator signal (PTY / shell integration); not a React layout concern. |
| **Hardware cursor hide/show + exit farewell** | Lifecycle **after** `unmount` / outside Ink’s paint cycle for some paths. |
| **readline `keypress`** (Ctrl+C, fetch-wait Esc, list Esc bridge) | **Phase 7** residue: stdin ordering / components without `useInput` for that key; **prefer eliminating** each bridge inside later sub-steps if Ink can own it without fighting. |
| **Full-screen clear (`/clear`)** | May remain explicit CSI via thin stdout layer if not expressed as `Static` reset. |

**Rule:** If a new exception is needed, **add it here and in JSDoc** in the same PR — no silent “just this once” `process.stdout.write`.

---

## Replacement bar (summary)

| Mode | Owner after migration | Thin TTY entry / readline |
|------|------------------------|---------------------------|
| Main command line | Ink **`useInput`** + **focus** (phases 2 + 4) | No duplicate key handling |
| Confirm / y/n | **`RecallInkConfirmPanel`** + **`@inkjs/ui` `StatusMessage`** (phase 6) | No duplicate |
| Lists (MCQ, tokens, selection) | **`RecallMcqChoicesLivePanel`** / **`AccessTokenPickerLivePanel`** + **`selectListInteraction`**; Esc bridge on **`keypress`** until removed | No duplicate list keys except **listed** Esc bridge |
| Scrollback / turns | Domain model → **`Static` items** + live props (phases 10–11) | Not stored only in legacy adapter closures |
| Piped / `-c` | **`pipedAdapter`** + **`renderer`** grapheme path | **Not** in TTY module (phase 9) |
| Residue | — | **Phase 14** + [Special cases](#special-cases-approved-ink-exceptions) (replaces “see ttyAdapter JSDoc” as sole doc) |

---

## What the UI layer is not

Not **business rules**, not a second **`processInput`**. **Not** a place for **piped** or **`-c`** layout. **Not** a second mutable copy of **chat history** / **command turns** that disagrees with domain state (phases 10–11). Domain branching stays in **`interactive.ts`** (and related modules), not in the thin TTY file.

---

## References (Context7)

- **Ink** — `vadimdemedes/ink`: `render` (options: **`patchConsole`**, custom **`stdout`**), `Box`, `Text`, **`Static`**, **`useInput`**, **`useStdout`** / **`write`**, `useApp`, `useFocus`, instance **`clear`** / **`unmount`**.
- **Ink UI** — `vadimdemedes/ink-ui`: `TextInput`, `ConfirmInput`, `Select`, `Spinner`, `ProgressBar`.
