# Local SUT healthcheck and restart

**Scope:** Dev and E2E-local use the same stack (`pnpm sut`); no separate “dev vs test” health model.

**Goal:**

- `pnpm sut:healthcheck` — verify everything `pnpm sut` starts is still behaving normally.
- `pnpm sut:restart` — clear stray listeners/processes for that stack, then start `pnpm sut` again.
- **Local fake LB:** rename everything around the old **prod-topology / e2e-proxy** naming to **`lb`**. No leftover script names, paths, env prefixes, log tags, or doc phrasing that implies “E2E-only proxy.”
- Update **CLAUDE.md**, **`.cursor/rules/general.mdc`**, **`.cursor/rules/cloud-agent-setup.mdc`**, **`docs/gcp/prod_env.md`**, and any other references so humans and agents know what to run when unsure whether SUT is up.

**Phases 1–3 (implemented):**

| Command | Implementation |
|---------|------------------|
| `pnpm sut:healthcheck` | [`scripts/sut-healthcheck.mjs`](../scripts/sut-healthcheck.mjs) — TCP 2525 / 9081 / 5173 / 5174 + HTTP readiness on **`http://127.0.0.1:5173/__lb__/ready`**. |
| `pnpm sut:restart` | [`scripts/sut-restart.mjs`](../scripts/sut-restart.mjs) — `lsof` + `SIGTERM` on **5173, 5174, 9081** only, then `pnpm sut`. Mountebank **2525** left alone. |
| Local LB | [`scripts/local-lb.mjs`](../scripts/local-lb.mjs) — **`pnpm local:lb`** / **`pnpm local:lb:vite`**; env **`LOCAL_LB_*`**; log tag **`[local-lb]`**. |
| Tests | `pnpm test:sut-healthcheck`, `pnpm test:sut-restart` (both run from **`pnpm lint:all`**). |
| `lsof` | In [`flake.nix`](../flake.nix) `basePackages` for reproducible **`nix develop`**. |

Run via project convention: `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` (and same prefix for `sut:restart`).

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

**Optional refinement (later — Phase 5 only if needed):** If CI/agents often fail right after SUT start (warm-up), add something like **`--wait-ready`** (retry HTTP readiness only, capped total wait, default remains **fail fast**). Don’t add until that failure mode shows up in practice.

**Exit codes (suggestion):**

- `0` — all checks passed.
- `1` — unhealthy / incomplete (listeners or readiness failed).
- Avoid a separate exit code for “starting” unless automation needs it; text + optional `--wait-ready` (if implemented) is enough.

---

## What `pnpm sut` runs today (single source for checks)

From root `package.json` **`sut`**:

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

### Readiness URL

**`GET http://127.0.0.1:5173/__lb__/ready`** — reserved path segment on the local LB; Spring health is probed server-side from the LB. Documented in **`docs/gcp/prod_env.md`** and **`scripts/local-lb.mjs`** header. Used by `wait-on`, `pnpm sut:healthcheck` default, CI, and cloud-agent docs.

---

## `pnpm sut:restart` behavior

**Implemented** in [`scripts/sut-restart.mjs`](../scripts/sut-restart.mjs).

1. **Stop** — For each of **5173, 5174, 9081**, `lsof … -sTCP:LISTEN -t`, then `SIGTERM` those PIDs. **2525** (mountebank) is **not** stopped (matches `start:mb` idempotency).
2. **Start** — `pnpm sut` from repo root (`stdio: inherit`, same install + `cli:bundle` + `run-p` stack as a manual `pnpm sut`).

**Risks:** Anything else bound to those ports can be signalled. Logs list **port** and **PID(s)** per step.

**Edge case:** Backend or another app on **9081** unrelated to SUT — same port-based risk as the design assumes; stop that process yourself if needed.

---

## Rename: **`lb`** (Phase 3 — done)

Canonical script: **`scripts/local-lb.mjs`**. pnpm: **`local:lb`** (static + backend), **`local:lb:vite`** (dev + Vite upstream on 5174). Env prefix **`LOCAL_LB_*`** only in that file; console **`[local-lb]`**; alt CLI bundle constant **`CLI_CYPRESS_ALT_INSTALL_BUNDLE`**.

**Grep hygiene:** no legacy fake-LB script names, old proxy filename, old LB env prefixes (superseded by **`LOCAL_LB_*`**), or old readiness path. Other **`E2E_*`** vars for Cypress DB, Gmail mocks, etc. stay.

---

## Documentation and Cursor rules (after scripts exist)

Add a short, consistent **“Unsure if SUT is running?”** block:

1. Run **`pnpm sut:healthcheck`**.
2. If not OK, run **`pnpm sut:restart`** (or start **`pnpm sut`** in a terminal if nothing was running).
3. Do **not** ask people to restart `pnpm sut` after normal code edits (existing rule stays).

**Files to touch:**

- **`CLAUDE.md`** — Common commands table + local dev paragraph (**`local:lb`**, **`local:lb:vite`**).
- **`.cursor/rules/general.mdc`** — same guidance near `pnpm sut`.
- **`.cursor/rules/cloud-agent-setup.mdc`** — VM instructions: **`local:lb`**, readiness **`/__lb__/ready`**, **`pnpm sut:healthcheck`** when debugging; if the VM has **no Nix**, say to run the same **`pnpm`** scripts from repo root and that **`sut:restart`** needs **`lsof`** on `PATH`.
- **`docs/gcp/prod_env.md`** — single write-up: ports, **`scripts/local-lb.mjs`**, env vars, readiness URL **`/__lb__/ready`** (post–Phase 3).

Also state the **standard agent command form** here: `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` / `… sut:restart` (same as project rules), so Phase 4 does not scatter one-off variants.

---

## Phased delivery (value first)

1. **Phase 1 — Healthcheck**  
   Status: **done** — `scripts/sut-healthcheck.mjs`, `scripts/sut-healthcheck.test.mjs`, `sut:healthcheck`, `test:sut-healthcheck`, covered by `lint:all`.

2. **Phase 2 — Restart**  
   Implement `pnpm sut:restart` (stop by port + spawn `pnpm sut`).  
   Status: **done** — `scripts/sut-restart.mjs`, `scripts/sut-restart.test.mjs`, `sut:restart`, `test:sut-restart`, `lsof` in `flake.nix`, covered by `lint:all`.

3. **Phase 3 — LB rename (complete)**  
   Status: **done** — `scripts/local-lb.mjs`, **`local:lb`** / **`local:lb:vite`**, **`LOCAL_LB_*`**, **`/__lb__/ready`**, CI + docs + rules + `wait-on` consumers updated in one change set.

4. **Phase 4 — Docs / rules**  
   “Unsure if SUT is running?” + **`/__lb__/ready`** + **`local:lb` / `local:lb:vite`** + **nix-prefixed** `sut:healthcheck` / `sut:restart` as the default agent recipe, with **cloud-agent-setup** covering the no-Nix / `lsof` caveat. Keep **`docs/gcp/prod_env.md`** as the port/URL source of truth.

**Optional Phase 5:** **`--wait-ready`** on healthcheck — only if warm-up flakiness appears in CI/agents; default stays fail-fast.

---

## Decisions (resolved)

| Topic | Decision |
|-------|-----------|
| **Mountebank on restart** | **Leave 2525** (implemented in `sut:restart`). Clean imposters: restart mountebank separately if needed. |
| **Readiness path** | **`/__lb__/ready`** at the LB (not `/local-lb/ready`). Legacy readiness path removed; single surface only. |
| **Healthcheck / restart invocation** | **Standard:** `CURSOR_DEV=true nix develop -c pnpm sut:healthcheck` and `… pnpm sut:restart`. **Phase 4** writes this into CLAUDE, **general.mdc**, **cloud-agent-setup** (plus no-Nix note), and **prod_env.md**. |
| **Phase 5 `--wait-ready`** | **Defer** until there is a concrete “just started SUT” failure mode to fix. |

### Completion check (Phase 3)

- [x] Grep clean for legacy LB script names, filename, env vars for LB, log tag, readiness path.
- [ ] `pnpm sut` and `pnpm test` (or CI-equivalent `run-p`) still work locally / in CI — verify in your environment after merge.
