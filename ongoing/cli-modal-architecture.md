# CLI interactive UI — Ink migration (remaining work)

Informal plan; update as work proceeds. **Testing:** observable behavior first — `runInteractive` / E2E; see `.cursor/rules/planning.mdc` and `.cursor/rules/cli.mdc`.

**Shipped (incl. TTY-only refactor):** Interactive shell is **TTY-only** (`runInteractive` → `runTTY`). **Non-TTY** `runInteractive` **exits** with an error; **`run.ts` rejects `-c`** (and `help` subcommand). **`pipedAdapter.ts` is removed** — there is **no** piped-stdin shell or `-c` script path. **`processInput`** remains the **shared command engine** wired from the TTY session and from **Vitest** via **`defaultOutput`**. **Phase 9.5 (done):** **`/clear`** removed; **`writeFullRedraw`**, **`renderFullDisplay`**, **`clearAndRedraw`**, and TTY **`clearScreen`** hook removed with it. **`doughnut help`** is **gone** (use **`/help`** in the TTY). **Command line buffer:** **single-line only** — no Shift+Enter newline; paste/newlines collapse via **`singleLineCommandDraft`** (`interactiveCommandInput.ts`); **`buildCommandInputDraftLines`** paints one row; persisted ↑↓ history entries are normalized on load.

## Plan invariant (non-negotiable)

There is **no** product support for **piped stdin** as an interactive shell and **no** **`-c`** (or equivalent). **Do not** add code, flags, or adapters that restore either. The only interactive UX is **TTY** + Ink; **`processInput` with `defaultOutput`** is a **test / direct-call harness** for the same engine — **not** a second user-facing mode and **not** “piped support.”

---

## North star (phases 1–9.5 shipped; **10+** = structural / Ink-native shell)

Interactive TTY = **one Ink `render()`** root: **`Static`** for append-only scrollback + **live subtree** driven by **`useInput`** / **`useFocus`** / **`@inkjs/ui`** where it fits (gate 5). **Business domain** owns the meaning of **chat history** and **command turns** (types + transitions); the shell expresses them as **React state** feeding **`Static` items** and live props — not as opaque mutable blobs inside a fat “adapter.”

**End state (this document, phases 10–14 incl. **10.5**; phases 9–9.5 TTY boundary + `/clear` removal shipped):**

- **No `ttyAdapter` monolith** — replace with a **thin TTY I/O + mount** entry (streams, raw mode, documented bridges only) and **Ink-root state** for shell UI.
- **TTY interactive module** stays free of **non-shell** layout drivers — **`renderer.ts`** vs Ink is **one** product surface (TTY); there is **no** second stdin-driven shell, **no** `pipedAdapter`, and **no** branching on “piped vs TTY” for the interactive shell.
- **`patchConsole`** on `render()` when `console.Console` is constructible (real Node TTY; **off** under Vitest `spyOn(console, …)` so Ink’s patch-console does not throw) — phase **13** shipped.
- **Do not patch, fight, or sidestep Ink** for keys/layout Ink already owns; remaining non-Ink bytes are **listed under [Special cases (approved Ink exceptions)](#special-cases-approved-ink-exceptions)**.
- **Cursor and command input** follow **Ink / `@inkjs/ui` convention** (e.g. **`TextInput`** or Ink’s caret behavior) — **no obligation** to keep today’s reverse-video caret, bordered box, or hidden-hardware-cursor pairing if something else fits Ink better (**phase 10.5 (done)** removed the bordered input box from **`CommandLineLivePanel`**; **gate 8** / **phase 11.5** for caret/**`TextInput`** if not folded into **11**).
- **`/clear`:** **Removed in phase 9.5** — no product command, no test harness special case, no leftover “optional clear” hooks whose only honest caller was `/clear` (gate 9 executed as delete).

**Current shipped (phases 1–9):** while the shell is active, **one stdin / keyboard owner** for command line, confirm, and lists. **readline** / **`keypress`** — only documented residue (Ctrl+C, fetch-wait Esc, token-list Esc bridge); see module JSDoc on **`ttyEntry.ts`** and **`stdin.on('keypress')`** in **`interactiveTtySession.ts`** (phase **14**).

**Phase 5 (done):** MCQ and token lists use Ink **`RecallMcqChoicesLivePanel`** / **`AccessTokenPickerLivePanel`** in **`liveSelectionGuidanceInk.tsx`** (`useInput` + **`selectListInteraction`**); readline **`keypress`** handles **Esc** on those lists only (bridge). **Phases 3–6 (done):** stop-confirm + session y/n on Ink (**`RecallInkConfirmPanel`**, shared stdin coalescing in **`inkStdinLogicalKeys.ts`**; **`@inkjs/ui` `StatusMessage`** for invalid keys). **Phase 7 (done):** audited **`ttyAdapter`** — no duplicate handlers for command line, confirm, or list keys; **`readline.createInterface` + `emitKeypressEvents`** kept only to attach this listener; list **Esc** bridge kept (documented on the **`keypress`** handler).

**`processInput`:** Shared command engine for the TTY adapter and for **tests** via **`defaultOutput`**; **not** a second interactive UI.

**Shell rule (replaces fat “adapter”):** TTY entry file may only wire **mechanism** (`TTYDeps` / streams / `render` options). **Domain branching** stays in **`interactive.ts`** (and siblings); **scrollback and turn state** are named domain concepts surfaced to the Ink root (phases **10 → 11**; **10.5** is input chrome only; **11.5** gate-8 caret / **11.6** remaining **`chalk`** paint migration).

**Layout bridge:** `cli/src/renderer.ts` + **`cli/src/terminalLayout.ts`** (phase **12**): grapheme-aware width/wrap/truncate in **`terminalLayout`**; **`renderer`** holds command-line paint, MCQ/token list strings, stage/separator/tone. **`writeFullRedraw` / `renderFullDisplay` / `clearAndRedraw`** removed with **`/clear`** (phase 9.5). Command-line paint for Ink is **`formatInteractiveCommandLineInkRows`** (+ stage/separator helpers); **no** legacy string `buildLiveRegionLines` / **`renderBox`**. **Not** piped stdin, **not** **`-c`**, **not** a second product UI. TTY live column stays Ink **`Text` / `Box`** wrap (phase 1; gate 4).

**Raw stdout:** Phase 13–14 converge on Ink-managed stdout + **`patchConsole: true`**; **`interactiveTtyStdout`** shrinks to **documented non-Ink bytes** (OSC, exit farewell) and **cursor hooks only if Ink does not fully own the caret** after gate 8.

---

## Layering (target after phase 11)

| Layer | Role |
|-------|------|
| **Business** | `interactive.ts`, `recall.ts`, … — orchestration; **chat history** and **command turns** as explicit domain concepts and callbacks. |
| **Shell state (Ink)** | Root React state / reducer (or equivalent) — **`Static` items** + live props; updated from business callbacks, not duplicated ad hoc in a legacy adapter. |
| **Interactive UI** | Presentational Ink components + **`useInput`**; dispatch via props; no product rules. |
| **TTY entry (thin)** | `render` options, stdin/stdout, raw mode, **`patchConsole`** when supported (phase **13**), documented **`keypress` residue** only where listed in [Special cases](#special-cases-approved-ink-exceptions). |
| **`processInput` + test console** | **Vitest** (or rare direct calls) exercise **`processInput`** with **`defaultOutput`** — same **command engine** as TTY, **not** a piped shell or **`-c`**. **After phase 9.5:** no **`/clear`** branch; no test-only full-screen redraw retained **only** for that command. |

---

## Ink vocabulary (use in code/comments)

| Avoid | Prefer |
|-------|--------|
| Modal stack | Conditional subtree / stacked UI state at root |
| Adapter “paints” | Props → `<Box>` / `<Text>` / `Static` |
| History scrollback | **`Static`** items — **append-only** (gate 2); do not rewrite prior history lines |
| Reverse-video caret + hidden HW cursor | Ink / **`TextInput`** caret and focus (gate 8) |

---

## Decision gates (pause and get sign-off)

1. Single Ink root vs islands / hybrid — **stdin ownership** — **resolved:** one Ink root (phases 1–3 shipped).
2. **`Static`** vs rewriting old history lines — **resolved:** **`Static` only** — append-only history scrollback; no in-place mutation of lines already emitted into history. If a future feature needs a mutating line, treat it as **live** subtree or a **new** gate — not silent rewriting of `Static` items.
3. **`useFocus`** / Tab vs ↑↓ in guidance / selection mode — **resolved:** **do not preserve** the legacy TTY model where **↑↓** globally toggled draft command history vs list selection. **New model:** Ink **`useFocus`** (or equivalent); **Tab** / **Shift+Tab** move focus among **focusable regions** in the live column. **↑↓** (and list-specific keys) apply **only inside** the focused region (draft history in the command area when that area is focused; choice highlight when the list/`Select` region is focused). **Phase 4** ships this for the command line + focus plumbing; **phase 5** attaches MCQ/token/slash **`Select`** as a peer focus target. Update **Vitest + E2E** and any **`.cursor/rules/cli.mdc`** terminology that still describes the old global ↑↓ behavior when phase 4 lands.
4. Ink `Text` wrap vs grapheme-aware layout (**CJK/emoji**) — **resolved:** TTY default live column uses Ink `Text` `wrap` inside `Box width={terminalWidth}` for current prompt + guidance; **`buildSuggestionLinesForInk`** (no per-line truncate) + **`formatInteractiveCommandLineInkRows`** (truncate/pad) feed strings into Ink where needed; shared width math lives in **`terminalLayout.ts`**. That path is **not** “piped mode” — **no** stdin shell, **no** **`-c`**. Subtle wrap differences vs grapheme-aware wrap accepted.
5. **`@inkjs/ui`** vs hand-rolled `useInput` — **resolved:** **complete replacement** toward Ink ecosystem — use **`Select`**, **`ConfirmInput`**, **`TextInput`** from **`@inkjs/ui`** when behavior maps **1:1** (or close enough with thin wrappers). If a primitive does not fit, use Ink **`useInput` inside the live subtree** only — **not** a second handler in the **TTY entry** / legacy **`ttyAdapter`**. Pure policy helpers (e.g. submit-line derivation in **`selectListInteraction`**) may stay **called from** Ink handlers; they are not a duplicate stdin path.
6. Visual parity (stage band, borders) — **declined** for this migration; slimmer Ink look OK
7. **`patchConsole`** / `console.log` vs layout corruption — **resolved (direction):** **`patchConsole: true`** in **phase 13** after TTY path routes user-visible output through Ink / `useStdout().write` / domain hooks — not raw `console.log` in the hot path. **Escape hatch:** if a regression cannot be fixed quickly, revert **`patchConsole`** only for that phase and fix forward (do not leave dual strategies long term).
8. **Cursor + input box (TTY)** — **resolved:** Follow **Ink’s convention**; **input box UI/UX may change** (e.g. **`@inkjs/ui` `TextInput`**, Ink-native focus/caret). Do **not** keep reverse-video caret + `HIDE_CURSOR` / manual caret sync **for nostalgia** — drop that model when migrating if Ink-native input is simpler. **Rollout:** **Phase 10.5** may remove the **bordered** input chrome (or use **background-only** if that stays simpler than a border — see **Phase 10.5** below). **North star** is **mostly or all `TextInput`** where it maps cleanly; **phase 11** may keep the current **`useInput`** command line if **11** is scoped to thin TTY + Ink root — then **phase 11.5** (or a later numbered phase) does the **`TextInput`** swap + test/E2E expectation updates. **Each phase:** full test pass, **no dead code** (no duplicate editors, no abandoned caret path).
9. **`/clear` command** — **resolved (execution = phase 9.5):** **Remove completely** — see **Phase 9.5** below. **No** product requirement to preserve it; **no** “keep a lighter `/clear`” without a **new** gate.

---

## Completed (no separate phase doc needed)

Ink shell, neutral `TTYDeps`, confirm/MCQ/token/fetch-wait display components, **J1** empty-Enter path **`shellInstance.clear()` before `unmount()`** (avoids stacked boxes — **E2E** `cli_interactive_mode` is the strong check), reverse-video caret in **`CommandLineLivePanel`**, one layout snapshot per `drawBox`, resize → rerender without full-screen clear, **`interactiveTtyStdout`** as single owner of adapter `stdout` writes.

### Phase 1 (done) — default live column: Ink `Box` / `Text` wrap

- **`CommandLineLivePanel`** builds the default live block in Ink (stage band, separators, optional grey current prompt, **`formatInteractiveCommandLineInkRows`**, guidance).
- **`buildSuggestionLinesForInk`** in **`renderer.ts`**: slash completion / hint rows for **`CommandLineLivePanel`** without per-line **`truncateToWidth`** (Ink **`Text` `wrap`**). **Phase 12:** removed duplicate **`buildSuggestionLines`** (pre-truncated variant). **Before phase 9.5:** **`writeFullRedraw`** via **`defaultOutput`** existed for **`/clear`** in **`processInput` tests**; **9.5 removes** that path — **not** a user-facing piped layout path.
- **Ink note:** this Ink version’s **`Text`** has no `width` prop — wrap width comes from a parent **`Box width={terminalWidth}`** per wrappable block. Do **not** put **`width={terminalWidth}`** on the **root** live column **`Box`** (breaks resize: box border stayed at old columns when **`stdout.columns`** changed in Vitest).
- **Verify:** `pnpm cli:test`; **`renderer.test.ts`** covers **`buildSuggestionLinesForInk`**.

---

## Remaining phases (numbered)

**Order (historical):** **2 → 3 → 4 → 5 → 6 → 7 → 8** (done).

**Order (extended track):** **~~9~~ (done)** → **~~9.5~~ (done)** → **~~10~~ (done)** → **~~10.5~~ (done)** → **~~11~~ (done)** → **~~11.5~~ (done)** → **~~11.6~~ (done)** → **~~12~~ (done)** → **~~13~~ (done)** → **~~14~~ (done)** — **~~10.5~~** = command-line **input chrome**: bordered `Box` removed from **`CommandLineLivePanel`** (borderless prompt row + full-width paint lines); **11** = thin TTY + Ink root state (today’s command-line **`useInput`** + reverse-video caret, no bordered input box); **~~11.5~~** = **gate 8** caret aligned with **`@inkjs/ui` `TextInput`** (`chalk.inverse` + fetch-wait disabled **`TextInput`**); editable multiline line still one **`useInput`** (see phase **11.5** note); **~~11.6~~** = **`terminalChalk`** (`Chalk({ level: 3 })`) + chalk-wrapped paint in **`renderer`**, **`listDisplay`**, **`interactiveTtyStdout`**; **`ansi.ts`** = cursor + **`RESET`** only; **~~12~~** = **`terminalLayout.ts`** + smaller **`renderer.ts`**, removed duplicate TTY **`buildSuggestionLines`** / dead **`ShellSessionRoot`** suggestion branches; **~~13~~** = **`patchConsole`** when `console.Console` works (real TTY); pre-Ink banner via **`process.stdout.write`**; **`index.test`** `process.exit` mock throws so **`exitCliError` `never`** is not violated under mock; **~~14~~** = **residue audit** + **`.cursor/rules/cli.mdc`** + **`ttyEntry.ts`** checklist.

**Rationale (planning.mdc):** **Phase 10.5** is a **small user-visible** slice (slimmer prompt area, simpler layout/tests) **before** **11**’s structural **`ttyAdapter`** removal — one behavior: **input presentation**, not domain or TTY entry. Phase **12** is **structure-first** (no new user story); justify with **full interactive Vitest + targeted E2E** unchanged in user-visible terms (wrap, transcripts). **Gate 8 (`TextInput`)** is **intentionally splittable:** if **11** is already large, **ship 11 with tests green and no dead code** — do **not** leave half-migrated `TextInput` + orphaned caret helpers; either finish **`TextInput` in the same phase** or **defer the whole gate-8 swap to 11.5**. Keep **at most one intentionally failing test** per planning TDD note when driving. **Phase 13** is user-visible only as “no corrupted interleaved logs”; treat **`patchConsole`** flip as **verify-heavy**. **~~Phase 14~~** is **audit + documentation** of approved exceptions (**done** — **`ttyEntry.ts`** + **`cli.mdc`**). **Phase 11.6** is **structure / maintainability** (ANSI → **`chalk`** where it clarifies); pair with **full Vitest**; **no** intentional transcript change.

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
| Typed `yes` / `no` + Enter | **Not supported** on TTY — **y** / **n** only. **`processInput`** still parses committed **`y`/`n`/`yes`/`no`** lines from the Ink confirm path via **`parseRecallYesNoLine`**. |
| Wrong keys | **`@inkjs/ui` `StatusMessage`** variant **`error`** (e.g. “Please press y or n”). |
| Session + stop: bare Enter | **`defaultChoice="cancel"`** semantics (Enter = **no**); one path for both (replaces session empty-Enter **noop**). |

**Shipped:** **`@inkjs/ui`** (**`StatusMessage`**). **`cli/src/ui/RecallInkConfirmPanel.tsx`** + **`cli/src/interactions/recallYesNo.ts`** (Ink outcomes, stop-confirm model, **`parseRecallYesNoLine`** for **`processInput`** / tests). **`dispatched`** guard per stdin callback for **`y`+Enter** coalescing. **`ttyAdapter`:** **`getRecallStopConfirmInkModel`**, snapshot field **`recallStopConfirmInkModel`**.

**Verify:** `pnpm cli:test`; **`pnpm cypress run --spec e2e_test/features/cli/cli_recall.feature`**; `pnpm cli:lint` / format.

### Phase 7 (done) — Complete replacement gate (mandatory)

**Audit:** No second path for command-line keys, recall y/n, or list arrows / Enter / typing — all Ink **`useInput`**. **Removed:** redundant trailing no-ops in the **`keypress`** handler (list keys already fell through). **Intentional residue** (see JSDoc on **`stdin.on('keypress')`** in **`ttyAdapter.ts`**): Ctrl+C; fetch-wait Esc (**`FetchWaitDisplay`** has no `useInput`); MCQ/token **Esc** bridge when stdin ordering is unfriendly to Ink. **`readline.Interface`** output is **`noopOutput`** — no **`line`** event path.

- **Verify:** `pnpm cli:test` interactive suite; **`ttyAdapter`** grep: **`keypress`** / **`readline`** only as above.

### Phase 8 (done) — ink-ui polish

Interactive fetch-wait: **`@inkjs/ui` `Spinner`** (`type="dots"`) in **`FetchWaitDisplay`**; removed adapter **`setInterval`** ellipsis tick + **`INTERACTIVE_FETCH_WAIT_ELLIPSIS_MS`**. Stage-band layout string is static blue label (**`interactiveFetchWaitStageIndicatorLine`**); **`needsGapBeforeLiveRegion`** gates the leading blank before the live column.

### Phase 9 (done) — No second shell / clean TTY boundary

**Shipped with TTY-only refactor:** **`pipedAdapter` deleted**; **`-c`** and any **piped-stdin interactive shell** **removed**; **`runInteractive`** requires **TTY**. **Going forward:** **no** **`-c`**, **no** `pipedAdapter`, **no** “if piped then …” shell entry in **`run.ts`** or **`interactive.ts`**.

**Original goal (satisfied):** TTY module (**`ttyAdapter`**) does not host a second stdin-driven shell. **`processInput`** + **`renderer`** live in **`interactive.ts`** / **`renderer.ts`** for **one** engine: TTY output goes through Ink. **Before phase 9.5:** TTY **`/clear`** used **`interactiveTtyStdout.clearScreen`** + Ink remount (not **`writeFullRedraw`**); **9.5 removes** **`/clear`** entirely.

- **Optional residual:** Grep in PRs that we do not reintroduce **`-c`**, a piped shell, or a non-Ink full-screen **product** path for the interactive shell.
- **Verify:** **`pnpm cli:test`** interactive + **`processInput.test.ts`**.

### Phase 9.5 (done) — Remove `/clear` (product, tests, and dead abstraction)

**Goal:** **`/clear` does not exist** — not as a command, not as a test scenario, not as a comment or help string. **Gate 9** is satisfied by **deletion**, not by a replacement feature.

- **Product:** Remove the slash command from **`processInput`**, **`ttyAdapter`** (including **`clearAndRedraw`** on the TTY **`OutputAdapter`** if it exists **only** to implement screen/history reset for **`/clear`**), **`help.ts`**, and any E2E / Vitest that asserts **`/clear`** behavior.
- **No historical trace:** Do **not** leave “removed `/clear`” comments, deprecated stubs, or doc sections that describe the old command. **Excise** strings and branches; the plan file may mention the phase, but **application code and user-facing copy** should read as if **`/clear` never existed**.
- **Abstractions:** Remove **`OutputAdapter.clearAndRedraw`**, **`writeFullRedraw`**, **`renderFullDisplay`**, or **`defaultOutput`** wiring **when** their **only** remaining purpose was **`/clear`** or test-only full redraw for that path. **No** optional “clear screen” hooks kept “for later.”
- **Verify:** **`pnpm cli:test`** (full CLI suite); **`pnpm cli:lint`** / format; repo **`rg`** for **`/clear`**, **`clearAndRedraw`**, and any removed symbols — **zero** hits in **`cli/`** (and **E2E** / **`help`**) except where the string appears inside **unrelated** content (if any); fix or rename to avoid false positives. Run any E2E feature that previously mentioned **`/clear`** only after deleting those scenarios or replacing them with unrelated coverage.

### Phase 10 (done) — Domain: chat history + command turns as first-class concepts

**Goal:** **`ChatHistory`** / scrollback entries and **command-turn** buffering are **named types and transitions** in the **business** layer (or a small **`cli/src/shell/`** module owned by business), with stable verbs (append output, commit input line, flush turn, etc.) — **not** ad-hoc arrays only inside a legacy adapter.

- **Shipped:** **`cli/src/shell/scrollbackModel.ts`** — `CommandTurnBuffer`, `commandTurnBufferAppendLog` / `AppendError` / `AppendUserNotice`, `scrollbackAppendOutput`, `scrollbackCommitInputLine`, `scrollbackFlushCommandTurnIfNonEmpty`. **`ttyAdapter`** routes TTY scrollback and `OutputAdapter` buffering through these helpers (immutable transitions + local `let` assignment).
- **Ink mapping:** **`Static` `items`** = function of domain history (append-only, gate 2); live region = function of current turn + stage — **one directional flow** from domain updates to props.
- **Verify:** Same observable transcripts as today (Vitest **`runInteractive`**, key E2E); **`cli/tests/shell/scrollbackModel.test.ts`** on the transition helpers.

### Phase 10.5 (done) — Command-line input: no border (optional background-only)

**User outcome:** The main command-line input is **easier to maintain and test** — **no** bordered `Box` around the editable line (**gate 6**). **Shipped:** **`formatInteractiveCommandLineInkRows`** (draft + caret, width fit, grey when typing disabled). Vitest **`ttyWriteSimulation`** + **`interactiveTty*.test.ts`** assert **`→`** / column-0 prompt. E2E **`cliSectionParser`** treats the live **`→`** row as the command-line boundary (function name **`countInputBoxTopBorderLines…`** kept for call-site stability).

**Verify:** **`pnpm cli:test`**; **`pnpm cypress run --spec e2e_test/features/cli/cli_interactive_mode.feature`**.

### Phase 11 (done) — Remove `ttyAdapter`: thin TTY entry + Ink root state

**Goal:** Delete the **`ttyAdapter` monolith** — replace with **(a)** a **thin TTY session file** (mount Ink, streams, raw mode, **`exitOnCtrlC`**, documented **`keypress` residue** only per [Special cases](#special-cases-approved-ink-exceptions)) and **(b)** **Ink root** (`InteractiveShellDisplay` or successor) holding **React state / reducer** that subscribes to domain callbacks instead of mirroring state in closure variables beside Ink.

- **Shipped:** **`cli/src/adapters/ttyEntry.ts`** — **`runTTY`** only. **`cli/src/adapters/interactiveTtySession.ts`** — readline + **`keypress`** bridge, **`render` / `rerender`**, **`patch` + `applyShellSessionPatch`** session model. **`cli/src/ui/ShellSessionRoot.tsx`** — Ink tree (`InteractiveShellDisplay` + live panels) from **`ShellSessionState`** + **`TTYDeps`** + handlers. **`cli/src/shell/shellSessionState.ts`**, **`cli/src/adapters/ttyDeps.ts`**. **`ttyAdapter.ts` removed.** Command line still **`CommandLineLivePanel`** **`useInput`**; gate **8** / **11.5** moved caret paint to **`chalk.inverse`** (TextInput-equivalent) + fetch-wait **`TextInput`**.
- **Principle:** Session updates go through **immutable `ShellSessionPatch`** and **`drawBox`/`rerender`**; domain (`setPendingStopConfirmation`, etc.) stays ordered **before** paint where the prior adapter did.
- **`/clear`:** Unchanged (**phase 9.5**).
- **Verify:** **`pnpm cli:test`** green; **`pnpm cli:lint`** green.

### Phase 11.5 (done) — Gate 8: command line → `@inkjs/ui` `TextInput` (and align cursor)

**Goal:** **North star** for the main command line: Ink-native editing — **`@inkjs/ui` `TextInput`** (or equivalent) **replaces** the bespoke **`useInput`** line editor + reverse-video caret where that swap is correct for behavior (draft history, slash picker, focus, submit). Remove **`interactiveTtyStdout.hideCursor`** / manual caret coupling **when** `TextInput` owns the UX. Further controls may move to `TextInput` in the same phase or a **later** small phase if each slice stays **test-green** with **no dead code**.

- **Shipped:** Command-line caret/placeholder inverse video uses **`chalk.inverse`** (same SGR sequence as `@inkjs/ui` TextInput; `Chalk({ level: 3 })` so Vitest always sees escapes). **Fetch-wait** live strip: disabled **`TextInput`** for the “loading …” line (`FetchWaitDisplay.tsx`) — `isDisabled` keeps Ink `useInput` inactive (no stdin fight). **Not shipped:** mounting stock **`TextInput`** for the **editable** command line — upstream is `defaultValue`-only; Ink delivers stdin to every `useInput`, so a second TextInput `useInput` would double-handle keys with `CommandLineLivePanel` unless the session editor is refactored into one owner. **After multiline removal:** the live buffer is **single-line** (paste newlines → spaces), which aligns the product with that upstream limitation.
- **Verify:** **`pnpm cli:test`**; **`pnpm cli:lint`**. Targeted E2E `cli_interactive_mode.feature` when Cypress is available locally/CI.

### Phase 11.6 (done) — Chalk: migrate remaining TTY paint (after 11.5)

**Goal:** Extend the **`paintChalk`** / **`chalk`** approach from **11.5** so **most** terminal styling in **`cli/src/renderer.ts`**, **`cli/src/ansi.ts`**, **`cli/src/listDisplay.ts`**, **`cli/src/adapters/interactiveTtyStdout.ts`**, and related call sites uses **one forced-level `Chalk({ level: 3 })`** (or a tiny shared module) instead of scattered `\x1b[…` constants — **only where readability wins**.

**Shipped:** **`cli/src/terminalChalk.ts`** — `terminalChalk` + **`sgrOpen`-derived** **`GREY`**, **`GREY_BG`**, **`INTERACTIVE_FETCH_WAIT_PROMPT_FG`** (for `toContain` / band tests). **`renderer.ts`** uses chalk for separators, stage labels, hints, past-input bg, command-line paint (with existing **`greyOutFullPaintRow`** + **`RESET`** split unchanged), **`applyChatHistoryOutputTone`**. **`listDisplay.ts`** — `gray` / `inverse`. **`interactiveTtyStdout.ts`** — `gray` for grey prompt lines; JSDoc on **`ctrlCExitNewline`** raw CSI. **`ansi.ts`** — **`RESET`**, **`HIDE_CURSOR`**, **`SHOW_CURSOR`** only. Chalk often ends segments with targeted closers (e.g. **39m**, **49m**) rather than a full **0m** reset; tests were updated where they required a trailing **0m**.

**Explicitly out of scope or keep raw (document in JSDoc):** private **OSC** (input-ready), **cursor** show/hide CSI, and any string logic that **intentionally** manipulates nested ANSI (e.g. **`greyOutFullPaintRow`** `split(RESET).join(…)`) — **do not** force chalk if the result is **more** obscure than the current helper.

**Verify:** **`pnpm cli:test`** (full CLI suite); **`pnpm cli:lint`**. **No intentional user-visible transcript change** — assert via existing Vitest stdout / stripAnsi checks and targeted E2E if a surface changes bytes.

### Phase 12 (done) — Greatly shrink `renderer.ts` for TTY

**Goal:** **TTY path** stops depending on large grapheme **live-region line builders** where Ink already wraps (gate 4). **`renderer.ts`** keeps shared tone helpers and **string props** into Ink (MCQ lines, separators); grapheme width / wrap / **`truncateToWidth`** live in **`terminalLayout.ts`** (re-exported from **`renderer`**). **No** duplicate pre-wrap suggestion builder: removed **`buildSuggestionLines`**; **`ShellSessionRoot`** only feeds **`buildSuggestionLinesForInk`** for the default live column (token/MCQ alternate panels never used the removed branches). **Never** bring back **piped** or **`-c`**.

- **Shipped:** **`cli/src/terminalLayout.ts`**; **`renderer.ts`** reduced; **`buildSuggestionLines`** deleted; **`ShellSessionRoot`** inlines default-panel guidance into **`DefaultCommandLineInkLayout`** (no separate measure step); tests updated for Ink-first guidance.
- **Verify:** **`pnpm cli:test`**; **`pnpm cli:lint`**.

### Phase 13 (done) — `patchConsole` + Ink-idiomatic stdout

**Goal:** Enable Ink **`patchConsole`** on real TTY; route intentional logging through **`useStdout().write`** or domain **`OutputAdapter`** hooks that cooperate with Ink — **no** raw **`console.log`** on the interactive TTY hot path fighting the tree.

- **Shipped:** **`interactiveTtySession`**: `patchConsole: inkPatchConsoleSupported()` (`Reflect.construct` probe on **`console.Console`** — **false** when Vitest has **`spyOn(console, …)`**, because Ink’s **patch-console** uses `new console.Console` and would throw). Pre-shell banner: **`process.stdout.write(\`${formatVersionOutput()}\n\n\`)`** instead of **`console.log`**. **`tests/index.test.ts`:** **`process.exit`** mock **throws** so **`exitCliError`** does not fall through into **`runInteractive`** after a mocked noop exit. **`interactiveTtyStdout`** JSDoc updated.
- **Gate 7:** Closed here per [Decision gates](#decision-gates-pause-and-get-sign-off).
- **Verify:** **`pnpm cli:test`**; **`pnpm cli:lint`**. **Stop if:** persistent corruption on real TTY — temporarily set probe to force **`patchConsole: false`** only as a **short** escape hatch while fixing root cause.

### Phase 14 (done) — Residue audit + special cases doc in code

**Goal:** Single checklist of **approved non-Ink** behavior (OSC, exit farewell, **readline `keypress`** bridges, and **hardware cursor** only if gate 8 leaves a gap). Remove any **unlisted** sideways hacks. Confirm **no** **`-c`**, **no** piped-stdin shell, **no** `pipedAdapter` (see [Plan invariant](#plan-invariant-non-negotiable)). Update **`.cursor/rules/cli.mdc`** for **command line** / **TTY entry** naming; **drop** any **`/clear`** documentation (phase **9.5**).

- **Shipped:** Module JSDoc on **`cli/src/adapters/ttyEntry.ts`** (checklist + invariant). **`.cursor/rules/cli.mdc`** — **Command line (live strip)** replaces bordered **Input box**; entry points name **`runTTY`** / **`interactiveTtySession`**; invariant line for no **`-c`** / piped shell. **`.cursor/rules/mutation-testing.mdc`** — `ttyAdapter` → **`interactiveTtySession`**. Plan “current shipped” pointer updated from legacy **`ttyAdapter`** to **`ttyEntry` / `interactiveTtySession`**.
- **Verify:** `rg` — no **`pipedAdapter`** / piped-stdin shell in `cli/`; **`run.ts`** still rejects **`-c`**. **`pnpm cli:test`**; **`pnpm cli:lint`**.

---

## Special cases (approved Ink exceptions)

These are **intentional** places the stack is **not** pure Ink — document **why** next to the code and keep the list minimal.

| Case | Why Ink alone is insufficient (today) |
|------|----------------------------------------|
| **Private OSC** **`INTERACTIVE_INPUT_READY_OSC`** | Invisible integrator signal (PTY / shell integration); not a React layout concern. |
| **Exit farewell** (and **cursor show** on exit if still needed) | Lifecycle **after** `unmount` / outside Ink’s paint cycle for some paths. **Hardware cursor hide/show during interactive typing** — **prefer dropping** once command line uses Ink-native caret (gate 8); only list here if a gap remains. |
| **readline `keypress`** (Ctrl+C, fetch-wait Esc, list Esc bridge) | **Phase 7** residue: stdin ordering / components without `useInput` for that key; **prefer eliminating** each bridge inside later sub-steps if Ink can own it without fighting. |

**Not a special case:** **`/clear`** — **removed in phase 9.5**; do **not** add CSI or adapter exceptions for a screen-clear command (gate 9).

**Rule:** If a new exception is needed, **add it here and in JSDoc** in the same PR — no silent “just this once” `process.stdout.write`.

---

## Replacement bar (summary)

| Mode | Owner after migration | Thin TTY entry / readline |
|------|------------------------|---------------------------|
| Main command line | After **11:** Ink **`useInput`** + **focus** (phases 2 + 4) unless **11** already includes `TextInput`; after **11.5:** prefer **`@inkjs/ui` `TextInput`** + **focus** (gate 8) | No duplicate key handling |
| Confirm / y/n | **`RecallInkConfirmPanel`** + **`@inkjs/ui` `StatusMessage`** (phase 6) | No duplicate |
| Lists (MCQ, tokens, selection) | **`RecallMcqChoicesLivePanel`** / **`AccessTokenPickerLivePanel`** + **`selectListInteraction`**; Esc bridge on **`keypress`** until removed | No duplicate list keys except **listed** Esc bridge |
| Scrollback / turns | Domain model → **`Static` items** + live props (phases 10–11) | Not stored only in legacy adapter closures |
| **`-c` / piped stdin shell** | **Must not exist** — **`run.ts`** errors on **`-c`**; **`runInteractive`** requires TTY | **Do not** add code paths that bypass this |
| **`processInput` + `defaultOutput`** | **Test harness** — **after 9.5:** no **`/clear`**; **`defaultOutput`** only carries hooks still needed for real **`processInput`** tests | Same engine as TTY; **not** piped shell, **not** **`-c`** |
| Residue | — | **Phase 14** + [Special cases](#special-cases-approved-ink-exceptions) (replaces “see ttyAdapter JSDoc” as sole doc) |

---

## What the UI layer is not

Not **business rules**, not a second **`processInput`**. **Not** **`-c`**, **not** a **piped-stdin interactive shell**, **not** **`pipedAdapter`** or any revived “dual mode” (those are **out of scope permanently** for this plan). **Not** a second mutable copy of **chat history** / **command turns** that disagrees with domain state (phases 10–11). Domain branching stays in **`interactive.ts`** (and related modules), not in the thin TTY file.

---

## References (Context7)

- **Ink** — `vadimdemedes/ink`: `render` (options: **`patchConsole`**, custom **`stdout`**), `Box`, `Text`, **`Static`**, **`useInput`**, **`useStdout`** / **`write`**, `useApp`, `useFocus`, instance **`clear`** / **`unmount`**.
- **Ink UI** — `vadimdemedes/ink-ui`: `TextInput`, `ConfirmInput`, `Select`, `Spinner`, `ProgressBar`.
