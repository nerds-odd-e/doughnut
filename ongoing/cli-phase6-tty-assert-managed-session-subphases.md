# Phase 6 — `tty-assert` managed session and Cypress assertion adapter

**Status:** Planned. This document refines **Phase 6** from [`ongoing/cli-terminal-test-library-extraction.md`](./cli-terminal-test-library-extraction.md).

---

## Problem to solve

Today the interactive CLI PTY is started in the Cypress plugin and stored as a live Node handle, but the assertion path does **not** keep using that same live session object:

- `createCliE2ePluginTasks()` keeps one `interactiveCliPtyHandle`.
- `outputAssertions.ts` re-fetches raw PTY bytes through `cliInteractivePtyGetBuffer` for every assertion attempt.
- Retry lives in Cypress page objects, not in `tty-assert`.
- xterm replay is rebuilt from the full raw transcript on every assertion instead of keeping one live emulator attached to the same session.

That split is the main boundary problem for Phase 6. The goal is to make `tty-assert` own the **session + replay + retry** relationship so Doughnut E2E steps stop passing raw PTY output back into the library.

---

## Design decisions

1. **`tty-assert` owns a long-lived managed session object.**
   It should keep the PTY process, cumulative raw transcript, xterm mirror, replay sync state, and assertion helpers together.

2. **Doughnut keeps Cypress-specific glue.**
   The npm package stays **Node-only** and **Cypress-neutral**. `e2e_test` registers `cy.task(...)` handlers, but those tasks delegate to `tty-assert` APIs instead of re-implementing retry and replay.

3. **Use one Cypress assertion task.**
   Keep start / write / dispose as separate side-effect tasks, but collapse assertions behind a single task such as `cliInteractiveAssert(request)`. This matches the desired “one assertion API only” boundary.

4. **Move retry into `tty-assert`, not Cypress page objects.**
   The browser-side code should describe the assertion and optionally take a screenshot on failure. It should not own PTY polling or raw-buffer re-reads.

5. **Keep one xterm instance per live PTY session.**
   Assertions should sync newly arrived bytes into the existing xterm terminal instead of rebuilding a fresh terminal from the full transcript every time.

6. **Cross the Cypress boundary with serializable requests only.**
   Regexes, anchor patterns, and options must cross as plain JSON data such as `{ source, flags }`, not live `RegExp` objects or callbacks.

7. **Keep Doughnut-specific semantics outside the package.**
   “Current guidance”, “past user messages”, and similar Doughnut names stay in page objects or thin request-builder helpers. `tty-assert` should only know generic surfaces, matchers, and style requirements.

8. **Prefer `dispose` semantics over `kill` semantics.**
   The public lifecycle should describe ending a session, not merely sending a signal. Disposal should be idempotent and safe when the child already exited.

9. **Do not build a multi-session framework yet.**
   Keep the current “one active interactive CLI session in the plugin” model. Phase 6 is about repairing ownership and lifecycle, not introducing pooled sessions.

---

## Proposed shape

### `tty-assert` library boundary

```ts
type SerializedPattern =
  | { kind: 'text'; value: string }
  | { kind: 'regex'; source: string; flags?: string }

type TtyAssertRequest = {
  needle: SerializedPattern
  surface: 'strippedTranscript' | 'viewableBuffer' | 'fullBuffer'
  timeoutMs?: number
  retryMs?: number
  strict?: boolean
  messagePrefix?: string
  startAfterAnchor?: Array<{ source: string; flags?: string }>
  fallbackRowCount?: number
  requireBold?: boolean
  rejectGrayForegroundOnlyWithoutGrayBackground?: boolean
  requireGrayBackgroundBlock?: boolean
}

type ManagedTtySession = {
  write(data: string): void
  submit(line: string): void
  assert(request: TtyAssertRequest): Promise<void>
  dumpFrames(): Promise<TtyAssertDumpFrames>
  dispose(): void
}
```

### Cypress adapter boundary

- `runRepoCliInteractive` / `runInstalledCliInteractive`
  Start the managed session and keep it in plugin memory.
- `cliInteractiveWriteRaw` / `cliInteractiveWriteLine`
  Forward to the same managed session.
- `cliInteractiveAssert`
  Delegate the request to `managedSession.assert(...)`.
- `cliInteractivePtyDispose`
  Dispose the managed session idempotently.

### Replay sync strategy

- The managed session stores the shared raw transcript.
- It also stores replay sync state, for example `replayedCharCount`.
- Before each assertion attempt, it feeds only `raw.slice(replayedCharCount)` into the xterm instance.
- If replay state becomes invalid, for example geometry reset or truncated raw buffer, it can rebuild from the full raw transcript as a safe fallback.

This keeps the common path incremental without introducing a second source of truth.

---

## Non-goals for Phase 6

- No Cypress dependency inside `packages/tty-assert`.
- No switch away from Cypress Cucumber CLI E2E.
- No new multi-session test framework.
- No change to Doughnut CLI rendering behavior.
- No PNG / animation artifacts yet; those remain later phases.

---

## Sub-phases

### 6.1 — Introduce managed session ownership inside `tty-assert`

**Outcome:** A Node consumer can keep one live PTY session together with one live xterm-backed assertion engine and call assertions **without passing raw transcript text in**.

**Work:**

- Add a managed-session type in `tty-assert` that wraps `BufferedPtySession`.
- Move assertion polling into that managed session.
- Keep replay sync state so xterm can be incrementally updated from newly arrived bytes.
- Preserve debug helpers such as `dumpFrames()` on the same object.
- Keep `startProgram` / `attachTerminalHandle` either delegating to the new type or replaced by a clearer managed-session entry point.

**Tests:**

- `tty-assert` unit tests for:
  - incremental replay sync across multiple assertions;
  - visible-text assertion without raw-string arguments;
  - dump output still reflecting the same replay state.

**Gate:** `pnpm tty-assert:test` green with new managed-session tests; no Doughnut E2E migration yet.

### 6.2 — Clarify session lifecycle: start, startup wait, dispose

**Outcome:** Starting and ending the interactive PTY uses one explicit lifecycle contract: start session, wait for startup marker, write, dispose. Teardown is idempotent and leak-safe.

**Work:**

- Make startup wait use the managed session’s own assertion path instead of ad hoc handle calls.
- Define what `dispose()` means when the child already exited.
- Remove lifecycle ambiguity between `kill()` and “clean end of session”.
- Ensure timers, listeners, and references created for retry or replay do not leak past disposal.
- Keep Doughnut plugin code thin: session slot + delegation only.

**Tests:**

- `tty-assert` unit tests for:
  - double-dispose;
  - child already exited;
  - timeout waiting for startup marker.

**Gate:** lifecycle unit tests green; targeted interactive CLI startup scenarios still green.

### 6.3 — Migrate current-guidance waiting and assertions to one Cypress assertion task

**Outcome:** The “Current guidance” path stops fetching raw PTY output in Cypress. Page objects send one serialized assertion request to the plugin, and `tty-assert` handles retry + replay internally.

**Why first:** This is the most visible xterm-dependent path and the best proof that the live session owns the emulator state.

**Work:**

- Add `cliInteractiveAssert(request)` in the Cypress plugin.
- Add Doughnut request builders for:
  - current guidance contains text;
  - current guidance contains bold text;
  - “wait until prompt visible, then write” flow used by `whenCurrentGuidanceContainsThen`.
- Keep screenshot capture in Cypress after task failure, because screenshots belong to the browser runner, not the Node package.

**Tests:**

- Targeted Cypress coverage for scenarios that use current guidance and prompt waiting.

**Gate:** relevant CLI E2E green; `outputAssertions.ts` no longer calls `cliInteractivePtyGetBuffer` for current-guidance paths.

### 6.4 — Migrate transcript and full-buffer assertions to the same task

**Outcome:** Past assistant messages, answered questions, and past user message display assertions all use the same `cliInteractiveAssert` task. Normal assertion paths no longer ship raw PTY output from plugin to browser and back again.

**Work:**

- Move `pastCliAssistantMessages`, `answeredQuestions`, and `pastUserMessages.expectDisplayed` to serialized requests handled inside `tty-assert`.
- Keep Doughnut-only composition in Doughnut code:
  - section names for failure prefixes;
  - blank-line-above rule for past user messages;
  - gray background block expectations.
- Delete Cypress-side retry helpers that exist only to support raw-buffer assertion loops.

**Tests:**

- Targeted Cypress coverage for interactive install/run and other scenarios that exercise transcript and full-buffer surfaces.

**Gate:** production assertion helpers no longer call `cliInteractivePtyGetBuffer`; touched CLI E2E remain green.

### 6.5 — Cleanup, docs, and debug-only escape hatches

**Outcome:** The steady-state API is obvious: start, write, assert, dispose. Raw-buffer access is no longer a normal test API, and the docs describe the new ownership boundary clearly.

**Work:**

- Update `packages/tty-assert/README.md` with managed-session lifecycle and serialized assertion request guidance.
- Update `ongoing/cli-terminal-test-library-extraction.md` to mark sub-phase completion as work lands.
- Decide whether `cliInteractivePtyGetBuffer` remains as:
  - a debug-only task not used by page objects, or
  - a removed task if nothing still needs it.
- Remove outdated comments that tell Cypress callers to avoid the Node `tty-assert` handle because messages differ; after migration there should be one normal assertion path.

**Tests:**

- No new broad suite required beyond the targeted tests above.
- Confirm lint and typecheck remain green for touched files.

**Gate:** docs match implementation; no normal Doughnut E2E assertion path depends on raw-buffer round-trips.

---

## Suggested implementation order

1. **6.1** first, because the rest of the phase depends on the managed-session object existing.
2. **6.2** next, so lifecycle semantics are stable before migrating more Cypress calls.
3. **6.3** then migrate the current-guidance path as the first real Doughnut slice.
4. **6.4** migrate the remaining assertion families once the task boundary is proven.
5. **6.5** cleanup only after all production assertions use the new path.

---

## Risks and mitigations

- **Risk:** Incremental xterm sync drifts from the raw transcript.
  **Mitigation:** Keep a tested rebuild-from-raw fallback and use it when replay state is invalid.

- **Risk:** Serialized assertion requests become Doughnut-shaped.
  **Mitigation:** Keep the task request generic and put Doughnut vocabulary in request builders only.

- **Risk:** Screenshot behavior regresses when retry moves into the library.
  **Mitigation:** Keep screenshot capture in Cypress wrappers around the single assertion task.

- **Risk:** Phase 6 accidentally becomes a big “framework” rewrite.
  **Mitigation:** Keep one active session only and migrate existing assertions one family at a time.

---

## Acceptance summary for Phase 6

Phase 6 is complete when all of the following are true:

- `tty-assert` owns a long-lived interactive session object with PTY + xterm + retry.
- Doughnut Cypress code uses one assertion task instead of `cliInteractivePtyGetBuffer` loops.
- Current guidance and transcript/full-buffer assertions all run through the same managed session.
- Session disposal is idempotent and documented.
- CLI E2E coverage for touched scenarios remains green.
