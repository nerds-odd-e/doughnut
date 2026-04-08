# `tty-assert` — Replace ad-hoc style booleans with locator-scoped cell expectations

**Status:** Phases **1–3 complete** in-repo; Phase 4 optional.

**Parent roadmap:** [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) (Phases 5–6 delivered the current `waitForTextInSurface` / managed-session wiring).

**Problem:** `requireBold`, `rejectGrayForegroundOnlyWithoutGrayBackground`, and `requireGrayBackgroundBlock` on `WaitForTextInSurfaceOptions` / `ManagedTtyAssertOptions` encode **specific Ink/chalk outcomes** as unrelated boolean flags. They are hard to extend, obscure when combined, and do not read as “find this text, then assert on the **resolved cells**.”

**Inspiration (`@tt` / [microsoft/tui-test](https://github.com/microsoft/tui-test)):** The repo no longer vendors `tt/` (removed in Phase 5.9); the relevant pattern is upstream **tui-test**:

- [`Locator`](https://github.com/microsoft/tui-test/blob/main/src/terminal/locator.ts) resolves `string | RegExp` against the same **row-major flat block** `tty-assert` already uses, producing a **list of cells** (`IBufferCell` + coordinates).
- Matchers such as [`toHaveBgColor`](https://github.com/microsoft/tui-test/blob/main/src/test/matchers/toHaveBgColor.ts) and [`toHaveFgColor`](https://github.com/microsoft/tui-test/blob/main/src/test/matchers/toHaveFgColor.ts) apply to **those cells**, not to ad-hoc global flags.
- **Bold:** tui-test does not ship a public bold matcher today; `tty-assert` can still align with the same **two-step** model (resolve range → assert on cells).

**Design principle:** Treat **text + surface + anchors + strict** as the **locator** (what to find). Treat **styling** as **expectations on the cell span** of the chosen occurrence. Avoid new product-specific booleans; add **composable, named expectation entries** (JSON-serializable for `cy.task`).

---

## Semantics to preserve (current behavior)

Documented in [`packages/tty-assert/src/waitForTextInSurface.ts`](../../packages/tty-assert/src/waitForTextInSurface.ts) and implemented in [`surfaceAttemptOnTerminal.ts`](../../packages/tty-assert/src/surfaceAttemptOnTerminal.ts):

| Current flag | After text match | Occurrence |
|--------------|------------------|------------|
| `requireBold` | Every cell in span must be bold (`isBold() !== 0`) | **First** `indexOf` in search region |
| Gray pair | Every cell in span must satisfy palette rules for “gray block” vs “gray fg only” | **Last** `lastIndexOf` in search region |

Any new API must keep these **first vs last** rules explicit per expectation group (or per check), so Doughnut’s `pastUserMessages` / `expectContainsBold` stay stable unless we intentionally change product tests.

---

## Proposed API shape (declarative, task-safe)

**Cypress tasks cannot pass functions** — keep a **serializable** structure, inspired by tui-test matchers but flattened for `ManagedTtyAssertTaskPayload`.

**Single API (no A/B fork):** one optional field replacing the three booleans — an **array** of `{ match, expectations }` blocks. Length 1 covers today’s callers; length > 1 covers “bold on **first** match **and** gray block on **last** match in **one** `assert` / `cy.task`” without merging unrelated rules into one block.

```ts
// Conceptual — exact naming TBD during implementation
cellExpectations?: Array<{
  /** Which substring/regex match defines the cell span for this block. */
  match: 'first' | 'last'
  expectations: Array<
    | { kind: 'allBold' }
    | { kind: 'allBgPalette'; index: number }
  >
}>
```

**Mapping from old flags:**

- `requireBold: true` → one block: `{ match: 'first', expectations: [{ kind: 'allBold' }] }`.
- `rejectGrayForegroundOnlyWithoutGrayBackground` + `requireGrayBackgroundBlock` (palette 8 / chalk `90m`/`100m`) → one block: `{ match: 'last', expectations: [{ kind: 'allBgPalette', index: 8 }] }` (gray fg without `100m` bg fails the same check).

**Implementation order:** Ship the **array** type from Phase 1 even if tests only pass **one** block at first — no second “merge Option B” phase. Add a multi-block payload only when a scenario needs a single round-trip with different `match` values.

**Naming:** Prefer **palette index** in JSON (aligned with xterm and tui-test numeric mode) over chalk escape documentation in the type shape; README can still explain chalk `90m`/`100m` ↔ palette 8.

---

## Internal refactor (library)

1. **Resolve:** After `matchCount` succeeds, compute **start offset** of the chosen occurrence (first or last) in the row-major haystack; map offset + length → `(x,y)` cells (reuse logic from `verifyBoldAtFirstStringMatchOnTerminal` / gray verifier, unified).
2. **Expect:** Run each expectation in the block against that cell list; failure messages cite **expectation kind** + **snapshot** (existing pattern).
3. **RegExp needles:** Today style flags require a **string** needle. Keep that rule until a later phase defines “span of first regex match” consistently (document as out of scope for phase 1 if unchanged).

---

## Phases (`.cursor/rules/planning.mdc`)

Each phase has a **single observable gate** and avoids multiple intentionally failing tests.

### Phase 1 — `tty-assert`: introduce `cellExpectations`, implement, unit-test

- Add `cellExpectations` (or chosen final name) to `WaitForTextInSurfaceOptions` and thread through `managedTtySession` / `SurfaceAttemptOpts`.
- Refactor `surfaceAttemptOnTerminal` to **resolve-then-assert**; reimplement existing bold + gray behavior as the default expansions above.
- **Keep** the three boolean options temporarily **deprecated**: if present, translate internally to `cellExpectations` **or** fail fast if both old and new are set (pick one rule in implementation).
- **Gate:** `pnpm tty-assert:test` green; behavior matches existing `waitForTextInSurface.test.ts` cases (same scenarios, possibly duplicated to call new API shape, then drop booleans in Phase 3).

### Phase 2 — Doughnut: migrate callers

- [`e2e_test/start/pageObjects/cli/outputAssertions.ts`](../../e2e_test/start/pageObjects/cli/outputAssertions.ts): replace `requireBold` / gray booleans with `cellExpectations` on the `cliInteractiveAssert` payload.
- Update [`ManagedTtyAssertTaskPayload`](../../e2e_test/config/cliE2ePluginTasks.ts) typing to match.
- **Gate:** Targeted CLI Cypress specs that use these assertions (e.g. steps touching `pastUserMessages`, `expectContainsBold`) green.

### Phase 3 — Remove deprecated booleans

**Status:** **Complete.**

- Removed the three boolean options from public types and docs; [`waitForTextInSurface.ts`](../../packages/tty-assert/src/waitForTextInSurface.ts), [`packages/tty-assert/README.md`](../../packages/tty-assert/README.md), and [`cli-phase6` doc](./cli-phase6-tty-assert-managed-session-subphases.md) updated accordingly.
- **Gate:** `tty-assert` tests + targeted CLI E2E subset green.

### Phase 4 (optional, value-driven) — Richer expectations without new booleans

- Add expectation kinds only when a **real** test needs them, e.g. `allFgPalette`, `italic`, `dim`, following the same resolve-then-assert pattern.
- Consider documenting a **mapping table** “Ink / chalk → xterm attributes” for Doughnut authors.

**Out of scope for this plan:** Adopting tui-test’s **full** Jest/Vitest matcher surface or `color-convert` hex strings in the task payload (can be a later ergonomic layer **inside** Node only, not in `cy.task` JSON).

---

## Update parent extraction doc

When Phase 1 starts, add a short pointer under Phase 5 / “assertion API” in [`cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md) to this file and note that style assertions are **locator-scoped cell expectations**, not boolean flags.

---

## Open decisions (resolve in Phase 1 implementation)

1. **Field name:** `cellExpectations` vs `styleChecks` vs `onMatch` — choose for grep clarity and README.
2. **Deprecation:** silent mapping from old booleans vs one-release warning in error messages.
3. **Strict interaction:** if `strict: true` and multiple matches, **first**/**last** still refer to haystack occurrences — confirm same as today before deleting old code.
