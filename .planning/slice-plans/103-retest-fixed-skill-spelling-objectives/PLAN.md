# Retest spelling objectives with one skill held fixed

**Identity:** SEED-073#retest-fixed-skill-spelling-objectives
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#retest-fixed-skill-spelling-objectives)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

Give the product owner a five-family comparison supporting a recommendation
to proceed to design, narrow and retest, or stop objective-and-cue review.
Preserve one declared retrieval skill per item across candidate conditions;
report failed preservation rather than substitute an easier skill. Agreement
by AI readers is evidence about these tasks, not learner benefit.

Use the archived brine, 伝手, おっしゃる, ～かける and meadow families and
the retained response matrices. Compare manual note/alias review, paired
objective/cue/rule review, and broader acceptance under the archived cue.
Resolve exact 伝手に頼る usage with attributable sources or keep it unresolved.
Meadow remains an ambiguity control. Report preparation effort, disagreements,
supplied-answer shortcuts and limitations, not just answer counts.

Deferred: live notes, aliases, grading, scheduling, UI, product design, new
learner trials, production accuracy estimates and paid API experiments.
No new export or current note fetch is needed. A later product decision belongs
to the owner; this story delivers the recommendation, not adoption.

## Existing solution and architectural fit

PFE: reuse the local November 14 replay's manifest, packet organization and
independent-reader method, plus the November 17
[case matrices and comparison](../../seeds/spelling-objectives-validation-evidence.md).
Its prior reviewers saw both versions in one packet; use separate fresh
contexts per condition here so a supplied form in one cannot answer another.
The old judgments remain dated evidence, not a fresh blind result or an oracle.

The smaller baseline is manual note and alias review. Existing
[spelling verification](../../../docs/note-assimilation.md) verifies a title or
alias from memory; that capability does not evaluate whether an experimental
cue preserves a chosen retrieval skill. Reuse its manual workflow as a paper
comparison without changing the product or building a reusable grader.
The [North Star](../../NORTH-STAR.md) governs no new architectural choice here.
No new ADR, shared responsibility or product structure is needed.

## Inputs and decisive premises

Read the private cache at
`/Users/terryyin/git/doughnut/.planning/tmp/recall-history/` from this linked
worktree. Read the shared cache without modifying it. Put any new private
packets, mappings, reviewer returns and scripts in this execution worktree's
Git-ignored `.planning/tmp/retest-fixed-skill/`, never in committed files.
Keep the final minimal report at
`.planning/seeds/fixed-skill-spelling-retest-evidence.md`.

| Premise / consuming operation | Observation and result | Remainder |
| --- | --- | --- |
| Recover the five archived tasks and original comparison inputs | On 2026-11-17 in the preparation worktree at `bea1604a1d`, the read-only command below parsed the cache and manifest, verified the export digest, recovered all ten selected attempt cues byte-identically and found each paired item in both packet mappings. | Recheck the digest before use; a changed/missing snapshot stops dependent review. |
| Reuse earlier response judgments as proposals, not linguistic truth | Read `spelling-objectives-validation-evidence.md`, dated 2026-11-17 at `0f7994a0d6` with recommendation review at `b1fad80d32`: tables explicitly distinguish accepted, rejected and unresolved forms and identify changed skills. | Exact 伝手に頼る usage remains an experiment question; failure to settle it is an allowed unresolved result. |

Literal input observation (no files written):

```sh
CURSOR_DEV=true nix develop -c python3 - <<'PY'
import hashlib, json
from pathlib import Path
cache = Path('/Users/terryyin/git/doughnut/.planning/tmp/recall-history')
raw = (cache / 'recall-answers-100d.json').read_bytes()
manifest = json.loads((cache / 'offline-replay/manifest.json').read_text())
attempts = json.loads(raw)['attempts']
assert hashlib.sha256(raw).hexdigest() == manifest['cacheSha256'] == '3071dac175df47cd172c6bbc54f8fe887f475658f950ca340e9e441dde0ad3df'
cases = [c for c in manifest['cases'] if c['proposal'] == 2]
assert len(cases) == 5
for case in cases:
    for row in [case['row'], *case['additionalRows']]:
        assert attempts[row - 1]['spelling']['prompt'] == case['original']['prompt']
    for mapping in manifest['packetMap'].values():
        assert len([v for v in mapping.values() if v['row'] == case['row']]) == 2
assert len(manifest['packetMap']) == 2
count = sum(1 + len(c['additionalRows']) for c in cases)
print(f'{len(cases)} families, {count} attempts: digest and archived cues match; paired items recover in both packets.')
PY
```

Observed: `5 families, 10 attempts: digest and archived cues match; paired items
recover in both packets.` The old report, `offline-replay/reviewer-answers.md`
and packet mappings were inspected together. No fresh reviewer outcome is
assumed. The new comparison itself is the missing observation this story owns.

## Outside-in proof ownership

| Promise / example | Owner | Observable proof |
| --- | --- | --- |
| Hold one skill, supplied information and answer boundary fixed; expose changed skills and answer leakage | Both slices | Frozen per-item specification plus independent reader accounts of what they had to retrieve; explicit preserved/changed/unresolved result. |
| Brine noun-only simplification cannot stand in for its archived word-family task | Both slices | Slot-to-form mapping and a new cue that retains the declared retrieval demand, or a reported failure to preserve it; material/process and spelling controls remain distinguishable. |
| 伝手 expression production stays distinct from reading; script and collocation are separate questions | Both slices | Blind phrase responses and judgments of kana/kanji variants, wrong reading/meaning controls, and sourced exact-usage adjudication or unresolved status. |
| Honorific form requirements survive legitimate script variation | Both slices | Declared script rule and judgments distinguishing おっしゃる / 仰る from おしゃる, with any change from the archived demand stated. |
| ～かける retrieval and formation are not replaced by a supplied suffix or altered completion boundary | Both slices | Cue-information and blank-ending comparison, with judgments of stem/full completion and connecting-form errors under the same declared task. |
| Meadow overlap remains visible | Both slices | Independent answers and credit/review decisions for meadow, grassland and pasture alongside spelling and sense-error controls; disagreement is reported. |
| Compare three approaches and recommend one bounded next step | 2 | Five-family result table, comparative judgments, effort observations and proceed/narrow/stop rationale that the product owner can assess. |
| Retain provenance, minimize published evidence and settle temporary use | 2 | Minimal dated report with revisions/digests, procedure, results and limits; explicit cache disposition and separate one-time cleanup reading. |

These are research observations at the owner-facing report boundary. No product
tests or broad backend/frontend runs are warranted: product behavior and test
support are unchanged. Use link/anchor checks, preparation-state validation and
the repository whitespace check for the documentation changes. The ordinary
commit hook remains required. During execution apply the repository's required
Jidoka, fresh post-change refactor, single coordinator `./scripts/run.sh pnpm
format:changed`, agent commit and push sequence from `AGENTS.md` and
`dough-execute-plan`; no API generation is implicated.

## Ordered slices

### 1. Freeze comparable tasks for the five spelling families

Type: Structure
Status: done
Proof: Inspect one private specification per family connecting archived slots,
the held skill, supplied information, remaining retrieval, permitted scripts,
blank boundaries and proposed accepted/rejected/unresolved controls. Check all
reviewer packets against that specification before dispatch. Existing dated
observations retain their wording and provenance.

Prepare only the comparison immediately consumed by slice 2. Start from the
archived retrieval demand: brine word-family production, whole-expression
production for connections, honorific lexical written forms, near-death
construction retrieval/formation, and vocabulary retrieval for meadow.
If a mixed archived task cannot be clarified under one skill without dropping
a demand, label that loss; do not hide it by selecting reading or guided
formation and claiming preservation. Inability to produce a preserving cue is
an evaluable failure of the proposal, not missing permission to finish.

Freeze the original, a manual note/alias repair, and a paired objective/cue/rule
candidate per family. Broader acceptance uses the original cue. Keep scoring
rules and the response pool separate from production packets; an answer-neutral
script/format instruction can be shown, an accepted-answer list cannot.
Each reader sees only one version of a family in a fresh context. Hide keys,
titles, learner answers, historical marks, other conditions and reader outputs.
Reuse the prior matrix controls, preserving their provisional status; add only
variants needed to resolve these declared demands. Do not build a harness or
expand to more families. This enables slice 2's independent comparison while
all live product behavior remains unchanged. Target: 15 active minutes.

### 2. Deliver the fixed-skill comparison and recommendation

Type: Behavior
Status: done
Proof: Two independent key-hidden reader returns per condition, frozen before
adjudication, support all five rows of the minimal final report. Trace each
reported judgment to its packet and declared rule; distinguish fresh reader
agreement, source-supported adjudication and unresolved questions.

Behavior: Given the frozen comparable tasks, run the bounded offline review.
Use two fresh reader contexts for each of the three conditions (six short
five-family passes); no context sees another version or prior returns.
First freeze answers, inferred task and remaining retrieval. Only afterwards
show the shuffled response pool and the applicable scoring rule without a key,
asking each reader to accept, reject or leave unresolved with reasons.
Broader acceptance is assessed against the original cue; the manual baseline
uses its reviewed aliases. Preserve disagreements rather than forcing a key.

Check sources for the exact 伝手に頼る collocation; general 頼る particle
alternation is insufficient. An unavailable or inconclusive source yields an
unresolved entry. No repeated rewriting until agreement: a later revision is
labelled separately and does not replace the frozen trial's result.

Publish the minimal dated evidence report: per-item skill survival and leakage,
answerability, legitimate variants, genuine form errors, unresolved overlap,
comparison with both alternatives, measured preparation/review effort when
available (otherwise explicitly unavailable), and the bounded recommendation.
Separate AI task judgments from unmeasured deployed grading, human usability
and learning gains. Keep the earlier report intact and link it as the baseline.

Record this story's continuing evidence need or release of use under the
[shared closeout contract](../../seeds/SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence).
Remove owned temporary files once their useful conclusions are minimally
handed off. Before any shared-cache removal, check every other consumer and
approved follow-up; remaining consumers retain it. Record actual cleanup in a
separate one-time reading, not a maintained absence test. Target: 15 active
minutes excluding reviewer/tool wait; if the bounded study overruns, follow
the execution skill's sizing rule without dropping families or proof.

## Execution evidence

- Slice 1 (2026-11-17, 3.9 active minutes): private frozen specification,
  six reader packets, mapping and pre-dispatch check under the ignored
  `.planning/tmp/retest-fixed-skill/` (digest rechecked; shared cache only
  read). C2 cues hold the archived skill in all five families: brine keeps
  six numbered word-family slots (a multi-answer format the product cannot
  take today); 伝手 keeps whole-expression production but cannot require
  伝手 without supplying it, so other natural expressions may qualify;
  おっしゃる drops the archived inflected example slots; ～かける keeps both
  blanks without naming the suffix but describes the construction category;
  meadow keeps the original description. C1 repairs aliases only, except
  meadow's note text; alias lists are withheld from C1 judges because they
  are the key. Archived shortcuts are retained, not introduced: visible
  `[[curing]]`, コネ, partial honorific masks and the Chinese gloss.

- Slice 2 (2026-11-17, 8.2 active minutes excluding reader wait): six fresh
  reader contexts (two per condition) answered, then judged a shuffled pool
  under each rule without a key; a separate web source check left
  伝手に頼る unresolved. Delivered
  [fixed-skill retest evidence](../../seeds/fixed-skill-spelling-retest-evidence.md):
  recommendation to proceed to design the paired objective/cue/rule, with
  open points (several-answer brine format, unforced 伝手 noun, ～かかる,
  meadow overlap). Learnings: the C1/C3 response pool revealed the 伝手
  answer to readers who could not produce it; one C3 reader misread brine
  slot 6; readers were often stricter than provisional marks.
- Cleanup reading (2026-11-17, one-time): after the report retained the
  conclusions, `rm -rf .planning/tmp/retest-fixed-skill` and the job's reader
  copies were run; `ls .planning/tmp` then reported no such directory in the
  execution worktree. The shared cache `~/git/doughnut/.planning/tmp/recall-history/`
  was only read and remains for the four still-queued SEED-073 validations.

## Current decisions and concerns

- One per-item specification owns all three comparisons; no separate grading
  machinery or semantic-grader implementation. Valid ambiguity and skill loss
  are useful negative results, not reasons to relax the declared task.
- Exact language usage is a result to investigate, not a readiness premise.
  No recommendation implies authorization to implement, run paid calls or
  recruit learners.
- Slice-plan refinement was not needed: one preparatory Structure slice feeds
  one bounded Behavior comparison, with shared proof ownership and no product
  integration. Two slices are below the eight-slice limit.
- No remaining slice-specific concern identified. All required local inputs
  were consumed by the read-only recovery observation; new reader judgments
  remain the planned work. No execution or new study result is recorded here.

## Execution complete

Product advice: no change to queued priorities. The report recommends
proceeding to design the paired objective/cue/rule review; adopting it, and
queueing a design story with its open points (several-answer brine format,
unforced 伝手 noun, ～かかる, meadow overlap), is the product owner's decision.
A BCCWJ or 明鏡/日本国語大辞典 check could settle 伝手に頼る before any rule
depends on it. Retrospective: no outcome correction; process findings DD-498
and DD-499 recorded in `DearDough.md`.
