# MCP server unit test optimization

Status: done

## Profiling baseline (2026-06-03)

Command: `cd mcp-server && CURSOR_DEV=true nix develop -c pnpm exec vitest run --reporter=json`

- **29 tests**, full suite wall ~1.0–1.5s (Vitest startup dominates; per-test CPU &lt; 3ms).
- **No `sleep` / fixed waits** in tests today.

### Top 10 slowest tests (by Vitest `result.duration`)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 2.17 | tests/tools/find-most-relevant-note.test.ts | should extract query when args is an object with query property |
| 2 | 1.94 | tests/tools/tool-builder.test.ts | checks draft-07 in generated schema… |
| 3 | 1.14 | tests/tools/get-note-graph.test.ts | should successfully fetch graph with valid tokenLimit |
| 4 | 1.11 | tests/tools/get-note-graph.test.ts | should be defined and have correct name |
| 5 | 0.98 | tests/helpers.test.ts | should handle Error objects |
| 6 | 0.80 | tests/tool-schemas.test.ts | simple tools should have basic object schema |
| 7 | 0.79 | tests/tool-schemas.test.ts | should have async handlers |
| 8 | 0.72 | tests/server.test.ts | should load all tools successfully |
| 9 | 0.70 | tests/server-config.test.ts | should have correct server metadata |
| 10 | 0.63 | tests/tools/get-note-graph.test.ts | should return error when tokenLimit is 0 |

### Grouping

Top 10 span **7 files**; per-file groups = 7, per-10 = **1**. Use **≤10 tests per phase** (smaller case count):

1. **Registry / schema redundancy** — `server.test.ts`, `server-config.test.ts`, `tool-schemas.test.ts` (16 tests → split 9 + 7)
2. **Tool behavior** — `find-most-relevant-note`, `get-note-graph`, `tool-builder` (8 tests)
3. **Helpers** — `helpers.test.ts` (10 tests)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits (`sleep`, arbitrary `setTimeout`).
3. Flaky = failure; tests must be deterministic.

---

### Phase 1: Consolidate registry tests (server + server-config)
Status: done

**Result (2026-06-03):** Merged into `mcp-server/tests/server.test.ts` (1 test); deleted `server-config.test.ts`. Registry tests **9 → 1**; full suite **29 → 21** tests.

**Scope:** `mcp-server/tests/server.test.ts`, `mcp-server/tests/server-config.test.ts` (9 tests).

**Goals:**
- Merge overlapping “tools array / structure / expected tools” coverage into one focused file or describe block.
- Delete tests that only assert literals (`expect(true).toBe(true)`, tautological `toContain` on same array).
- Remove duplicate per-tool “defined + name” checks if covered by registry tests.
- Keep behavior-focused coverage: tool names, schemas, handlers exist.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`

---

### Phase 2: Simplify tool-schemas tests
Status: done

**Result (2026-06-03):** Removed tautological tests; merged input-schema and handler checks into two loop tests; async verified via `AsyncFunction` without invoking handlers. **7 → 2** tests in this file; full suite **21 → 17** tests.

**Scope:** `mcp-server/tests/tool-schemas.test.ts` (7 tests).

**Goals:**
- Remove useless tests (`basic object schema` asserting a local literal; `expect(true).toBe(true)`).
- Fold duplicate handler signature checks into one loop test.
- Avoid calling real tool handlers with empty args if that causes flaky async work — prefer type/structure checks only.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`

---

### Phase 3: Optimize tool unit tests
Status: done

**Result (2026-06-03):** Removed redundant name tests (covered by `server.test.ts` registry); added shared `setupMockApiClient()` helper; simplified query-extraction test to assert search args only; table-drove token-limit errors. **8 → 5** tests in this scope; full suite **17 → 15** tests.

**Scope:** `find-most-relevant-note.test.ts`, `get-note-graph.test.ts`, `tool-builder.test.ts` (8 tests).

**Goals:**
- Remove trivial “should be defined and have correct name” if registry phase covers names.
- Share `beforeEach` / mock setup via helpers where duplicated.
- Simplify `runQueryExtractionTest` if over-asserting.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`

---

### Phase 4: Optimize helpers tests
Status: done

**Result (2026-06-03):** Table-driven `test.each` for `createErrorResponse` (4 cases) and `getApiConfig` (3 cases); removed per-test `vi.resetModules()` (not needed — `getApiConfig` reads `process.env` at call time). **7 tests** in this file (unchanged count, shorter file); full suite **15** tests.

**Scope:** `mcp-server/tests/helpers.test.ts` (10 tests).

**Goals:**
- Collapse `createErrorResponse` variants if table-driven is shorter without losing coverage.
- Avoid `vi.resetModules()` per test unless required; reduce env test duplication.

**Verify:** `CURSOR_DEV=true nix develop -c pnpm mcp-server:test`

---

### Phase 5: Re-profile and close
Status: done

**After (2026-06-03):** `pnpm mcp-server:test` — **15 tests**, 6 files, Vitest **~219ms** (tests 15ms, import 571ms). No sleeps in tests.

#### Top 10 slowest (post-optimization)

| # | ms | file | test |
|---|-----|------|------|
| 1 | 2 | tests/tools/tool-builder.test.ts | checks draft-07 in generated schema… |
| 2 | 2 | tests/tools/find-most-relevant-note.test.ts | should extract query when args is an object with query property |
| 3 | 1 | tests/helpers.test.ts | should handle Error objects (test.each row) |
| 4 | 1 | tests/tool-schemas.test.ts | each tool has JSON object input schema |
| 5 | 1 | tests/server.test.ts | exposes expected tools with required shape |
| 6 | 1 | tests/tools/get-note-graph.test.ts | returns error when tokenLimit is 0 |
| 7 | 1 | tests/tools/get-note-graph.test.ts | returns error when tokenLimit is 5 |
| 8 | 1 | tests/tools/get-note-graph.test.ts | should successfully fetch graph with valid tokenLimit |
| 9 | 0 | tests/tool-schemas.test.ts | each tool exposes an async handler |
| 10 | 0 | tests/helpers.test.ts | remaining test.each rows |

#### Summary

| Metric | Before | After |
|--------|--------|-------|
| Test count | 29 | **15** |
| Test files | 7 | **6** (`server-config.test.ts` removed) |
| Redundant handler invocations | yes | **no** |

**Commits:** `0c05d56ef2` (phase 1), `c23398aaa3` (phase 2), `95ab1de53d` (phases 3–4).
