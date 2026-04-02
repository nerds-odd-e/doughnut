# Phase 5 sub-phases — Locators, assertion API tidy, finish xterm migration

**Parent plan:** [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) (Phase 5: coherent **`tty-assert`** surface, **xterm-backed replay everywhere** product code needs it, and **better terminal locators** so Doughnut E2E does not rely on “search the entire PTY history and hope section labels disambiguate”.)

**This document:** Splits Phase 5 per [`.cursor/rules/planning.mdc`](../../.cursor/rules/planning.mdc) — **one deliverable per sub-phase**, each with an explicit **gate**, ordered by **dependency and value**. **Do not execute** until prior work is merged; treat this as the execution checklist for Phase 5 only.

---

## Research — [microsoft/tui-test](https://github.com/microsoft/tui-test)

[TUI Test](https://github.com/microsoft/tui-test) is an end-to-end terminal testing framework that renders the PTY with **xterm.js** (same family as our Phase 4 path) and exposes a **Playwright-style** API (`getByText`, `expect(…).toBeVisible()`). Doughnut should **not** adopt the whole runner; borrow **patterns** for locating text and reporting failures.

### Local `tt/` tree (temporary — **do not keep**)

The workspace may include a copy under [`tt/`](../tt/) for close reading while designing `tty-assert`. **Delete `tt/` when no longer needed**; do **not** add a package dependency on it. Prefer citing upstream GitHub for permanent links; use local paths below while the folder exists.

| File | Takeaways for `tty-assert` / Phase 5 |
|------|-------------------------------------|
| [`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts) | **`getBuffer()`** — `_getBuffer(0, buffer.active.length)`: every line in the **active** xterm buffer (scrollback + viewport). **`getViewableBuffer()`** — `_getBuffer(baseY, buffer.active.length)`: only lines from **`buffer.active.baseY`** upward (the “viewport” slice in buffer coordinates when scrolled to the bottom). **`_getBuffer`** builds each row as **per-cell** `getChars()`, using a **space** for empty cells — then locators flatten rows. **`getByText(text, { full?, strict? })`** returns a **`Locator`**; **`strict` defaults to `true`** (`options?.strict ?? true`). |
| [`tt/src/terminal/locator.ts`](../tt/src/terminal/locator.ts) | Search builds **`block = buffer.map(row => row.join("")).join("")`** — **row-major flatten with no `\n` between rows** (fixed width `cols` per row). Substring / `RegExp` via `matchAll` runs on that block; **strict** throws if **multiple** matches (`strict mode violation` prefix from [`tt/src/utils/constants.ts`](../tt/src/utils/constants.ts)). On timeout, snapshot is **row strings** `trimEnd()` joined with **`\\n`** (different shape than the search `block`). **Cell mapping** uses `baseY + y` into `buffer.active.getLine` — **`full` uses `baseY = 0`**, viewable uses **`buffer.active.baseY`**. |
| [`tt/src/utils/poll.ts`](../tt/src/utils/poll.ts) | **`poll(fn, delay, timeout, isNot?)`**: async loop — run `fn` immediately, then **`setTimeout(..., delay)`** until success or wall-clock **`timeout`** from first call. Implements **`isNot`** early exit semantics for negative assertions. For **Vitest**-hosted code, prefer **deterministic** tests (fixed `raw`) over wall-clock polling; Cypress-side retries already re-read the PTY buffer. |
| [`tt/src/test/matchers/toBeVisible.ts`](../tt/src/test/matchers/toBeVisible.ts) | Labels comparison method (string vs regex); wraps **`locator.resolve(timeout, isNot)`**; surfaces strict-mode errors and timeout snapshots through **`expect` message** hooks. |
| [`tt/src/test/matchers/toHaveBgColor.ts`](../tt/src/test/matchers/toHaveBgColor.ts) (and fg) | **`locator.resolve` → cells** → per-cell color checks against xterm **`IBufferCell`**. Relevant **later** for Doughnut **past user gray-background** style checks (today raw-byte SGR window in `outputAssertions`) — likely **Phase 7+**, not required for Phase 5 locator text MVP. |
| [`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts) `serialize()` | **Box-drawn** snapshot (`╭`/`│`/`╯`) plus a **`Map`** of per-cell style **shifts** for golden snapshots — inspiration for **structured** failure output later ([`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) Phase 7–8), not Phase 5 scope. |

**Live PTY vs batch replay:** In tui-test, **`Terminal`** calls **`_term.write(data)`** on each **`onData`** chunk from the PTY. Doughnut **`ptyTranscriptToVisiblePlaintextViaXterm`** feeds the **full** transcript in one **`write(raw, callback)`**. For **final-state** assertions, reading **`buffer.active`** after the write completes is still analogous to tui-test’s **post-drain** buffer — implement full-buffer / viewable helpers in `tty-assert` by mirroring **`_getBuffer` / `getViewableBuffer`** line ranges on a headless `Terminal`, not by depending on `tt/` code.

**Flattening contract (important):** tui-test **search** uses a **single string** with **no newlines** between logical rows; our existing replay helper joins viewport lines with **`\\n`**. Phase 5 should **document and test** which flattening `tty-assert` locators use (tui-test parity vs newline-separated plain text) so substring and failure snapshots stay predictable.

### [`src/terminal/locator.ts`](https://github.com/microsoft/tui-test/blob/main/src/terminal/locator.ts) (ideas)

- **Search surface:** Resolution runs against either a **viewable** buffer (`getViewableBuffer`) or the **full** buffer (`getBuffer`) depending on a `full` flag — i.e. *where* to look is explicit, not implied by copying a substring from step wording into “the whole transcript”.
- **Matching:** Supports **string** or **RegExp**; finds indices in a flattened block, then maps hits back to **cells** in the xterm buffer (for richer assertions later).
- **Timing:** `resolve` **polls** (e.g. 50 ms cadence) until the match appears or **timeout** — aligns with Cypress retries but belongs in the **library** for Node consumers too.
- **Strictness:** Optional **strict** mode errors when multiple matches exist — useful when Ink duplicates short strings.

### [`src/test/matchers/toBeVisible.ts`](https://github.com/microsoft/tui-test/blob/main/src/test/matchers/toBeVisible.ts) (ideas)

- **Failure shape:** Clear **expected** vs **matches found**, plus on timeout a **terminal snapshot** (`---START---` / `---END---`) built from the buffer under test — better than a single “substring not in section X” without showing *which* surface was searched.

### Gap vs Doughnut today

| tui-test idea | Doughnut today (`outputAssertions.ts` et al.) |
|---------------|-----------------------------------------------|
| Choose **viewable** vs **full** buffer | **Current guidance:** xterm **viewport** replay + Ink heuristic (`extractCurrentGuidanceFromReplayedPlaintext`). **Past assistant / answered:** **entire** ANSI-stripped cumulative PTY string — no explicit “viewport only” vs “scrollback included” switch. |
| Poll + timeout in one place | Cypress **`retryCliOutputAssertion`** + ad hoc asserts per section. |
| Snapshot on failure from the **same** buffer that was searched | **Mixed:** guidance failures show replay tail + guidance region; stripped-transcript failures show head/tail of **stripped** cumulative bytes — not always the same geometric model as xterm. |
| **Search haystack shape** (row-major vs `\n`-joined) | tui-test **Locator** flattens rows **without** newlines ([`tt/src/terminal/locator.ts`](../tt/src/terminal/locator.ts)); timeout snapshots use **newline-joined** rows. Doughnut replay plaintext uses **newlines**. `tty-assert` must pick and document one or expose both. |
| **Strict** default for ambiguous text | tui-test **`getByText` defaults `strict: true`** ([`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts)). Doughnut today has no equivalent on cumulative strip searches. |

### Gap vs current `tty-assert` xterm helper

[`ptyTranscriptToVisiblePlaintextViaXterm`](../packages/tty-assert/src/ptyTranscriptToVisiblePlaintextViaXterm.ts) returns **viewport-only** plaintext (`viewportY` … `viewportY + rows`). There is **no** exported API yet for “flattened **full** xterm buffer (scrollback + viewport) as searchable plain text”, which is what tui-test’s `full: true` path approximates for *session-wide* visibility checks. Phase 5 **may** add that (or an options flag on replay) so some assertions can move off raw cumulative bytes **without** losing scrollback-aware semantics.

---

## Context (code anchors, April 2026)

| Location | Behavior today |
|----------|----------------|
| [`outputAssertions.ts`](../e2e_test/start/pageObjects/cli/outputAssertions.ts) | **Replay (viewport):** `getGuidanceContext` → xterm → `extractCurrentGuidanceFromReplayedPlaintext`. **Past assistant / answered:** `assertStrippedPtyTranscriptContains` on **full** stripped transcript. **Past user:** stripped transcript + gray SGR + line-padding heuristics. |
| [`facade.ts`](../packages/tty-assert/src/facade.ts) | **`ptyTranscriptToVisiblePlaintext`** (legacy) for replay-shaped diagnostics — should move to xterm in 5.1. |
| [`cliE2ePluginTasks.ts`](../e2e_test/config/cliE2ePluginTasks.ts) | Startup wait: **`waitForVisiblePlaintextSubstring`** (stripped cumulative). No use of `getReplayedScreenPlaintext` / `dumpFrames` yet. |
| [`step_definitions/cli.ts`](../e2e_test/step_definitions/cli.ts) | Steps delegate to `interactiveCli()` fluents (`pastCliAssistantMessages`, `answeredQuestions`, `currentGuidance`, …). |

**Reference:** Phase 4 — [`cli-phase4-tty-assert-xterm-subphases.md`](./cli-phase4-tty-assert-xterm-subphases.md). Stack habits — [`.cursor/rules/cli.mdc`](../../.cursor/rules/cli.mdc), [`.cursor/rules/e2e_test.mdc`](../../.cursor/rules/e2e_test.mdc).

---

## Scope boundaries (all of Phase 5)

| In scope | Out of scope (later phases) |
|----------|------------------------------|
| **`tty-assert`:** xterm replay in facade; canonical replay export; **locator-style helpers** (explicit search surface, poll+timeout, optional strict/regex); **viewable vs full** buffer line ranges as in [`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts) | **Playwright-style** `expect` integration, multi-shell runners, vendoring **`tt/`** — borrow **patterns** only; delete temporary `tt/` when done |
| **Doughnut E2E:** **Change** some assertions from “whole history + section name as documentation” to **better locators** (viewport, guidance region, full xterm buffer, or retained stripped transcript where that remains the right product contract) | PNG / animation artifacts — **Phases 9–10** |
| **Docs:** `tty-assert` README describes strip vs replay **vs locator surfaces** | Unified **lifecycle** API — **Phase 6**; row rulers / annotated regions — **Phase 7** |

**Important:** Sub-phases that **migrate** Gherkin-facing assertions may **intentionally** tighten behavior (e.g. text must appear in **Current guidance** rather than anywhere in scrollback). Each migration sub-phase should name the **scenario** and the **observable** user expectation so failures are interpretable.

---

## Sub-phase 5.1 — Facade replay uses xterm (library-only)

**User-visible outcome:** None. **Developer outcome:** `TtyAssertTerminalHandle` replay-shaped methods use the same xterm pipeline as Current guidance.

**Work:**

- In [`facade.ts`](../packages/tty-assert/src/facade.ts), replace **`ptyTranscriptToVisiblePlaintext`** with **`ptyTranscriptToVisiblePlaintextViaXterm`** for replay-derived text.
- Expose **async** APIs where replay is involved (`getReplayedScreenPlaintext`, `dumpFrames` previews that include replay).
- Minimal Vitest proving the facade wiring on a **fixed small `raw` string** (no need for full PTY if a direct helper is testable).

**Gate:** `pnpm tty-assert:test` + `pnpm tty-assert:lint` green.

---

## Sub-phase 5.2 — Canonical replay export + quarantine legacy

**User-visible outcome:** None. **Developer outcome:** One obvious import for “transcript → **viewport** replay plain string (xterm)”; legacy hand-rolled replay reserved for **parity / regression** tests.

**Work:**

- Canonical export name (TBD) wrapping or aliasing the xterm async function; **`package.json` exports** updated if needed.
- **`@deprecated`** on **`ptyTranscriptToVisiblePlaintext`** for non-test use; grep gate: no production imports outside allowlist.

**Gate:** `pnpm tty-assert:test` + `pnpm tty-assert:lint` green; grep gate satisfied.

---

## Sub-phase 5.3 — Locator primitives in `tty-assert` (tui-test–inspired, runner-agnostic)

**User-visible outcome:** None. **Developer outcome:** Callers can assert visibility with an **explicit search surface** and shared **poll + timeout** behavior (same spirit as [`tt/src/terminal/locator.ts`](../tt/src/terminal/locator.ts) + [`tt/src/utils/poll.ts`](../tt/src/utils/poll.ts); **do not** vendor or depend on `tt/`).

**Work (minimal first slice):**

- **Buffer surfaces (mirror [`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts)):** After replaying `raw` into a headless xterm, align **viewable** vs **full** with **`getViewableBuffer()`** (lines **`baseY` … `length-1`**) vs **`getBuffer()`** (`0` … `length-1`). Build each row like **`_getBuffer`** (per-cell `getChars()`, space for empty cells).
- **Flattening contract:** Document and test how the **search haystack** is built — tui-test uses **row-major concat without `\n`** between rows for matching (see **Research → Local `tt/`**); `ptyTranscriptToVisiblePlaintextViaXterm` uses **newline-joined** viewport lines. Phase 5 must **pin** which shape `tty-assert` locators use (or expose both explicitly).
- **Types:** e.g. `TtySearchSurface = 'viewableBuffer' | 'fullBuffer' | 'strippedTranscript'` (names TBD) — map to the xterm slices above + `stripAnsiCliPty` where needed.
- **API:** async `waitForTextInSurface({ raw, needle, surface, timeoutMs, retryMs, strict? })` or a small **Locator** object — **Node-only**, no Cypress. **Strict:** tui-test defaults **`strict: true`** for multiple matches ([`term.ts`](../tt/src/terminal/term.ts) `getByText`); allow opt-out for duplicate Ink text if required.
- **Poll:** Vitest tests usually use a **fixed** `raw` (no wall-clock poll). Optional Node poll may mirror [`poll.ts`](../tt/src/utils/poll.ts) (`setTimeout` between attempts); Cypress path keeps re-reading **`cliInteractivePtyGetBuffer`** (see [`.cursor/rules/cli.mdc`](../../.cursor/rules/cli.mdc)).
- **Vitest:** failure messages name the **surface** and include a readable snapshot (tui-test timeout path uses **newline-joined** `trimEnd` rows in [`locator.ts`](../tt/src/terminal/locator.ts), which differs from the **search block** — decide whether `tty-assert` unifies snapshot vs search string for debuggability).

**Gate:** `pnpm tty-assert:test` + `pnpm tty-assert:lint` green.

**TDD note:** One failing test for “substring absent until buffer updated” simulated by two-phase `raw` progression is optional; static fixtures are enough if poll logic is thin.

---

## Sub-phase 5.4 — Document public API (`tty-assert` README)

**User-visible outcome:** None. **Contributor outcome:** README explains **strip vs replay vs viewable/full buffer locators** and the **flattening contract** from 5.3; links [microsoft/tui-test](https://github.com/microsoft/tui-test) as **prior art**. Optionally note that a temporary local **`tt/`** tree was used for design research and **is not part of the product** (remove `tt/` from the repo once the team no longer needs it).

**Gate:** README (or `docs/` + link) exists; lint OK.

---

## Sub-phase 5.5 — Doughnut: Cypress adapter uses locator / surface helpers

**User-visible outcome:** None **if** all migrated steps preserve the same user-visible expectations; some steps may **tighten** in 5.6+.

**Work:**

- Refactor [`outputAssertions.ts`](../e2e_test/start/pageObjects/cli/outputAssertions.ts) internals to call **`tty-assert` locator helpers** from 5.3 where appropriate, keeping **fluent** exports (`pastCliAssistantMessages`, `currentGuidance`, …) stable **unless** a later sub-phase deliberately renames or splits them.
- Align **retry** timing with locator `timeoutMs` / `retryMs` so there is **one** obvious polling story (Cypress chain still drives re-read of `cliInteractivePtyGetBuffer`).

**Gate:** Targeted CLI Cypress: `cli_access_token.feature`, `cli_recall.feature`, `cli_install_and_run.feature` — **green with no intentional assertion tightening yet** (adapter-only refactor).

---

## Sub-phase 5.6 — E2E assertion inventory + migrate “wrong surface” checks (scenario-first)

**User-visible outcome:** Tests match **where** users see text (viewport / guidance / scrollback), reducing false positives from “anywhere in history”.

**Work:**

- **Inventory** every interactive CLI assertion path: [`step_definitions/cli.ts`](../e2e_test/step_definitions/cli.ts), [`removeToken.ts`](../e2e_test/start/pageObjects/cli/removeToken.ts), and any other `expectContains` on PTY output. For each, record: **intended surface** (e.g. “must be visible in current guidance” vs “must have appeared in session log”).
- **Migrate** cases that today use **`pastCliAssistantMessages` / `answeredQuestions` / stripped full transcript** but **should** use **Current guidance** or **viewport** search — switch steps or page-object implementation to the correct locator surface (may require new fluent methods, e.g. `currentGuidance().expectContains` already exists; add only what is needed).
- **Order by value:** start with scenarios that already express **region** in Gherkin (“in the Current guidance”) vs those that only say “I should see X” — tighten the latter only when the **product** expectation is clear.

**Gate:** Same targeted CLI Cypress specs **plus** any feature files touched by inventory rows; scenarios remain **@ignore**-aware per CI policy.

**Planning discipline:** Prefer **one scenario group per PR slice** if the diff gets large (e.g. recall vs access token), each with its own sub-phase **5.6a / 5.6b** in execution notes — keep **at most one** intentionally failing test while driving each slice.

---

## Sub-phase 5.7 — Section contracts: past assistant, answered, past user

**User-visible outcome:** Documented, stable **search surfaces** per domain term (see [`.cursor/rules/cli.mdc`](../../.cursor/rules/cli.mdc) terminology).

**Work:**

- For **past CLI assistant messages**, **answered questions**, **past user messages**: explicitly choose **fullBuffer** (xterm), **strippedTranscript** (cumulative), or **viewport** — per region. Implement with **5.3** locators; remove redundant “section label only in error string” patterns where the **surface** now carries the meaning.
- **Past user** gray-block / padding rules may stay **Doughnut-specific** (Ink layout); only the **primary search** should move to a locator where it improves precision.
- Update step wording where necessary so **dumb automation** stays explicit ([`.cursor/rules/e2e_test.mdc`](../../.cursor/rules/e2e_test.mdc) §9).

**Gate:** Targeted CLI Cypress green; `outputAssertions` header comment lists **surface per fluent** (short table).

---

## Sub-phase 5.8 — `interactiveCli` / `ttyAssertTerminal` fluents match the tidied model

**User-visible outcome:** None beyond 5.6–5.7.

**Work:**

- Update [`ttyAssertTerminal.ts`](../e2e_test/start/pageObjects/cli/ttyAssertTerminal.ts) and [`interactiveCli.ts`](../e2e_test/start/pageObjects/cli/interactiveCli.ts) comments and any **re-exports** so they describe **locators / surfaces**, not “opaque substring = past assistant only”.
- Remove or replace **`getByText(…).expectVisibleInPastAssistantMessages`** if it encodes the old **one-surface** assumption.

**Gate:** Lint + targeted CLI Cypress green.

---

## Dependency chain

- **5.1 → 5.2 → 5.3:** Replay plumbing before locators that depend on xterm state.
- **5.3 → 5.4:** Docs name real APIs.
- **5.3 → 5.5:** Cypress adapter imports locator helpers.
- **5.5 → 5.6 → 5.7:** Adapter first, then scenario migrations, then explicit section contracts.
- **5.7 → 5.8:** Fluents reflect final contracts.

---

## Phase-complete checklist (parent plan)

1. Update [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) Phase 5 status and **scope** (API + E2E locator migration).
2. **No stray** undocumented dual paths: each assertion kind maps to **one** primary search surface.
3. README + `outputAssertions` header **table** stay in sync.
4. **Remove** temporary [`tt/`](../tt/) research tree from the repo when no longer needed (ideas live in `tty-assert` + this plan, not as a sibling package).
5. Trim or archive this doc when Phase 5 is done.

---

## Deferred (Phase 6+)

- **Phase 6:** PTY lifecycle unify (`start` / `write` / `read` / `stop`).
- **Phase 7+:** Structured debug views; **box-drawn** / **serialize**-style snapshots ([`tt/src/terminal/term.ts`](../tt/src/terminal/term.ts) `serialize()`); **cell-level** fg/bg matchers ([`toHaveBgColor.ts`](../tt/src/test/matchers/toHaveBgColor.ts)) as an alternative path for Ink **gray-block** checks that today inspect raw SGR in `outputAssertions`.
