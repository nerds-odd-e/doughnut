# Execution evidence

[Active plan](PLAN.md) · [Connection follow-ups](connection-evidence.md) · [OAuth correction proof](oauth-correction-evidence.md)

## Decisive premises and prior evidence

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/refine-commissioned-learning-chatgpt`,
branch `codex/refine-commissioned-learning-chatgpt`, prepared from
`0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c` on 2026-10-28.
This checkout identifies preparation evidence, not the deployment to be tested.

- The original implementation's accepted MCP/OAuth/SDK observations are retained
  at `75fece3754879b7d3f840b002d58adfa7beafc91:.planning/slice-plans/077-commissioned-learning-session-in-chatgpt/execution-evidence.md`.
  On 2026-10-28 the final backend run passed 2,981 tests; the record names
  `LearningSessionRecordMcpEndpointTest` for A/B/C and empty Reports and
  `AssistantLearningSessionTest` for consent-issued dual-scope recording.
  These are historical implementation observations, not a new test run or
  ChatGPT acceptance evidence.
- That record's credentialed attempt on 2026-10-28 observed production at
  `ff368082aedaf3c1ca8cf2449298e8ef15eb28f8`, advertising only `notes:read`.
  Its CUA browser host was unavailable and access to `com.openai.codex` was
  refused. No UI or credential action occurred. This is dated evidence, not a
  claim about the current deployment or current tool access.
- The 2026-10-28 execution probe below establishes the published production
  revision and advertised scopes. The current plugin's grant, consent and
  request/record operations remain unobserved. Slice 1 is the permitted early
  probe: establish these before slices 2–5. No user grant or functional success
  is inferred from discovery. Use the connected Donut plugin directly in this
  chat, as Terry clarified on 2026-10-28; no separate ChatGPT app is needed.

## Proof ownership and observation contract

| Source promise | Owner | Required observation |
| --- | --- | --- |
| Real ChatGPT connection requests both scopes and informed consent permits recording | 1, confirmed by 2 | Actual scope string and consent, then successful request and recording through that connection |
| Japanese's Request, timezone and tutoring instructions lead to saved Feedback | 2 | Request/tool transcript, interactive tutoring, Report response and matching Donut history |
| Ending after A saves A while B/C retain history and schedule | 3 | A/B/C Request, completed-only Report, recorded result and before/after learning state |
| Immediate end submits an empty Report and records nothing | 4 | Empty Report call/result and unchanged baseline for all session items |
| No due session is explained in ChatGPT | 5 | No-session tool result and ChatGPT's response |
| Evidence is attributable and guide steps were observed | Each slice | Dated origin/revision, preconditions, tool/UI observations, limitations and corresponding guide edit |

The assistant runs the rounds through the connected Donut plugin in this chat.
Terry supplies learning responses and handles only connection/consent UI or
Donut observations the available tools cannot expose. When owner-operated
observations are needed, provide the whole bundle of rounds below once; reuse
answered setup and earlier observations rather than asking again between rounds.
Establish the actual HTTPS origin, deployed revision, environment/owner and
usable registered connection without asking again for facts already observed.
Record the account reference without credentials and arrange each round's due
items through supported operations on owner-approved data. Save the baseline
before each write; baseline data is setup, while the actual ChatGPT call and
Donut result are the observation. A notebook used in an example imposes no
product limit on how many notebooks may exist.

For each round append retained proof to these linked execution records: promise, covered boundary,
date/revision and inspected setup, actual owner-performed steps, observation
locations, result and limitations. Include literal tool inputs/outputs and
before/after Feedback and schedule values where promised. Redact secrets, codes
and credentials. A prose success claim without the tool/history observations
leaves that proof open. Persist lasting setup/usage facts in the guide before
eventual story-history cleanup.

## Slice 1 probe evidence — 2026-10-28

Covered boundary: unauthenticated deployment discovery and the tool inventory
offered to this chat. No account setup or credentials were needed for these
read-only observations. The commands ran from the originating checkout while
execution startup prepared the owned checkout; they observe public production,
not the local checkout's application.

- `CURSOR_DEV=true nix develop -c curl --silent --show-error --fail --max-time 20 https://doughnut.odd-e.com/api/healthcheck`
  returned `OK. Active Profile: prod. Commit: 0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c`.
- `CURSOR_DEV=true nix develop -c curl --silent --show-error --fail --max-time 20 https://doughnut.odd-e.com/.well-known/oauth-protected-resource/api/mcp`
  returned canonical resource `https://doughnut.odd-e.com/api/mcp` and
  `scopes_supported:["notes:read","learning_session:record"]`.
- `CURSOR_DEV=true nix develop -c curl --silent --show-error --fail --max-time 20 https://doughnut.odd-e.com/.well-known/oauth-authorization-server`
  returned that same scope set, `client_secret_basic` and S256 support.
- Enumerating the available Donut tools in this chat returned only
  `list_note_candidates`, `read_note` and `get_note_graph`.
  Neither `request_learning_session` nor `record_learning_session` is available
  to invoke. This does not establish the connection's actual granted scopes or
  whether its registered endpoint is the observed production origin.
- Asked Terry to refresh or reconnect the plugin with both scopes. No Request,
  Report, consent, Feedback-history or learning-schedule observation has been
  obtained. Public discovery is partial probe evidence only; it does not
  establish any of the learning-session examples. No deployment or real-client
  activation was performed.
- Refresh follow-up on 2026-10-28: Terry replied "I refreshed". A new tool
  inventory in that resumed turn still exposed only the same three note tools;
  neither session tool became available. The owned checkout and Take ancestry
  were verified; the execution claim and uncommitted notes are preserved.
- Read `McpServerConfiguration.mcpStatelessSyncServer` at the deployed
  `0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c` source: it registers all five
  tool specifications without a scope-conditioned tool-list filter. This
  reading identifies the expected registration, not an authenticated runtime
  `tools/list` observation or the plugin's actual registered endpoint.
- Official OpenAI documentation, fetched on 2026-10-28, instructs plugin
  owners to open the existing plugin's MCPs, choose the server, then select
  Issues → Rescan and inspect tool availability after server changes:
  [Scan your latest changes](https://developers.openai.com/plugins/deploy/submission#scan-your-latest-changes).
  Asked Terry whether that scan exposes both session tools. This is a next
  diagnostic step, not an observed remedy or a verified operator-guide step.
- Custom-app clarification on 2026-10-28: Terry reported using **Refresh tools**
  and supplied a screenshot of the app details. The screenshot shows a dev-mode
  app at `https://doughnut.odd-e.com/api/mcp`, with OAuth supported and used,
  and the **Refresh tools** control. This establishes the configured endpoint
  and authentication mechanism, not the granted scope set or refresh result.
  The available inventory in this turn still contained only the three note
  tools. The prior Rescan advice above addressed a different management flow.
- The more applicable official
  [custom-MCP refresh guidance](https://developers.openai.com/apps-sdk/deploy/connect-chatgpt#refresh-metadata),
  fetched on 2026-10-28, says to refresh the connection, confirm changed metadata,
  and start a new conversation for the affected tests. Terry's reported
  **Refresh tools** action is the appropriate control for the shown custom
  app. A fresh-conversation tool inventory and all learning-session observations
  remain outstanding; no successful refresh or remedy is inferred.
- Direct runtime confirmation on 2026-10-28, requested by Terry to check whether
  the deployed backend was old: an authenticated HTTP POST to
  `https://doughnut.odd-e.com/api/mcp` returned status 200 for
  `{"jsonrpc":"2.0","id":42,"method":"tools/list","params":{}}`.
  The request used `Content-Type: application/json`,
  `Accept: application/json, text/event-stream`, and
  `MCP-Protocol-Version: 2025-03-26`. A temporary Python standard-library HTTP
  probe read the existing Donut CLI credential from its configured store into
  memory; the credential was not printed, copied into source, or changed.
  The response listed `list_note_candidates`, `read_note`, `get_note_graph`,
  `request_learning_session` and `record_learning_session` (five tools).
  Request requires `timezone`; Record requires `notebookId` and
  `reportMarkdown`. A concurrent GET to `/api/healthcheck` returned status 200
  and production commit `0a1b38a49584d7135413c0d3dfc3dd2bb7f8cb4c`.
  This directly establishes deployed tool registration rather than inferring
  it from source or advertised OAuth scopes. The connected plugin in this chat
  still exposes only the three note tools. Personal-token tool enumeration
  does not establish the plugin's OAuth grant or any learning-session example;
  no `tools/call` or recording operation was performed by this probe.

## Connected-plugin Request and empty-Report probe — 2026-10-28

- After the refresh/new-conversation guidance, Terry reported "worked". The
  available Donut tool inventory in this execution then exposed all five tools,
  including `request_learning_session` and `record_learning_session`.
- Called `request_learning_session` through the connected plugin with
  `{"timezone":"Asia/Singapore"}`. It returned `notebookId:15`,
  `notebookName:"日本語学習"` and the existing Learning Session Request.
  The sole Session Item is `なにしろ`, with seven previous sessions, most
  recently 2026-09-18. Its instructions say to wait for the learner before
  starting, then tutor in intermediate Japanese and report only taught items.
  A second Request under Terry's explicit follow-up returned the same item.
- Terry selected "End immediately to test an empty Report". On the separate
  consent question Terry answered "I did not check the wording"; informed
  recording consent remains unobserved rather than being inferred from tool
  availability.
- Before the recording attempt, read the ordinary production recall queue,
  tracker and recall-history APIs with the existing CLI credential. The queue
  contained only notebook 15 for commissioned learning. The item is note 1297,
  tracker 130239, with seven history entries. Its persisted schedule was
  `lastRecalledAt:2026-09-18T04:31:29.000Z`,
  `nextRecallAt:2026-10-25T13:31:29.000Z`, `stability:897.0`,
  `difficulty:8.42064`. Baseline timestamp: `2026-10-28T13:25:21.325996+00:00`.
  The credential was kept in memory and excluded from evidence.
- Called the connected plugin's `record_learning_session` once with
  `notebookId:15` and this exact `reportMarkdown`:

  ```markdown
  # Learning Session Report

  <session_item_feedback>
  </session_item_feedback>
  ```

  Actual result: `403 FORBIDDEN "learning_session:record permission is required"`.
  The connection can retrieve a Request, but its credential cannot record.
  No Grade was invented or submitted, and no tutoring was started.
- Re-read `/api/memory-trackers/130239` and
  `/api/memory-trackers/130239/recall-history` after the refusal. The complete
  tracker JSON and all seven history entries were identical to the baseline
  (`changedTrackerFields:[]`). Private observation files are
  `/tmp/seed090-empty-report-6cg4ppva-baseline.json` and
  `/tmp/seed090-empty-report-6cg4ppva-after-refusal.json`.
  This proves unchanged state after the authorization refusal; it does not
  prove successful empty-Report recording.
- Asked Terry to reconnect/re-authorize with both `notes:read` and
  `learning_session:record` and observe the consent wording. Tool refresh and
  visibility have been established; the permission and consent paths remain
  open. Do not replay a successful report; this rejected attempt recorded none.

An independent source-only review was returned by
`/root/source_review_commissioned_learning`, launched without conversation
inheritance and supplied only the selected story and explicit owner amendment.
No slice has been accepted; retain and allocate that returned review before
the next acceptance.

## Historical connection status before the OAuth repair

- All five slices remain planned. Both tool schemas are exposed, and the first
  connected Request succeeded. Selecting Donut again recovered invocation of
  the Report tool, but both subsequent empty-Report calls returned the missing
  `learning_session:record` permission refusal. The current connection's actual
  requested scopes and recording consent remain unobserved. Resume slice 1 by
  inspecting that authorization request, then verify its recording grant with
  the empty Report. No accepted slice result or story completion is claimed.

## Original execution setup receipts

- `./scripts/run.sh bash scripts/worktree_setup.sh` passed with the locked
  workspace installation current. The same checkout's
  `./scripts/run.sh node .agents/skills/dough-product-backlog/scripts/product-backlog.mjs read-state --link 'seeds/SEED-090-commissioned-learning-via-mcp.md#story-2'`
  passed and read the published preparation. Existing planned-work replanning
  authority remains; no overrun replan was selected.
