# OAuth correction evidence

[Active plan](PLAN.md) · [Earlier execution evidence](execution-evidence.md) · [Connection follow-ups](connection-evidence.md)

## Slice 6 accepted boundary proof — 2026-10-29

Trigger/setup → expected → observation → owner: a consent-issued read-only
OAuth token invokes Report; the MCP result must request both recording scopes
without changing learning state. `AssistantLearningSessionTest` now observes
the exact `result._meta["mcp/www_authenticate"]` insufficient-scope challenge,
canonical resource metadata, zero recall logs and unchanged due date. Its
upgrade example consumes the challenge scopes through the real authorization,
consent-context, Allow and token-exchange handlers, records Feedback, and then
observes the old token being challenged with only one Feedback in history.
`McpEndpointTest.ProtocolNegotiation.initializesAndListsRegisteredToolsThroughServlet`
observes all five descriptors through `/api/mcp`, requiring read scope for the
four read/Request tools and both scopes for Report. The Java SDK exposes these
through the documented `_meta.securitySchemes` compatibility field.

- Implementation agent `/root/oauth_scope_repair` reproduced the mismatch with
  `./scripts/run.sh pnpm backend:test_only`: exit 1, 2981 tests completed,
  two expected failures and two skipped; 1m16s. After the correction the same
  complete command exited 0 in 1m12s. XML totals: 2981 tests, zero failures,
  zero errors, two skipped. Parent inspected the changed product, fixture
  flow and observing assertions, rather than inferring success from test names.
- Existing `AssistantConsentTest` checks exact request/consent scope sets;
  denial and CSRF proof ran. Existing `LearningSessionRecordMcpEndpointTest`
  covers personal-token recording, partial and empty Reports and notebook
  ownership. No controller DTO or OpenAPI contract changed; API generation is
  not triggered. No schema change is needed.
- Consumer trace command:
  `./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/verify-commissioned-learning-chatgpt --base c51f7b619a --term securitySchemes --term mcp/www_authenticate --term learning_session:record`.
  The filled consumer dispositions are in `/tmp/seed090-oauth-consumer-trace.md`.
  All affected backend/MCP consumers ran in the complete suite. Vue's consent
  context, SDK dual-scope/renewal fixture, Swagger/OpenAPI namesakes and workflow
  readers retain their existing contracts; their individual disposition reasons
  identify why the changed MCP metadata/challenge does not reach those boundaries.
- Proof workload was the prepared local execution checkout, host load around
  four, with no other known owned backend verification. Complete backend suite
  time was about 75 seconds. Subsequent proof is reconsidered only if refactor,
  reconciliation or another change invalidates the accepted boundary.
- These observations accept the source commitments allocated to slice 6 only.
  Actual ChatGPT metadata refresh, recording consent and production Report
  remain open in slices 1–5 after release. The private read-only consent
  screenshot remains a failed-journey observation, not recording consent.

## Release and post-release observations — 2026-10-29

- Reviewed correction commit `358ad5c0767a179134ff295b1bcfdfc43959f736`
  published to the execution branch and integrated unchanged on `origin/main`.
  The clean default checkout fast-forwarded. The guarded fresh preparation's
  first attempt refused trailing blank lines in three evidence files; those
  mechanical issues were corrected before successful commit/publication.
- The independent refactor returned `## REFACTOR COMPLETE`: no product edits;
  cohesive planning-only evidence extraction, approximately 15 active minutes.
  The existing backend proof remained valid. This overran the five-minute
  refactor hypothesis; the remaining product correction should keep evidence
  maintenance separate and bounded rather than repeat that broad pass.
- Main CI run 40789602019, attempt 1, passed every job at this exact commit.
  Execution-branch CI run 40789533523 also passed. The three required, unexpired
  artifacts were present: backend jar, frontend-dist and cli-dist.
- Published immutable annotated tag `v1.3.61`; raw tag object
  `c12c4d431798faf9e18c302a83664d6e74e82bf4`, release/build SHA both
  `358ad5c0767a179134ff295b1bcfdfc43959f736`. Application Release run
  [40790678996](https://github.com/nerds-odd-e/doughnut/actions/runs/40790678996)
  selected this tag and CI run/attempt. Admission succeeded; publication job
  123175055587 succeeded in 5m54s. The rollout temporarily served backend 503;
  its target template was reached and health recovered. The job observed HTTP
  200 with the selected build and wrote the successful deployment record.
- Production smoke at `2026-10-29T00:03:31.893221+00:00` observed that build,
  frontend index and script bytes matching the exact CI artifact, CLI bytes
  matching its artifact, and all five live tool descriptors. Four declared
  read scope; Report declared both scopes in `_meta.securitySchemes`. Retained
  private inspection files: `/tmp/seed090-production-smoke.json`,
  `/tmp/seed090-release-admission.log`, `/tmp/seed090-release-publication.log`.
  No Report was written through the personal credential used for tool listing.
- Captured seven-entry history/tracker baseline at
  `2026-10-29T00:03:51.662902+00:00`, in
  `/tmp/seed090-post-release-empty-baseline.json`. Retried the previously
  authorized notebook-15 empty Report through the connected plugin. It returned
  `Authentication for Donut was requested and accepted. Retry this tool call now.`
  with `isError:true`; the subsequent retry returned the same result. Neither
  result confirms report recording or zero recorded items. Terry then asked to
  wait while disconnecting/reconnecting; no further Report call was made.
- The CI observer was stopped on that wait: directory
  `/tmp/dough-ci-501/watch-f9raBS`, terminal `stopped`, coverage `ended`, pending
  CI `unobserved`, recorded/delivered/unread all zero. Stream cell 31 completed;
  elevated `ps` confirmed PID 35943 exited. Independently observed successful CI
  runs above remain valid, but the observer itself recorded no delivered events.
- Terry next supplied
  `/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/codex-clipboard-22b7164d-a530-46dd-9970-54d8c15060af.png`
  and reported allowing the new connection. It still shows only **Allow Donut
  in ChatGPT to read your notebooks?**, without recording consent. This leaves
  real recording authorization unverified after the successful deployment.
- The assistant opened the existing ChatGPT app settings and ran **Refresh
  tools**. The disabled loading button returned to enabled; no success toast or
  actual stored scope values were exposed. The details UI lists one write and
  four read tools, but exposes no OAuth scope settings. Export download did not
  yield a file path; browser policy blocked the download-management URL. No
  alternate download surface or filesystem workaround was attempted.
- Reassessment: OpenAI's reference describes `_meta.securitySchemes` as a
  compatibility mirror and shows a primary `securitySchemes` declaration too.
  The first automated test observed only the mirror, so that proof does not
  establish the primary field or actual ChatGPT scope selection. Reopened
  slice 6 with G4 for the missing primary field. This is an evidenced wire gap,
  not a claim that it is the sole cause of the installed app's read-only scope.

## Primary descriptor correction — 2026-10-29

- Reproduction at base `358ad5c0767a179134ff295b1bcfdfc43959f736`:
  `./scripts/run.sh pnpm backend:test_only` exited 1, 2981 tests, one expected
  missing-primary-field failure, two skipped. The servlet assertion expected
  five `$.result.tools[*].securitySchemes[0].type` values and found none.
- PFE: reuse `McpToolAuthorization` as the sole scope owner and the existing
  `McpServerConfiguration` response handler as the SDK wire seam. The SDK's final
  Tool record has no primary field, while its ListToolsResult response can be
  mapped using the SDK mapper. The bounded adaptation retains every SDK field
  and projects each tool's existing metadata into primary securitySchemes.
  No SDK upgrade, parallel domain DTO or authorization-store change is needed.
- The same full backend command then exited 0: 2981 tests, zero failures/errors,
  two skipped, 1m22s test stage plus migration. The canonical servlet assertion
  now observes primary declarations, exact mirror equality, four read scope
  sets, both Report scopes, and standard Java SDK JSONRPCResponse→ListToolsResult
  decoding. Existing challenge/consent/Feedback/original-token proof passed.
  Logs: `/tmp/seed090-primary-descriptor-red.log` and
  `/tmp/seed090-primary-descriptor-green.log`.
- `/root/oauth_primary_descriptor` returned the uncommitted correction. Parent
  inspected the configuration, mapper seam, servlet assertions and documentation.
  Consumer trace `/tmp/seed090-primary-consumer-trace.md` follows Java and TS SDK
  parsing through Cypress discovery. The TS SDK strips unknown primary fields
  but keeps its existing names/metadata; exact-base main CI 40789602019 supports
  unchanged journeys, and current standard SDK decoding observes the new field.
- Fresh `/root/oauth_primary_refactor` returned `## REFACTOR COMPLETE`, no edits,
  no uncovered code consumers, approximately five active minutes. It reused
  accepted proof, ran whitespace checking and launched no additional heavy tests.
- The installed app's actual scope defaults remain unexposed. Official OpenAI
  refresh guidance, fetched again on this date, explicitly requires a new
  conversation after auth-metadata refresh. Earlier same-chat authentication
  retries do not establish that fresh-context journey. Asked Terry whether to
  create a fresh verification chat after the forward release; no new chat is
  created without that authorization. No new client registration is proposed.

## Forward release and fresh-context verification — 2026-10-29

- Corrected commit `8e90688d04aa2ef7a6a2137fea14797a89e8123f` passed guarded
  formatting/agent commit and published to the execution branch, then integrated
  unchanged on main. The clean default checkout fast-forwarded. Main CI
  [40796058190](https://github.com/nerds-odd-e/doughnut/actions/runs/40796058190),
  attempt 1, passed every job; execution-branch CI 40795965781 also passed.
- All three unexpired artifacts were confirmed before tagging. Immutable
  `v1.3.62` has raw tag object `825d8a0a54bdae9a9b45c603a37cca67b694c080`;
  release/build SHA both `8e90688d04aa2ef7a6a2137fea14797a89e8123f`.
  [Application Release 40796899837](https://github.com/nerds-odd-e/doughnut/actions/runs/40796899837)
  selected that identity and CI attempt. Admission succeeded and publication
  job 123195649747 succeeded in 6m11s. Its log records the target template
  reached, HTTP 200 with the selected build, and successful deployment record.
- Smoke at `2026-10-29T01:00:38.039265+00:00` confirmed selected backend health,
  frontend index/script and CLI bytes matching the exact artifacts, all five
  tool descriptors, and primary securitySchemes equal to the metadata mirror.
  Report declares `notes:read` and `learning_session:record`; four other tools
  declare reading alone. Evidence files: `/tmp/seed090-production-smoke-v62.json`,
  `/tmp/seed090-v62-release-admission.log`,
  `/tmp/seed090-v62-release-publication.log`. Personal credentials were used
  only for read-only tool listing and state inspection, never Report recording.
- After deployment, the assistant ran **Refresh tools** on the existing app
  again. The loading control returned to enabled. Screenshot:
  `/tmp/seed090-v62-refreshed-plugin.png`. This UI does not expose stored OAuth
  default scopes or prove that a conversation has loaded changed auth metadata.
- Terry answered **Yes, create a verification chat** to the explicit
  fresh-context question. Created **Verify Donut recording permission**, thread
  `01a1852d-cd35-7273-8a16-d324e2b533ba`, local host, after deployment and refresh.
  Its initial instruction authorizes the matching Japanese Request and empty
  Report only, no tutoring or Grades, no automatic further session, one retry
  after an authentication-accepted result, then a stop on an authentication loop.
  Any expanded consent remains Terry's action. It reads no CLI credentials.
- Parent's before-state: `/tmp/seed090-v62-fresh-empty-baseline.json`, observed
  `2026-10-29T01:01:36.973679+00:00`, tracker 130239 with seven history entries.
  Parent owns the full tracker/history comparison after the new chat's result.
  Creating the chat does not itself accept any learning-session example.
- The fresh chat requested `request_learning_session({"timezone":"Asia/Singapore"})`.
  Its native status then reported `waitingOnApproval`. The available turn read
  contains the tool marker but no Request result body or Report result, so it
  does not yet establish either a matching Request or recording permission.
  Parent asked Terry to approve the built-in prompt in the new chat. The actual
  OAuth consent scope/wording and the empty Report result remain unverified.
