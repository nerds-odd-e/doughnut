# Correct the short lessons' fit-check record

**Identity:** SEED-073#correct-short-lessons-fit-check
**Source:** [correction story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#correct-short-lessons-fit-check)

## Provenance

Retrospective correction of SEED-073#validate-short-recall-lessons (plan
105, commits `47f09f6176`, `08df87c943`, `289bff89de`, `76226515ad`,
`10f3fd3259` after Take `c653f1232b`). Plan 105 slice 1's rule: "a group
passes when both readers supply the intended word, or a script or inflected
form of it, for every sentence of every pair in it." The story requires
sentences "in which exactly one of the two words fits".

## Current findings (read 2026-11-18)

From the private cache `short-lessons/` (`check-items.json`,
`round1-scores.json`, `answers-C.json`, `answers-D.json`), read only:

- The sentence author's accepted lists, frozen before any reader answered
  (`pairs.json` 00:27:56, answers 00:29:05 SGT), include different words.
  The scoring script `score_round1.py` counts any accepted entry as a pass.
  Passing sentences answered only with such a word:
  - G03 経つ/過ぎる/過ごす: S016 both readers 越して, S018 both 切れた
    (no reader wrote 過ぎる).
  - G22 ただいま/たった今: S062 both readers さっき.
  - G26 凄い/恐ろしい: S070 both readers 怖く.
  - G20 いくらか/いくら: S057 one reader 幾分.
  - G21 それで/それに: S059 one reader で.
- G23 やつ/やっこ: both readers wrote 奴 for both sentences; 奴 is the
  kanji of both words and is accepted for both, so the readers did not
  separate the pair.
- Under the plan's rule with the pair separated, 14 groups qualify: lesson
  half G10, G17, G18, G24, G25, G27, G29 (7); comparison half G01, G07,
  G08, G11, G12, G13, G31 (7). The six above are G21, G22, G26 (lesson)
  and G03, G20, G23 (comparison).
- The evidence file's counts table says the 20 groups were "answered with
  the intended word by both readers"; that is not so for the six.
- The evidence file and plan 105 give the assignment time as "00:32:18
  UTC"; `assignment.json` records `2026-11-17T16:32:18+00:00`, which is
  00:32:18 SGT on 2026-11-18. The order before the lessons holds.

## Preserved promises and constraints

The frozen check items, their digest, the split, its seed, the lesson and
check pages, and the lesson record stay unchanged; nothing is re-read or
re-randomized. The check still covers all 20 groups. Production stays read
only; private files stay uncommitted (ADR 0002, ADR 0003). The
recommendation still fixes no pass mark.

## Outside-in proof ownership

Records only; no product test. Proof: `git diff --check`; a link and anchor
reading of the changed files; the group lists in the evidence file match
`round1-scores.json` and `answers-C/D.json` read again with `python3 -I`;
`node .claude/skills/dough-product-backlog/scripts/product-backlog.mjs read-state --link 'seeds/SEED-073-learning-refinement-and-retrieval.md#check-short-recall-lessons'`
shows the check story's state recorded after its edit.

## Ordered slices

### 1. Name the qualified groups in the evidence and the check plan
Type: Behavior
Status: planned
Proof: the evidence file's fit check names the 14 groups that met the rule
and the six apart with their reason, and the assignment time is in SGT; the
check story's limits and plan 106 slice 1 report the 14 groups as the
trial result and the six apart; the read-back above matches.

Behavior: the cached reader answers → correct the fit-check counts table
and add the six groups with their reason (accepted different word, or the
shared kanji 奴); correct the assignment time; in the check story's
"Limits to state with the result" and plan 106 slice 1, state that scoring
reports the 14 qualified groups (7 a half) as the comparison and the six
groups apart, keeping the existing near-synonym reporting. Record the check
story's preparation state again after its section changes.

## Current decisions

- The six groups stay in the check and the split; only their reporting
  changes. This follows plan 105's stated rule; the owner may choose to
  read all 20 as the trial instead when he reviews the recommendation.
- Must land before plan 106 slice 1 (check window opens 2026-11-23).

## Refinement and concerns

Refinement not needed: one slice, one outcome. This review identified no
remaining slice-specific concern.
