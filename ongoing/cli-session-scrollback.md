# CLI — SessionScrollback (replace in-tree past messages)

**Status:** Plan only — not started.

**Intent:** Replace the current `messages` + `flatMap` rendering in `InteractiveCliApp` with a **single** Ink `<Static>` region (**SessionScrollback**), append-only, **content-agnostic** at the shell: callers push **typed entries**; one place maps entries to Ink UI (`PastUserMessageBlock`, plain `Text`, future recall blocks). Aligns with Ink’s one-`staticNode` model and with `ongoing/cli-recall-answered-questions.md` (answered content must survive recall unmount).

**Out of scope for this document:** Full rich recall answered UI — that stays in the recall plan; this plan only delivers the **plumbing** and shell migration, then a **minimal** recall-driven append to prove the API.

**Constraints**

- **Exactly one** `<Static>` mounted under the interactive Ink root (see Ink 6 reconciler: `rootNode.staticNode` is singular).
- **Callers push data**, not arbitrary `ReactNode` blobs: discriminated entries + central render (or a small kind→component map) keeps SessionScrollback agnostic and testable.
- **Stable keys:** each entry carries a monotonic `id` (or equivalent) for Static child keys — avoid array index keys for append-only lists.
- **Terminal layout rules:** reuse existing width / wrapping helpers for any new entry shapes; no raw string `.length` for columns.

---

## Phase 1 — SessionScrollback + interactive shell parity

**User-visible outcome:** The interactive CLI looks and behaves like today: initial version line, committed user lines (gray block + spacing rules), assistant lines, stage + command line below; `/exit` ordering unchanged.

**Work (concise)**

- Add **SessionScrollback**: holds `useState` (or reducer) for `ScrollbackEntry[]`, renders `<Static items={entries}>…</Static>` above the dynamic column (stage + `MainInteractivePrompt`).
- Define **`ScrollbackEntry`** as a discriminated union; **phase 1** only needs variants that reproduce current `TranscriptMessage` rendering (e.g. `user_line`, `assistant_text` — names TBD; map `role: user|assistant` into these or rename at call sites).
- **Central renderer** component (e.g. `ScrollbackLine`) switches on `entry.kind` and returns the same Ink tree as today (`PastUserMessageBlock`, `Text`, gap `Box` height 1 between user and following assistant where applicable).
- **InteractiveCliApp** stops using `messages` for the main column; all former `setMessages` paths call **`appendScrollbackEntry`** (or a single `appendEntries` batch where order matters in one commit).
- Keep **`TranscriptMessage`** only if still useful for slash-command docs/tests; otherwise fold into `ScrollbackEntry` and update imports.

**Tests**

- **Vitest / Ink:** Existing `InteractiveCliApp` (and any related) tests stay green; assert stdout still contains the same patterns (version, user block styling, assistant text, gaps).
- **E2E:** No new scenarios required if parity is complete; if CI catches a drift, add or tighten one assertion on an existing `cli_install_and_run` or recall feature — prefer the smallest existing spec.

**Phase-complete when:** Shell-only flows (version banner, unsupported line, `/help`, `/exit`, stage settle messages) match current behavior under Static.

---

## Phase 2 — Append API for stages (context); recall uses it once

**User-visible outcome:** Recall (or another stage) can add at least **one** scrollback entry that is **not** only the generic `onSettled(assistantText)` path — proving stages write to the same SessionScrollback the shell uses.

**Work**

- Expose **`SessionScrollbackContext`** (or hook `useSessionScrollback`) providing **`appendScrollbackEntry`** / **`appendScrollbackEntries`**, implemented with functional `setState` updates. Provider wraps the same subtree as today (`InteractiveCliApp` body inside existing providers).
- **Do not** add a second `<Static>` inside `RecallSessionStage` (or any stage).
- Extend **`InteractiveSlashCommandStageProps`** only if a callback must be explicit for typing/testing; **prefer context** so `createElement(Stage, …)` does not need new props on every stage.
- **Minimal recall integration:** pick the smallest real path (e.g. one answered-summary line or one session line already specified in `cli-recall-answered-questions.md` phase 1) and append via the new API instead of (or in addition to) local `answeredRecallLines` **only as much as needed** to prove wiring — avoid duplicating the entire recall UI plan in this phase.

**Tests**

- **Vitest:** `runInteractive` through recall (or the chosen path); spy APIs with `makeMe` as today; assert new scrollback content appears in stdout in the **past** transcript region (same helpers as existing CLI tests).
- **E2E:** Extend an existing `cli_recall.feature` scenario with one `Then` if the new line is user-visible and not already covered.

**Phase-complete when:** At least one recall code path appends via SessionScrollback; no second Static; tests green.

---

## Phase 3 — Recall “answered region” ownership (optional slice, may overlap recall doc)

**User-visible outcome:** Rich or plain answered recall rows (per `cli-recall-answered-questions.md`) **primarily** live as scrollback entries, so they remain after recall unmount without reimplementing a second Static.

**Work**

- Migrate **`answeredRecallLines`** (and related) toward **scrollback entries** with new `kind` variants + renderer branches; remove redundant in-stage duplication once scrollback is authoritative for “frozen above.”
- Coordinate **copy and E2E** expectations with `ongoing/cli-recall-answered-questions.md` so one plan does not fight the other — this phase can be merged into that plan’s phases if the team prefers a single file.

**Tests:** Follow the recall plan’s Vitest + E2E scope; SessionScrollback adds no new test layer beyond entry types.

**Note:** If Phase 2 already moves all recall persistence into scrollback, **Phase 3 shrinks or merges** — update this document when that becomes clear.

---

## Design notes (carry through)

| Topic | Choice |
|--------|--------|
| Entry identity | Monotonic `id` (e.g. counter ref or `crypto.randomUUID` per append) on every entry for React keys |
| Shell vs recall | One provider at `InteractiveCliApp`; recall imports hook |
| `onSettled` | Keep for stage completion assistant line **or** route through `appendScrollbackEntry` inside handler — avoid double append |
| Future non-recall stages | Same hook; new `ScrollbackEntry` variants + renderer cases |

---

## References

- `cli/src/InteractiveCliApp.tsx` — current `messages` / `PastUserMessageBlock` layout
- `cli/src/commands/interactiveSlashCommand.ts` — `TranscriptMessage`, stage props
- `cli/src/commands/recall/RecallSessionStage.tsx` — `answeredRecallLines` / chrome
- `ongoing/cli-recall-answered-questions.md` — scrollback persistence requirement for answered blocks
- Ink 6: single `staticNode`; `<Static items={…}>` append-only semantics
