# Reliable recall question checks: comparison evidence

<a id="comparison-2026-11-17"></a>

## 2026-11-17 SGT: proceed to bounded design

**Recommendation: proceed to design** a note-supported check for wrong keys and
multiple defensible answers. The fixed held-out rule passes: the candidate
caught 6/6 defects, flagged 0/18 sound questions and gave the same binary verdict
in all three runs for 24/24 questions. Choices-only caught 5/6 with 0/18 false
flags. This supports a bounded design decision for stored-question checking;
it authorizes no grading, discard rule or product change.

Data were captured on **2026-11-16 UTC** on the
[exploration branch](https://github.com/nerds-odd-e/doughnut/tree/5bed0c952cc3691e01b2f0578dce05f47af18ef2),
through revision `5bed0c952cc3691e01b2f0578dce05f47af18ef2`.
Fresh checks at 16:21–16:24 UTC occurred on 2026-11-17 SGT; this report was
assembled that day. Earlier [dated observations](learning-refinement-and-retrieval-evidence.md)
retain their original labels, inputs and limits, including the older row 5347
claim. The fresh note-backed label governs this comparison.

<a id="method-and-reproduction"></a>

## Inputs, actions and reproducibility

The private 100-day export, retained question census, requests and responses
supplied historical and held-out questions. Read-only note retrieval supplied
current focus and required direct-linked evidence. Two independent fresh-session
AI reviewers per cohort judged key-hidden packets; stored keys were joined only
after both returns. Labelling added no paid calls. A note-at-fault case is
unscored; any reviewer disagreement remains unresolved and unscored. Changed
content is scored only when both reviewers can judge the available evidence.
These reviewers provide a reference label, not independent human ground truth.

| Check | Input and decision |
| --- | --- |
| Deployed | Product-built verifier request with focus-only fixtures; real product decoder; flags a choice/key mismatch or infeasibility |
| Choices-only | Same verifier response; flags selected choices differing from the singleton stored key; zero extra calls |
| Frozen candidate | Full current focus plus available direct links; asks for every defensible, note-supported choice index; local rule flags a supported set differing from the singleton stored key |

All calls used `gpt-6.1-sol`. Verifier/candidate settings matched: low reasoning,
500 maximum output tokens and low verbosity. Generation used the current
product builders, maximum 1,000 output tokens and omitted reasoning setting.
Candidate inputs contained no stored key, rationale, labels, earlier verdicts
or row/note IDs. Baseline fixtures supplied only the focus although Production's
shared retriever may include links. Historical row 3748 had the product's marked
truncated focus prefix. Candidate inputs supplied full focus and direct links:
this comparison changes **prompt and context together**, and establishes neither
an instruction-only causal effect nor exact Production context parity.

One candidate was developed against historical evidence only, with zero paid
wording calls. Its complete JSON instructions/schema/settings template froze at
`2026-11-16T12:53:40.523238Z`, SHA-256
`3591bc73411b7cf0eb14ae8f0e48b959d9c539f93bbbca8255913e9dc42944a5`.
Held-out labels preceded its first held-out call at `13:54:38.721112Z`.
All 41 historical, 24 held-out and 24 fresh candidate request templates match
that freeze; later outcomes caused no prompt or pass-mark retuning.

Reproduce the figures locally from the default checkout's private
`.planning/tmp/recall-history/integrity/pilot/` cache; do not publish its contents:

| Figure | Governing private artifacts |
| --- | --- |
| Historical labels/context | `historical-labels.json`, `historical-label-summary.json`, `historical-question-records.json`, `historical-review-provenance.json` |
| Historical decisions | `historical-verifier-results.json`, `historical-verifier-summary.json`, `historical-verifier-decisions.json`, `historical-candidate-results.json`, `historical-candidate-summary.json` |
| Held-out labels/decisions | `heldout-labels.json`, `heldout-label-summary.json`, `heldout-question-records.json`, `heldout-review-provenance.json`, `heldout-evaluation-results.json`, `heldout-evaluation-summary.json`, `heldout-evaluation-acceptance-proof.json` |
| Freeze and accounting | `candidate-prompt.txt`, `candidate-freeze-manifest.json`, `ledger.jsonl`, response files referenced per row |
| Fresh generation | `fresh-20261116/generation-summary.json`, `source-timing-qualification.json`, `post-generation-evidence.json` in that directory |
| Fresh labels/decisions | `fresh-20261116/qualified-review-provenance.json`, `qualified-labels.json`, `fresh-evaluation-results.json`, `fresh-evaluation-summary.json`, `fresh-evaluation-proof.json`, `fresh-evaluation-independent-audit.json` in that directory |
| Report reconciliation | `report9-audit-v1.py`, `report9-figure-source-audit.json`, `report9-before-snapshot.json` |

Use `generation-summary.json` for the generation outcome; its earlier
`summary.json` is preserved pre-terminal partial evidence. Fresh semantic
results supersede only the earlier observation that checking was unmeasured.
No original scientific artifact was rewritten and no model was rerun for this
report. The private report audit reconciled every response's usage, all per-row
costs, result votes, label joins, source dates and frozen request templates.

<a id="cohort-results"></a>

## Separate cohort results

Historical: all 47 reviewed; 41 scored (33 sound, six multiple-answer, two
wrong-key); three note-at-fault and three unresolved excluded, including two
reviewer disagreements. Held-out: 32 reviewed from a fixed 16 keyless-signal /
16 seeded-control selection; all six labelled defects retained with 18 sound controls; two note-at-fault excluded and six sound
reserves; zero unresolved or reviewer disagreements. Selection was fixed before
outcomes. Fresh: 24 product-accepted drafts, zero rejected; 24 sound, zero
defects, note-at-fault, unresolved or disagreements after two new qualified
blind reviews. Every accepted fresh draft received both checks.

Historical and held-out flags require at least two of three runs. Unanimity
means the same **binary flag** across all three, rather than identical wording.
Fresh checks ran once each; fresh detection and repeatability are unmeasured.

| Cohort | Check | Wrong keys caught | Multiple answers caught | Sound false flags | Unanimous verdicts |
| --- | --- | ---: | ---: | ---: | ---: |
| Historical | Deployed | 2/2 | 6/6 | 13/33 | 38/41 |
| Historical | Choices-only | 2/2 | 5/6 | 0/33 | 40/41 |
| Historical | Candidate | 2/2 | 6/6 | 0/33 | 41/41 |
| Held-out | Deployed | 1/1 | 5/5 | 10/18 | 22/24 |
| Held-out | Choices-only | 1/1 | 4/5 | 0/18 | 24/24 |
| Held-out | Candidate | 1/1 | 5/5 | 0/18 | 24/24 |
| Fresh | Deployed | unmeasured | unmeasured | 7/24 | unmeasured |
| Fresh | Choices-only | unmeasured | unmeasured | 0/24 | unmeasured |
| Fresh | Candidate | unmeasured | unmeasured | 0/24 | unmeasured |

Minimal example: sound historical row 8739 was flagged by deployed feasibility
in all three runs although its selected choice matched the key. Choices-only
and candidate upheld it in all three. Row 5347 is note-at-fault and unscored;
its original paid diagnostic is retained separately, with its original ledger
purpose, and charged below rather than folded into the 123 scored baseline calls.

<a id="route-age-and-context"></a>

## Route, age and available note context

| Cohort | Generation route | Question age / generation dates | Context condition |
| --- | --- | --- | --- |
| Historical, 41 scored | Inferred 33 batch / 8 on-demand | 4.792–647.605 days; 2025-02-07–2026-11-12 SGT | Of 47 reviewed, 18 focus and 17 linked contexts changed after generation; scored: 13/41 focus, 15/41 linked |
| Held-out, 24 scored | Inferred 9 batch / 15 on-demand | 14.833–1,154.044 days; 2023-09-19–2026-11-02 SGT | 22/24 focus and 20/24 linked contexts changed; historical versions unavailable |
| Fresh, 24 accepted | Actual 12 on-demand sync / 12 batch-rendered sync fallback | Generated and checked in this execution; 26.907–102.834 minutes from provider request creation to check send | All 24 focus contents match generation snapshots; one supplied linked snapshot differs from post-read evidence |

Historical/held-out ages use fixed cutoff `2026-11-16T14:00:00Z`; changed counts
are question instances, not unique-note counts. Their route labels use a
minute-zero heuristic, especially uncertain before batch code existed; these
stored questions were checked offline, not through live serving routes.
Historical content at generation was not reconstructed. Held-out focuses are
outside all 155 historical focus/linked IDs, but three linked IDs overlap.
Fresh drafts reuse **all 24 held-out focuses** outside those 155 IDs; they are
new questions, not a third independent note cohort.

Current generation's product on-demand builder and batch renderer/reference
index used 113 captured note snapshots plus notebook metadata. Original folders
and other Production retrieval candidates were not reconstructed. All 113
post-generation GETs returned 200. The observed updated timestamps precede
provider response `created_at`; that field denotes request creation, not token
completion. No post-generation edit was observed through the post-read, but this
does not reconstruct complete at-generation evidence.

Minimal source-timing example: one fresh draft used a linked snapshot read at
13:20:57 UTC; that source was updated at 13:32:02 UTC **before generation**.
The 15:56:55 UTC post-read differed. Its qualified reviewers and candidate saw
the current post-read link, while generation had used the older captured link.
Both qualified review packets carried this condition; no post-generation edit
or fully historical replay is inferred from it.

The real 12-row Batch job was submitted at `14:40:20.698Z`, with the one-hour
deadline `15:40:20.698Z`; terminal cancellation was observed at `15:50:55.945Z`.
It completed zero rows with explicit aggregate usage zero. All 12 attempts count;
12 distinct same-body synchronous fallbacks produced the drafts. Turnaround
4,235.247 seconds includes cancellation settlement. Asynchronous Batch output
quality and per-row latency are unmeasured. Provider input/error-file deletion
receipts remain private. All 24 generation bodies remain unchanged.

Of the fresh sound drafts, deployed false flags were 2/12 on actual on-demand
and 5/12 on batch-rendered fallback; choices-only and candidate were 0/12 on
each. All fresh **checks** themselves used synchronous transport.

<a id="usage-and-latency"></a>

## Usage, latency and owner-rate accounting

| Cohort / paid operation | Attempt rows | Input tokens | Output tokens | USD | API seconds mean / median / max |
| --- | ---: | ---: | ---: | ---: | --- |
| Historical verifier, scored | 123 | 109,308 | 13,969 | 0.358306 | 5.053 / 4.402 / 9.483 |
| Historical candidate | 123 | 176,880 | 2,322 | 0.376980 | 2.870 / 2.651 / 9.092 |
| Held-out verifier | 72 | 64,632 | 9,884 | 0.228104 | 6.326 / 6.427 / 10.885 |
| Held-out candidate | 72 | 109,527 | 2,195 | 0.241004 | 4.602 / 3.685 / 12.588 |
| Fresh verifier | 24 | 21,732 | 2,441 | 0.067874 | 5.598 / 5.183 / 9.734 |
| Fresh candidate | 24 | 36,616 | 384 | 0.077072 | 3.204 / 2.780 / 6.103 |
| Generation on-demand | 12 | 24,880 | 4,188 | 0.091640 | 9.557 / 8.862 / 14.344 |
| Generation sync fallback | 12 | 24,501 | 4,337 | 0.092372 | 10.233 / 9.894 / 15.757 |
| Generation Batch cancelled | 12 | 0 | 0 | 0.000000 | unmeasured per row |
| Separate row 5347 diagnostic | 1 | 940 | 94 | 0.002820 | 8.127 / 8.127 / 8.127 |
| Total | 475 | 569,016 | 39,814 | 1.536172 | no pooled latency claim |

Latency is the observed API round trip; it excludes note retrieval, labelling,
backend work and the full learner journey. Cost uses each response's actual
usage at the **owner-fixed** USD 2/M input and USD 10/M output, Batch half-price.
These rates were found from aggregators on 2026-11-16; this is reconciled
experimental accounting, not a verified provider invoice.

The ledger has 463 complete responses plus 12 cancelled zero-usage attempts,
with no unknown reservation. Allocation: 246 historical, 144 held-out, 24
original generation attempts, 48 fresh checks, 12 fallback attempts, one
diagnostic, zero wording. Retry/probe/fallback uses 13/20; seven plus 18 unused
wording calls leave 25/500. Both hard caps remained unreached; no fresh checks
were left unsent. Earlier 60 prior-story calls are outside this 500-call/USD 5
budget. The sender's cap refusal is retained in the earlier proof.

<a id="design-interpretation"></a>

## What the recommendation supports

The fixed decision tests stop first if candidate catches fewer defects or
flags more sound cases than choices-only. Neither occurred (6 versus 5 catches;
0 versus 0 false flags). Then all conditions pass: at least six defects and
15 sound controls (6/18), false flags at most one in ten (0/18), defects caught
at least two-thirds (6/6), and unanimous verdicts at least nine in ten (24/24).

The supported failure classes are **note-supported wrong keys and multiple
defensible answers in stored MCQs checked offline**. The single held-out wrong
key gives thin class-specific evidence. Choices-only already removed every
observed sound false flag, while candidate caught one additional held-out
multiple-answer defect. Extra linked evidence prevents attributing that gain
only to prompt wording. Small signal-enriched samples, AI reference labels,
current content, heuristic historical routes and one model limit transfer.
Fresh current on-demand and batch-rendered fallback questions support the
sound-case observation only; fresh defect reliability, repeated fresh verdicts
and actual asynchronous Batch quality remain unmeasured. No population rates
or age-based discard conclusion follow.

Checking **before display** adds a check call and its latency but could protect
the initial mark. Checking **after a miss** spends selectively, but the initial
wrong mark has already been shown. Either design must address legacy waiting
stored questions: generation-only checking cannot cover them. Copying this
experiment's three-run majority would triple check calls and cost; end-to-end
latency, parallel execution and a production policy were not measured. The
fresh sound-only one-pass result cannot establish one-pass defect reliability.

<a id="temporary-evidence-disposition"></a>

## Temporary evidence disposition

This exploration releases its own use of the raw evidence at closure; no new
approved raw-evidence follow-up is created by this recommendation. Its lasting
handoff is this nonsensitive report and minimal examples. On 2026-11-17 SGT,
[the backlog](../PRODUCT-BACKLOG.md) still queues five other SEED-073 validations:
spelling objectives, short recall lessons, source-backed note edits, learning
objectives and generation abstention. Their evidence dispositions are unsettled,
so this closure is **not the last story owner** and the shared cache remains.

The [shared closeout contract](SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence)
continues: the future last owner checks every other validation disposition and
approved follow-up use. Once all are settled and uses finished or covered by a
minimal deidentified handoff, that owner removes the default-checkout
`.planning/tmp/recall-history/` cache and investigation-created copies, including
raw exports, derived datasets, private reports, packets, requests/responses and
scratch scripts; verifies the files are gone; and records shared closeout.
Only necessary nonsensitive conclusions and minimal examples remain. Physical
shared-cache cleanup has not occurred here. Product notes and recall history
remain outside this cleanup.
