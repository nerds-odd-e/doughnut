# Truthful removal handoff evidence

## Slice 1: historical proof-item review

Input: `git show 7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md`,
the whole historical plan, especially its mixed outside-in proof row and slice
5 Proof. Both prescribe `no [role="status"]` despite the scope citing principle
7. This review reconstructs that handoff; it does not execute the historical
change or establish universal future compliance.

Reviewed at execution base `0a0def3db215338e3e323741db85d58bf5ec50ed` with the
uncommitted slice 1 guidance. The resulting proof-table rows are:

| Promise | Proof | Slice |
| --- | --- | --- |
| Button states, feedback, and busy editing rules | Mounted spoken-title specs: idle accessible name and unpressed state; listening name, pressed state and style; converting disabled then idle; silent/empty response leaves title unchanged, idle and no toast; microphone refusal shows the existing error toast then idle; conversion failure shows its error toast, preserves title/caret, returns idle, and permits a fresh recorder; New note Submit disabled while busy and Enter inert while listening. E2E clicks by accessible names; by-eye placement reading; documentation describes these states. | 5 |
| Read-only authorization remains enforced | Mounted read-only note provides no title-editing button. | 5 |

Resulting slice Proof: mounted spoken-title specs observe all states, feedback,
title/caret preservation, fresh-recorder recovery, and busy editing rules in
the first row; E2E name, by-eye placement, and documentation observations stay.
Separately, mount a read-only note and observe that it provides no title-editing
button. Record the one-time whole-product sweep as an acceptance reading.

Resulting implementation brief: make the title microphone an icon button and
delete the status-line behavior and exclusively dependent test support
transitively. Retarget existing mounted specs and E2E helpers to accessible
button names. Carry every observation in the first proof row into the
implementation and acceptance, including silent completion and both error
paths. Separately retain the read-only authorization proof in the second row.
Perform the historical slice's whole-product sweep as a one-time acceptance
reading. Skip the inherited `no [role="status"]` assertion.

Classification: idle unpressed state, `noToastShown`, and inert Enter prove
surviving outcomes; negative syntax alone is not grounds for deletion. The
read-only control restriction proves an actively enforced authorization rule.
The status-element assertion observes the removed interface and is omitted
from all three forms. Helper deletion is implementation scope; the sweep is a
separate reading.

Implementation returned uncommitted edits in `AGENTS.md`, `CLAUDE.md`, and the
Donut-authored `unit-testing` skill. The coordinator inspected those diffs and
the historical row and slice Proof, confirmed the two principle 7 additions
match and `unit-testing#removal-proof` resolves, and accepted the bounded
classification above. `./scripts/run.sh bash scripts/check_diff_whitespace.sh`
passed (exit 0). Runtime proof was unchanged; no product suite was run for prose.

Fresh independent refactor reviewed the five changed files, found no cohesive
refactor candidate, made no edits, and returned `## REFACTOR COMPLETE` with
accepted proof unchanged. `./scripts/run.sh pnpm format:changed` passed (exit 0).

Slice 1 publication accepted `6fadfdbf13bd0e105042d0ff71cc404b36c7f577` on
`refs/heads/codex/make-donut-removal-handoffs-preserve-valid-proof`, suffix base
`0a0def3db215338e3e323741db85d58bf5ec50ed`, with the retained observer reused.

## Slice 2: real evidence-removal handoff

Coordinator brief at accepted slice 1 revision
`6fadfdbf13bd0e105042d0ff71cc404b36c7f577`: delete the approved dated passage
section and solely dependent material, plus SEED-066's corresponding discovery
bullet and anchor. Follow the shared orchard input and later comparisons;
make independent navigation input self-contained from recorded facts, and
delete claims that cannot stand alone. Preserve October 3 provenance and
limitations, saved source/destination text, PATCH/reload results and causal
uncertainty; preserve October 6 setup, timing table, results and limitations.
Compare with this base, read the result independently, and follow all affected
local links/anchors, including SEED-066 navigation and the whole-file link in
`docs/voice-input.md`. Keep the one-time sweep as a separate acceptance reading;
do not add an absence test, rename historical actions, or make fresh observations.

The final leaf has no later outcome; deletion is compatible with the source's
retained-evidence promises. The story-obligations listing had zero entries.

Implementer returned uncommitted changes in `docs/voice-input-observations.md`
and SEED-066, with about three minutes active work. Its comparison and final
standalone reading used:

```sh
git show 6fadfdbf13bd0e105042d0ff71cc404b36c7f577:docs/voice-input-observations.md
git diff -- docs/voice-input-observations.md .planning/seeds/SEED-066-voice-input.md
cat docs/voice-input-observations.md
```

All exited 0. Coordinator inspected the same diff and retained document:
October 3 observation boundary, both saved bodies, navigation actions and
PATCH/reload/fresh-page results remain; the exact orchard input and synthesis
facts now make that journey self-contained. October 6 setup, table, timing
comparison, reload results, capitalization example, and limits remain. The
lighthouse reload/no-duplication subset stays truthful in both documents;
the dependent later-session comparison is deleted. No action is relabeled.

The affected consumers were selected by this reading (exit 0):

```sh
rg -n '\[[^]]+\]\([^)]*voice-input-observations[^)]*\)|\[[^]]+\]\([^)]*observation-boundary[^)]*\)|\[[^]]+\]\([^)]*voice-input\.md[^)]*\)' docs/voice-input-observations.md docs/voice-input.md .planning/seeds/SEED-066-voice-input.md
```

All four resolve: observations to `voice-input.md`, responsiveness to
`#observation-boundary`, SEED-066 to the retained navigation heading, and
`voice-input.md` to the whole observations file. The joining-rule link was
solely in the deleted section. `./scripts/run.sh bash scripts/check_diff_whitespace.sh`
passed (exit 0); product tests and real-service observation were outside this
prose change. Existing runtime tests remain unchanged.

Separate one-time acceptance sweep reading (exit 0):

```sh
rg -n --hidden --no-ignore --glob '!**/.git/**' --glob '!**/node_modules/**' --glob '!**/.gradle/**' --glob '!**/build/**' --glob '!**/dist/**' --glob '!**/target/**' --glob '!mysql/**' --glob '!redis/**' --glob '!**/*.log' --glob '!env.sh.secret' 'Write text now|requested mid-speech conversion|requested-mid-speech-conversion|Flush' .
```

Scope: product source/tests, docs, hidden agent guidance, planning, and generated
artifacts; dependencies, build output, logs and secrets excluded. The three
exact retired terms appeared only in this active plan's sweep instructions.
`Flush` matches describe ORM, Vue, autosave, or retained scheduler processing.
Coordinator inspected `audioProcessingScheduler.flush.spec.ts:150` and
`audioProcessingScheduler.ts:76,97`: timer recovery and `tryFlush` serve the
silence-triggered scheduler, independently of the retired control. The
corresponding SEED-066 reference is deleted. No product residue was found.
The active story, plan, evidence, and open finding remain temporary review
material for ordinary retrospective/wrap-up, not maintained product residue.

Fresh independent refactor read the retained observations and both consumers
against the same base, confirmed the four links and independent lighthouse
subset, and returned `none — already clean`, `## REFACTOR COMPLETE`, with no
edits or proof invalidation. Coordinator accepted that unchanged preservation
proof and the separately classified sweep. `./scripts/run.sh pnpm format:changed`
passed (exit 0). No generation trigger changed.
