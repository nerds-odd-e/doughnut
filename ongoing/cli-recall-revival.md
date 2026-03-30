# CLI recall revival (plan only)

**Status:** Phase 1 complete (recall status). Phase 2.1 complete (Just Review E2E un-ignored + bold guidance assertion). Phase 2.2 complete (`/recall` just-review stage, PTY rows 48 for stable guidance replay). Phase 2.3 complete (just-review edge Vitest: invalid y/n commits, empty title/details + no notebook line). Phase 5.1 complete (full *Recall session* scenario un-ignored; `I answer … to prompt …` waits on Current guidance). **Next:** Phases 3–4 (Temp1 / Temp2) before Phase 5.2. Phases 5.2–10 still ahead; this file stays high-level planning, not a step-by-step implementation spec.  
**Goal:** Restore behaviors in `e2e_test/features/cli/cli_recall.feature` with **observable E2E coverage**, **minimal dead code**, and **architecture that does not repeat the pre-removal shape** (heavy global mutable recall state and recall orchestration embedded in `interactive.ts`).

**Guidance:** `.cursor/rules/planning.mdc`, `.cursor/rules/cli.mdc`, `ongoing/cli-architecture-roadmap.md` — prefer **Ink/React composition and stage-local state**, **thin Cucumber steps**, **centralized terminal assertions**, and **reuse of shared API client code** (`doughnut-api` / existing backend client helpers). Challenge big abstractions until repetition justifies them.

---

## Git history (inspiration only — do not resurrect architecture)

Recent removals (around **2026-03-28**) show what existed before strip-down; use only to remember **APIs, copy, and user-visible flows**, not file layout.

| Commit     | Summary |
|-----------|---------|
| `1307f7b5a` | Removed recall session handling from CLI page objects, step definitions, section parsing; marked recall scenarios ignored. |
| `5cbe3ad95` | Removed deprecated recall session handling / CLI input paths. |
| `6177a4481` | Removed `/recall-status` and related interactive wiring; trimmed tests and `interactiveFetchWait`. |
| `ef97ec629` (earlier) | Recall command updated to newer backend API client — useful reminder of **which controllers/DTOs** matter. |

**Prior shape (avoid repeating):** `interactive.ts` imported many recall helpers and owned **module-level mutable recall session state** (`pendingRecallAnswer`, `recallSessionMode`, load-more, stop confirmation, MCQ guidance lines, etc.). `cli/src/commands/recall.ts` was ~250 lines mixing HTTP, result typing, and some formatting. **Replace with** a bounded recall **stage** (or cohesive module + single parent component) so `interactive.ts` stays orchestration-light.

**Still in tree today:** `cli/src/commands/recall.ts` retains **`recallStatus` only** (plus HTTP error classification via `recallStatus` in `cli/tests/sdkHttpErrorClassification.test.ts`, and pluralization in `cli/tests/recallStatus.test.ts`). Backend `RecallsController` / recall domain remains; web E2E recall steps (`e2e_test/step_definitions/recall.ts`, etc.) are unrelated to CLI.

---

## Cross-cutting constraints (all phases)

1. **E2E gate:** Run the relevant `--spec e2e_test/features/cli/cli_recall.feature` (or single scenario via tags if the project supports it) after un-ignoring each scenario — see `.cursor/rules/e2e_test.mdc`.
2. **Assertions:** Extend **`e2e_test/start/pageObjects/cli/outputAssertions.ts`** (and friends) for recall-specific visible state; keep failures **diagnostic** (expected vs visible).
3. **Steps:** Keep **`e2e_test/step_definitions/cli.ts`** thin; restore or add **page-object fluents** under `e2e_test/start/pageObjects/cli/` (e.g. a `recallSession()`-style helper) rather than logic in steps.
4. **Terminology:** Match `.cursor/rules/cli.mdc` — past assistant vs current prompt vs current guidance; **y/n** for recall confirmations do not create past user message rows; MCQ choices in **current guidance**.
5. **OpenAI scenarios:** `@usingMockedOpenAiService` — ensure mock/stub ordering matches scenario tables (contest/regenerate needs **sequenced** mock responses).
6. **Deploy gate:** Per planning discipline, prefer **commit + CD deploy** between top-level phases when the team expects it.

---

## Phase 1 — Scenario: *Recall status shows count when notes are due* — **complete**

**User outcome:** `/recall-status` shows `1 note to recall today` (E2E); other counts covered by unit tests below.

- **1.1 / 1.2:** First scenario in `e2e_test/features/cli/cli_recall.feature` is active (no `@ignore`); `/recall-status` wired to `recallStatus`; copy appears in past CLI assistant messages; help lists the command where the project aggregates slash commands.
- **1.3:** `cli/tests/recallStatus.test.ts` — black-box `recallStatus` against a local HTTP stub returning `DueMemoryTrackers` JSON: `0` notes (missing and empty `toRepeat`), `1` note, `2` notes, `10` notes. No timezone/query unit tests (no client-side branching on that in `recallStatus`).

**Next:** Phase 2 (*Recall Just Review*), then Phases 3–4 (Temp1 / Temp2) before the full *Recall session* phase.

---

## Phase 2 — Scenario: *Recall Just Review*

**User outcome:** `/recall` enters recall; **current guidance** shows note title, markdown-stripped details, styled emphasis, and “Yes, I remember?”; `y` → **past** assistant shows “Recalled successfully”.

### Phase 2.1 — E2E fails for the right reason — **complete**

- Un-ignored **Recall Just Review**; added missing step **`… styled in the Current guidance`** → `currentGuidance().expectContainsBold` (bold SGR + substring in replayed guidance).
- Failures surface as **missing expected text in Current guidance** (with replayed guidance + tail preview) or **bold styling** message, or **past assistant** missing `Recalled successfully` — not undefined steps or opaque PTY-only errors.

### Phase 2.2 — Pass E2E with minimum production change — **complete**

- Implement **next-due recall fetch** and **just-review** path using backend APIs (same conceptual operations as pre-removal `recallNext` / `markAsRecalled` — **re-derive names and structure**, do not paste old file).
- Render per `cli.mdc`: stage indicator if appropriate, notebook line in **current prompt**, body and y/n in **current prompt** vs guidance as per vocabulary table.
- Ensure **markdown rendering** matches expectations: “Put” bold, “sedation” emphasis, stripped markers from plain-text expectations in the feature.
- **Done:** `RecallsController.recalling` + `showMemoryTracker` + `getRecallPrompts` (reject pending MCQ / spelling with clear errors); `MemoryTrackerController.markAsRecalled`; `RecallJustReviewStage` + `/recall` registration; `y\r` PTY chunk handling; E2E PTY **48 rows** so `extractCurrentGuidanceFromReplayedPlaintext` sees recall content under the last `> ` line.

### Phase 2.3 — Edge cases (scenario scope only) — **complete**

- **Invalid key during y/n:** `cli/tests/recallJustReviewInteractive.test.tsx` drives `InteractiveCliApp` + stub API: empty Enter and `q`+Enter stay on prompt; `mark-as-recalled` fires once after `y`.
- **Empty details / missing notebook title:** Same file: whitespace-only title → `Note`, omitted `details` and `notebookTitle`, then `n` → `Marked as not recalled.`

---

## Phase 3 — Scenario: *Temp1 — multiple notes in session*

**User outcome:** Two notes due today; one `/recall` session; **y** twice in response to **Yes, I remember?** (second prompt after the first recall). Feature: `Temp1 - multiple notes in session` in `cli_recall.feature`.

### Phase 3.1 — E2E fails for the right reason

- Un-ignore **Temp1**; failures should cite missing **second** just-review prompt, wrong ordering of notes, or stuck session — not undefined steps or unrelated guidance.

### Phase 3.2 — Pass E2E with minimum production change

- Extend recall stage so **multiple due just-review items** run in one session: after first **y**, fetch/show the next item (reuse Phase 2 per-item rendering). Session state stays **stage-local** (roadmap §4.2); no load-more requirement for this slice.

### Phase 3.3 — Edge cases (scenario scope only)

- Only what Temp1 implies (e.g. invalid key between items) if not already covered by Phase 2.3; avoid duplicating load-more or full-session scenarios.

---

## Phase 4 — Scenario: *Temp2 — ending session with n*

**User outcome:** One note due; **y** on **Yes, I remember?**; **n** on **Load more from next 3 days?**; **past** assistant shows **Recalled 1 note** (singular). Feature: `Temp2 - ending session with n` in `cli_recall.feature`.

### Phase 4.1 — E2E fails for the right reason

- Un-ignore **Temp2**; failures should cite missing load-more prompt, wrong summary line, or pluralization — not Temp1-only iteration bugs alone.

### Phase 4.2 — Pass E2E with minimum production change

- After the last due item for the current window, surface **Load more from next 3 days?**; **n** ends the session and commits the **session summary** to **past assistant messages** with correct singular copy.
- Depends on session counter / summary path introduced with multi-item work (Phase 3); keep **one** coherent session model.

### Phase 4.3 — Edge cases (scenario scope only)

- **n** when no further items:** Align with API empty response; thin unit coverage if needed.

---

## Phase 5 — Scenario: *Recall session — complete all due, summary, load more*

**User outcome:** Multiple just-review items in one `/recall` session; summary “Recalled 2 notes”; “Load more from next 3 days?” **n** then exit; new `/recall` + **y** on load more → continue; final “Recalled successfully”. Builds on Phases 3–4 (Temp1 / Temp2); remove or fold those scenarios once this phase is green if the team prefers a single long scenario only.

### Phase 5.1 — E2E fails for the right reason — **complete**

- Un-ignored **Recall session** scenario; added **`When I answer … in the interactive CLI to prompt …`** — waits until **Current guidance** contains the prompt (same diagnostics as guidance assertions), then sends the line — so timeouts point at missing **Yes, I remember?** (second item), **Load more from next 3 days?**, or later steps fail past assistant with missing **Recalled 2 notes**.

### Phase 5.2 — Pass E2E with minimum production change

- Complete the **full** session flow Phases 3–4 do not cover alone: plural summary **Recalled 2 notes**, second `/recall`, **y** on load more, and continued just-review through **Recalled successfully**.
- **Load more** with `dueindays` (or equivalent API parameter) and any remaining session teardown; reuse the same session abstractions as Temp1/Temp2.

### Phase 5.3 — Edge cases (scenario scope only)

- **Load more when nothing left:** Unit-level behavior if API returns empty; do not duplicate MCQ/stop flows.
- **Session summary wording:** Edge pluralization unit tests if not E2E-covered.

---

## Phase 6 — Scenario: *Recall MCQ — choose correct answer and see success*

**User outcome:** OpenAI-mocked MCQ; stem and choices in **current guidance**; answer `1` → “Correct!” and “Recalled successfully” in **past** assistant messages.

### Phase 6.1 — E2E fails for the right reason

- Un-ignore; failure should reflect missing MCQ UI, wrong choice placement, or missing submit API — not mock misconfiguration only (if mock is wrong, fix steps/fixtures so the **reason** is still “feature missing”).

### Phase 6.2 — Pass E2E with minimum production change

- Integrate **MCQ recall prompt** from API; display stem + **numbered choices in current guidance** with ↑↓ selection per `cli.mdc`.
- Submit answer via existing recall/quiz endpoints (same domain as web recall).
- Ensure **current stage** / “Recalling” band behavior is consistent with roadmap §5 / vocabulary.

### Phase 6.3 — Edge cases (scenario scope only)

- **Wrong answer path:** Unit or Vitest for messaging if not covered by later scenarios.
- **Choice ordering / shuffling:** If API can shuffle, unit-test mapping from index to choice id — only if needed for MCQ correctness.

---

## Phase 7 — Scenario: *Recall MCQ — ESC cancels with y/n confirmation*

**User outcome:** `/stop` during MCQ → recall session stopped (per step `the recall session was stopped`); `/recall-status` still shows due count.

### Phase 7.1 — E2E fails for the right reason

- Un-ignore; failure should say stop/confirm/stop-session behavior missing, not MCQ from Phase 6.

### Phase 7.2 — Pass E2E with minimum production change

- Implement **`/stop`** (or equivalent documented in help) in recall stage: **y/n confirmation**, session teardown, no orphaned pending API state.
- Confirm **due note not consumed** incorrectly when stopping (status count unchanged).

### Phase 7.3 — Edge cases (scenario scope only)

- **n** on stop confirmation → return to MCQ (if product expects — only if scenario or help implies; otherwise defer to avoid scope creep).
- **Escape vs /stop:** If both exist, document one source of truth; unit-test the branch that E2E does not hit.

---

## Phase 8 — Scenario: *Recall MCQ — down arrow and Enter to select*

**User outcome:** Down-arrow moves selection; Enter submits **incorrect** choice; still ends with “Incorrect” (or equivalent) and “Recalled successfully” per feature.

### Phase 8.1 — E2E fails for the right reason

- Un-ignore; ensure step `When I input down-arrow selection for "/recall"…` is implemented or replaced — failure should point at **selection index** or **Enter handling**, not Phase 6-only code.

### Phase 8.2 — Pass E2E with minimum production change

- Align **list selection** with existing `MainInteractivePrompt` patterns (e.g. `cycleListSelectionIndex`) so MCQ choice navigation does not fork a second keyboard model.
- Wire Enter to submit selected index.

### Phase 8.3 — Edge cases (scenario scope only)

- **Wrap-around** at ends of choice list: unit tests if quick.
- **Width wrapping** of long choices: unit tests for line breaking if not E2E-stable.

---

## Phase 9 — Scenario: *Recall MCQ — contest and regenerate before answering*

**User outcome:** First question shown; `/contest` triggers legitimacy evaluation + regeneration; second stem appears; answer `1` → success messages.

### Phase 9.1 — E2E fails for the right reason

- Un-ignore; failures should cite **contest** command, mock sequence, or regeneration API — not generic MCQ.

### Phase 9.2 — Pass E2E with minimum production change

- Implement **`/contest`** in recall MCQ stage calling backend contest/regenerate flow (match **Gherkin table order** for OpenAI mocks).
- Preserve session/stage isolation: contest is a **subst**ep that returns to “show question” without leaking parent internals.

### Phase 9.3 — Edge cases (scenario scope only)

- **Contest when not allowed / API error:** User-visible error string — unit or Vitest if E2E too heavy.
- **Regenerate returns same stem:** Behavior defined by backend; CLI displays what it receives — unit test only if there is client-side logic.

---

## Phase 10 — Scenario: *Recall spelling — type correct spelling and see success* (second Rule block)

**User outcome:** After “Yes, I remember?” → **Spell:** prompt; typed `sedition` → “Correct!” and “Recalled successfully”.

### Phase 10.1 — E2E fails for the right reason

- Un-ignore; failure should indicate missing **SPELLING** path or wrong prompt sequence.

### Phase 10.2 — Pass E2E with minimum production change

- Extend recall flow for **spelling** question type from API; free-text input on command line with validation/submit API calls.
- Reuse markdown/styling rules from Phase 2 where applicable for the review phase before spelling.

### Phase 10.3 — Edge cases (scenario scope only)

- **Wrong spelling:** Message + whether note stays due — unit tests aligned with backend contract.
- **Case sensitivity / unicode:** Unit tests for normalization if client-side.

---

## After all phases

- Remove any remaining `@ignore` on these scenarios; confirm **CI policy** for CLI E2E (today only install feature is active — decide whether recall feature joins CI or stays tag-gated; document in team process, not necessarily in this file).
- **Optional:** Update `ongoing/cli-architecture-roadmap.md` with one short **decision record** (where recall stage lives, how `/stop` and `/contest` relate to command registry) if it clarifies future work.
- Delete or shrink **this** plan when revival is complete.

---

## Suggested dependency graph (top-level)

```mermaid
flowchart LR
  P1[Phase 1 status]
  P2[Phase 2 just review]
  P3[Phase 3 Temp1 multi]
  P4[Phase 4 Temp2 load-more n]
  P5[Phase 5 full session]
  P6[Phase 6 MCQ happy]
  P7[Phase 7 stop confirm]
  P8[Phase 8 MCQ arrows]
  P9[Phase 9 contest]
  P10[Phase 10 spelling]
  P1 --> P2
  P2 --> P3
  P3 --> P4
  P4 --> P5
  P2 --> P6
  P6 --> P7
  P6 --> P8
  P6 --> P9
  P2 --> P10
```

Phases 7–9 depend on MCQ (Phase 6). Phase 10 can proceed in parallel with Phases 3–9 after Phase 2 if resourcing splits, but **sequential delivery** per scenario order is the default.
