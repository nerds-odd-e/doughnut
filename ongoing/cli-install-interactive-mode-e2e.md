# Plan: Install + interactive mode E2E (`cli_install_and_run.feature`)

Informal plan for the scenario **“Install and run the CLI in interactive mode”** — see `e2e_test/features/cli/cli_install_and_run.feature`.

After **Phase 1**, the scenario is **no longer `@ignore`**: the first two steps run in CI; the **`/exit`** steps stay **commented out** in the feature file until a later phase uncomments them (see below).

**Scenario steps (authoritative):**

```gherkin
When I run the installed doughnut command in interactive mode
Then I should see "doughnut 0.1.0" in past CLI assistant messages
When I enter the slash command "/exit" in the interactive CLI
And I should see "/exit" in past user messages
```

**Progressive feature file (by phase):**

- **After Phase 1:** Remove `@ignore` from the scenario. Leave steps 3–4 **commented** (with a one-line note, e.g. `# Phase 2: uncomment when /exit E2E is ready`), so Cucumber runs only steps 1–2.
- **Phase 2 onward:** Uncomment each step **as soon as** that step’s production + E2E behavior works (steps 3–4 together when `/exit` is done).

**Scope:** Only production behavior and automated tests required for **this** scenario. No extra CLI features, no un-ignoring other interactive features unless they become unavoidable shared infrastructure.

**Roadmap alignment** (`ongoing/cli-architecture-roadmap.md`):

- §2.1 / §2.2 — Keep non-interactive (`version` / `update`) separate from the interactive TUI path; this scenario exercises **both** install + **interactive** entry.
- §3 — Message-based session: startup assistant content and the **`/exit` slash command** should fit the “messages in transcript” model (slash commands as messages).
- §5.1 — Prefer idiomatic Ink + React **when** the slice needs full-screen TUI; use the **smallest** UI that satisfies observable assertions (challenge big framework work unless the scenario requires it).
- §6 — Terminal I/O stays behind an adapter boundary where practical; Vitest can inject mocks at that edge (`runInteractive`, stdin/stdout).
- §8 — E2E stays **PTY-shaped**, thin Cucumber steps, behavior in page objects / tasks.
- §9 — Terminal-visible assertions go through **one** CLI E2E assertion path (extend `e2e_test/start/pageObjects/cli/`, not ad-hoc string checks in steps).
- §10 — On failure, assertions should emit **readable** diagnostics (text/HTML snapshot acceptable for this slice; full “screenshot-like” artifacts can stay roadmap Phase 4 unless cheap to add here).

**Current baseline (important):**

- `cli/src/interactive.ts` only checks `stdin.isTTY` and returns; with a TTY there is **no** visible session, transcript, or **`/exit`** handling yet.
- `runInstalledCli` in `e2e_test/config/cliE2ePluginTasks.ts` uses pipes and **ends stdin immediately** — correct for non-interactive, wrong for interactive runs; Phase 1 adds a **separate** PTY-backed path for “installed + interactive”.
- Gherkin phrases such as “enter … in the interactive CLI” / “past CLI assistant messages” appear in several feature files but **have no step definitions** in `e2e_test/step_definitions` today; Phase 1 must implement the subset needed for **steps 1–2** (and Phase 2 extends for **3–4**).

**Testing strategy** (`.cursor/rules/planning.mdc`):

- **Vitest:** Drive **`runInteractive`** (or the stable CLI entry that reaches it) with a **mock TTY** stdin; assert stdout / transcript behavior. Do not depend on E2E for every edge of parsing.
- **E2E:** After Phase 1, `cli_install_and_run.feature` runs the interactive scenario’s **first two steps** in CI; run the **spec** while iterating, not the whole suite.
- **TDD:** Where possible, add/adjust a test first, confirm it fails for the right reason, then implement.

**Deploy gate:** After each phase that changes production or CI-visible tests, treat **commit + push + CD** as the team’s usual gate before starting the next phase (planning checklist).

---

## Phase 1 — First two scenario steps: production + Vitest + PTY E2E + partial scenario in CI

**User-visible outcome:** Running the **installed** CLI **with a TTY** and **no** subcommand shows **`doughnut 0.1.0`** in **past CLI assistant messages**, verified **end-to-end** in the install feature.

**Production:**

- Implement the minimal interactive session so the process does **not** exit immediately after the TTY check.
- Emit an initial assistant message that includes the same string as `formatVersionOutput()` (`cli/src/commands/version.ts`) so the scenario text stays stable.

**Vitest:**

- Extend coverage (e.g. `cli/tests/index.test.ts` or a focused interactive test) using a **fake TTY stdin** and capturing output consistent with how the TUI renders the greeting (observable surface only).

**E2E (moved forward from the old “Phase 3/4” split):**

- Add a Cypress `task` that spawns **`node <installed doughnutPath>`** with **no args** over a **PTY** (e.g. `node-pty` or equivalent — native addon / ABI: pin and document).
- Pass env via existing `cliEnv()` (`e2e_test/config/cliEnv.ts`).
- Thin step + page object: **`When I run the installed doughnut command in interactive mode`** → task + alias for captured PTY output / session handle.
- Centralized assertion layer: **`Then I should see … in past CLI assistant messages`** for this scenario (smallest parser that can find the version line in visible transcript; roadmap §9). Add **minimal** failure diagnostics if cheap (roadmap §10.2).

**Feature file gate (end of Phase 1):**

- Remove **`@ignore`** from this scenario.
- **Comment out** the two **`/exit`** steps (lines 3–4 of the scenario body), with a short `# Phase 2: …` note.
- Confirm: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature` passes.

**Explicitly out of scope for Phase 1:** **`/exit`** behavior, slash-command input over PTY, “past user messages” assertion for `/exit`, full Ink layout from the long-term study doc unless required to render the version line.

---

## Phase 2 — `/exit` slash command + uncomment last two steps

**User-visible outcome:** Full scenario passes: **`/exit`** is recorded as **`/exit`** in **past user messages**, session ends cleanly.

**Production:**

- Implement **`/exit`** on the slash-command path; transcript shows **`/exit`** in user history (not bare `exit`).
- Ensure teardown flushes output so E2E can observe transcript lines deterministically.

**Vitest:**

- Mock TTY, send **`/exit`** on the same path as slash-command entry, assert **`/exit`** in past user messages and exit behavior.

**E2E:**

- Extend PTY session / page object: **`When I enter the slash command "/exit" in the interactive CLI`** (reuse the shared slash-command step shape used in other CLI features if it fits).
- **`And I should see "/exit" in past user messages`** via the centralized assertion layer.
- **Uncomment** the two previously commented Gherkin lines in `cli_install_and_run.feature`.

**Failure diagnostics:** Extend Phase 1 diagnostics if assertions on user transcript need clearer diffs.

---

## Phase 3 — Lock documentation and retire this plan

- Update `.cursor/rules/cli.mdc` **Active CLI E2E** bullet: this scenario is fully active (all four steps), not ignored.
- Final sanity: run `cli_install_and_run.feature` spec.
- Delete **this** plan file when the team no longer needs the checklist (or archive elsewhere).

---

## Notes

- **`@bundleCliE2eInstall`:** Keep using the E2E install bundle so the served binary matches install tests; no need to change that tag for this scenario.
- **Interim vs study doc:** `ongoing/cli-implementation-study.md` describes a larger layer cake (Ink app, `processInput`, etc.). If that lands mid-work, **replace** the minimal UI with the study architecture **without** changing the scenario’s observable expectations. Until then, the smallest compliant implementation is fine (planning: interim behavior allowed).
- **Other `@interactiveCLI` features:** Un-ignoring them is **out of scope**; shared step defs from Phase 1–2 may make later work easier but must not pull in recall/token scenarios into this delivery.
