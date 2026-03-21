# CLI tests — move toward “observable behavior first”

Informal plan. Aligned with `.cursor/rules/planning.mdc` → **Observable behavior first**.

## Current snapshot (~23 files)

| Bucket | Files (examples) | Fit with preference |
|--------|------------------|---------------------|
| **Pure / black-box** | `renderer.test.ts`, `markdown.test.ts`, `listDisplay.test.ts`, `version.test.ts`, `sdkHttpErrorClassification.test.ts` | Strong — I/O or formatting only, no internal call graph. |
| **App entry** | `index.test.ts` | Strong — argv → stdout / exit. |
| **Full interactive stack** | `interactive/*.test.ts` (TTY keystrokes, `runPipedInteractive`), `interactiveExitFarewell.test.ts` | Strong — `runInteractive` + stdout / console. |
| **Large `processInput` suites** | `interactive/processInput.test.ts` (~580 lines), `recall.test.ts` (~910 lines) | Mixed — many cases are **log-on-mock-adapter**: observable *text* but **not** the real TTY/piped path; heavy overlap between the two files on recall; easy to **miss wiring** (e.g. wrong `interactiveUi` at adapter). |
| **Adapter / wait plumbing** | `interactiveFetchWait.test.ts` | Weaker — spies on `OutputAdapter` callbacks (`onInteractiveFetchWaitChanged`, etc.); justified only where TTY timing is flaky (per `cli.mdc` loading UI note). |
| **Command docs / help** | `help.test.ts` | Mixed — `formatHelp` tests are fine; `processInput('/help')` duplicates public contract with index path slightly. |
| **Side-effect modules** | `accessToken.test.ts`, `gmail.test.ts`, `update.test.ts` | Case-by-case — often file/env mocks; keep if asserting stable outcomes, not private helpers. |

**Main debt:** Recall and multi-turn **interactive** behavior is spread across **`processInput.test.ts`**, **`recall.test.ts`**, and several **`interactiveTty*`** files, with **duplicate scenarios** and **two styles** (mock `log` vs real terminal output).

---

## Phase 1 — Inventory and dedupe (no behavior change) ✅ done

**Outcome:** Less duplication, clearer “where to add a test.”

- Map **recall-related** cases: list tests in `processInput.test.ts` vs `recall.test.ts` that assert the **same user-visible outcome** (same mocks, same messages); **merge or delete** duplicates, keeping the clearer assertion messages.
- Tag or section headers only: mark tests as **contract** (`processInput` + default console — matches `-c` / piped log) vs **TTY-only** (must use `interactiveTty*`).
- **Deliverable:** Shorter total line count or same coverage with fewer tests; no production changes.

**Done (2025-03-21):** Merged duplicate-style coverage in `processInput.test.ts` (spelling happy-path + `thinkingTimeMs`; standalone MCQ `thinkingTimeMs` folded into `/contest` MCQ flow). Section comments label **contract** vs explicit **OutputAdapter**; `recall.test.ts` header comment + renamed `describe` for the processInput load-cancel block. No overlap found between `recall.test.ts` recall API tests and `processInput` recall mocks worth deleting (different layers).

---

## Phase 2 — One home for “recall + `processInput`” contract ✅ done

**Outcome:** Single obvious file for “recall command through `processInput` with mocked API,” aligned with planning **cohesion for one behavior**.

- Move or merge so **either** `recall.test.ts` **or** `processInput.test.ts` owns **multi-turn recall via `processInput` + `console.log`**, not both.
- Keep **`recall.test.ts`** focused on **`recall.ts`** pure behavior (`recallStatus`, `recallNext` wiring with mocks) where tests **do not need** `processInput`.
- **Deliverable:** New contributors know where to add recall tests; duplicated describe blocks gone.

**Done (2026-03-21):** `interactive/processInput.test.ts` owns all `processInput` `/recall` contract tests (default `console.log` flows + `describe('contract: /recall load — real recallNext + OutputAdapter cancel')`). `recall.test.ts` no longer imports `processInput`; `cli.mdc` loading-UI note updated.

---

## Phase 3 — TTY vs piped: one observable surface per gap ✅ done

**Outcome:** Fewer “log-only” tests where **real** user-visible difference is TTY bytes.

- For behaviors already covered in **`interactiveTty*`** (ESC, MCQ, token list, fetch-wait repaint), **drop** redundant `processInput` tests that only repeat the same **plain string** unless they guard **non-TTY** (`-c` / default adapter) semantics.
- Where **TTY and piped differ** (e.g. buffered `ttyOutput` vs `console.log`), keep **at most one** focused test per surface (same idea as `interactiveExitFarewell.test.ts`).
- **Deliverable:** No loss of coverage on either surface; less processInput-only noise.

**Done (2026-03-21):** Removed duplicate `/help` contract from `processInput.test.ts` (same assertions live under `help.test.ts` → `processInput with /help`). Folded “load more” prompt + first `recallNext(0, …)` check into `/recall load more: user says n`. Merged the two MCQ markdown rendering tests into one default-console case (stem + choices).

---

## Phase 4 — `interactiveFetchWait`: shrink adapter-shaped tests ✅ done

**Outcome:** Minimum spies on `OutputAdapter`; keep what Cypress cannot cover.

- List each test in **`interactiveFetchWait.test.ts`**: classify as **(a)** user-visible wait/cancel outcome, **(b)** internal callback ordering only.
- Replace **(a)** with **`runInteractive` + TTY** where stable (reuse `DOUGHNUT_CLI_SLOW_RECALL_LOAD_MS` / existing patterns) when it **reduces** reliance on mock adapter shape.
- Retain **(b)** only if **no** practical observable hook exists without flaking.
- **Deliverable:** Smaller file or same coverage with fewer `mockInteractiveOutputAdapter`-style tests; `cli.mdc` “no E2E for transient loading” still respected.

**Done (2026-03-21):** Dropped `onInteractiveFetchWaitChanged` / `cancelInteractiveFetchWaitFor` adapter tests duplicated by **`interactiveTtyFetchWaitEsc.test.ts`** (Esc → `Cancelled by user.`). Replaced “start/end signal” cases with **TTY** assertions on visible loading copy (`Loading recall questions`, `Loading recall status`) in that file. **`interactiveFetchWait.test.ts`** now keeps renderer/formatting, **`processInput` + minimal adapter** (log / logError only) for `/recall` error, `/recall-status` success, `/add-access-token` + signal, and **`getInteractiveFetchWaitLine()`** after `runInteractiveFetchWait` rejection instead of callback counts.

---

## Phase 5 — Help, access tokens, gmail: entry-point alignment

**Outcome:** Public CLI contract tested like a user runs it where cheap.

- **`help.test.ts`:** Prefer **`formatHelp`** / doc structure tests + **one** path through **`run(['-c', '/help'])`** or **`processInput`** — avoid three parallel ways to assert the same listing.
- **`accessToken.test.ts` / `gmail.test.ts`:** Prefer outcomes (files written, messages) over **private** function names; subprocess or `index` only if it **simplifies** and stays fast.
- **Deliverable:** Clear story: “non-interactive contract” vs “interactive TTY” vs “pure helpers.”

---

## Phase 6 — Guardrails (ongoing)

- **New interactive behavior:** Default to **`runInteractive`** + observable output; add **`processInput` + mock adapter** only when the contract **is** non-interactive (`-c`) or helper-only.
- **`processInput.test.ts`:** Treat as **contract tests** for the **default `OutputAdapter`** (console), not as a second interactive engine.
- Re-read **`cli.mdc` → Vitest: observable behavior** when adding tests.

---

## Order and gates

Phases **1 → 2** give the most cohesion per effort. **3–4** need care not to drop **piped** or **-c** coverage. After each phase: **`pnpm cli:test` green**; optional targeted Cypress CLI spec if a scenario touched install/interactive E2E assumptions.

When this is **stable**, delete or trim this file.
