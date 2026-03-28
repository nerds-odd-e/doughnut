# CLI interactive mode — `cli_interactive_mode.feature` (four scenarios, four phases)

**Scope:** All four scenarios in `e2e_test/features/cli/cli_interactive_mode.feature`, delivered as **four separate phases** (one user-visible behavior per phase). **Sub-phases** within each phase support small commits, green CI, and no dead code.

**Roadmap alignment** (`ongoing/cli-architecture-roadmap.md`): **§2.2 / §3** message-based interactive session; **§8** PTY E2E, thin steps, **centralized terminal assertions** (`outputAssertions.ts`); **§8.2** explicit **kill**-based teardown for `@interactiveCLI`.

**Planning rules** (`.cursor/rules/planning.mdc`): observable assertions first, at most one intentionally failing enabled scenario while driving a change, no dead code.

**Cucumber order:** `Before` runs **before** `Background` and steps. **`Before(@interactiveCLI)`** must **install + start the PTY** (cannot rely on Background for `doughnutPath`). Use **`After(@interactiveCLI)`** + **dispose at start of `Before`** so leaks are bounded when `After` does not run on failure (see `hook.ts` MCP comment).

**Tagging:** Add **`@bundleCliE2eInstall`** on this feature (or each scenario) so `installCli` matches `cli_install_and_run.feature`. **`@withCliConfig`:** add a hook when a scenario needs it; not assumed for every phase below.

**Recommended order:** Complete **Foundation** once, then **Phase 1 → 2 → 3 → 4** in sequence (later phases may reuse steps/assertions added earlier).

---

## Foundation (once, before un-ignoring scenarios)

Shared by all four scenarios; not itself a Gherkin scenario.

### F.1 — PTY dispose task (plugin)

- Expose e.g. `cliInteractivePtyDispose`: **`pty.kill()`**, clear session reference, **idempotent**, clear errors.
- **CI:** Green.

### F.2 — `@interactiveCLI` Before / After + feature tags

- `Before({ tags: '@interactiveCLI' })`: `cliInteractivePtyDispose` → `installCli(e2eAppBaseUrl())` → `.as('doughnutPath')` → `runInstalledCliInteractive({ doughnutPath })`.
- `After({ tags: '@interactiveCLI' })`: `cliInteractivePtyDispose`.
- Feature file: **`@bundleCliE2eInstall`** (and `@withCliConfig` only if/when hooked).
- **CI:** Green; all scenarios still `@ignore`.

### Note on other features

`cli_access_token.feature` and `cli_recall.feature` also use `@interactiveCLI`. When enabling them, drop redundant install/PTY start steps so the hook owns the session.

---

## Phase 1 — “Not supported” for a plain line

**Scenario:** TTY interactive responds "Not supported" to a plain line

**Outcome:** `hello` appears in **past user messages**; **Not supported** in **past CLI assistant messages**.

### 1.1 — E2E

- Step: `When('I enter {string} in the interactive CLI', …)` → `cliInteractiveWriteLine` (no leading `/` required).
- Remove `@ignore` from **this scenario only**.
- **Failure should cite** missing `"Not supported"` or `"hello"` in the parsed sections, not PTY/install errors.
- **CI:** Red until 1.2 unless merged with 1.2 in one PR.

### 1.2 — Product

- Interactive path: unknown / non-slash line → user message + assistant line **Not supported** (exact string).

**CI:** Green.

---

## Phase 2 — After `/help`, empty Enter keeps a normal input box

**Scenario:** After /help, consecutive Enter on empty input keeps a normal input box

**Outcome:** After `/help` and several **Enter** on **empty** input, the TTY still shows a **normal** command/input strip (no stuck modal, no broken layout). Exact check lives in the **centralized assertion layer** (extend `outputAssertions` / page object; avoid scattering raw PTY string checks in step defs).

### 2.1 — E2E

- Step: `When('I press Enter in the interactive CLI', …)` → send **empty** commit (e.g. write `\r` only, or whatever matches Ink “submit empty line” — align with product).
- Step: `Then('the input box UI should be normal', …)` → one fluent assertion that fails with a **readable** message (snapshot / structured expectation per roadmap §9–§10).
- Remove `@ignore` from **this scenario only** (others stay ignored).
- **CI:** Red until 2.2 unless merged with 2.2.

### 2.2 — Product

- Ensure `/help` + repeated empty Enter does not corrupt focus, prompt, or past/current regions; behavior must match the assertion contract chosen in 2.1.

**CI:** Green.

---

## Phase 3 — `/help` lists subcommands and interactive commands

**Scenario:** /help lists subcommands and interactive commands

**Outcome:** Past assistant transcript contains **`/recall`**, **`exit`**, **`update`**, **`version`** (strings as in the feature file).

### 3.1 — E2E

- Reuses `I enter the slash command "/help"…` and existing `pastCliAssistantMessages` assertions.
- Remove `@ignore` from **this scenario only**.
- **CI:** Red until 3.2 unless merged with 3.2.

### 3.2 — Product

- `/help` output (interactive) includes those entries — preferably by reusing the same help aggregation as non-interactive help where that already exists (**roadmap §2.1 / §7** reuse), without duplicating command lists in two divergent places.

**CI:** Green.

---

## Phase 4 — `exit` shows “Bye.”

**Scenario:** exit ends the session after Bye

**Outcome:** User enters **`exit`** (plain line, as in the feature); **Bye.** appears in **past CLI assistant messages**.

### 4.1 — E2E

- Reuses `I enter {string} in the interactive CLI` with `"exit"` (from Phase 1) or an equivalent single step.
- Remove `@ignore` from **this scenario only**.
- Assert **Bye.** in past assistant messages. **Do not** change **`After(@interactiveCLI)`**: it should still **`kill`** the PTY for cleanup; the scenario asserts **user-visible copy**, not “process exited cleanly vs killed.” If the CLI exits on its own after `exit`, dispose remains safe (kill no-op or already dead).
- **CI:** Red until 4.2 unless merged with 4.2.

### 4.2 — Product

- Plain `exit` (and/or align with existing `/exit` if you unify) prints **Bye.** into the session transcript as specified.

**CI:** Green.

---

## Summary checklist

| Block | What | CI |
|-------|------|-----|
| F.1 | `cliInteractivePtyDispose` | Green |
| F.2 | `@interactiveCLI` hooks + `@bundleCliE2eInstall` | Green |
| 1.1–1.2 | Plain line → Not supported | Green after 1.2 |
| 2.1–2.2 | `/help` + empty Enters → normal input UI | Green after 2.2 |
| 3.1–3.2 | `/help` lists recall, exit, update, version | Green after 3.2 |
| 4.1–4.2 | `exit` → Bye. | Green after 4.2 |

---

## Remove this document when

All four scenarios are enabled and stable, and lasting conventions are folded into `.cursor/rules/cli.mdc` / `e2e_test.mdc` if desired.
