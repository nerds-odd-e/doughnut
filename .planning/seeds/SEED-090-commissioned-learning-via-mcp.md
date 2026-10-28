---
id: SEED-090
status: dormant
planted: 2026-10-28
planted_during: owner-directed backlog addition for commissioned learning through the ChatGPT plugin
trigger_when: enabling learners to conduct Donut commissioned learning directly in ChatGPT through MCP
scope: medium
---

# SEED-090: Verify commissioned learning directly in ChatGPT

Donut exposes the commissioned Learning Session Request and Report through MCP
with a narrow hosted-assistant recording grant. Verify the real ChatGPT journey
and retain only operator steps that have actually been observed.

## Story

<a id="story-2"></a>

### Verify a commissioned learning session directly in ChatGPT

**Identity:** SEED-090#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Goal:** A learner using Donut in ChatGPT can complete a commissioned
  Learning Session and see Feedback saved in Donut, including when ending early.
- **Scope:** The owner operates a real ChatGPT connection to an HTTPS deployment
  containing the MCP Request/Report tools. Request `notes:read` and
  `learning_session:record`, observe consent describing recording, retrieve the
  first Request, tutor from its instructions, and submit a Report for completed
  items only. Verify Feedback in learning history and incomplete items unchanged.
  Observe the no-session response and an empty Report when ending immediately.
  Record origin/revision, actual scope string, consent wording, tool calls,
  history observations and limitations. Update the hosted-assistant guide only
  with steps actually observed. If ChatGPT cannot request or retain both scopes,
  retain the evidence and refine the remaining interoperability work.
- **Key examples:** Japanese is the sole available notebook and its completed
  items receive Feedback; with A/B/C, ending after A records A only; ending
  immediately records nothing; with no due session, ChatGPT explains none exists.
- **Exclusions:** Personal-token-only or SDK-fixture proof cannot establish this
  ChatGPT journey. Program choice, automatic further sessions, a replacement
  protocol and general note editing remain outside scope.
- **Owner / operation:** Terry will conduct the credentialed probe directly in
  ChatGPT. Environment and permitted UI access are setup prerequisites; no
  credentials belong in prompts or source control.
- **Provenance:** Moved from former slice 4 of the commissioned-learning MCP
  execution by owner instruction on 2026-10-28. Existing MCP/OAuth automated
  proof is reusable; the real ChatGPT journey remains unobserved.
  Original probe contract: `75fece3754879b7d3f840b002d58adfa7beafc91:.planning/slice-plans/077-commissioned-learning-session-in-chatgpt/PLAN.md` (former slice 4).

## References

- [Hosted assistant account linking](../../docs/assistant-oauth.md).
- [Commissioned learning session protocol](../../docs/commissioned-learning-session-protocol.md).
- [Feedback and scheduling](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session).
