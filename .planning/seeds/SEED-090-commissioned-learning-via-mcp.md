---
id: SEED-090
status: dormant
planted: 2026-10-28
planted_during: owner-directed backlog addition for commissioned learning through the ChatGPT plugin
trigger_when: enabling learners to conduct Donut commissioned learning directly in ChatGPT through MCP
scope: medium
---

# SEED-090: Conduct commissioned learning through the ChatGPT plugin

## Why This Matters

A learner connected to Donut through the ChatGPT plugin should be able to ask
ChatGPT to run their commissioned learning and return the results to Donut.
The owner's current example is Japanese learning. The existing commissioned
learning protocol supplies the tutoring instructions, learning material, and
report format; MCP should carry that same interaction.

## Alternatives and Decision

- **Existing manual handoff:** Copy the Learning Session Request from Donut
  into ChatGPT and paste its Learning Session Report back into Donut.
- **Requested direction:** Fetch the first available learning session action
  and record its report through the authenticated MCP connection. Japanese is
  the initial example; selection follows Donut's available actions for any
  notebook.
- **Deferred promises:** Choosing among learning programs and automatically
  progressing through additional sessions can be considered later.

## Story Decomposition

<a id="story-1"></a>

### Conduct a commissioned learning session in ChatGPT through MCP

**Identity:** SEED-090#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A Donut learner can ask their connected ChatGPT tutor to
  conduct a commissioned learning session and save the resulting Feedback
  directly to Donut, including when they finish early.
- **Scope:** On the learner's request, obtain the first available action from
  Donut's Learning session actions for that learner. Load its Learning Session
  Request, including notebook-specific instructions and Session Items, and
  interactively tutor the learner according to that request. When the session
  finishes or the learner asks to end it, submit a Learning Session Report
  through MCP for the completed Session Items in the selected notebook. Use
  the same commissioned learning protocol: Grade 1–4 and descriptive Feedback,
  title matching within the notebook, and the existing recording of Feedback
  in learning history and scheduling. Items not completed receive no Feedback
  and retain their current learning state. If no action is available, tell the
  learner there is no available learning session.
- **Key examples:**
  1. **Japanese session:** Japanese is the only available action. The learner
     asks ChatGPT to start learning from Donut. ChatGPT retrieves the request,
     follows its tutoring instructions, teaches interactively, and submits
     Feedback for the completed items. The learner can see that Feedback in
     Donut's learning history.
  2. **Several actions:** Donut supplies multiple learning session actions.
     ChatGPT uses the first action in Donut's returned order and tutors from
     that action's request.
  3. **End early:** A request contains items A, B, and C. After completing A,
     the learner ends the session before completing B or starting C. ChatGPT
     submits Feedback only for A; B and C remain unscored with their existing
     schedules. Ending before completing any item records no Feedback.
  4. **Nothing available:** Donut supplies no learning session action.
     ChatGPT explains that there is no available session.
- **Value / learning:** Make commissioned practice usable within the connected
  conversation, with progress retained when the learner has limited time.
- **Effort hypothesis:** M–L (1–4 hours), low confidence until refinement checks
  the MCP interaction against the existing session request and report behavior.
- **Safe stopping point:** One requested session can be taught and its completed
  items recorded, including an early finish. Further program-selection and
  multi-session workflows remain deferred.

## Breadcrumbs

- Owner request in this chat, 2026-10-28: queue this story first, use the first
  learning session action, reuse the commissioned learning protocol, and report
  only finished items when the learner ends early.
- [Commissioned learning session protocol](../../docs/commissioned-learning-session-protocol.md).
- [Commissioned learning vocabulary](../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session).
- [Feedback and scheduling](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session).
