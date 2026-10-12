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
