# Private recall help

After answering a multiple-choice question, the learner can open private help
with the original question, selected answer, marked result, and tested focus
visible. The conversation's subject identifies the recall attempt; its access
ownership identifies its audience. Creating help requires the memory-tracker owner.
Reopening compatible help uses the same recall prompt, initiator, and personal
ownership. Shared conversations retain their existing audience.

## Explanation and practice

Learner reasoning is optional. Explanations treat interpretations as tentative,
accept clarification, and examine ambiguity or missing evidence. The original
attempt and currently accessible note content remain distinct sources.

“Check this distinction” requests a changed example as an ordinary conversation
message. The assistant waits for the learner's response before giving feedback.
The question, response, and feedback remain in the conversation. Practice leaves
the original answer, grade, recall log, and memory-tracker schedule intact; it
does not establish mastery or decide a contest.

## Learner-confirmed ambiguity exclusion

When the wording of a multiple-choice question does not support a unique
answer, the assistant can propose excluding the learner's attempt as an
ambiguous question, with a reason that explains the defensible reading. It
proposes this only for genuine ambiguity, not for an option that is
grammatical but has the wrong meaning, and only in private help for a
multiple-choice attempt. A proposal changes nothing. The help dialog shows it
as "Proposed correction: exclude this attempt as an ambiguous question" with
the reason and the choices Confirm and Decline.

Only the learner who owns the attempt can confirm. Declining leaves the
result and the schedule as they were. Confirming records the exclusion time
and the reason on the attempt. Confirming again returns the same decision and
writes nothing new.

The evidence stays. The question, the selected answer, the original mark, the
recall log with its Grade, and the help conversation that led to the decision
all remain. Wherever the attempt is shown (the recall page, the help subject,
and the memory tracker's recall history) it reads "Excluded — ambiguous
question" with the reason, the confirmation time, and the original mark,
beside its original recall log. The selected choice is not coloured as wrong.

Confirmation is not a Grade. It adds no recall log and keeps the same memory
tracker. The tracker's scheduling state is rebuilt by replaying the recall-log
events that remain valid, in order, at their original times, under the current
scheduling policy of
[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md). A
recall log is valid unless it belongs to an excluded attempt. The memory state
cached on each remaining log is realigned to what the replay produces. A
tracker with no valid event left is new again and due at its assimilation
time, so the corrected due time can already be in the past. Replay uses only
what recall logs hold: scheduling state that is not held in recall logs is not
carried over.

An excluded attempt is left out of evaluation readouts: the tracker's recall
count, the frequent-failure warning, and recall statistics (retention,
accuracy, and calibration). It still counts as recall activity at the time the
learner answered.

After confirmation the help dialog stays open and the page behind it shows the
excluded result and the corrected schedule at once. The assistant sees the
exclusion in the attempt record on later requests and does not propose it
again.

Deferred: invalidating the question for other learners, regenerating or
editing the question, undoing a confirmed exclusion, excluding several
attempts together, spelling attempts, and changes to the note. A formal
contest verdict is a separate decision.

## Reading and returning

A successful visible conversation fetch acknowledges reading and replaces the
shared unread state with the server's result. Contextual help, Messages, and
direct note conversations share this behavior. A failed fetch preserves the
unread state.

The shared modal stack lets the learner close a child refinement and return to
help. Closing help preserves recall progress and returns focus to its opener.

## Reviewed refinement

Note refinement uses existing edit rights. For a multiple-choice conversation,
the refinement context includes the selected answer and an explicitly reviewed
clarification. The private transcript stays in the conversation. Spelling
conversations retain their ordinary authorized refinement flow.

[ADR 0002](adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
keeps private learning data outside Portable content and requires accepted
content changes to use Git publication.
[ADR 0003](adrs/0003-spaced-repetition-scheduling-policy-accepted.md) governs
grade transitions and recall scheduling.
