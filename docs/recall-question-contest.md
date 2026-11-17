# Recall question contest

A new recall question is shown as generated; nothing checks it before the
learner sees it. A learner who doubts a question contests it, and the contest
is judged on correctness alone.

## The check

One request per contest asks the administrator's evaluation model which
choices the note supports. The question is sent without its stored answer,
with the same focus context and question-generation instructions as
generation (see [question-generation instructions](question-generation-instructions.md)).
The model returns only the supported choice positions
(`SupportedChoices`); it gives no verdict and no opinion on question quality.

The instruction text in `SupportedChoices.INSTRUCTIONS` is the wording that
was measured against reviewed recall questions. Keep it word for word; a
changed wording needs its own measurement.

## The verdict

The product decides from the supported positions and the stored answer:

| The note supports | Result |
| --- | --- |
| exactly the stored answer | Refused. The advice says the note supports exactly one choice and asks the learner to answer; it names no choice. The question stays and is not marked contested. |
| any other set, including none | Accepted. The question is marked contested. The advice names the stored answer and the supported choices, and the replacement question is generated with that advice. |

How easy, guessable, or well-focused a correct question is does not affect
the verdict. When the model gives no response, the contest has no verdict.
