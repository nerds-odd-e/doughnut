# Tutor descriptive feedback in Learning Session protocol

**Status:** complete

Shipped in product docs and code. Canonical record:

- [Commissioned learning session protocol](../../../docs/commissioned-learning-session-protocol.md)
- ADR 0001 **Feedback** gloss (Grade and descriptive text)

A Tutor Report prefers `<session_item_feedback>` (`###` heading, `Grade: N`, prose).
Doughnut records the text on the tutor RecallLog; the learner reviews it in recall
history; the next Request carries the last two dated Feedbacks per Session Item.
Legacy grade-only Report shapes remain accepted.
