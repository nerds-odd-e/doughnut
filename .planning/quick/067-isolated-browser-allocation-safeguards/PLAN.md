# Refuse unverified isolated browser allocations

## Source, goal, and scope

[SEED-015 story 2a](../../seeds/SEED-015-concurrent-worktree-environments.md#story-2a),
correcting the allocation/health promises of delivered story 2 (quick 061).
**Status: complete.**

Developers and AI tasks must not receive a successful isolated browser check
against unrelated listeners or a silently replaced invalid allocation.
Retain normal first-use setup, valid recorded allocation reuse, and primary
defaults. No new mock/client support, retirement, automatic repair, copied-ID
recovery, or ownership-framework redesign. Follow ADR 0006's visible failures.

## Delivered design

- Live application process group is published on authenticated owner control as
  `applicationGroupId` (omitted until the child exists / for control-only owners).
- Isolated health requires that group and verifies recorded listeners belong to
  it before TCP/HTTP readiness counts.
- Cypress setup uses that health result and refuses before fixture reset.
- Present invalid `e2e`/database/port values refuse before provisioning; only
  omitted fields are first use. Ports are all omitted or three distinct integers
  1–65535.

## Ordered slices

### 1. Expose the live application group to owning health

Type: Structure
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/sut-services.test.mjs scripts/sut-services-child-exit.test.mjs scripts/sut-isolated-restart.test.mjs scripts/sut-owner-application-group.test.mjs`

### 2. Report foreign application listeners as unhealthy

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-start.test.mjs scripts/sut-isolated-health.test.mjs scripts/sut-healthcheck.test.mjs scripts/sut-restart.test.mjs scripts/sut-owner-application-group.test.mjs`

### 3. Refuse browser verification before reset on foreign endpoints

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress.test.mjs scripts/isolated-cypress-owning-health.test.mjs scripts/sut-start-health-wait.test.mjs scripts/sut-isolated-health.test.mjs`

### 4. Refuse present invalid database allocation

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/sut-isolated-e2e-database.test.mjs scripts/browser-worktree-isolation.test.mjs`

### 5. Refuse present invalid application-port allocation

Type: Behavior
Status: done
Proof: `CURSOR_DEV=true nix develop -c node --test scripts/sut-e2e-port-allocation.test.mjs scripts/browser-worktree-isolation.test.mjs`
(pass).
