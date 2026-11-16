# Fresh generation evidence and refinement boundary

Captured on 2026-11-16 UTC; accepted on 2026-11-17 SGT. The private home is
`pilot/fresh-20261116/` under the established default-checkout cache.
`generation-summary.json` governs this outcome. Earlier `summary.json` remains
an unchanged pre-terminal partial observation, not the current result.

## Preserved generation outcome

- Product code rendered 12 on-demand requests and one 12-row Batch JSONL job,
  with `gpt-6.1-sol`, 1,000 output tokens and omitted reasoning, as the product
  specifies for generation. Real reference indexes reconstructed 113 captured
  focus/direct-linked notes and notebook metadata; original folders and other
  retrieval candidates were not reconstructed. All 24 focus notes are outside
  the 155 historical IDs and reuse the 24 held-out focuses.
- The one Batch job was submitted at `2026-11-16T14:40:20.698Z`; its one-hour
  deadline was `15:40:20.698Z`. The cancellation receipt and history retain the
  actual transition; terminal cancellation was observed at `15:50:55.945Z`.
  The provider reported 12 total, zero completed/failed rows and explicit
  aggregate input/output usage of zero. All 12 attempts remain counted as
  `batch_cancelled_zero_usage`, preserving original reservations and timestamps.
- Twelve original synchronous generations and twelve distinct same-body
  synchronous fallback calls completed. Batch output quality is unmeasured.
  The 4,235.247-second job turnaround includes cancellation settlement and is
  not per-row inference latency. Provider input/error files were removed only
  after retaining local artifacts; `cleanup-*.json` holds the receipts.
- Product parsing/validation accepted 24 drafts and rejected zero. The full
  backend suite passed 3,199 tests, zero failures/errors and two skips in both
  the initial renderer run and recovered actual-ready product-proof run.
  `ScratchPilotFreshGenerationTest.java`, `render-test.xml`,
  `decode-render-recovered-test.xml` and their logs retain setup and assertions.
- Fresh generation accounts for 36 attempts: 12 sync, 12 cancelled-zero batch,
  12 fallback. Sync usage is 24,880 input/4,188 output, USD 0.091640;
  fallback 24,501/4,337, USD 0.092372. Mean/median/max HTTP seconds are
  9.557/8.862/14.344 and 10.233/9.894/15.757 respectively. Total ledger:
  427 rows, USD 1.391226; no unknown reservations. Prior 391 rows are identical.
- Coordinator read-only reconciliation exited 0: unique call IDs, exact prior
  prefix, all 24 original file/body hashes, batch JSONL/body equality, product
  settings, historical exclusion/held-out overlap, literal per-row token/rate
  arithmetic, 24 accepted drafts, terminal zero usage and removed scratch copy.
  The command and terminal signal are retained in this execution conversation.

## Source timing and remaining comparison

All 113 post-generation GETs returned HTTP 200. All 24 focus contents matched
generation inputs, and all observed post-read entity update times preceded
provider response creation. Creation is not token completion. One linked note
differs: fresh24 used a snapshot read at `13:20:57Z`, although the note was
updated at `13:32:02Z`, before generation. Post-read evidence at `15:56:55Z`
contains the later content. This establishes an older supplied linked input,
not an observed post-generation edit. `source-timing-qualification.json`
preserves exact hashes, times and interpretation. Current candidate/reviewer
evidence uses post-read content; verifier focus matches that post-read focus.

The prepared fresh24 packet's temporal annotation is incomplete. Slice 8 must
retain the prepared originals, explicitly carry the older-linked-input condition
into both blind packets before launch, and audit actual candidate inputs.
No reviewer or fresh paid check was launched. Independent labels, at most 48
once-per-check calls and product decoding remain with slice 8; report 9 retains
cohort, context, transport and timing qualifications. The frozen candidate hash
remains `3591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5`.

## Attempt limit and safe disposition

The original combined slice exhausted approximately 15 active minutes; exact
active timing was not instrumented. Concurrent authorship counted active and
overlapping command waits were unioned. No claim of a measured sub-limit bound
is made. The sizing assumption failed on cancellation-row settlement and a
premature unpaid proof gate, before reviewers/checks. Compatible completed
generation proof stays; incomplete scratch work is parked privately and its
execution copy removed. Every command ended. No product behavior changed.

The initial manager exited 1 on cancellation records with `response:null`.
`resume_cancelled.py` settled that same job from explicit aggregate-zero usage,
completed the distinct fallback identities and cleaned provider files, exit 0.
A premature unpaid suite failed its immutable-body guard; no paid body changed.
The recovered sequential post-read/product-proof/preparation command exited 0.
Raw failures remain provenance; they are not passing proof. The in-progress
split preserves one generation outcome and gives semantic comparison its own
proof loop; the plan has nine leaves under the in-progress exception.

## Original combined slice 7 proof contract

Proof: 12 on-demand and 12 batch generation lines in the ledger, by route,
for notes outside the historical set; each generated question passes or
fails the product's own generation checks as recorded; each stored
question has a label as in slice 2 and one decision from each paid check
(at most 48 lines). A batch fallback, if used, is recorded with its row
count.

Behavior: 24 notes outside the historical set → the product's current
on-demand and batch requests generate one question each → the questions
are labelled and checked once by the deployed verifier, its choices, and
the frozen prompt. Nothing is written to Production.

## Independent refactor and refinement review

Fresh `/root/fresh_generation_refactor` preserved six completed contracts and
accepted summaries verbatim in `completed-slice-contracts.md`, retaining old
historical anchors. Static validation passed 42 links, six exact comparisons,
file sizes/EOF and whitespace; obligations passed 50 commitments/15 entries.
Its installed consumer trace disposed 32 consumers; all scientific proof is
unchanged and reused. Coordinator reviewed the nine-leaf cumulative design:
same supported-answer rule, explicit generation/semantic proof boundary,
preserved scope and all named remaining obligations. Slice 8 reuses completed
request preparation and product decoder; slice 9 reconciles the report. The
in-progress split is ready for continued authorized execution, without a new
story, claim or scope decision.
