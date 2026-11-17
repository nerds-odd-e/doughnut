# Validate spelling objectives and cues

**Identity:** SEED-073#validate-spelling-objectives
**Source:** [Refined story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-spelling-objectives)
**Source review:** [Independent commitments](source-review.md)

## Goal and scope

Give the product owner a bounded recommendation: proceed to design, narrow and
retest, or stop the objective-and-cue proposal. Compare the five selected case
families using the existing key-hidden review, then evaluate answer variants
against the stated skill. This is evidence work.

Use the private cache at
`/Users/terryyin/git/doughnut/.planning/tmp/recall-history/` directly from this
worktree. Put private working matrices under that cache, not in Git. Publish
only minimal de-identified examples and conclusions in
`.planning/seeds/spelling-objectives-validation-evidence.md`, linked from the
story's permanent completion record at wrap-up. Preserve the provenance and
limitations of the November 14 observations.

No product code, UX/UI design, grader implementation, new objective model,
history edits, scheduling changes, paid calls, or learner trial is included.
The product owner's adoption decision follows the delivered recommendation;
it is not a prerequisite for performing this validation.

## Existing solutions and current decisions

- Reuse `offline-replay/manifest.json`, `packet-a.md`, `packet-b.md`,
  `reviewer-answers.md`, `results.md`, `second-pass.md`, and
  `final-wording-check.md`; do not rebuild a review harness or rerun unchanged
  judgments. Match each reused answer to its exact packet item and cue.
- PFE: manual note edits and title/alias review are the existing smaller
  alternative. `Note.matchAnswer` checks the title and authored aliases;
  `SpellingRecallGrading` uses that result and handles overlaps separately.
  Those owners provide baseline context, not an objective-aware semantic
  grader. This study adds no implementation responsibility or new architecture.
- Compare that baseline, one explicit objective plus an intact cue and answer
  rule, and broader acceptance under the original cue. Judge each against
  clarity, legitimate credit, retained distinctions, changed skill, and author
  effort. Do not assume that semantic broadening repairs an ambiguous task.
- Use calibration's controls as an experimental analogy: valid variants test
  credit; actual form errors and semantic neighbors test discrimination.
  Language admits multiple defensible answers, so agreement is not a unique
  key, causal estimate, or measurement of learning.
- Fix the objective before comparing cue or scoring changes. Record separately
  what the objective supplies, what remains to retrieve, and which changes
  switch the skill. A rule that merely repeats the intended key is insufficient.
  Preserve disputed answers as unresolved rather than treating them as errors.
- Historical correctness describes what was recorded, not ground truth. An
  unavailable original cue can support an answer-history observation only,
  never a wording judgment. The selected ten cues are recoverable at the
  inspected snapshot; do not replace them with current live note content.
- The [shared evidence closeout](../../seeds/SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence)
  governs cleanup. Other queued validations still use the shared cache; this
  story cannot remove their inputs. Record this story's released or approved
  continuing use and the other consumers. Remove only disposable copies made
  for this work once their useful conclusions are retained.
- Existing memory identities, authored content and ownership, and the scheduling
  policy in [ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
  remain context for evaluating a later design. No product operation is invoked
  here, and no North Star change is warranted.

## Decisive premises observed during preparation

Observed on 2026-11-17 in this owned worktree at `af84555ebf`, using the
default checkout's private snapshot read-only. Reuse these observations while
the named inputs and their digests still match; otherwise recheck before use.

| Premise and consuming operation | Observation and result | Unverified remainder |
| --- | --- | --- |
| Slice 1 can pair actual recoverable cues with the reviewed candidates. | Nix/Python observation below: export SHA-256 `3071dac175df47cd172c6bbc54f8fe887f475658f950ca340e9e441dde0ad3df` equals `manifest.cacheSha256`; 11,091 attempts; all ten selected prompts nonempty and no `promptUnavailable`; each of the five primary prompts equals its manifest original exactly. Related attempts share their family's cue. | None for pairing this snapshot. No claim about cues outside the selection or subsequent note edits. |
| Slice 1 can reuse key-hidden judgments without rerunning a paid or live service. | Read the November 14 `results.md` §2 and the selected packet items, manifest mappings, and `reviewer-answers.md`. Both reviewers answered the revised 伝手 reading, honorific, solution noun, and inflection tasks; the meadow review retained a broader alternative. The packets omit keys, history, and note titles. | These are retained AI judgments, not new learner observations or independently established linguistic truth. Adjudicate disputed variants with sources or retain the uncertainty. |
| A successful rewrite can change the measured skill. | Compare the exact `question-pairs.md` Connections reading and Single inflected blank pairs with `results.md` §2: the former supplies 伝手 and requests つて; the latter names ～かける. This reaches the visible tasks being compared, not merely their labels. | Whether learners benefit remains a validation hypothesis outside this offline study's claim. |

Literal local snapshot observation:

```sh
CURSOR_DEV=true nix develop -c python3 - <<'PY'
import hashlib, json, pathlib
p = pathlib.Path('/Users/terryyin/git/doughnut/.planning/tmp/recall-history')
raw = (p / 'recall-answers-100d.json').read_bytes()
a = json.loads(raw)['attempts']
m = json.loads((p / 'offline-replay/manifest.json').read_text())
assert hashlib.sha256(raw).hexdigest() == m['cacheSha256']
for row in [3077, 3685, 3304, 3046, 737, 3763, 4141, 7970, 5947, 5764]:
    s = a[row - 1]['spelling']
    assert s.get('prompt') and not s.get('promptUnavailable'), row
for c in [c for c in m['cases'] if c['proposal'] == 2]:
    assert c['original']['prompt'] == a[c['row'] - 1]['spelling']['prompt']
print(len(a), 'attempts; ten recoverable cues; five exact primary pairs')
PY
```

The export service withholds spelling cues when notes changed after an answer
(`RecallAnswerHistoryService.spelling`); this explains what recoverability
means. The local snapshot and prompt equality, rather than the presence of
current note text, establish the comparison's input.

## Outside-in proof ownership

The stable boundary is the product owner's evidence report, backed by the
private case matrix and exact retained observations. Product tests would not
prove an offline judgment or learner benefit. No automated absence test or
new application test is needed for this evidence-only outcome.

| Source promise / key example | Owner | Observable proof |
| --- | --- | --- |
| Compare recoverable cues with keys hidden and distinguish cue, rule, and objective changes. | Slice 1 | Every family has a manifest/packet/answer mapping, declared objective, original and revised interpretation, and same-skill/changed-skill verdict. Reuse only matching key-hidden observations. |
| Credit legitimate variants while preserving intended spelling and usage distinctions. | Slice 1 | Response matrix records accepted, rejected, or unresolved with reasons under each declared objective; includes valid variants, real form errors, and close semantic neighbors, not only the intended keys. |
| Brine separates solution from process; 伝手 separates reading, script, and phrase/particle use. | Slice 1 | Case verdicts show why *curing* does or does not fit each actual cue and separately classify つてをたよる, 伝手に頼る, and the reading-only task. |
| Small っ and complete inflection retain genuine form distinctions. | Slice 1 | Matrix compares おっしゃる/おしゃる and the ～かける complete-blank answers, documenting permitted script variants and supplied construction. |
| Meadow exposes residual ambiguity instead of assuming exact-name grading. | Slice 1 | Review includes meadow/grassland/pasture under the actual cues; any guided comparison is labeled as such, with remaining overlap or disputed rule visible. |
| Recommend proceed to design, narrow/retest, or stop, without claiming measured learner benefit. | Slice 2 | Public report compares the three approaches on the same criteria, cites observations and uncertainty, and names a bounded next question. A follow-up is a proposal for the owner, not silently queued or implemented. |
| Retain non-sensitive evidence and settle this story's temporary-evidence use. | Slice 2 | Report records provenance, limits, other cache consumers, and this story's retention disposition; acceptance reading checks this work's disposable copies and publication contents. The shared cache remains while other consumers need it. |

## Execution context

Established 2026-11-17: Story Branch Mode; owned execution checkout
`/Users/terryyin/git/doughnut/.worktrees/validate-spelling-objectives-and-cues`,
branch `codex/validate-spelling-objectives-and-cues`, agent DavidKo-chan,
publisher `dashboard-territory.local-doughnut`; origin/main is the authorized
trunk. Starting revision `c115ab7b9862af6986f8d69d10f67a0bcf1ef84b`;
published claim/base `0f7994a0d6b9f3fbc6b00c0cb2bda014a4f22200`.
Default checkout `/Users/terryyin/git/doughnut` is separate; shared private
evidence is read from it without mutating its Git state. Checkout setup and
`./scripts/run.sh bash scripts/check_diff_whitespace.sh` passed. Existing
planning authority permits in-scope refinement; no new scope is authorized.
CI: GitHub `nerds-odd-e/doughnut`, verified `ci.yml` (`donut CI`), branch target
`codex/validate-spelling-objectives-and-cues`. Codex stream coordinator
`spelling-validation-260ab727`, directory `/tmp/dough-ci-501/watch-3tFqTn`.
Claim CI on trunk is unobserved;
this stream covers branch increments. No product generation is triggered.

## Ordered slices

### 1. Resolve the selected cue and scoring interpretations

Type: Behavior
Status: done
Proof: The five-family matrix and report's case verdicts satisfy the Slice 1
rows above; inspect the cited packet answers and historical cues directly.

Behavior: Given the matched export and existing key-hidden reviews → compare
the original cue, candidate, and objective-specific response rules → the
product owner can see where clarity or scoring improves, where real errors
remain distinguishable, and where the skill changed or ambiguity remains.

Keep preparation, adjudication and case conclusions together. Reuse the
existing observations for unchanged prompts; this slice does not require a
new cue or review service. Do not relabel informed new judgments as blind.
Source-check only disputed language distinctions; if unavailable, record the
specific uncertainty and its effect on the recommendation. Keep private
payloads local and publish only the minimal de-identified table. Expected to
fit 15 active minutes through reuse of the five existing pairs; apply the
execution workflow's sizing rule if missing adjudication overruns that bound.

### 2. Deliver the bounded validation recommendation

Type: Behavior
Status: done
Proof: The report satisfies the Slice 2 rows above and remains traceable to
Slice 1's case verdicts; inspect conclusions, references and cleanup together.

Behavior: Given the case verdicts and residual uncertainties → compare the
three approaches and close this study's evidence use → the product owner has
one reasoned proceed-to-design, narrow-and-retest, or stop recommendation,
with its limits and any proposed follow-up question.

Preserve the dated review's provenance rather than rewriting its conclusions.
If recommending design, identify what learners need to see and what authors
must decide as questions for that later story, without designing or building
it here. Carry the evidence link into the story's permanent completion record
during wrap-up. Settle the cache
disposition under the shared contract and remove this study's spent copies;
do not erase sibling inputs. Expected to fit 15 active minutes with one
report and no new trial. Standard execution refactoring, formatting, commit,
publication and wrap-up gates remain with their owning workflow.

## Plan review

Execution placement decision: the 307-line source seed will fall below 250
lines when wrap-up removes this completed story. Avoid a transient edit merely
to add a link that closure would delete; retain the report link in the permanent
done record instead. This changes no source promise or report content.

Slice-plan refinement was not needed: two bounded Behavior slices own the
case judgments and resulting decision, with one common comparison rubric.
No remaining slice-specific concern was identified. Uncertain experimental
results and linguistic adjudications are reportable outcomes of the study,
not assumed product facts or claims of learning improvement.

## Accepted observations and delivery

Slice 1: accepted report case sections and private matrix mapping against the
actual archived cues and 20 retained packet answers. Digest/mapping proof:
`node .agents/skills/dough-execute-plan/scripts/slice-clock.mjs run --log /tmp/spelling-slice1-proof.log --exit-file /tmp/spelling-slice1-proof.exit -- ./scripts/run.sh python3 /Users/terryyin/git/doughnut/.planning/tmp/recall-history/spelling-objectives-matrix.py`
passed (exit 0): ten recoverable cues, five primary/related families, twenty
exact packet/answer/report mappings. Whitespace proof via the clock and
`./scripts/run.sh bash scripts/check_diff_whitespace.sh` passed. Clock paused
at 6.5 active minutes. Source freshness: unchanged on pinned trunk
`0f7994a0d6`; no incoming sibling landing affects these evidence judgments.
Consumer trace inspected report readers and excluded generic runtime filename
matches: only human evidence/closure consumers change. Source-review IDs 1–16
record the inspected expected/actual observations. No application tests or
expensive checks apply to this report boundary.
Independent refactor `/root/refactor_cases` completed: plan-only consistency
edits, unchanged case proof reused, whitespace passed; no follow-ups.
Published slice 1: `b1fad80d32e06e5b113cc117ceea6e4b1c23237d`, branch target;
suffix base was the published claim. Same observer attached.

Slice 2: accepted report's common five-criterion comparison, narrow-and-retest
recommendation, bounded proposed next question and conditional cache closeout.
All Slice 1 case judgments remain unchanged. Source-review IDs 17–26 record
expected/actual observations. One-time acceptance reading: three owned scratch
files gone; all eight original export/replay inputs retained for four queued
consumers. No approved follow-up needs this story's use; it is released.
Whitespace and consumer-trace clock runs passed (exit 0); direct report reading
and trace found only human consumers. Clock paused at 3.8 active minutes.
Freshness unchanged at `0f7994a0d6`; no incoming sibling change affects proof.
Independent refactor `/root/refactor_recommendation` completed: already clean;
no edits or tests, accepted proof reused, no follow-ups.

## Execution complete

Product advice: keep the report's narrow-and-retest recommendation as an owner
proposal; no product, queue, scope or priority change is authorized or needed.
Retrospective reviewed published `b1fad80d32` and `ee409e7bbc` against the original
story; the Take and adjacent sibling preparation are provenance, not delivery.
Evidence-only proof is appropriate; no product/E2E tests changed, no residue or
ADR 0003/scheduling conflict. Intent Coverage is enabled but no protection risk
applies; process review ran without a new supported finding or record change.
Slice 2 published `ee409e7bbcef8c6b7b4ab40d03e0be6c3feec95b` from `b1fad80d32`.
CI stream ended after GitHub HTTP 502; completion recovery remains required.

## Story obligations

### G1. Exact particle collocation remains unresolved
Reported: slice 1 — "The particle paper’s full PDF returned HTTP 429; 伝手に頼る remains unresolved, not rejected."
Story clause: "Unsettled linguistic alternatives are findings for review, not automatic rejections."
Disposition: proved by slice 1: report Connections table separates passing script from unresolved particle; its source-limit paragraph and U definition direct review without automatic rejection.
