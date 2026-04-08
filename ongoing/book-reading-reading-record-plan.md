# Plan: Reading record (Story: Reading record)

**User story:** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *Story: Reading record* (scenarios below map **one scenario → one phase**).

**Architecture (ReadingRecord, BookRange, direct content vocabulary):** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md).

**UX context (drawer, book layout, current range, Reading Control Panel):** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) and shipped Story 2 work in [`ongoing/book-reading-read-a-range-plan.md`](book-reading-read-a-range-plan.md).

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per phase, scenario-first ordering, test-first workflow when adding behavior, at most one intentionally failing test while driving a phase.

**Testing for this story:** **Phase 2** uses **E2E** for the main book layout + prompt + mark behavior. **Phases 1, 3, and 4** do **not** add E2E; they rely on **unit-style tests** in the sense of `.cursor/rules/planning.mdc` — drive **observable** surfaces (HTTP responses from **controllers**, **mounted** Vue behavior via Vitest where the UI is the contract), use **black-box** inputs/outputs, prefer **few** focused tests over a 1:1 map to implementation files, and use **direct** tests only for deliberate small contracts (pure predicates, mapping, validation messages).

**This document is a delivery plan only** — not executed here.

---

## Principles for this work

- **Progress on chunks:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `ReadingRecord` refers to a **`BookRange`**, not a `SourceSpan`. Per-range **read / skim / skip** states belong on that model (or equivalent rows), not on arbitrary PDF coordinates.
- **Fine-grained “where on the page”** (exact scroll restore) is an **open architecture question** in the same roadmap; Phase 1 may persist a **viewport-aligned** snapshot (see below) without pretending it is a substitute for long-term `ReadingRecord` semantics.
- **Direct content** between two **book ranges** is **conceptual** today (no required DB column for the gap). Phases 3–4 rely on a **documented heuristic** (e.g. anchor proximity in **MinerU-normalized** space, same-page `y0` ordering, or “next range start immediately follows previous start”) — pick one rule per implementation and test it; revisiting the heuristic is allowed if product feedback demands it.
- **Book layout reading order** for “previous range” and auto-marking should match the **same linear order** the UI uses for the **current range** (depth-first preorder over the `BookRange` tree unless product explicitly chooses another walk).
- **Observable tests (this plan):** **Phase 2** — Cypress for the full reading-record UX. **Phases 1, 3, 4** — Spring **controller** tests (status, body, persistence side effects via follow-up reads) and/or **mounted** frontend tests where the behavior is UI-shaped; **pure** black-box tests for predicates and formatting. Avoid tests that only pin private helpers when a controller or mounted component already proves the path.

---

## Phase 1 — Remember book last read position

**User story scenario:** *Remember book last read position* — scroll to a position, leave, return, land on the **same** place.

**User outcome:** When the user opens the book reading page again (same user, same notebook book), the PDF **restores** to the **last reading position** they had in a prior session (or prior navigation away), within normal browser refresh constraints.

**Suggested shape (implementation detail, not fixed in this plan):**

- Persist a **viewport snapshot** keyed by **user + book** (notebook has at most one book): e.g. **page index** + **within-page vertical position** in the **same normalized space** already used for the **current range** (`0–1000` MinerU-style), or an agreed scalar stored server-side; avoid a second ad-hoc coordinate system if possible.
- **Save triggers:** debounced updates while scrolling (reuse debounce discipline similar to **current range** updates) and/or **save on visibility unload** — choose the smallest set that **passes tests** and feels reliable on mobile.
- **API:** e.g. read with book payload or dedicated `GET`/`PATCH` under the notebook book resource; follow existing controller and OpenAPI patterns, then `pnpm generateTypeScript`.

**Tests (no E2E for this phase):** Prove behavior through **observable** layers per `.cursor/rules/planning.mdc`:

- **Backend:** **`@WebMvcTest` / controller tests** (or full MVC slice if that is the project habit) — e.g. `PATCH`/`GET` returns expected JSON and **persists** position; **error** bodies for wrong notebook, unauthorized user, missing book.
- **Frontend:** **Mounted** `BookReadingPage` / composable tests (Vitest) — debounced **save** sends the API payload that matches the **viewport descriptor** already produced for **current range** logic (mock `fetch`/generated SDK); **restore on load** applies stored page/Y into the viewer contract **without** importing pdf.js internals.
- **Pure helpers** (if any): minimal **inputs → outputs** tests only where that API is the intentional contract.

**Out of scope for this phase:** Per-range read/skim/skip, prompts, read badges on **book ranges** in the book layout.

---

## Phase 2 — Mark a book range as read

**User story scenario:** *mark a book range as read* — at title “2.3 …”, answer **read** to whether the **direct content** of “2.2 …” was read; the **book layout** shows “2.3 …” (or the range that encodes the confirmed disposition—interpret per final copy) as **read**.

**UX (Reading Control Panel):** The user completes this flow from the **Reading Control Panel** — a **bottom-anchored** region **inside the PDF main pane** (see [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md)). **Expanded:** short context (which range’s **direct content** is in question) and **Mark as read** (and room for later skim/skip). **Minimized:** a **small bar** with **one or two** controls (e.g. expand + quick mark, or equivalent). The panel must **not** capture document scroll; PDF remains the hero. Product copy still ties the **question** to the **previous** range’s direct content in reading order unless the story is updated in `book-reading-user-stories.md` in the same PR.

**Clarify in implementation (product copy):** The Gherkin ties the **question** to the **previous** range’s direct content and an **observable mark** on a range; adjust labels and which row shows the checkmark so the UI matches the story **literally** or update the story in `book-reading-user-stories.md` in the same PR as the behavior. The plan assumes: user **confirms disposition** for **direct content belonging to a specific predecessor range** in reading order, and **persistence** reflects that as a **`ReadingRecord`** (or one row per user + range).

**User outcome:**

- Server persists **`ReadingRecord`** (per user, per `BookRange`) with at least **status = read** and sensible **timestamps** (`startedAt` / `lastReadAt` / `completedAt` as appropriate — minimal first slice: mark **completed** when they confirm read).
- Book JSON (or a dedicated endpoint the page already calls) exposes enough for the **book layout** to **render read state** for ranges that have a record.
- The user can complete **confirm read** from the panel **without** breaking PDF scroll, pinch/zoom, or drawer behavior; panel can be **minimized** after use if product wants that default.

**Data model:** New table/entity aligned with roadmap diagram: `User` + `BookRange` + status + timestamps; enforce **uniqueness** (user + book_range_id). Foreign keys consistent with existing `book_range` and user entities.

**E2E:** Fixture **book layout** with **distinct titles** “2.2 …” and “2.3 …” (or equivalent) → drive scroll/selection so the panel shows the **expected context** → user activates **Mark as read** (from expanded or minimized state, per chosen default) → assert **visible mark** on the correct **book range** (DOM attribute or text pattern stable for Cypress).

**Unit / focused tests (optional but useful):** Status transitions, invalid range id, wrong notebook/book — via controller or service black-box tests per `.cursor/rules/backend-development.mdc`.

---

## Phase 3 — Mark a range with no direct content as read automatically

**User story scenario:** *mark a book range with no direct content as read automatically* — no meaningful gap between title “xxx” and “ooo”; scrolling through **xxx** then **ooo** results in **xxx** shown as read **without** an explicit answer.

**User outcome:** When the system classifies **direct content between range A and the next range B in reading order** as **empty / not meaningful** (per the agreed heuristic in Principles), **entering B** (or passing the boundary—define one rule) **automatically** creates or updates **`ReadingRecord`** for **A** as read (or equivalent “no gap” disposition), and the **book layout** updates like Phase 2.

**Depends on:** Phase 2 (persistence and **book layout** display of read state).

**Tests (no E2E for this phase):**

- **Pure predicate** (if extracted): **no-direct-content** between two anchors / ranges — table-style **inputs → outputs**; no callers from tests that bypass the real rule.
- **Service or controller slice:** when the “next range” transition runs, assert **`ReadingRecord`** (or API response) shows **A** marked read **without** a user answer — use **real DB** `@Transactional` tests if that is the project pattern for this feature, or controller tests that observe **HTTP + repository** outcomes, **not** private method names.

Keep **one behavior’s assertions together** (one focused test class or `describe`) where practical.

---

## Phase 4 — Mark a book range as skimmed or skipped

**User story scenario:** *mark a book range as skimmed/skipped* (Gherkin to be completed in `book-reading-user-stories.md` when this phase starts).

**User outcome:** The same flow family as Phase 2 supports **skimmed** and **skipped** from the **Reading Control Panel** (expanded actions), aligned with **Direct content disposition** names in the architecture doc. The **book layout** distinguishes states **at least** as much as product requires (could be icon, label, or shared “touched” vs “completed” — decide in implementation; roadmap allows enum-style status).

**Depends on:** Phase 2 (and reuses Phase 3 heuristic only if skim/skip also applies to “no prompt” cases—optional; do not expand scope unless a single cohesive UX falls out naturally).

**Tests (no E2E for this phase):**

- **Controller / API:** `POST`/`PATCH` (or whatever surface answers the prompt) accepts **skimmed** and **skipped**; responses and **403/404** paths; persisted status round-trips on **GET** book or records endpoint.
- **Frontend:** **Mounted** tests — choosing **skim** vs **skip** in the prompt UI calls the SDK with the right enum/body and updates **book layout** presentation props or DOM hooks you expose for testing (prefer **user-visible** strings/roles over implementation-only `data-testid` unless the project already standardizes them).
- **Validation / enum:** illegal status rejected with stable **error text** or code if that is part of the public contract.

**API / schema:** Extend `ReadingRecord.status` (or equivalent) with **skimmed** and **skipped**; migrate forward-only per `.cursor/rules/db-migration.mdc`.

---

## Phase discipline (checklist)

After each phase:

1. **Clean up** dead code and temporary flags.
2. **Deploy gate** — commit/push and let CD deploy before the next phase unless the team agrees otherwise.
3. **Update this plan** — mark the phase done, drop obsolete notes, link to merged PR or components if helpful.
4. **Architecture doc** — if a **default** in [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) changes (e.g. how fine-grained position relates to `ReadingRecord`), update *Current directional choices* or *Open architecture questions* there in the same delivery stream.

---

## Document maintenance

When phases ship, trim duplication here; keep the architecture roadmap as the single place for long-lived conceptual rules. **Phase 2** may add or extend a Cypress feature path — link it here for onboarding. Phases 1, 3, and 4 rely on unit/controller/mounted tests only.
