# Consumer and proof routes

## Consumers and proof routes

- Shared MCP destination resolution feeds both note-read tools; checked deliberate
  failures retain their identity through the current MCP failure helper. Their changed arguments reach
  `ReadNoteMcpEndpointTest`, `GetNoteGraphMcpEndpointTest`,
  `AssistantMcpAccessTest`, and `AssistantTokenRenewalTest`: migrate/run in 4/8.
- `ListNoteCandidatesTool` feeds `mcpAgentActions.callToolOnFirstCandidate`,
  `hostedAssistant`, and MCP step definitions. Inspect their actual request and
  observation chains, migrate read consumers in 4 and graph consumers in 8.
  Run `mcp_services.feature` and `assistant_account_linking.feature` for each
  changed contract; retain isolation, budgets, revision, renewal, and withdrawal.
- `FocusContextWikiUri` feeds retrieval/BFS/folder peers, then graph JSON and
  `FocusContextMarkdownRenderer`. Export and AI question/conversation/session builders
  consume the renderer: run full backend proof in 9; preserve content/traversal
  purposes while updating intentional address spelling assertions.
- Shared focus/related graph models reach OpenAPI and frontend generated types:
  regenerate and run frontend proof in 10. Generator-owned files are regenerated,
  not manually edited or split.
- During execution, run the installed `consumer-trace.mjs` against each current
  diff (`--base HEAD`), adding terms `read_note`, `get_note_graph`,
  `callToolOnFirstCandidate`, and the changed MCP step phrases. Record every
  located consumer as run, reused with matching evidence, or excluded with an
  effect-based reason; extend this trace when the actual implementation differs.

Commands, from the execution checkout, with Nix:

- Every backend slice: `CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
  The backend skill requires the full suite. Do not substitute filtered tests.
- Changed MCP caller journeys in 4/5/7/8/9/10:
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature,e2e_test/features/mcp/assistant_account_linking.feature`.
- Shared graph DTO change in 10: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`,
  then full backend proof against regenerated OpenAPI, plus
  `CURSOR_DEV=true nix develop -c pnpm frontend:test`,
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`, and
  `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`.
  These broader checks are required by the API-generation and frontend skills.

Slice6 actual SDK ambiguity consumer proof: run
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
after one recovery scenario is added. This drives the client with cached tool
output validators; hosted qualified success remains unchanged and is reused
with its inspected prior setup/observations.
