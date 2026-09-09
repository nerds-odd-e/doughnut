# Prove isolated CLI peer safety, then admit MCP E2E workflows

Sources: [SEED-015 Story 4](../../seeds/SEED-015-concurrent-worktree-environments.md#story-4)
and [Story 5](../../seeds/SEED-015-concurrent-worktree-environments.md#story-5),
plus completed quick/088 (recover its plan at `419135973b`;
merged by `c3c2a5d3a9`).
Status: done.

## Outcome

The allowlisted CLI clone/pull/publish workflow now has paired reset-isolation
proof and documented commands. Isolated Cypress also admits
`mcp_services.feature`: tool calls use that worktree's origin and notes,
disconnect awaits SDK `close()` so the spawned server exits, mixed specs still
refuse, and an active run still vetoes `pnpm worktree:retire --check`.

## Completed slices

1. CLI notebook state survives a peer worktree reset — done.
   Proof: `pnpm test:browser-worktree-isolation` passed.
   Live: `node scripts/worktree-reset-isolation-harness.mjs --mode cli --peer /Users/terryyin/git/doughnut-089-isolated-mcp-e2e --resetter /Users/terryyin/git/doughnut-089-browser-resetter`
   (peer 4/4, resetter 1/1, reset after seed, uncrossed notebooks).
2. Await bounded MCP server child cleanup on disconnect — done.
   Proof: `cli/node_modules/.bin/tsx --test e2e_test/support/mcp_client.test.ts`
   passed (child gone before disconnect returned). Primary
   `pnpm cy:run-on-sut --spec e2e_test/features/mcp/mcp_services.feature`
   passed 5/5.
3. Admit the MCP services workflow into isolated concurrent runs — done.
   Proof: `pnpm test:browser-worktree-isolation` passed (MCP single-spec
   admitted; MCP+note-editing refused). Live overlapping
   `pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature` in the
   execution and resetter worktrees with distinct `PeerMcpAlpha` /
   `ResetterMcpBeta` markers (6/6 each, uncrossed, children exited).
   `pnpm worktree:retire --check` refused with `busy Cypress runner lease`.

## Learnings

- CLI harness mode needs per-role specs (`peerSpec` / `resetterSpec`).
- Distinct MCP note markers plus a two-file seeded handshake are required;
  identical default CS-concepts fixtures cannot prove uncrossed data.
- awaiting story review: [SEED-015 Story 4](../../seeds/SEED-015-concurrent-worktree-environments.md#story-4)
  and [Story 5](../../seeds/SEED-015-concurrent-worktree-environments.md#story-5)
  still say ready-for-planning; both outcomes this plan closed may now be
  delivered.
