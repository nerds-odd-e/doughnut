# Finish bounded execution safeguards with shorter instructions

## Source, goal, and status

Sources: [SEED-012 Stories 5–7](../../seeds/SEED-012-priority-execution-process-improvements.md#story-5).
Status: in progress; quick 014 is committed and the shared file is released.

The developer explicitly grouped these distinct stories in quick 015. Keep their
outcomes separate: consistent refactor handoffs, lost-handle observer shutdown,
and conditional storage readiness. Start after quick 014 releases `wrap-up.md`;
preserve its staging safeguards. No completed slice is replaced or renumbered.

Deliver the observer fallback before relying on another long unattended run.
Install the storage-readiness instruction here; apply its experiment requirement
only to a later change with a concrete unresolved storage assumption.

## Scope and decisions

- Instruction owners by slice: `execute-plan/references/wrap-up.md`,
  `execute-plan/references/ci-notify-codex.md`, and `slice-planning/SKILL.md`,
  all under `.agents/skills/`. Preserve existing obligations and unrelated edits.
- Each changed instruction document must finish shorter than its execution-start
  version. Capture `wc -w <document>` before editing and after completion. Replace
  repetition; do not move text elsewhere, remove obligations, or use shorthand.
- Slice 2 also owns the small startup-receipt change in
  `execute-plan/scripts/ci-mailbox.mjs` and focused lifecycle tests. The size
  target applies to instructions, not necessary runtime code/tests. No new
  recovery registry, runner, generalized process manager, or repair scheduler.
- Reuse existing handoffs, PLAN notes, mailbox stop, and test fixtures. Add no
  agent or review stage. Preserve explicit developer verification requests and
  all other host policies. No product/storage changes or Pygardon rollout.

## Ordered slices

### 1. Keep routine refactor delegation and return consistent with decide-first proof

Type: Behavior
Status: done
Proof: Four read-only prompt/handoff replays below, a smaller word count, and
ordinary review confirming existing obligations survive the rewrite.

Behavior: Coordinator prepares or consumes a routine refactor handoff → applies
the canonical decide-first clause → prevents contradictory test requests and
recognizes a consistent return without unnecessary verification.

Rewrite the existing refactor steps: no edits means no tests and
`skipped — no refactor edits`; edits mean only invalidated focused proof, naming
why a replacement is needed if its boundary moved. Correct contradictory prompt
additions before dispatch. At return, check the existing edit/test report as well
as its completion marker; retain current Jidoka and missing-evidence handling.

If an unnecessary run already happened, report it as a process deviation and
reuse otherwise valid proof. Do not repeat tests/review to manufacture compliance,
or let a reported pass erase an actual failure. Preserve existing failure routing.

| Supplied replay | Required observation |
|---|---|
| Routine draft asks for a full suite to confirm no changes. | Remove that contradiction before dispatch; keep the decide-first clause. |
| No edits; report says `skipped — no refactor edits`. | Accept without tests. |
| Edits invalidate proof A, leaving B valid. | Rerun only A; reuse B. Explain any focused replacement for a moved boundary. |
| No edits, but an unnecessary suite was already run. | Report the deviation, reuse valid evidence, and correct delegation; no repeat or false compliance claim. |

Use supplied outcomes, not real product tests or extra review agents. Report
replay decisions as instruction evidence and the literal size command/counts
in the existing handoff. No replay harness or additional report format.

### 2. Stop the retained Codex observer after volatile handles are lost

Type: Behavior
Status: done
Proof: Extend the existing Codex lifecycle test to lose its saved handle map,
recover the startup identity from an on-disk note, stop that exact mailbox through
the real CLI, and observe process exit plus retained terminal/failure evidence.
A second observer remains running; no replacement is launched.

Behavior: The execution retained its startup identity but Codex handles are
unavailable at shutdown → use the saved mailbox directory to request cooperative
stop → confirm the recorded stream exits and report persisted coverage honestly.

Reuse `ci-mailbox.mjs stop DIRECTORY`, `request.json`, `result.json`, and existing
bounded terminal waiting. Include `process.pid` alongside `directory` in the
stream's existing startup receipt; preserve other receipt fields and native
launch behavior. In the Codex adapter, retain directory/PID with coordinator and
checkout in the active PLAN after startup and before the first push. Recover
that note before considering a replacement launch. No new state file or index.

Rewrite the adapter's shutdown wording around the existing normal-handle path
and this fallback, making the document shorter. Validate the saved directory's
checkout/request, use cooperative stop, and confirm exit with a finite local
wait. The recovered path never signals a PID. Missing/mismatched identity means
no guessed stop; a stop error, missing terminal result, or unconfirmed exit means
an explicit unresolved report, not a green receipt or a forced kill. Preserve
recorded failures and `pendingCi: unobserved`; do not change acknowledgment or
repair semantics. Older observers without a retained identity remain outside
automatic recovery.

Use the fake-GitHub process fixtures in `ci-codex-lifecycle.test.mjs`; do not
contact GitHub, terminate a real execution observer, or require actual compaction.
Assert the startup PID identifies the real child and exercise recovery without
its volatile handle. Replay missing/mismatched identity and unavailable terminal
evidence as instruction branches; the real-process happy path must run.

Focused command:
`CURSOR_DEV=true nix develop -c node --test .agents/skills/execute-plan/scripts/ci-codex-lifecycle.test.mjs .agents/skills/execute-plan/scripts/ci-observer-stream.test.mjs`.

One lifecycle proof loop using existing stop behavior, targeting about five
minutes with medium confidence. Broader forced-termination recovery is excluded.

### 3. Require a targeted proof only for uncertain storage behavior

Type: Behavior
Status: planned
Proof: Apply the amended planning-readiness instruction to the three source
story examples: uncertain DDL, uncertain transaction-fixture visibility, and
routine/already-proven behavior. Only unresolved assumptions require new proof.

Behavior: Planning encounters a concrete unresolved engine or transaction
assumption → reuse matching evidence or obtain one isolated representative proof
→ declare the affected work ready only with a demonstrated critical postcondition.

Rewrite `slice-planning`'s existing execution-context check, making the skill
shorter. Keep the assumption, engine/version, literal command, observed
postcondition, and result in the existing PLAN. For uncertain DDL, use the exact
sequence plus a representative later parent update; for transaction fixtures,
use the actual ownership/visibility semantics with isolated data. Failed evidence
changes the plan before broad implementation. Routine migrations and applicable
existing proof add no experiment; do not invent uncertainty to trigger one.

Perform a read-only readiness replay with supplied evidence, not a database
experiment in this process plan. No migration, fixture redesign, shared/production
database mutation, generic spike, or new testing framework. One instruction
decision loop, targeting about five minutes with medium confidence.

## Coverage and completion

| Selected promise | Owning evidence |
|---|---|
| Refactor consistency without unnecessary tests | Slice 1's four replays |
| Durable identity leads to exact cooperative stop, exit, preserved evidence, and no duplicate launch | Slice 2 real CLI/process proof with an unaffected second observer |
| Missing identity/evidence never causes guessed termination or false closure | Slice 2 instruction branch replays |
| Unknown DDL/transaction assumptions receive relevant proof; known cases avoid extra work | Slice 3's three readiness replays |
| Each instruction document becomes shorter with other obligations intact | Each slice's word counts and ordinary review of its full diff |

A passing replay/test cannot close a slice whose instruction document grew or
lost required guidance. Slice 2's process proof is required; instruction replays
alone cannot establish shutdown. Other slices need no runtime test suites.

Follow ordinary `execute-plan` delivery wrap-up. Keep compact replay/count results
in this PLAN while active. Mark each source story only when its owned slice is
delivered; retain this PLAN until all three are complete. Then keep brief results
in their home sections and remove the spent PLAN. SEED-011's remaining repair work
stays deferred. Observe natural relevant executions for live effectiveness; add
no monitor or forced product exercise and do not overstate simulated evidence.

## Readiness

Slice 2: `CURSOR_DEV=true nix develop -c node --test .agents/skills/execute-plan/scripts/ci-codex-lifecycle.test.mjs .agents/skills/execute-plan/scripts/ci-observer-stream.test.mjs`
passed all 5 tests after refactor. Assertions establish actual receipt PID,
cleared handle map, saved-note recovery and request validation, real CLI stop,
bounded exit, retained failure/terminal evidence, and a live second observer with
no replacement. Missing/mismatched identity and missing result/exit instruction
replays remain unresolved without guessing or forced termination.
`wc -w .agents/skills/execute-plan/references/ci-notify-codex.md`: 717 → 633.
Formatting expanded the lifecycle file; the independent refactor extracted two
test helpers into existing `watch-ci-test-fixtures.mjs`, then reran the invalidated
focused proof. Nix daemon access was denied before tests; authorized access
resolved that environmental launch failure. No test failure was discounted.

Execution evidence: Slice 1's four supplied replays pass: remove contradictory
full-suite additions; accept no-edit skip; rerun invalidated A and reuse B with a
reason for any moved-boundary replacement; report prior needless runs without
repeat or concealed failures. `wc -w .agents/skills/execute-plan/references/wrap-up.md`:
557 → 547. Full diff preserves obligations; independent refactor made no edits,
so tests were skipped. Coordinator formatting passed.

Observer: coordinator quick015-01a07594, checkout `/Users/terryyin/git/doughnut`,
repository nerds-odd-e/doughnut main; yielded cell 7, session 99469.
The initial Nix launch returned without a session/receipt because its cache was
read-only; tooling succeeded with cache access and the observer was relaunched.
This observer predates the PID receipt change; retain its normal session handle.

Three separate Behavior leaves in the developer-requested bundle. Each has one
outcome and bounded proof loop; the lifecycle slice reuses an existing command
and harness. Ready for direct execution once file ownership is clear; no
additional refinement is currently needed. Apply existing timing and learning
escalation on overruns; refine this PLAN rather than expand the lifecycle scope
or waive concision. Sizing is not an execution-time guarantee.
