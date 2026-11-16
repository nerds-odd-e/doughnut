# Completed slice contracts

These original contracts and accepted summaries belong to the [active plan](PLAN.md).
Inspected observations, dates, commands, settings and limitations remain in
[acceptance evidence](acceptance-evidence.md) and [historical evidence](historical-acceptance-evidence.md).
The accepted summaries and Proof/Behavior text below are preserved verbatim.

## Slice 1 original proof contract

Accepted (2026-11-16): the three sender guards refused before transport;
one row 5347 replay returned HTTP 200 (USD 0.00282); both product generation
routes included the fetched linked-note evidence. The full backend suite
passed (3,199 tests, zero failures/errors, two skips). Row 5347 is diagnostic;
[inspected setup, commands and evidence](acceptance-evidence.md#slice-1-acceptance-observations)
retain the exact proof. Independent refactoring and delivery passed.

Proof: With the token absent the sender exits before sending. With a
scratch ledger of 500 lines, and separately one totalling USD 5, the sender
refuses. One paid replay of an existing historical request returns HTTP
200 and adds one ledger line with latency, tokens and cost. One on-demand
and one batch generation request for one Production note with its linked
notes are rendered to the cache without being sent.

Behavior: the execution shell holds the token → the sender is asked for
one call → the call is sent, the response is stored, and the ledger holds
one line; at either limit nothing is sent. Early probe: a missing token or
a generation request that cannot be rendered with real linked context
stops the dependent slices (all paid slices; slice 7) and changes this
plan. The paid replay counts toward the cap; its question is now unscored.

## Slice 2 original proof contract

Accepted observations: 47 reviewed; 41 scoreable (33 sound, 6 multiple-answer,
2 wrong-key), 3 note-at-fault and 3 unresolved. Independent key-hidden reviews,
provenance, unchanged ledger and inspected scoring are retained in
[slice 2 acceptance](acceptance-evidence.md#slice-2-acceptance-observations).
The owner accepted the fresh labels and revised historical allocation.

Proof: `pilot/historical-labels.json` in the cache lists each of the 47
earlier-labelled questions with both reviewers' key-hidden judgments, the
outcome (wrong key, more than one defensible answer, sound, note at fault,
unresolved), whether the note changed after generation, and any missing
historical content. The count of scored questions is recorded. No ledger
line is added.

Behavior: the 47 questions with their notes and required linked evidence
read read-only → two fresh reviewers label them with the key hidden → the
scored historical set (about 40) is fixed. Example: reviewers disagree on a
question → it is listed unresolved and scored nowhere.

## Slice 3 original proof contract

Accepted: all 123 calls and product decoding passed; deployed caught 8/8
defects with 13/33 sound false flags, choices-only 7/8 with 0/33. See
[slice 3 observations](acceptance-evidence.md#slice-3-paid-observations).

Proof: Per scored historical question, a table of 3 product decisions and
3 choice readings against the key, with the 2-of-3 verdict for each check;
the ledger holds 3 lines per question (123). Totals by label for
detected, false flag and same-verdict-in-all-runs. Row 8739 appears under
sound with three results; row 5347 remains a separate unscored diagnostic.

Behavior: scored historical questions and the product's verifier request →
each is sent 3 times → detection, false flags and repeat agreement are
recorded for the deployed rule and for choices-only.

## Slice 4 original proof contract

Accepted: the frozen candidate caught 8/8 defects, flagged 0/33 sound and
was unanimous on 41/41; [slice 4 proof](acceptance-evidence.md#slice-4-acceptance-observations)
retains its hash, timing, isolation, costs and historical-only limits.

Proof: `pilot/candidate-prompt.txt` and its SHA-256 exist; wording used at
most 18 ledger lines and only historical questions. After the freeze, 3
runs per scored historical question (123 lines) give the same
table as slice 3 for the narrower prompt.

Behavior: the historical labels and slice 3's results → one prompt that
judges only which choices the note supports, carrying required linked
evidence, is worded, frozen, and sent 3 times per question → its detection,
false flags and repeat agreement are recorded beside slice 3's.

## Slice 5 original proof contract

Accepted: 24 scored questions (1 wrong key, 5 multiple-answer, 18 sound),
all six labelled defects retained; [slice 5 proof](acceptance-evidence.md#slice-5-acceptance-observations)
retains independent labels, selection, exact keys and unchanged ledger.

Proof: `pilot/heldout-labels.json` lists about 24 scored questions from
notes absent from the historical set, with the same label fields as slice
2, the selection rule and seed, and how many candidates were labelled to
reach them. Every labelled defect found is included. No ledger line is
added, and no held-out question was sent or read during slice 4.

Behavior: misses from the 100-day export on notes outside the historical
set, chosen by a recorded rule that favours the keyless defect signals and
adds randomly drawn controls → the reviewers label them as in slice 2 → the
held-out set is fixed. Example: only 4 defects are found → the set is kept
and the pass mark's "narrow and retest" bound applies in slice 6.

## Slice 6 original proof contract

Accepted: the candidate caught 6/6 defects, flagged 0/18 sound and was
unanimous on 24/24; the fixed rule yields proceed to design. See
[slice 6 observations](acceptance-evidence.md#slice-6-acceptance-observations).

Proof: The ledger's first held-out line is later than the freeze. Per
held-out question, 3 runs of each paid check (at most 144 lines) and the
choices reading. A result file states the three pass-mark counts for the
narrower prompt, the same counts for the deployed rule and choices-only,
and the recommendation the pass mark gives.

Behavior: the frozen prompt and the labelled held-out set → each question
is sent 3 times to each paid check → the pass mark yields "proceed to
design", "narrow and retest" or "stop". Example: 8 defects and 16 sound; 6
caught, 1 sound flagged, 22 of 24 with one verdict in all runs → "proceed
to design".
