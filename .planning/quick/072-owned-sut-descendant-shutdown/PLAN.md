# Owned SUT descendant shutdown

Source: [SEED-015 story 2b](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2b),
correcting the supporting ownership change delivered during quick/070.
Status: done.

## Goal and scope

Developers can stop their isolated SUT without leaving its owned forked backend
running, so restart can reuse the allocation while a peer remains usable.
Correct the existing verified-owner shutdown used by `pnpm sut:restart` in
local Nix worktrees. Retain ownership through parent exit and bounded
escalation. Preserve private mock cleanup callers, same-group shutdown, and
current restart refusals.

Exclude database retirement, new registries or control protocols, listener-based
adoption, already-orphaned child recovery, supervisor hard-kill recovery,
processes newly forked during shutdown, PID-reuse hardening, continuous ancestry
tracking, CLI/MCP expansion, and Cloud VM/CI changes. No new command or UI.

## Outside-in proof and ordered slices

### 1. Stop forked children even when the parent exits first
Type: Behavior
Status: done
Proof: `scripts/sut-isolated-restart.test.mjs` cooperative owned-backend restart
case — captured parent/backend PIDs gone, port bindable, peer responding at
start callback; same-group and busy/stale-owner refusal retained.

### 2. Complete bounded shutdown when a forked child ignores termination
Type: Behavior
Status: done
Proof: Same restart boundary with TERM-acking, still-listening backend —
escalation exits the child and rebinds the port before start; final failed wait
throws. Shared-caller regressions:
`scripts/isolated-cypress-openai-mock-cancel.test.mjs`,
`scripts/isolated-cypress-openai-mock-failure.test.mjs`,
`scripts/sut-services-child-exit.test.mjs`,
`scripts/sut-isolated-start-release.test.mjs`.

## Current decisions

- Capture owned descendants before SIGTERM; reuse that set through SIGKILL.
- One shared stop promise coordinates owner-control shutdown and the services
  close callback.
- Unsuccessful bounded wait after SIGKILL fails loudly (ADR 0006).
- Production shutdown never discovers targets by port.
