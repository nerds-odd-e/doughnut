# Clear CI proof docs of local-proof wording

## Source

**Source:** [Clear CI proof docs of local-proof wording](../../seeds/SEED-104-remove-local-proof.md#clear-ci-docs-of-local-proof)
**Source review:** [source-review.md](./source-review.md)
- **Identity:** SEED-104#clear-ci-docs-of-local-proof
- **Provenance:** Retrospective of SEED-104#remove-local-proof after planned
  slices 1–3. Related product commits: `fe9092acc6`, `e96ef120f3`, `07044faff7`
  (E5 observed on run 42037897618); plan evidence `a7896672b2`.

## Goal and scope

Maintained CI proof documentation describes only surviving report surfaces.
Remove the leftover "proof recording" wording from `docs/ci-proof-summary.md`.
Scope and examples are in the correction story. This plan adds no promise.

**Exclusions:** Re-running the whole-product local-proof token sweep as a
permanent test; changing CI workflows, converters, or Intent locations; editing
Open Dough managed `.agents/skills/dough-*` copies; dated findings provenance.

## Current findings (retrospective)

At tip `a7896672b2`, `docs/ci-proof-summary.md` still says local runs, the Cloud
VM, **and proof recording** write no CI runner report. That phrase names the
deleted local proof feature. SEED-104 promised maintained files would not
describe local proof and would keep no historical feature notes. The planned
token sweep returned empty because it did not match this prose; source review
marked the "describes" clause observed from that empty reading.

## Preserved behavior

- CI proof summary production, fragments, dashboard contract, and
  `pnpm test:ci-proof` stay as they are after SEED-102 / SEED-104.
- Intent locations in `.planning/open-dough.json` stay.

## Current decisions

- **One Behavior slice:** edit the one sentence in place; no code or test churn.
- **Proof:** surviving `test:ci-proof` plus a one-time reading that the doc no
  longer names proof recording / local proof; no permanent absence assertion.
- **PFE:** the only product hit for "proof recording" outside exclusions is this
  file; edit the existing doc rather than adding a parallel note.

## Decisive premises

| Premise | Consuming operation | Observation | Result | Remainder |
| --- | --- | --- | --- | --- |
| Only `docs/ci-proof-summary.md` still names "proof recording" outside exclusions | Slice 1 doc edit | Retrospective reading at `a7896672b2` (2026-11-05): product search for local-proof prose outside `.planning/`, managed `dough-*`, and dated findings | Single hit at `docs/ci-proof-summary.md:22` | none |

## Outside-in proof

| Promise | Owner | Proof |
| --- | --- | --- |
| Example 1: CI proof docs name only surviving non-report surfaces | Slice 1 | One-time reading of `docs/ci-proof-summary.md` after the edit |
| Example 2: CI proof tests still pass | Slice 1 | `./scripts/run.sh pnpm test:ci-proof` |

## Slices

### 1. CI proof summary docs omit removed local proof wording

Type: Behavior
Status: done
Proof: accepted — `docs/ci-proof-summary.md:22` reads "Local runs and the Cloud
VM write no such report."; one-time reading found no proof-recording /
local-proof residue in that file; `./scripts/run.sh pnpm test:ci-proof` 8 pass.
Consumer-trace: production `ci-proof-summary` script/action hits excluded
(collector basename, not maintained-doc prose); guidance/planning/DearDough
hits excluded (managed Open Dough copies or non-product records). Source review
commitments 1–7.2 observed in `source-review.md`.

Behavior: a developer opens `docs/ci-proof-summary.md` → the runner-report step
states that local runs and the Cloud VM write no such report, without naming
proof recording.

Work: in `docs/ci-proof-summary.md`, change "Local runs, the Cloud VM and proof
recording write no such report." to name only local runs and the Cloud VM.

Sizing: about 2 minutes including the focused CI-proof test run.

## Concern evidence

None remaining for this bounded correction.

## Learnings

- A token-only removal sweep can miss natural-language residue that still
  describes the deleted feature (see DearDough DD-346).

## Execution complete

Product advice: no change
