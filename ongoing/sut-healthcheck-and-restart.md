# Local SUT healthcheck and restart

**Scope:** Dev and E2E-local use the same stack (`pnpm sut`); no separate “dev vs test” health model.

**Goal:**

- `pnpm sut:healthcheck` — verify everything `pnpm sut` starts is still behaving normally.
- `pnpm sut:restart` — clear stray listeners/processes for that stack, then start `pnpm sut` again.
- **Local fake LB:** rename everything around the old **prod-topology / e2e-proxy** naming to **`lb`**. No leftover script names, paths, env prefixes, log tags, or doc phrasing that implies “E2E-only proxy.”
- Update **CLAUDE.md**, **`.cursor/rules/general.mdc`**, **`.cursor/rules/cloud-agent-setup.mdc`**, **`docs/gcp/prod_env.md`**, and any other references so humans and agents know what to run when unsure whether SUT is up.

---

## Advice: “starting” vs “down”

**Recommendation: distinguish three situations in output, keep one exit code story simple.**

| Situation | What it means | Suggested message / behavior |
|-----------|----------------|------------------------------|
| **Nothing listening** | Typical ports (see below) have no TCP listener | **Not running** — “Start with `pnpm sut` or run `pnpm sut:restart`.” |
| **Listeners up, readiness fails** | e.g. `5173` answers but readiness URL is not 200, or `5174` dead while LB expects Vite | **Unhealthy or still starting** — explain which check failed. If the user *just* started SUT, suggest waiting a few seconds and re-running healthcheck (Gradle continuous + Spring can lag). |
| **All checks pass** | — | **OK** |

**Why not only “up/down”?**  
A single binary “down” blurs “forgot to start” with “backend still booting” or “Vite crashed.” Per-service lines (mountebank, backend, local LB, Vite) plus the **readiness HTTP probe** (see below) give actionable detail without requiring a formal state machine.

**Optional refinement (later):** a short **retry loop** (e.g. 15–30s total, 1s interval) only for the readiness URL so CI/agents treat “warming up” as OK; for interactive dev, default can stay **fail fast**. Decide in implementation: one mode flag vs always quick retries with max wait.

**Exit codes (suggestion):**

- `0` — all checks passed.
- `1` — unhealthy / incomplete (listeners or readiness failed).
- Avoid a separate exit code for “starting” unless automation needs it; text + optional `--wait-ready` is enough.

---

## What `pnpm sut` runs today (single source for checks)

From root `package.json` **`sut`** (script names will become `local:lb:vite` — see rename section):

1. **`backend:sut`** — `run-p` **`backend:watch`** + **`backend:sut:ci`** → Spring **9081** (E2E profile).
2. **`start:mb`** — Mountebank **2525** (script skips start if port in use).
3. **Local LB** — **5173**, Vite upstream **5174** when using dev layout.
4. **`frontend:sut`** — Vite dev server **5174**.

**Healthcheck should align with** `docs/gcp/prod_env.md` (**Local dev / Cypress**) so ports and URLs stay one place; duplicate port numbers with a comment pointing to that doc unless a tiny shared module is worth it.

**Checks (concrete):**

- TCP **2525** (mountebank).
- TCP **9081** (backend).
- TCP **5173** (local LB).
- TCP **5174** (Vite) — required for **`sut`** dev layout.
- **HTTP** readiness on the LB (see **Readiness URL** below) → **200**.

**Out of scope for v1:** asserting Gradle watcher process, JVM sub-process trees, or Cypress.

### Readiness URL (remove `e2e` from the path)

**Today:** `GET http://127.0.0.1:5173/__e2e__/ready` (used by `wait-on`, Cypress flow, cloud-agent docs).

**Target:** Use an **`lb`-named path** everywhere, e.g. **`/__lb__/ready`** (exact string to choose in implementation — must not collide with app routes). **Implementation:** register the same handler for the new path; **remove** `/__e2e__/ready` once all repo references are migrated so there is no dual “historical” surface.

**Update consumers:** root `package.json` (`cy:run-with-sut`, `cy:run-on-sut`, any new `sut:healthcheck`), **`.github/workflows`**, **`.cursor/rules/cloud-agent-setup.mdc`**, **`docs/gcp/prod_env.md`**, **`cypress`** config / plugins if they hardcode the old path, **`wait-on`** invocations, **E2E constants** if any reference the path.

---

## `pnpm sut:restart` behavior

1. **Stop** processes listening on **5173, 5174, 9081** (see open decisions for **2525**).
2. **Start** `pnpm sut` (same as today — includes install + `cli:bundle`).

**Risks:** port-based kill can touch unrelated apps on those ports. Output should name ports and PIDs; keep implementation **simple** (e.g. `lsof` on macOS/Linux as in dev env). Nix/Linux: confirm `lsof` or equivalent is in dev shell.

**Edge case:** user runs backend on 9081 for something else — same risk as today; document briefly.

---

## Rename: **`lb`** — complete, no historical trace

### pnpm script names (root `package.json`)

| Role | New name | Command shape |
|------|-----------|----------------|
| Static `frontend/dist` + backend (CI **`pnpm test`**) | **`local:lb`** | `node scripts/local-lb.mjs` (no Vite upstream) |
| Dev **`pnpm sut`** (Vite on 5174) | **`local:lb:vite`** | `LOCAL_LB_VITE_UPSTREAM=http://127.0.0.1:5174 node scripts/local-lb.mjs` |

**Remove:** `e2e:prod-topology-proxy`, `e2e:prod-topology-proxy:dev` (no compatibility aliases).

### Implementation file

- **Move** `e2e_test/e2e-prod-topology-proxy.mjs` → **`scripts/local-lb.mjs`** (single canonical location; not under `e2e_test/`).
- **Delete** the old file path so nothing points at `e2e-prod-topology-proxy`.

### Environment variables (replace `E2E_PROXY_*` / `E2E_*` for this process)

Use one prefix, e.g. **`LOCAL_LB_*`**, read only in `scripts/local-lb.mjs`:

| Old | New |
|-----|-----|
| `E2E_STATIC_ROOT` | `LOCAL_LB_STATIC_ROOT` |
| `E2E_PROXY_TARGET` | `LOCAL_LB_BACKEND` (or `LOCAL_LB_BACKEND_URL` — pick one, document in file header) |
| `E2E_PROXY_VITE_UPSTREAM` | `LOCAL_LB_VITE_UPSTREAM` |
| `E2E_PROXY_LISTEN_PORT` | `LOCAL_LB_LISTEN_PORT` |
| `E2E_BACKEND_PATH_HINTS_JSON` | `LOCAL_LB_ROUTING_JSON` (or similar; document default path to `infra/gcp/path-routing/doughnut-routing.json`) |

**Update** `package.json` **`local:lb:vite`** to set `LOCAL_LB_VITE_UPSTREAM=...` only.

### Logs

- Replace console prefix **`[e2e-prod-topology-proxy]`** with **`[local-lb]`** everywhere in that script.

### Internal code names in the moved script

- Rename identifiers like **`CLI_E2E_INSTALL_BUNDLE`** to neutral names (e.g. **`CLI_CYPRESS_ALT_INSTALL_BUNDLE`**) if they only exist to support Cypress install scenarios — **no** `E2E` in the local LB source unless unavoidable for cross-package contracts (prefer neutral naming).

### Documentation and config (exhaustive grep pass)

After edits, **`rg`** (or repo search) must show **no** remaining:

- `e2e:prod-topology-proxy`
- `e2e-prod-topology-proxy`
- `prod-topology-proxy` (as script or filename)
- `E2E_PROXY_` / `E2E_STATIC_ROOT` / `E2E_BACKEND_PATH_HINTS_JSON` **for the LB** (other `E2E_*` for Cypress DB, Gmail mocks, etc. **stay** — do not conflate)

**Files known to update (non-exhaustive — re-grep):**

- `package.json` (`sut`, `test`, Cypress `wait-on` scripts)
- `.github/workflows/ci.yml`
- `.cursor/rules/cloud-agent-setup.mdc`, `.cursor/rules/cli.mdc`
- `CLAUDE.md`
- `README.md`
- `.gitpod.yml`
- `docs/gcp/prod_env.md`, `docs/gcp/prod-frontend-static-lb.md` (wording: “local LB” / `scripts/local-lb.mjs`, not “prod-topology proxy”)
- `ongoing/sut-healthcheck-and-restart.md` (this file — trim obsolete names when work is done)

**Cypress / E2E:** any hardcoded old path or old script name; **`e2e_test/config/constants.ts`** or similar if they document the ready path.

---

## Documentation and Cursor rules (after scripts exist)

Add a short, consistent **“Unsure if SUT is running?”** block:

1. Run **`pnpm sut:healthcheck`**.
2. If not OK, run **`pnpm sut:restart`** (or start **`pnpm sut`** in a terminal if nothing was running).
3. Do **not** ask people to restart `pnpm sut` after normal code edits (existing rule stays).

**Files to touch:**

- **`CLAUDE.md`** — Common commands table + local dev paragraph (**`local:lb`**, **`local:lb:vite`**).
- **`.cursor/rules/general.mdc`** — same guidance near `pnpm sut`.
- **`.cursor/rules/cloud-agent-setup.mdc`** — VM instructions: **`local:lb`**, new readiness URL, **`pnpm sut:healthcheck`** when debugging.
- **`docs/gcp/prod_env.md`** — single write-up: ports, **`scripts/local-lb.mjs`**, env vars, readiness URL.

---

## Phased delivery (value first)

1. **Phase 1 — Healthcheck**  
   Implement `pnpm sut:healthcheck` (readiness URL = **`/__lb__/ready`** after Phase 3, or temporary old path + switch in same PR as rename — prefer **one PR** for LB rename + path so healthcheck never encodes the dead URL).
   Status: done (`scripts/sut-healthcheck.mjs`, `scripts/sut-healthcheck.test.mjs`, `package.json` scripts `sut:healthcheck` and `test:sut-healthcheck`).

2. **Phase 2 — Restart**  
   Implement `pnpm sut:restart` (stop by port + spawn `pnpm sut`).  
   Status: done (`scripts/sut-restart.mjs`, `scripts/sut-restart.test.mjs`, `package.json` scripts `sut:restart` and `test:sut-restart`; `lsof` in `flake.nix`).

3. **Phase 3 — LB rename (complete)**  
   New scripts **`local:lb`** / **`local:lb:vite`**, move to **`scripts/local-lb.mjs`**, **`LOCAL_LB_*` env**, **`[local-lb]`** logs, readiness path **`/__lb__/ready`**, remove old file and all references listed above. CI, docs, rules, Cypress/wait-on updated in the **same phase** so the tree never sits half-renamed.

4. **Phase 4 — Docs / rules**  
   “Unsure if SUT is running?” + any remaining cross-links.

**Optional Phase 5:** `--wait-ready` on healthcheck.

---

## Open decisions

- **Mountebank on restart:** always kill **2525** vs leave if already up — recommend **leave** by default in `sut:restart` to match `start:mb` idempotency; document `mb` stop if tests need a clean imposter state.
- **Healthcheck and `nix develop`:** root scripts should run under the same prefix as other commands (`CURSOR_DEV=true nix develop -c pnpm sut:healthcheck`) per project rules; document for agents.
- **Readiness path string:** finalize **`/__lb__/ready`** vs e.g. `/local-lb/ready` (must not conflict with SPA routes — `__lb__` mirrors prior `__e2e__` pattern).

### Completion check

Before closing the rename phase:

- [ ] Grep clean for old script names, old filename, old env vars for LB, old log tag, old readiness path.
- [ ] `pnpm sut` and `pnpm test` (or CI-equivalent `run-p`) still work locally / in CI.
