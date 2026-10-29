# Fresh ChatGPT authorization and empty Report — 2026-10-29

## Environment and provenance

- Origin: `https://doughnut.odd-e.com`; production release `v1.3.62`, build
  `8e90688d04aa2ef7a6a2137fea14797a89e8123f`. Terry's existing account/client;
  timezone `Asia/Singapore`. No new client was activated.
- Parent reviewed the actual recent turns of **Verify Donut recording permission**,
  thread `01a1852d-cd35-7273-8a16-d324e2b533ba`, and **Verify Donut empty report
  after consent**, thread `01a18595-c857-7420-8a4e-a41078c8cf65`, on the local host.
  The second chat was explicitly requested by Terry in the first; parent only
  inspected it and did not send additional instructions or make another Report.
- The original matching Request used `request_learning_session` with
  `{"timezone":"Asia/Singapore"}` and returned notebook 15, 日本語学習,
  sole item なにしろ: note 1297, tracker 130239. Terry selected immediate end;
  there was no tutoring and no completed item or invented Grade.

## Actual scope mismatch and informed authorization

- Terry explicitly instructed the first verification chat to inspect server logs
  and fix the mismatch, stopping before consent. That chat's turn
  `01a1857d-5a37-78b2-9c27-7414edbdba45` records the investigation and correction.
- Load-balancer observations at `2026-10-29T01:25:10.125009Z` and
  `2026-10-29T01:09:39.645213Z` show actual scope string
  `learning_session:record notes:read`, canonical MCP resource, and the same
  client fingerprint `97130bd51a6c`.
- Read-only registration inspection found **Donut in ChatGPT** allowed only
  `notes:read`. The authorized correction updated exactly one existing client
  to `notes:read,learning_session:record`; an independent read confirmed it.
  This resolves the observed `invalid_scope` without another code release.
  The earlier parent gcloud reauthentication stop is superseded by this proof.
- The chat reached Donut consent and stopped before Allow. Parent inspected
  its screenshot at
  `/Users/terryyin/Documents/Codex/2026-10-29/use-donut-plugin-dev-6ae1514907ec8191b8cf93be18b67c47-created/outputs/donut-recording-consent.jpg`.
  Heading: **Allow Donut in ChatGPT to read your notebooks and record learning
  sessions?** Body explicitly describes saving Feedback and updating schedules
  for completed items. Terry subsequently said **I clicked Allow. Retry the
  empty Report test and confirm it returns zero recordedItems.**
- The first chat's stale routing still returned `Unknown tool`, including after
  Refresh tools. Terry then explicitly requested the second fresh verification
  chat. Earlier failures remain in the preceding evidence; they are not successes.

## Empty Report and unchanged learning state

- The first handoff HTML-escaped its delimiters. That native call returned
  `isError:false`, zero recorded items, but two parse rejections. It is retained
  as a malformed attempt and does not prove the clean empty-Report example.
- Corrected native call in the second chat, turn
  `01a18597-4f94-7a03-8eb7-8ce01c5c28ca`, completed in 7034 ms. Exact arguments:

```json
{"notebookId":15,"reportMarkdown":"# Learning Session Report\n\n<session_item_feedback>\n</session_item_feedback>\n"}
```

- The tool's reported actual result was `isError:false`, `recordedItems:[]`,
  `rejectedEntries:[]`. Parent inspected the completed native tool marker,
  exact raw-delimiter arguments and the originating chat's structured result;
  the thread reader exposes that reported result, not a separate raw MCP body.
  No additional Request or successful Report was replayed to gather proof.
- Before-state `/tmp/seed090-v62-fresh-empty-baseline.json` was captured at
  `2026-10-29T01:01:36.973679+00:00`. After-state
  `/tmp/seed090-v62-fresh-empty-after-success.json` was captured at
  `2026-10-29T03:13:36.947701+00:00`. Read-only GETs of tracker 130239 and its
  recall-history used an existing personal credential kept in memory; all
  Report submissions used the native Donut plugin.
- Complete tracker JSON and complete seven-entry history match the baseline.
  Thus every returned item's history and schedule remain unchanged. Health at
  the after-reading still identifies build `8e90688d04aa2ef7a6a2137fea14797a89e8123f`.
  The two reads are sequential observations, not an atomic database snapshot.
- These observations accept real authorization (slice 1) and the immediate-end
  example (slice 4). Completed-item authority is still owned by slice 2's real
  learner round. Full tutoring, A-only partial completion and explicit no-session
  examples remain unobserved; no further session was automatically started.

- Fresh `/root/fresh_chat_evidence_refactor` inspected all five evidence/guide
  paths, returned `## REFACTOR COMPLETE` with no edits and no introduced gaps.
  Whitespace checks passed; no tests reran because no product boundary changed.
  Source freshness on origin/main at `3bcf2454de655ebb39673e435b340ba0230e35e8`
  was unchanged; the slice-1 check accepted four obligations and 77 commitments.
  Parent's active evidence recovery and comparison took approximately ten minutes;
  the earlier user authorization, cloud diagnosis and native calls were external
  observations reused here rather than repeated.
