# Source review

Launch: 2026-11-18, a fresh general-purpose reviewer with no conversation
inheritance. Input: only the selected story's text (Goal, Why, Owner
decision, Required behavior, Out of scope, Key examples, Interaction, Found
during refinement, Effort hypothesis) from
[the story](../../seeds/SEED-073-learning-refinement-and-retrieval.md#source-and-smallest-change-in-corrections).
No plan, product, proof or slice scope was given. The reviewer returned the
numbered promises below.

Unnumbered context as returned: Goal and Why are reasons; the owner's split
is a delivery deferral; every promise applies only to a pending content
correction the author may accept; the Out of scope items are limits, with
"They keep today's display" read as a settled baseline; the Found during
refinement notes are reasons and design notes; the example notes are
fixtures; the effort hypothesis is context.

## Commitments

| Id | Source commitment | Slice | Observation or obligation |
| --- | --- | --- | --- |
| P1 | Removed and added text are marked in the review. Condition: "For a pending content correction that the author may accept". "the review shows the change between the current note content and the wording in the editor: removed text and added text are marked". Goal: the author "sees what the correction would change in the current note, without comparing two full versions". Examples 1 and 2. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "marks the removed and added lines of a pending content correction": del and ins lists; e2e note_content_completion.feature step 'the review should mark' |
| P2 | Unchanged text is not marked. "and unchanged text is not." Example 1: "`type`, `url` and `a part of` are not marked." Covers unchanged lines and properties; unchanged text inside a changed line is P4. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "marks the removed and added lines of a pending content correction": type, url, a part of lines are in the change text and in neither the del nor the ins list |
| P3 | Property changes are marked like any other change. "Note properties are part of the content, so a changed, added or removed property is marked like any other change." Example 1: the two property lines are marked. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "marks the removed and added lines of a pending content correction": old property lines in del, new ones in ins |
| P4 | Inside a changed line, only the changed part is marked. "Inside a changed line, only the changed characters are marked, not the whole line." Example 2: "only those words are marked. The rest of the paragraph is not."; example 1: "the reworded words marked". | slice 2 | |
| P5 | The same holds for text without spaces. "This also holds for text without spaces, such as Japanese." Example 4: 「最後に「か」がつくと」 added in the middle of a line → "the added characters are marked, not the whole line." | slice 2 | |
| P6 | The marks update when the author edits the wording. "The change follows the editor. When the author edits the wording, the marks update." Example 3: the author "types the old line back into the editor → that mark disappears". | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "removes the mark of a line typed back and accepts the wording without that change": del and ins lists after setValue |
| P7 | Accept publishes the edited wording, without the change the author removed. Example 3: "and Accept publishes the wording without that change". Qualification: "acceptance stays as it is (ADR 0002)", so this is existing acceptance behaviour that must still hold at the end of the removal journey. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts same test: acceptCorrection called with the edited reviewedValue |
| P8 | After Load current content, the change is shown against the note as just loaded. Example 5: "Accept is refused as stale and the author chooses **Load current content** → the change is shown against the note as it now reads." | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "marks the change against the content loaded after a stale acceptance" |
| P9 | The review says there is no change when the wording equals the current content. Example 6. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "says there is no change when the wording equals the current content" |
| P10 | "The full current content, the original proposal and the editor stay available." All three present for a pending content correction. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "shows the change before the current content, the original proposal and the editor": Current content heading, Original proposal details and textarea value asserted |
| P11 | The change is what the author reads first. "For a pending content correction the change is what the author reads first." Position relative to the editor, the full current content and the original proposal. | slice 1 | observed: frontend/tests/components/conversation/ConversationInner.correctionChange.spec.ts "shows the change before the current content, the original proposal and the editor": change section precedes Current content, Original proposal and the textarea in the review html |
