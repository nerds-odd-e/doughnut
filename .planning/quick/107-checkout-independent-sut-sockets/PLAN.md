# Bring up isolated E2E stacks from long checkout paths

Status: planned; planning-only instruction, implementation not started.
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
Status: planned
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

Focused command (extend existing files or add the new regression to this command):
`CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs scripts/sut-isolated-start.test.mjs scripts/sut-isolated-start-release.test.mjs scripts/browser-worktree-isolation.test.mjs scripts/sut-retirement-admission.test.mjs`.

For live acceptance, select one currently supported small feature from the
runner's current spec policy and record its literal command and result here.
Run `CURSOR_DEV=true nix develop -c pnpm cy:run --spec <selected-feature>` in the
execution checkout with the old socket address longer than 104 bytes. Use the
documented worktree setup; preserve shared services and unrelated checkouts.
If the ordinary execution path is short, use a disposable long-path linked
checkout of the implemented revision for this proof. Do not claim completion
from stand-ins alone or convert a startup failure into a passing skip.

Estimate: 8–10 minutes active work including focused proof and local cleanup;
scrutinize after 5 minutes. The indivisible change is allocation plus its release
and recovery: splitting these would deliver resource leakage. Real Nix/service
startup and focused live feature runtime are explicit wait-time exceptions.
If active work exceeds 10 minutes, stop safely, record the concrete overrun,
and refine remaining work rather than expanding this slice silently.

Safe stop: the whole ownership lifecycle works with the new endpoint; no
preparation-only or test-only delivery boundary. No extra Structure slice:
the current owner record already supplies the required indirection.

## Delivery and execution identity

On separately authorized execution, apply dough-execute-plan: inspect origin
branch/index; commit the exact Backlog-list → Taken claim first; create the
execution worktree/branch using project conventions and retain origin,
execution location, and integration target here before delegation.
No execution identity has been allocated during planning.

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
