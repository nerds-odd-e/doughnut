# Interactive `/add gmail` — blocking stage (unmount main prompt)

Informal plan: apply the **stage + unmount main prompt** architecture to `/add gmail` only. Generalize to other slash commands only after a second command needs the same pattern (see `.cursor/rules/planning.mdc` — avoid a framework before repetition).

**Hard constraint:** The first scenario in `e2e_test/features/cli/cli_gmail.feature` — *add gmail adds account when OAuth callback is simulated* — must **keep passing** after each phase that touches product code (assertion unchanged: `Added account e2e@gmail.com` in **past** CLI assistant messages).

**Related:** E2E wiring, Mountebank, bundle secrets, and PTY OAuth simulation are described in `ongoing/cli-gmail-e2e-phases.md` (Phase 1). This document owns **shell UX and interactive structure** for `/add gmail`.

**Roadmap:** `ongoing/cli-architecture-roadmap.md` §4 (stages), §5 (Ink composition).

---

## Target behavior (user-visible)

| Behavior | Rule |
|----------|------|
| Past transcript | User’s `/add gmail` line appears in **past user messages** as today. **Only the final** success or error assistant line is appended to the transcript when the operation finishes — no spinner frames or interim status lines in the persisted message list. |
| While OAuth/API work runs | **Main command line is unmounted** (no `useInput` on the default prompt): user cannot type a new slash command in the main buffer. **Live** UI shows a **spinner** and a short status message (copy TBD; one line is enough). |
| When finished | Spinner/stage UI unmounts; **main prompt remounts**; one assistant message appended (success wording must still satisfy E2E: includes `Added account <email>`). |

**Cancellation (optional follow-up):** Roadmap expects Escape where appropriate; if not in scope for this slice, defer explicitly rather than half-implementing.

---

## Phase 1 — Blocking stage + unmount main prompt (single user-visible slice)

**Outcome:** `/add gmail` matches the table above; first Gmail E2E scenario still green; Vitest proves the new behaviors through **`render(InteractiveCliApp)`** (or `runInteractive` if you already standardize on it for this app — pick **one** entry point and stay consistent with `cli.mdc`).

### 1.1 — Unit tests first (observable, no structure-mirroring)

Add or extend tests **next to** existing interactive Gmail coverage (prefer `cli/tests/InteractiveCliApp.addGmail.test.tsx` or merge into `InteractiveCliApp.test.tsx` if that keeps “one behavior, one place” — avoid a new file unless the describe block becomes unreadable).

Drive **stdout / frames** only; do not assert on internal React state or private module names.

Suggested cases (adjust naming to match final copy):

1. **While work is in flight (controlled async):** Mock `addGmailAccount` (or the smallest hook the stage calls) so it resolves after a microtask delay. After stdin sends `/add gmail` + Enter, within that window: output shows the **spinner/status** region and **does not** show the normal interactive **`> `** command line (main prompt unmounted). Use `setImmediate` / turn-limited wait helpers; **no fixed `setTimeout` ms** (`cli.mdc`).

2. **Transcript / past assistant content:** After the mocked flow **completes**, the combined output contains **exactly one** new assistant outcome for that command (e.g. success or error string), not repeated interim lines that read like final assistant messages. If asserting “no interim in past” is awkward in one test, split: one test for “spinner visible while pending”, one for “final assistant line once” — still **one describe** focused on `/add gmail` stage behavior.

3. **Regression:** Keep the existing **missing OAuth credentials** path covered (`InteractiveCliApp.addGmail.test.tsx`): user still sees the user line + a single clear error in the transcript; behavior may move into the stage but the **observable** outcome stays the same.

**While driving Phase 1, keep at most one intentionally failing test** at a time (`planning.mdc`).

### 1.2 — Product: extract main prompt, mount `/add gmail` stage

- Extract the default **`> `** line, `useInteractiveCliLineBuffer`, and the **`useInput`** handler that commits lines and dispatches slash commands into a **main-only** subtree (e.g. `MainInteractivePrompt` or equivalent name). Parent `InteractiveCliApp` holds **transcript** `messages` and **stage** state (`main` vs `addGmail`).

- On **`/add gmail`** match: append **user** transcript line; set stage to **add gmail**; **do not** mount the main prompt subtree until the stage completes.

- Implement **`AddGmailStage`** (or equivalent): Ink UI with **Spinner** + message; run **`addGmailAccount`** (reuse `cli/src/commands/gmail.ts`); on settle append **one** assistant `TranscriptMessage`; clear stage back to **main** (main prompt remounts).

- Other slash commands and “Not supported” paths stay on the **main** prompt only — no change unless required for compilation/cohesion.

- Keep **`createAddGmailCommand`** as the place for **docs + line string** if helpful; the **async work + UX** may live in the stage module to avoid splitting orchestration across three places. Prefer **one clear owner** for “what runs when user submits `/add gmail`”.

### 1.3 — E2E gate

Run the first scenario only (single spec):

`CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_gmail.feature`

Filter by tag if your Cypress setup supports running one scenario; otherwise run the file and rely on scenario 2 remaining `@ignore` until Phase 2 of the Gmail plan.

Confirm **Then I should see "Added account e2e@gmail.com" in past CLI assistant messages** still passes (spinner must not leak into the **past assistant** section that page objects parse).

### 1.4 — Plan hygiene

- Update **`ongoing/cli-gmail-e2e-phases.md`** Phase 1.4 bullet to point here for the interactive shell behavior (or merge one short paragraph) so future readers do not assume the old “fire `run()` and append assistant when promise resolves” shape without staging.

- When this slice is done and stable, you may delete **this** file or trim it to a one-line pointer — per your usual `ongoing/` cleanup.

---

## Out of scope (until a second command needs it)

- A generic “every slash command may register a React stage” registry.
- Changing **`InteractiveSlashCommand`** type for all commands — only introduce a shared type or optional field **after** a second staged command exists (`planning.mdc`).

---

## Commands (reference)

- CLI unit tests: `CURSOR_DEV=true nix develop -c pnpm cli:test` (or targeted `pnpm -C cli test tests/InteractiveCliApp.addGmail.test.tsx`).
- Gmail E2E feature: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_gmail.feature`

Cloud VM: drop the `nix develop -c` wrapper per `cloud-agent-setup.mdc`.
