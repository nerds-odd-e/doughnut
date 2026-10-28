# Connection follow-up evidence

[Active plan](PLAN.md) · [Earlier discovery and empty-Report probes](execution-evidence.md) · [OAuth correction proof](oauth-correction-evidence.md)

## Reauthorization follow-up — 2026-10-28

- In response to the reconnect instructions, Terry reported "done". Those
  instructions were to withdraw this assistant's old grant in Donut, reconnect
  the same account from the existing plugin, and check for consent describing
  notebook reading and learning-session recording before allowing it. Record
  this as the owner's report; no separate consent screenshot or actual
  requested scope string was supplied.
- Captured a fresh tracker/history baseline before retrying the previously
  authorized empty Report: `/tmp/seed090-empty-retry-v6ar2xaf.json`, observed
  `2026-10-28T13:47:32.741045+00:00`. Tracker 130239 still had seven history
  entries and the same schedule recorded above.
- Retried `record_learning_session` with `notebookId:15` and the same empty
  Report markdown. Actual runtime error:
  `-32001: Unknown tool({"name":"donut.record_learning_session"})`.
  This attempt did not return a recording result or establish the new grant.
- Bounded read-only comparison: `list_note_candidates({"query":"なにしろ"})`
  returned Unknown tool for `donut.list_note_candidates`;
  `request_learning_session({"timezone":"Asia/Singapore"})` returned the same
  error for `donut.request_learning_session`. All five Donut tool schemas still
  appear in the tools offered to this run. The runtime failure now affects
  existing note access as well as the two session tools.
- Direct production controls still passed: GET `/api/healthcheck` returned
  status 200 and commit `0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c`; an
  authenticated POST `/api/mcp` with
  `{"jsonrpc":"2.0","id":43,"method":"tools/list","params":{}}` returned
  status 200 and all five expected tools. These checks used the existing CLI
  credential in memory and do not prove the plugin's OAuth grant.
- The after-observation file
  `/tmp/seed090-empty-retry-v6ar2xaf-after-unknown-tool.json` contains a tracker
  and all seven history entries identical to the fresh baseline. No learning
  state change was observed. The live plugin's recording result remains
  unverified. The next bounded check is to select Donut again in this chat's
  composer, then retry the empty Report through that connection.

## Reselected-plugin retry and connection inspection — 2026-10-29 (Asia/Singapore)

- Terry selected Donut explicitly and asked to retry the empty Report. The
  tool was callable again; the result was still
  `403 FORBIDDEN "learning_session:record permission is required"`, rather
  than the preceding Unknown tool error. The pre-call observation file
  `/tmp/seed090-empty-reselected-_pwcbccr.json` was captured at
  `2026-10-28T13:57:47.744528+00:00`. The after-observation file
  `/tmp/seed090-empty-reselected-_pwcbccr-after-refusal.json` contains the same
  complete tracker and seven-entry history. This after-observation was made
  during the following continuation; no successful Report occurred between
  those observations.
- On Terry's next "continue", captured a fresh baseline at
  `2026-10-28T22:53:59.531041+00:00` (2026-10-29 in Singapore), in
  `/tmp/seed090-empty-continue-m8jrtgi5.json`. Retried the connected plugin's
  `record_learning_session` with `notebookId:15` and exactly the empty Report
  markdown retained above. Actual result:
  `McpServerError: 403 FORBIDDEN "learning_session:record permission is required"`;
  `isError:true`, `error_code:UNAVAILABLE`. No Grade or tutoring was invented.
- Read the ordinary tracker and recall-history endpoints after this refusal,
  at `2026-10-28T22:56:03.334270+00:00`. The full tracker and all seven history
  entries match the fresh baseline. Retained after-observation:
  `/tmp/seed090-empty-continue-m8jrtgi5-after-refusal.json`. Public health still
  reports production revision `0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c`,
  and authorization metadata still advertises both scopes. Unchanged state
  after refusal does not establish the promised successful empty-Report path.
- Read the existing Donut plugin settings in Chrome. They show the production
  `/api/mcp` endpoint, OAuth, and Terry's Donut account. The account menu has
  **Reconnect**. Opening it and selecting **Connect Donut** reached a loading
  state; native Chrome then exposed
  `https://chatgpt.com/auth/login?next=%2Fconnector%2Foauth_callback` with an
  empty page body. The browser tab inspection timed out, and a screenshot
  attempt yielded a blank image; the native screenshot attempt also timed
  out. No Donut consent, actual requested scope string, successful OAuth grant,
  or access expansion was observed. A later native observation showed that
  the user tab had moved to **Skills | ChatGPT**; no further UI action was taken.
  This inspection does not establish why the connection lacks recording scope.
- The next required evidence is the actual Donut consent/request scope set for
  this existing connection. Repeating tool refresh or the same failed Report
  cannot establish it. No deployment, client registration, production data
  reset, or personal-token recording was attempted. The existing execution
  checkout and uncommitted evidence remain available; the CI observer remains
  stopped with pending CI unobserved, as recorded by its earlier stop receipt.

## Owner-observed read-access consent — 2026-10-29 (Asia/Singapore)

- Terry supplied `/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/codex-clipboard-0102f96f-9623-41a1-b8ac-0cea5cce1424.png`
  and reported clicking **Allow**, then seeing the connection complete. The
  screenshot is Donut's consent UI. Its heading reads **Allow Donut in ChatGPT
  to read your notebooks?**, names Donut account **Terry**, and describes
  notebook access and renewal. It contains neither the recording clause nor
  the paragraph about saving Feedback and updating learning schedules. This
  establishes the observed reading consent, not informed recording consent.
- Source inspection: `AssistantConsentController.getAssistantConsentContext`
  returns the pending authorization request's scopes. `AssistantConsentPage.vue`
  displays the recording clause only when that set contains
  `learning_session:record`. The screenshot and repeated missing-scope refusal
  therefore support a read-only authorization journey; the literal authorization
  request scope string and served frontend asset revision were not captured.
- The existing personal ChatGPT setup guide instructs operators to keep
  `notes:read` as the default scope. This is a candidate setup cause, not an
  observation of this plugin's current advanced OAuth settings. Repeating that
  same consent cannot be treated as a recording grant.
- A further read-only comparison found that `RecordLearningSessionTool` has no
  per-tool `securitySchemes` metadata and throws a missing-scope 403 without an
  `mcp/www_authenticate` tool-result challenge. Official OpenAI authentication
  documentation fetched on 2026-10-29 requires tool auth metadata and that
  challenge to trigger tool-level OAuth linking:
  [Triggering authentication UI](https://developers.openai.com/plugins/build/auth#triggering-authentication-ui).
  This is a source-level interoperability concern, not proof of which scopes
  the installed plugin requests. The earlier statement that no release could
  be needed was too broad: the tools and recording enforcement are deployed,
  but that does not establish a complete ChatGPT permission-upgrade journey.
  No implementation repair, deployment, client activation, further Report,
  or permission change was performed during this inspection.
