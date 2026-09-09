# Release the private OpenAI mock after Cypress

Source: [SEED-015 story 3](../../seeds/SEED-015-concurrent-worktree-environments.md#story-3).
Status: done.

## Goal and scope

Developers and AI tasks can finish an isolated OpenAI Cypress run without an
owned private Mountebank process or runner lease surviving the run. Preserve
the Cucumber preprocessor's lifecycle behavior, the CLI screenshot sink, the
four-spec isolated allowlist, cancellation and mock-failure cleanup, and peer
ownership refusal.

This is the missing successful-completion part of Story 3's existing lifecycle
contract. It does not add mock support, persistent mock ports, a generic Cypress
plugin framework, new recovery/adoption behavior, or cleanup of foreign
processes.

## Current decisions

Cypress keeps one server callback per event. Isolation registered `after:spec`
and `after:run` cleanup first; Cucumber and the screenshot sink registered later
and replaced it. Composition stays at `setupNodeEvents`: wrap `on` so every
handler for those completion events runs and returned promises settle. Do not
drop Cucumber or screenshot handlers.

## Outside-in proof and ordered slice

### 1. Finish an isolated Cypress run with no owned mock left behind
Type: Behavior
Status: done
Proof: `composeCypressPluginEvents` at the Cypress node-event boundary; focused
node tests plus live completion and paired OpenAI reset on two owning worktrees.

Behavior: An isolated worktree runs the supported OpenAI completion feature
successfully -> Cypress completes its spec/run lifecycle -> all same-event
handlers settle, the owned mock is stopped, and the checkout can immediately
acquire its runner lease for another run.

Focused verification (pass):

```bash
CURSOR_DEV=true nix develop -c node --test \
  scripts/isolated-cypress.test.mjs \
  scripts/isolated-cypress-openai-mock.test.mjs \
  e2e_test/config/composeCypressPluginEvents.test.mjs
```

Live proof (2026-09-10), both SUTs left answering, no owned pidfile, no owned
`mb` process, `runnerLeaseHeld: false` after each command:

| Role | Path | Origin | SUT pid / group |
| --- | --- | --- | --- |
| Execution | `/Users/terryyin/.cursor/worktrees/doughnut/093-release-private-openai-mock` | `http://127.0.0.1:59759` | 80342 / 80365 |
| Peer | `/Users/terryyin/.cursor/worktrees/doughnut/093-peer` | `http://127.0.0.1:59749` | 4912 / 4935 |

Commands: ordinary `pnpm cypress run --spec e2e_test/features/ai_generated_content/note_content_completion.feature` (2 passing); paired `node scripts/worktree-reset-isolation-harness.mjs --mode openai-mock` both directions (each 1 passing per checkout). Shared primary `mb` pid 3434 in `/Users/terryyin/git/doughnut` was left untouched.

## Learnings

A worktree path that makes `.sut.local.lock/owner.sock` exceed macOS's ~103-byte
unix-socket limit causes `pnpm sut` to exit immediately with no new `sut.log`.
Shorten the checkout path rather than treating that as a mock-leak.
