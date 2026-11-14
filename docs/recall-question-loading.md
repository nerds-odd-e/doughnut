# Recall question loading

## Question ownership and lifetime

Each question belongs to the memory tracker through which it was asked. Its
note context comes from that tracker's current note. Questions and their
recorded answers, choices, grades and ambiguity exclusions follow the tracker
when it moves between a relationship note and a source property. Deleting a
note the tracker moved away from preserves that evidence.

Recall history, assistant recall answers, prompt conversations, contest and
regeneration context, and recent-question exports use the current tracked note.
Exported choices remain specific to the learner who answered.

Generation atomically persists the selected question with its prompt. Clearing
unanswered prompts preserves a question that an answered prompt still uses.
Removing a tracker removes its prompts and their questions through the shared
application-owned lifecycle.

## Shared due list order

Eligible due work for web and CLI comes from the shared due list. Order is
ascending 12-hour UTC bands of `next_recall_at` (hour floors). Within a band,
SPELLING trackers stay ahead of other types; remaining peers are randomized
when the list is built. Client-side shuffles are not the product rule for that
order. Eligibility and commissioned separation stay with the due query.

[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md) owns
Grade scheduling, including whole-hour interval fuzzing when writing the next
due. Exact fuzz bands and durable hash inputs live with the scheduling
implementation beside FSRS.

## Question load states

Web recall distinguishes a pending question, a failed load, and a loaded prompt
for each memory tracker. A failed load offers **Retry** on that tracker without
changing recall progress. Multiple-choice failures also offer **Self-review**,
which opens the existing full note context and Good/Again controls. Choosing
Self-review alone does not grade the tracker.

Question loading and retrying record no answer, Grade, or schedule transition.
The existing backend operation reuses an unanswered prompt. Access checks and
foreground error reporting remain applicable. Background prefetch failures stay
with their tracker until the learner reaches it and deliberately retries.

The web card's shared `useRecallPromptFetching` owner keeps pending, failed, and
loaded state by tracker. It prevents duplicate pending requests and associates
each completion with its initiating request. Navigation remains available while
a request is pending; a late response preserves the visible card and its input.
Spelling entry receives the loaded prompt from this owner and is presented only
when that prompt is available.

After spelling OVERLAP, the card invalidates the answered cached prompt and
loads a fresh attempt on the same tracker. The overlap explanation stays visible
during loading and recovery. Successful recovery opens an empty, focused answer
input. Thinking time starts when the prompt is presented and excludes loading.

[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md) owns Grade,
OVERLAP and timing semantics. [ADR 0006](adrs/0006-failure-handling-accepted.md)
owns the failure-reporting boundary used by explicit recovery.
