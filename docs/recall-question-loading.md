# Recall question loading

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
