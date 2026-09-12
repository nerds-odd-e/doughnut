# Bring up isolated E2E stacks from long checkout paths

Status: done.
Source: [SEED-015 story 9](../../seeds/SEED-015-concurrent-worktree-environments.md#story-9).
Historical finding: `a7e0fe1dc4`; its deleted quick/107 publication-verification
plan is provenance, not this plan. Number allocated after highest present
quick entry 106, using the current allocation convention.

## Outcome and boundaries

Developers and AI coordinators can run a supported `pnpm cy:run --spec …`
invocation in an otherwise supported long-path worktree, with no checkout move,
rename, or socket override. Socket location must not weaken checkout ownership,
duplicate-start refusal, runner leases, owned shutdown, or peer isolation.
Clean shutdown and dead-owner recovery must permit another invocation.

Do not restore removed public `pnpm sut`/restart commands. Preserve current
runner-owned lifecycle and supported multi-feature behavior. No new concurrency
capability, Cloud VM/CI changes, arbitrary filesystem-limit guarantee, or
Production/Development data operations. No application/API change is needed.

## Existing solution and selected change

PFE: `scripts/sut-owner.mjs` owns claim/reclaim/release, and
`scripts/sut-owner-control.mjs` reads token + controlPath from owner.json.
`sut-owner-control-server.mjs` binds that address; `sut-start.mjs` passes it to
the supervisor. `e2e-runner.mjs` already owns startup, lease, and shutdown.
Change this existing ownership mechanism; do not add a registry or transport.

`worktree-identity.mjs` owns durable database/port identity, not transient owner
endpoint lifetime. `sut-e2e-port-claims.mjs` uses tmpdir for port arbitration;
its shared claim root has different ownership semantics and tmpdir can itself
be long on macOS. MySQL socket paths belong to database services, not SUT
control. These are not suitable endpoint owners.

Selected approach: allocate an exclusive private short directory with Node
mkdtemp under `/tmp` for each acquired SUT owner, with `owner.sock` inside.
Keep checkout-local lock arbitration and owner.json as the discovery authority.
A stable socket filename derived from checkout identity is unnecessary: the
record already distributes the address. Preserve token authentication.
Use the short lexical path (do not expand it to a potentially longer realpath).

Claim, dead-owner recovery, and normal release must manage that private endpoint
as one resource. Remove only the recorded owned socket/directory, never a shared
parent or another checkout's resources; handle existing checkout-local socket
records without recursively deleting the checkout. Keep live old-format owners
discoverable through their recorded address and refuse duplicates. Preserve
supervisor/wrapper repeated-release behavior and held ownership semantics.
Inspect both release callers before changing ordering; stop owned processes
before making the claim available again. Update fixtures that otherwise leave
new external directories behind.

Direction: PRODUCT-BACKLOG.md's concurrent isolated verification. Relevant
Accepted decisions: [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
(prove ownership, preserve environment boundaries) and
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
(surface unexpected failures). No conflict or new North Star topic is needed.

## Representative infrastructure evidence

On 2026-09-12, macOS / Node v26.7.0 through the Nix runtime: a real bind at
132 bytes failed with EINVAL; a 41-byte `/tmp` endpoint bound successfully.
The disposable directory was removed. This confirms the OS assumption only,
not application acceptance. Reproduction command:

```sh
CURSOR_DEV=true nix develop -c node --input-type=module <<'JS'
import net from 'node:net';
import {mkdtemp, mkdir, rm} from 'node:fs/promises';
const root = await mkdtemp('/tmp/donut-socket-proof-');
const deep = root + '/' + 'x'.repeat(90);
await mkdir(deep);
async function bind(p) {
 const s = net.createServer(c => c.end());
 try { await new Promise((resolve,reject)=>{s.once('error',reject);s.listen(p,resolve)}); return 'bound'; }
 catch(e) {return e.code;}
 finally {if(s.listening) await new Promise(r=>s.close(r));}
}
try {console.log(JSON.stringify({platform:process.platform,node:process.version,longBytes:Buffer.byteLength(deep+'/owner.sock'),longResult:await bind(deep+'/owner.sock'),shortBytes:Buffer.byteLength(root+'/owner.sock'),shortResult:await bind(root+'/owner.sock')}));}
finally {await rm(root,{recursive:true,force:true});}
JS
```

## Ordered slice

### 1. Run an owned E2E invocation independently of checkout path length
Type: Behavior
Status: done
Behavior: Given an otherwise supported long-path isolated checkout, when the
E2E runner starts its SUT and completes its selected feature, then the invocation
can finish with its owned resources settled and other checkouts undisturbed.

Start with a failing long-path regression at the existing runner/lifetime
boundary, reusing `e2e-runner.test.mjs` and `sut-isolated-fixtures.mjs` real owner
server/process stand-ins. Wait for real owner readiness; a spawn spy alone does
not prove socket binding. Extend existing observable tests instead of exporting
path helpers solely to test them. Implement allocation and all affected cleanup
in the same slice so no delivered version leaks the relocated socket.

Proof ownership (all owned by this slice):

| Promise | Observable proof |
| --- | --- |
| Long path, no override | Runner/lifetime fixture with old socket path over 104 bytes reaches real owner readiness and completes |
| Independent checkouts | Two owners at distinct path depths answer authenticated requests; settling one leaves the peer reachable |
| Duplicate refusal | Second start in a live checkout refuses and original owner remains reachable, including an existing local-socket owner record |
| Lease protection | Shutdown during a held lease refuses; after release, owned shutdown succeeds |
| Cleanup and reuse | Normal shutdown, failed startup, and dead-owner recovery remove only owned endpoints and permit a new claim; live peer survives |
| Existing workflows | Existing runner, start/release, browser-isolation, and retirement-admission tests remain green |
| User entry point | One supported feature completes through pnpm cy:run from an actual long-path linked checkout, then owned SUT is stopped |

Focused verification (pass, 86 tests, after post-change refactor):

```
CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs scripts/sut-isolated-start.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/browser-worktree-isolation.test.mjs scripts/sut-retirement-admission.test.mjs
```

Live proof (2026-09-12): disposable long-path linked checkout
`/tmp/donut-107-live-proof/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep/deep`
(old socket path 115 bytes). Command:

```
SUT_TIMEOUT_MS=360000 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/worktree_note_editing.feature
```

Result: pass (1 scenario). Owned SUT stopped afterward (`verifyLiveSutOwner` not ok; lock dir gone). Disposable worktree retired; shared MySQL 3309 / Redis 6380 and unrelated checkouts left untouched.

Estimate was 8–10 minutes active work; actual ~12 minutes excluding Nix/service startup and Cypress runtime. See Learnings.

Safe stop: the whole ownership lifecycle works with the new endpoint; no
preparation-only or test-only delivery boundary. No extra Structure slice:
the current owner record already supplies the required indirection.

## Delivery and execution identity

On separately authorized execution, apply dough-execute-plan: inspect origin
branch/index; commit the exact Backlog-list → Taken claim first; create the
execution worktree/branch using project conventions and retain origin,
execution location, and integration target here before delegation.

Planned-execution identity (claimed 2026-09-12, Taken commit `c8a0e2f934`):

| Role | Checkout | Branch |
| --- | --- | --- |
| Originating | `/Users/terryyin/git/doughnut` | `main` |
| Execution | `/Users/terryyin/git/d107-checkout-independent-sut-sockets` | `execute/107-checkout-independent-sut-sockets` |
| Integration target | `/Users/terryyin/git/doughnut` | `main` |

Each delivered slice requires Jidoka, a fresh dough-post-change-refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, plan proof update,
check-only commit hook, commit/push, and the host's asynchronous CI handling.
No generated API trigger is expected. Preserve unrelated working-tree changes.
Retain this plan for retrospective/wrap-up after completion.

## Concern assessment

One coherent endpoint ownership model; no special long-path fallback branch.
The cross-process release order is the sizing risk, but its existing callers
are identified and all cleanup ships in the same bounded change. The live
feature run remains required proof, with external runtime excluded as above.
No additional slice-specific concern requiring a separate refinement pass was
identified in this assessment; revisit if implementation invalidates this sizing.

## Learnings

Slice 1 completed as the one authorized Behavior. Active work was about 12
minutes excluding wait-time exceptions, over the 10-minute hard limit. The
overrun was leftover-endpoint fixture cleanup required so the relocated socket
does not leak, not a second outcome. No remaining slice to refine; record the
overrun rather than splitting a delivered allocation+release change.

CI observation is unavailable for this execution branch: `.github/workflows/ci.yml`
(`donut CI`) is push-triggered only on `main`. Host probe printed a
`CI_OBSERVER` receipt without `CI_MONITOR_READY`. Do not claim CI coverage.
