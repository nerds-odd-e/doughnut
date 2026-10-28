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
