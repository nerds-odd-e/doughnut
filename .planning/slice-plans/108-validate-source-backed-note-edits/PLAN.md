# Validate source-backed note edits from recall

**Identity:** SEED-073#validate-source-backed-note-edits
**Source:** [selected story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#validate-source-backed-note-edits)
**Source review:** [source-review.md](source-review.md)

## Goal and scope

Give the owner a recommendation, per addition, on what to add to today's
reviewed note correction: a quoted outside source, the edit shown as the
smallest change, and a question made from the note before and after the
edit. Included: six cases, two real tries of today's conversation, the
question check with the product's generator, the owner's verdicts on a
local page, the recommendation, and this story's evidence disposition.
Excluded: as the story says. No product code changes; scratch code is
never committed.

## Inputs and private files

- Shared cache: `/Users/terryyin/git/doughnut/.planning/tmp/recall-history/`.
  Attempts: `recall-answers-100d.json`, array `attempts`; a row number in
  `offline-replay/manifest.json` is a one-based position in that array.
- This story's private folder: `note-edits/` inside the cache. It holds
  `cases.json`, the scratch test, request and response files, the review
  page and the owner's saved answers. Nothing in it is committed.
- Committed evidence: `.planning/seeds/source-backed-note-edits-evidence.md`
  (created by slice 1): counts, the short claims and edits, the owner's
  reasons, limits. No attempt text beyond a short example per case.
- Production is read only for the agent: current notes are read with the
  installed CLI token (`POST /api/notes/search`, `GET /api/notes/{id}`),
  never printing the token. Only the owner writes, in slice 3, by pressing
  Accept. Run Python as `python3 -I`.
- The 20 lessons-trial groups (`short-lessons/assignment.json`, halves
  `lesson` and `comparison`) are neither shown nor edited before 2026-12-16.

## Decisive premises

| Premise | Consumed by | Observation | Result | Remainder |
| --- | --- | --- | --- | --- |
| The four faulty claims are in the current notes | 1 | Production read, 2026-11-18, notes 5953, 7323, 10891, 3265 | All four present; two are note properties | Notes may change before execution: slice 1 re-reads and freezes them |
| Attempts for the faults resolve from the export | 1 | `python3 -I` over `recall-answers-100d.json`, 2026-11-18, rows 3582, 8630, 5211, 10467, 8502, 4635, 3686 | Each row is a miss on the named note | none |
| One-sense candidates exist outside the trial | 2 | `short-lessons/fit-status.json` and `assignment.json`, 2026-11-18 | G16 整う/調う and G30 脇/腋: outside both halves, no overlap declared | Whether either fits one sense only needs a source; if neither does, slice 2 reports the kind as not covered |
| A scratch backend test renders the product's generation request for a note | 4 | Scratch `ScratchGenerationRequestTest` calling `NoteQuestionGenerationService.generateQuestion`, run in this worktree at 87d7d24ade with `pnpm backend:test:worktree --tests`, 2026-11-18, then removed | Body captured: `model`, `instructions`, `input`, `text`, `max_output_tokens` 1000; the focus note's properties are in the input; 4,310 characters for a two-line note | The local request holds only the notes the scratch test creates, not production's retrieved context: a stated limit |
| Production generates questions with `gpt-6.2-sol` | 4, 5 | `GET /api/settings/current-model-version`, 2026-11-18 | `questionGenerationModel: gpt-6.2-sol` | Its price is not recorded here; slice 4 obtains it before any call |
| Stored requests can be sent from the Nix shell | 5 | `integrity/send_one.py` and `sent-log.jsonl`: 60 calls, 2026-11-16, token from `OPENAI_API_TOKEN` | HTTP 200 for `gpt-6.1-sol` | Paid: the first call of slice 5 shows whether the token serves `gpt-6.2-sol` |
| A local page can collect the owner's answers | 6 | `short-lessons/build_pages.py`; `lesson-answers-2026-11-18.json` saved from the page | Worked on 2026-11-18 | none |
| Today's note conversation proposes a reviewed correction in production | 3 | [Documented](../../docs/recall-help.md#reviewed-note-corrections); code read | Presence only | Owner-held: slice 3 is the observation, and what it shows is a result either way |

## Outside-in proof ownership

Research observations at the owner-facing evidence file; no product test
is part of the proof. For committed documents: `git diff --check`, a link
and anchor reading of the changed files, and
`node .claude/skills/dough-product-backlog/scripts/product-backlog.mjs read-state --link 'seeds/SEED-073-learning-refinement-and-retrieval.md#validate-source-backed-note-edits'`.
Before each commit, `git status --short` shows no scratch file.

## Ordered slices

### 1. Freeze the four fault cases with sources and smallest edits
Type: Behavior
Status: done
Proof: `cases.json` holds, per fault, the attempt (question, answer given,
mark), the current note content with its read time, the faulty passage, a
quoted source passage with its link, and the smallest edit as old and new
text; the evidence file lists the four cases with claim, source and edit.

Behavior: the four notes are re-read from production → each case is frozen
with a source the agent has opened and quoted (the earlier links are
starting points, not evidence) → the evidence file gains the dated case
list. A claim no longer in the note, or a source that does not support the
edit, is recorded as such and the case is dropped, not replaced.

### 2. Freeze the one-sense case and the no-change case
Type: Behavior
Status: done
Proof: `cases.json` holds both cases in the same form; the evidence file
names them, and names any candidate left out with the reason.

Behavior: from 整う/調う and 脇/腋, pick the pair where a source shows the
typed word fits one sense of the cue only → the edit accepts it for that
sense. From the replay's sound questions on a correct note, pick one miss
where a source supports the note → the proposal is "no change". Check
each chosen note against the trial halves; a trial pair is left out.

### 3. The owner tries today's conversation on two faults
Type: Behavior
Status: done
Proof: the evidence file's dated section records, for Solid OIDC (fault
in a property) and prisoner's dilemma (fault in the body): what was
proposed, whether the rest of the note came back unchanged against the
frozen content, whether the owner could judge it, minutes, and Accept or
Decline. An accepted edit is confirmed by a read of the note.

Behavior: ask the owner once, with all steps: open each note in
production, start a conversation on it, say in his own words what is
wrong and ask for the correction, copy the proposed content into a file
in `~/Downloads` before deciding, note the minutes, then decide as he
would normally. This comes before the review page so that the page's
edit does not shape what he asks. If no proposal appears, that is the
recorded result.

### 4. Render the generation requests and show the cost
Type: Behavior
Status: done
Proof: `requests/` holds one body per case and state (before, after; the
no-change case before only), each with model `gpt-6.2-sol`; the owner is
shown the number of calls, measured input size, the price source and the
total, and answers go or no.

Behavior: a scratch backend test in the execution workspace creates each focus note
with its frozen content, and the notes it links to with their current
content, and writes the product's generation request. Three runs per
body: 33 calls. Take the price of `gpt-6.2-sol` from the provider's
published list or from the owner; do not reuse another model's price.
Compute the total from the measured request sizes and 1,000 output tokens
a call. Send nothing in this slice. Remove the scratch test from that
workspace after copying it to `note-edits/`.

### 5. Generate the questions and check them
Type: Behavior
Status: done
Proof: `responses/` and a sent log with one line per paid call; the
evidence file gives, per case and state, how many of three questions had
a key that agrees with the source, and for the after state whether a
valid near miss was told apart; calls sent and actual cost.

Behavior: only after the owner's go in slice 4 → send one request first
with a copy of `send_one.py` adapted to this model and output limit; a
refusal stops the slice and is reported → send the rest, never resending
a stored response → two reviewers in this session answer every question
with key and state hidden → compare with the stored key and the source.
For the no-change case the check is that the questions agree with the
source as the note stands.

### 6. The owner records his verdicts on the review page
Type: Behavior
Status: done
Proof: the saved answers file; the evidence file gives, per case, accept
as it is, accept with his wording, or decline with his reason; and per
addition (source, smallest change, before-and-after question) in how many
cases it changed or eased the decision.

Behavior: build one local page from `cases.json` and slice 5 → per case it
shows the attempt, the passage, the quoted source and link, the edit as a
change against the current text, and one question from before and after
→ ask the owner once: the file path, about 40 minutes → he saves his
answers to `~/Downloads` (list it with a bounded command). The two cases
of slice 3 are shown too, marked as already tried.

### 7. Report the recommendation and close out
Type: Behavior
Status: planned
Proof: the evidence file ends with, per addition, design it, narrow and
retest, or stop, the counts and reasons behind each, the comparison with
slice 3, the story's limits, and the closeout paragraph.

Behavior: all records are in → write the recommendation for the owner's
review; fix no pass mark → record this story's evidence disposition:
delete `note-edits/` once the conclusions are in the evidence file, and
release or keep the shared cache under the
[shared contract](../../seeds/SEED-073-learning-refinement-and-retrieval.md#close-out-temporary-recall-evidence)
after checking the other queued validations.

## Learnings

- Slice 1 (2026-11-18): all four notes read 02:20Z, each claim still
  present; four cases kept. Sources for 存じる (kotobank 日国 gives 他動詞;
  Wiktionary labels the 思う sense 自動詞) and んじゃない (only the note's
  own cited article; it ties men's language to a final か) are weaker than
  the other two. Row 8502's stored key was itself wrong. In the export the
  question is under `multipleChoice`, not `note`.
- Slice 2 (2026-11-18): one-sense case 整う/調う (note 28353, rows 2012
  and 2484, spelling misses; source 文化庁「異字同訓」の漢字の使い分け例).
  Its rows have no recoverable prompt (note edited 2026-11-12), so the
  review page shows the export's prompt-unavailable text. No-change case
  〜ことだから/〜ことから (note 22629, row 4032). 脇/腋 left out: no source.
- Slice 3 (2026-11-18): the owner asked the agent to make both tries and
  decide on his behalf. Both proposals came on the first request and were
  accepted (Solid OIDC 5ed4e72e, prisoner's dilemma fa5efa4d). Solid OIDC
  also changed `an instance of` to `builds on`, unasked. Both notes now
  differ from the frozen content; slices 4–6 keep using the frozen
  content, and the page marks these two as already corrected.
- Slice 4 (2026-11-18): 11 bodies in `note-edits/requests/`; price
  $2.00/$10.00 per 1M tokens; about $0.43 for 33 calls; owner: go.
  tiktoken is unavailable, so token counts are estimates.
- Slice 5 (2026-11-18): `OPENAI_API_TOKEN` is only in the owner's
  interactive zsh; the owner allowed `zsh -i -c`. 33 of 33 calls 200,
  about $0.18. Blind reviewers matched every key. Questions touched the
  edit in 2 of 5 after cases (prisoner's dilemma, 整う), none from a
  property. Responses give `questionStem`, `correctAnswer`, `distractors`;
  the packet shuffles choices itself.
- Slice 6 (2026-11-18): the owner filled the page himself; answers in
  `note-edits/note-edit-verdicts-2026-11-18.json`. The page was checked
  by parsing only; the owner's save worked. He asked for advice on his
  answers.

## Current decisions

- The comparison is with today's reviewed correction, not manual editing
  (owner, 2026-11-18).
- The owner's real tries come before he sees the prepared edits. On
  2026-11-18 the owner delegated both tries and their decisions to the
  agent; the evidence states that limit.
- Paid calls wait for the owner's go on the computed cost. Estimate for
  orientation only, not a limit: 33 calls of at most about 5,000 input
  and 1,000 output tokens each.
- The story executes without product changes; any execution mode works,
  and no slice acts from trunk.
- Refinement: not needed; the slices follow the story's required
  behaviors one to one and each has its own record.
- Remaining concerns: none identified in this review. The unpriced model
  and the owner-held conversation are bounded by slices 4 and 3.
