# CLI implementation — self-study plan

Informal progress tracker: tick off layers as you read. For **each** layer below, work through the checklist (same six questions every time). Add bullets under **Notes / follow-ups** when something is unclear — answer later with code, tests, or a teammate.

## Checklist (repeat per layer)

1. **Responsibility & surface** — What does this layer own? What does it export or present to callers/UI/users?
2. **Knowledge** — What domain or runtime facts does it hold or assume (env, TTY, API shape, file paths)?
3. **Dependencies** — What does it import/call? What callbacks or contracts does it require from outside?
4. **Design “why”** — Why is the boundary here (split from neighbors, testability, Ink vs readline, single responsibility)?
5. **Tests** — Which tests cover it? Entry-point style (`runInteractive`, `processInput`) vs direct unit tests?
6. **Notes / follow-ups** — Your questions as you read.

---

## Layer 0 — Map and docs (once)

- Skim `.cursor/rules/cli.mdc` terminology (Current prompt, live column, `processInput` vs TTY).
- Skim `cli/package.json` scripts (`bundle`, `test`) and where the built artifact goes.
- **Tests:** `cli/tests/index.test.ts` (smoke / argv routing if present).

**Progress:** [ ] done **Notes:**

---

## Layer 1 — Process entry and routing (highest level)

| Files | Role |
|-------|------|
| `cli/src/index.ts` | Top-level `main()` + fatal error handler |
| `cli/src/main.ts` | `-P` / `--production` env normalization |
| `cli/src/run.ts` | Arg routing: reject `-c`, `version` / `update` / `help` vs interactive default |
| `cli/src/cliExit.ts` | Consistent error exit for invalid CLI use |

**Progress:** [ ] done

**Tests:** `cli/tests/index.test.ts` (and any tests that spawn argv paths).

**Notes / follow-ups:**

---

## Layer 2 — Interactive entry and shared command engine

| Files | Role |
|-------|------|
| `cli/src/interactive.ts` | `runInteractive`, `processInput`, wiring of slash commands, `OutputAdapter`, token-list / recall integration |

This is the **main facade** between “run the shell” and TTY session + command handlers.

**Progress:** [ ] done

**Tests:** `cli/tests/interactive/processInput.test.ts` (adapter contract); TTY suites indirectly exercise the same paths.

**Notes / follow-ups:**

---

## Layer 3 — Types and injectable shell dependencies

| Files | Role |
|-------|------|
| `cli/src/types.ts` | Command configs, stage indicators, shared CLI domain types |
| `cli/src/interactiveShellDeps.ts` | Boundaries for fetch, filesystem, etc. (what the shell is allowed to do) |

**Progress:** [ ] done

**Tests:** Often covered indirectly; note any file that imports these in isolation.

**Notes / follow-ups:**

---

## Layer 4 — TTY session adapter (stdin/stdout, Ink lifecycle)

| Files | Role |
|-------|------|
| `cli/src/ttyAdapters/interactiveTtySession.ts` | PTY session: readline/keypress bridges, Ink `render`, `InteractiveAppTerminalContract`, approved non-Ink bytes |
| `cli/src/ttyAdapters/interactiveTtyStdout.ts` | Input-ready sequences, exit/farewell, cursor policy helpers |

**Progress:** [ ] done

**Tests:** `cli/tests/interactive/interactiveTtySession.test.ts`, `interactiveTtyOutput.test.ts`, `interactiveExitFarewell.test.ts`, `interactiveTtyFetchWaitEsc.test.ts`, and related `interactiveTty*.test.ts`.

**Notes / follow-ups:**

---

## Layer 5 — Ink app shell (orchestrates UI modes)

| Files | Role |
|-------|------|
| `cli/src/ui/interactiveApp.tsx` | Large: props from session, mode switching, passes state into subtree |

Read this **after** Layer 4 so you know what the session injects.

**Progress:** [ ] done

**Tests:** Many `interactiveTty*.test.ts` files; `mainCommandLineInkTyping.test.ts` where relevant.

**Notes / follow-ups:**

---

## Layer 6 — UI composition (transcript + live column)

| Files | Role |
|-------|------|
| `cli/src/ui/ShellSessionRoot.tsx` | Two-region model: past transcript + live column |
| `cli/src/ui/liveColumnInk.tsx` | Command line, lists, focus, MCQ/token panels |
| `cli/src/ui/FetchWaitDisplay.tsx` | Fetch-wait stage + Esc/Ctrl+C handling |
| `cli/src/ui/RecallInkConfirmPanel.tsx` | Recall y/n strip |
| `cli/src/ui/PatchedTextInput.tsx`, `cli/src/ui/patchedTextInputKey.ts`, `cli/src/ui/inkStdinLogicalKeys.ts` | Text input behavior vs Ink defaults |

**Progress:** [ ] done

**Tests:** `interactiveTtyMcq.test.ts`, `interactiveTtyTokenList.test.ts`, `interactiveFetchWait.test.ts`, `interactiveTtyRecall*.test.ts`, etc.

**Notes / follow-ups:**

---

## Layer 7 — Shell session state and past messages

| Files | Role |
|-------|------|
| `cli/src/shell/shellSessionState.ts` | Session state transitions |
| `cli/src/shell/pastMessagesModel.ts` | Parsed transcript / past assistant vs user blocks |

**Progress:** [ ] done

**Tests:** `cli/tests/shell/pastMessagesModel.test.ts`; TTY tests for visible transcript behavior.

**Notes / follow-ups:**

---

## Layer 8 — Command line input, history, masking

| Files | Role |
|-------|------|
| `cli/src/interactiveCommandInput.ts` | Live buffer, history ↑↓, commit line, integration with readline/Ink |
| `cli/src/userInputHistoryFile.ts` | Persisted history |
| `cli/src/inputHistoryMask.ts` | Mask secrets (e.g. access tokens) before storage/display |

**Progress:** [ ] done

**Tests:** `interactiveCommandInput.test.ts`, `userInputHistoryFile.test.ts`, `inputHistoryMask.test.ts`, `interactiveTtyInputHistory.test.ts`, `interactiveTtyAddAccessTokenMask.test.ts`.

**Notes / follow-ups:**

---

## Layer 9 — Completion and list display helpers

| Files | Role |
|-------|------|
| `cli/src/slashCompletion.ts` | `/` command completion |
| `cli/src/listDisplay.ts` | Listing/formatting for terminal |

**Progress:** [ ] done

**Tests:** `slashCompletion.test.ts`, `listDisplay.test.ts`.

**Notes / follow-ups:**

---

## Layer 10 — Interaction helpers (keyboard → domain actions)

| Files | Role |
|-------|------|
| `cli/src/interactions/selectListInteraction.ts` | MCQ / token list key dispatch |
| `cli/src/interactions/recallYesNo.ts` | Recall confirmation key handling |

**Progress:** [ ] done

**Tests:** `selectListInteraction.test.ts`, `recallYesNo.test.ts`; TTY recall suites.

**Notes / follow-ups:**

---

## Layer 11 — Async fetch wait and abort

| Files | Role |
|-------|------|
| `cli/src/interactiveFetchWait.ts` | Staging long-running fetches for UI |
| `cli/src/fetchAbort.ts` | AbortSignal / cancellation wiring |

**Progress:** [ ] done

**Tests:** `interactiveFetchWait.test.ts`, `interactiveTtyFetchWaitEsc.test.ts`, `interactiveTtyBufferAfterFetchWait.test.ts`.

**Notes / follow-ups:**

---

## Layer 12 — Rendering and terminal primitives

| Files | Role |
|-------|------|
| `cli/src/renderer.ts` | SGR, stage band, re-exports layout helpers |
| `cli/src/terminalLayout.ts` | Column width, wrap, truncate (grapheme-aware) |
| `cli/src/ansi.ts` | ANSI parsing/building |
| `cli/src/terminalChalk.ts` | Chalk/terminal styling glue |
| `cli/src/markdown.ts` | Markdown → terminal-friendly text |

**Progress:** [ ] done

**Tests:** `renderer.test.ts`, `markdown.test.ts`; many interactive tests assert visible strings.

**Notes / follow-ups:**

---

## Layer 13 — Command modules (feature verticals)

Study **`commands/help.ts` first** (registry and docs), then handlers as you care about product flows:

| Files | Role |
|-------|------|
| `cli/src/commands/help.ts` | Aggregated `/help` |
| `cli/src/commands/recall.ts` | Recall session |
| `cli/src/commands/accessToken.ts` | Tokens |
| `cli/src/commands/gmail.ts` | Gmail OAuth flow |
| `cli/src/commands/version.ts`, `cli/src/commands/update.ts` | Non-interactive subcommands |

**Progress:** [ ] done

**Tests:** `help.test.ts`, `recall.test.ts`, `recallMcqDisplay.test.ts`, `accessToken.test.ts`, `gmail.test.ts`, `version.test.ts`, `update.test.ts`, `sdkHttpErrorClassification.test.ts` (if relevant to HTTP).

**Notes / follow-ups:**

---

## Layer 14 — Config and credentials

| Files | Role |
|-------|------|
| `cli/src/configDir.ts` | Config directory resolution |
| `cli/src/credentials.ts` | Stored credentials |

**Progress:** [ ] done

**Tests:** Covered via commands / integration; grep tests for `DOUGHNUT_CONFIG_DIR` if needed.

**Notes / follow-ups:**

---

## Layer 15 — Build-only / bundle edge

| Files | Role |
|-------|------|
| `cli/src/shims/react-devtools-core-stub.ts` | Esbuild alias for optional Ink devtools import |

**Progress:** [ ] done

**Tests:** None dedicated; bundle script proves resolution.

**Notes / follow-ups:**

---

## Parallel: E2E (product-shaped, not a substitute for reading src)

- Features: `e2e_test/features/cli/*.feature`
- Steps / page objects: `e2e_test/step_definitions/cli.ts`, `e2e_test/start/pageObjects/cli/`

Use after you know Layers 1–6 to connect vocabulary to real scenarios.

**Progress:** [ ] skimmed **Notes:**

---

## Suggested reading order (summary)

1. Layers **0 → 4** (entry → `processInput` → TTY adapter)  
2. Layers **5 → 6** (Ink)  
3. Layers **7 → 11** (state, input, interactions, fetch)  
4. Layer **12** (rendering) can be read **whenever** you need clarity on wrapping/colors — often alongside Layer 6.  
5. Layer **13** (commands) alongside or after **2**, depending on whether you prefer “what runs” before “how it paints.”  
6. Layers **14 → 15** when you touch config or the bundle.

Adjust order if you prefer **command handlers first**: read Layer **13** right after Layer **2**, then return to TTY/Ink.
