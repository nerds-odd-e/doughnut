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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/078-verify-commissioned-learning-in-chatgpt/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0e9aa4ff18fb7ae912965c1fc6e4327208b57810e57bd720b4455b3864c6fb65","plan":"c513e5006a305c7c57150691a7baa514a11f2665a032c273216b84d817e63e8f"}}
```

- **Goal:** Terry, as a learner using Donut in ChatGPT, can verify that a real
  commissioned Learning Session saves Feedback in Donut for the items actually
  completed, including when ending early. The observed journey supplies reliable
  setup instructions for other learners.
- **Scope:** Verify the existing implementation through a real ChatGPT OAuth
  connection to an operator-authorized HTTPS deployment containing the MCP
  Request/Report tools. Request `notes:read` and `learning_session:record`,
  observe Donut consent describing report recording, and demonstrate that the
  resulting connection can both request and record a session. An existing
  read-only connection needs fresh authorization for both scopes.
  ChatGPT retrieves the first available Request with the learner's IANA timezone,
  follows its tutoring instructions, and submits the existing Report markdown
  for completed Session Items when the session finishes or ends early. Compare
  the returned recording result with actual Feedback in Donut recall history;
  incomplete items retain their prior Feedback and learning schedule. Verify an
  empty Report on immediate end and the explicit no-session outcome.
- **Key examples:**
  1. Japanese is the sole notebook with a Potential learning session. Asking
     ChatGPT to start learning returns Japanese's Request. After the learner
     starts and completes its items, ChatGPT records their Grades and descriptive
     Feedback; the same Feedback is visible in Donut recall history.
  2. A Request contains A/B/C. The learner completes A and ends before B/C.
     ChatGPT submits Feedback for A only. Donut history contains A's new
     Feedback; B/C's prior history and schedule match their recorded baseline.
  3. A Request is available, but the learner ends before completing any item.
     ChatGPT submits an empty Report; the result records zero items and the
     session's items retain their prior history and schedule.
  4. The learner has no due commissioned items. Asking to start returns the
     explicit no-session result, which ChatGPT explains to the learner.
- **Evidence and documentation:** For each round retain the observation date,
  deployment origin and revision, account/data preconditions, learner timezone,
  actual requested scope string, consent wording, ChatGPT tool arguments/results,
  and Donut history/schedule comparisons. Record failures and unreached steps
  precisely. Update the existing hosted-assistant guide with the setup and
  session steps actually observed; keep credentials out of evidence. Automated
  MCP/OAuth and SDK proof supports its own boundary and cannot establish the
  ChatGPT journey. A failed or unavailable probe leaves the relevant example
  unverified; retain the evidence and refine the remaining interoperability work.
- **Deferred promises:** Notebook/program choice, automatic further sessions,
  a replacement Request/Report protocol, general note editing, additional client
  compatibility, and activation of a new real client remain deferred.
- **Owner amendment — 2026-10-29:** Terry authorized: "Fix Donut’s OAuth
  integration so ChatGPT requests recording permission, then release it."
  Correct the existing integration's declaration of required recording scopes
  and its permission-upgrade response, prove those contracts at the MCP/OAuth
  boundary, and release the tested correction through the application release
  workflow. Preserve read-only access until informed authorization grants both
  scopes; do not widen an existing token or bypass consent. Continue verification
  through the existing connected plugin after release. Real ChatGPT success
  remains a separate observation from automated proof and deployment success.
- **Closure amendment — 2026-10-29:** After the remaining checks were identified
  as partial completion and no-session verification, Terry instructed: "skip them
  and wrap up." For this story's closure, the owner waives further manual
  verification of the A/B/C partial-completion example and the no-session
  example, including their corresponding observations. They remain unobserved
  and are not represented as passing. Close on the observed authorization,
  full Japanese session with saved Feedback, and immediate-end empty Report.
  No further learning session or new follow-up story is requested.
- **Owner / operation:** On 2026-10-28 Terry clarified that the assistant should
  use the connected Donut plugin directly in this chat as the ChatGPT surface;
  opening a separate ChatGPT app is unnecessary. The assistant runs available
  plugin tools; Terry supplies learning responses and any connection UI or
  consent observations the tools cannot expose. The first probe establishes the
  actual HTTPS origin/revision, connection scopes, existing client registration
  and suitable session data. These are execution setup prerequisites, not claims
  that a candidate is already deployed. Use ordinary product operations on
  owner-approved data; credentials stay in the connection UI.
- **Provenance:** Moved from former slice 4 of the commissioned-learning MCP
  execution by owner instruction on 2026-10-28. Existing MCP/OAuth automated
  proof is reusable; the real ChatGPT journey remains unobserved.
  Original probe contract: `75fece3754879b7d3f840b002d58adfa7beafc91:.planning/slice-plans/077-commissioned-learning-session-in-chatgpt/PLAN.md` (former slice 4).

## References

- [Hosted assistant account linking](../../docs/assistant-oauth.md).
- [Commissioned learning session protocol](../../docs/commissioned-learning-session-protocol.md).
- [Feedback and scheduling](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session).
