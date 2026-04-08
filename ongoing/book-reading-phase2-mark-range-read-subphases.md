# Plan: Phase 2 sub-phases — Mark a book range as read

**Parent delivery plan:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — **Phase 2** (*Mark a book range as read*).

**User story (authoritative scenario):** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *mark a book range as read*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `ReadingRecord` per user + `BookRange`, progress on chunks, not `SourceSpan`.

**UX:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — Reading Control Panel placement, coexistence with PDF scroll and book layout.

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per sub-phase where possible; scenario-first ordering; observable tests; **no dead production code** (each sub-phase leaves only code that current tests or user flows use); at most one intentionally failing test while driving a change.

**Status (API shape):** **`GET …/book`** returns **book layout only** (ranges, anchors, metadata). Per-user reading state is **not** embedded on `BookRange`. It is exposed by **`GET /api/notebooks/{notebook}/book/reading-records`** (list of `{ bookRangeId, status, completedAt }` for the current user), plus **`PUT …/book/ranges/{bookRange}/reading-record`** to mark read. Sub-phase **2.14** (frontend) should merge **`getBook`** + **`getNotebookBookReadingRecords`** (generated SDK) instead of expecting read flags on each range in the book payload.

---

## Scenario (product contract)

```gherkin
Given I choose the book range "2.1 xxx"
And I scroll the PDF until the book range "2.2 xxx" is the current range in the book reader
When I mark the book range "2.1 xxx" as read in the Reading Control Panel
Then I should see that book range "2.1 xxx" is marked as read in the book layout
And I should see that book range "2.2 xxx" is selected in the book layout
```

**Interpretation for engineering**

| Phrase | Meaning |
|--------|--------|
| **Choose** the book range | User establishes **selected range** in the book layout (distinct from **current range** driven by PDF viewport where the product already distinguishes them — see shipped Story 2 sync). |
| **Scroll the PDF until** the book range "2.2 …" **is the current range** | PDF viewport + sync logic yield **current range** = that **book range** (same **reading-order walk** as today’s current-range highlight). |
| **Reading Control Panel** | Bottom-anchored control in the **PDF main pane** per UX roadmap; **does not** own document scroll. |
| **Mark "2.1 …" as read** | Records disposition for **the selected range’s direct content** (conceptually the gap **from 2.1’s start through the next range’s start in reading order** — architecture *direct content*). The disposition attaches to **book range 2.1**, not to 2.2. **Before persistence is wired in the UI (through 2.10):** session **in-memory** only. **From sub-phase 2.14 onward:** the **Reading Control Panel** persists via **`PUT …/reading-record`** and the client **refreshes read state** from **`GET …/book/reading-records`** (and may re-fetch **`GET …/book`** if layout must stay in sync). Backend **write** is **2.12**; **read list** contract is **2.13**. |
| **Marked as read in the book layout** | **Observable** styling on the **2.1** row: **right border** in a **fixed semantic color** (exact token: pick one DaisyUI / Tailwind semantic and reuse everywhere). |
| **"2.2 …" is selected** | After the action, the layout still reflects **2.2** as the **active selection** row (the same notion as “chosen” / selection affordance used elsewhere on the book reading page — **not** necessarily the same CSS as *current range* if the product keeps two tracks; the scenario asserts the **selection** track matches 2.2). **Design rule:** Marking read **must not** steal selection back to 2.1 or clear selection in a way that contradicts this Then. |

**Panel visibility rule (from product intent)**

- The **selected range** is the range whose **direct content** the user is treating as “in progress.”
- The **Reading Control Panel** is shown **only when** the **current range** (viewport-derived) is the **immediate successor** of the **selected range** in **book layout reading order** (same linear order as current-range computation: **depth-first preorder** unless an explicit product decision changes it — if so, update architecture roadmap once).
- When the user has **not** yet scrolled to that successor, the panel is **hidden** (or not mounted — implementation detail; tests assert **observable** presence/absence).
- This replaces the older plan wording (“question tied to the **previous** range”) with an equivalent **selection + successor** formulation aligned with the user story.

---

## Design decisions (record)

1. **Frontend-first, in-memory through 2.10** — Sub-phases **2.1–2.10** drive the **in-memory** story **from Cypress** (see **E2E-driven cycle** below). **No** requirement that the scenario **survives reload** or **hydrates from server** until the **frontend persistence slice (2.14)** — the Cypress path may stay in **one session** with state held in the running app through **2.10**. **Sub-phase 2.11** may add **only** the **Flyway** table for reading records (**no** application code using it yet). **No** reading-record **HTTP** surface until **2.12** (write) and **2.13** (**`GET …/book/reading-records`** + OpenAPI/TS — **not** embedded in **`GET …/book`**).

2. **Identity of “read” (after schema + backend)** — One row per **(user, book_range_id)** with a **status** enum; ship only **`READ`** in the Phase 2 backend slice (parent Phase 4 adds skim/skip). **Uniqueness** enforced in DB (**2.11**) and application layer (**2.12+**).

3. **Timestamps (backend write slice)** — On first “mark read,” set **`completedAt`** (keep minimal: one **completion** instant for Phase 2; avoid unused columns). Implemented with **2.12** write path.

4. **API surface (split across 2.12 / 2.13)** — Prefer **few cohesive endpoints**:
   - **Write (2.12):** **`PUT`** `.../book/ranges/{rangeId}/reading-record` → **204**; **stable error shapes** for wrong notebook / range not in book / unauthorized.
   - **Read (2.13):** **`GET`** `.../book/reading-records` → JSON array of **`BookRangeReadingRecordListItem`** (`bookRangeId`, `status`, `completedAt`) for the **current user** and **that notebook’s book** — **one bulk query** server-side, no per-range N+1. **`GET …/book`** stays **unchanged** (no transient fields on **`BookRange`**).

5. **OpenAPI + TypeScript (2.13)** — Land OpenAPI changes **with** the **reading-records** endpoint in **2.13**, then **`pnpm generateTypeScript`**. The **frontend** (**2.14**) **must** use **generated** types for **`BookFull` / `BookRangeFull`** and for the **reading-records** list — **no** long-lived parallel hand-written DTO for the same payloads. Sub-phases **2.1–2.10** may keep a **minimal interim** client shape; **2.14** **refactors** so read borders derive from **merged** book layout + **reading-records** response (or equivalent client merge), not from fields on each range in **`GET …/book`**.

6. **Right border = read** — Single **semantic** color; optional **non-color cue** for a11y in the **green** sub-phase that implements the first **`Then`** (see **2.8** in the table below).

7. **After “Mark as read”** — **2.1–2.10:** update **client** read state immediately (**in-memory**); **do not** change **selection** away from **2.2** per scenario. **2.14 onward:** after successful **PUT**, **refresh reading records** (**GET …/book/reading-records**) so borders match the server; re-fetch **`GET …/book`** only if the layout payload must be refreshed for another reason; same selection rule.

8. **Out of scope for Phase 2** — Auto-mark when no direct content (parent **Phase 3**), skim/skip (parent **Phase 4**), persisting panel expand/minimize preference, fine-grained in-range scroll restore beyond existing Phase 1 work.

---

## Sub-phases (E2E-led in-memory slice, then persistence)

**In-memory work (2.1–2.10)** follows an **E2E-driven cycle** aligned with `.cursor/rules/planning.mdc` (**at most one intentionally failing test** while driving a slice). **Persistence** is **after** the full scenario is **green** without server-backed reading state in the UI, split into **2.11–2.14** below (schema first, then backend write, then **reading-records** **`GET`** + contract, then frontend).

### E2E-driven cycle (repeat for each new Gherkin step)

Use this **two-beat** pattern (your **x.1 / x.2**); **x.3** is **“uncomment the next step and repeat”** — i.e. start the next **x.1**.

| Beat | Goal |
|------|------|
| **Red (odd sub-phase: 2.1, 2.3, 2.5, …)** | Add the **full** scenario to [`e2e_test/features/book_reading/reading_record.feature`](e2e_test/features/book_reading/reading_record.feature) (or adjacent feature) with the **exact** user-story Gherkin. **Uncomment only the next step** toward the end state; **comment out** all **later** steps (Gherkin `#` lines) **or** equivalent (separate `@wip` scenario with a single new step — pick **one** approach per repo habit). **Run Cypress** and confirm **one** failure for the **right reason** (**feature not implemented**), with a **clear** step definition / assertion message (fix wording if the failure is ambiguous). Improve step defs / page objects **in this beat** if the harness cannot express the step yet. **Do not** leave multiple new steps failing at once. |
| **Green (even sub-phase: 2.2, 2.4, 2.6, …)** | **Minimum** production change to make the **currently uncommented** scenario prefix **pass**. **No dead code**; remove scaffolding; keep code clean. **Optional** small **mounted** tests only if they reduce duplication without mirroring structure — E2E remains the **driver** for this slice. |

Then **uncomment the next line** → **Red** again → **Green** again, until **all** lines are active and the scenario passes.

**Fixture:** Use a book with **distinct** titles **2.1 …** and **2.2 …** from the first **Red** beat that needs them; step defs per [`e2e_test.mdc`](.cursor/rules/e2e_test.mdc). **No** reload / re-navigation / server read-state requirements for **2.1–2.10**.

### Mapping: Gherkin line → sub-phases

| Order | Gherkin (uncomment up to here) | Red | Green (minimum outcome) |
|-------|--------------------------------|-----|-------------------------|
| 1 | `Given I choose the book range "2.1 xxx"` | **2.1** | **2.2** — selection + stable hook for “chosen” range |
| 2 | `And I scroll the PDF until the book range "2.2 xxx" is the current range in the book reader` | **2.3** | **2.4** — scroll / viewport drives **current range** to **2.2**; assert in E2E |
| 3 | `When I mark the book range "2.1 xxx" as read in the Reading Control Panel` | **2.5** | **2.6** — panel at **successor** boundary + **Mark as read** updates **in-memory** state for **2.1** (no HTTP) |
| 4 | `Then I should see that book range "2.1 xxx" is marked as read in the book layout` | **2.7** | **2.8** — **right border** + same **DOM hook** as Cypress; optional non-color cue (design **6**) |
| 5 | `And I should see that book range "2.2 xxx" is selected in the book layout` | **2.9** | **2.10** — selection **still** **2.2** after mark; **full** scenario **green** |

If a **single** Gherkin step is **too large** (two user-visible outcomes), **split** it in the feature file with **intermediate** steps **still commented** until their Red/Green pair — **never** more than **one** new failing assertion driver per Red beat.

---

### Phase 2.11 — Reading-record **schema only** (Flyway)

**User-visible value:** **No** (no product behavior change until a later sub-phase uses the table).

**After** **2.10** is green (or in the same delivery stream as agreed by the team).

**Deliverables**

- **DB:** Flyway migration — table for per-user per-range reading state (e.g. **`book_range_reading_record`**): **`user_id`**, **`book_range_id`**, **`status`**, **`completed_at`**; FKs to **`user`** and **`book_range`** with **`ON DELETE CASCADE`**; **`UNIQUE (user_id, book_range_id)`**.

**Explicit non-goals**

- **No** JPA entity, **no** repository, **no** HTTP endpoints, **no** OpenAPI / TypeScript changes, **no** frontend changes.

**Tests / verification**

- Migration applies cleanly (e.g. **`migrateTestDB`** / CI pipeline used by this repo).

---

### Phase 2.12 — Backend: **write** path (mark read)

**User-visible value:** **No** from the browser until **2.14** (API exists but UI may still be in-memory).

**Deliverables**

- **JPA** entity + **`ReadingRecordRepository`** mapping to the **2.11** table.
- **`BookService`:** **`markRangeRead`** (validate range belongs to notebook’s book; idempotent upsert acceptable — document in tests).
- **`NotebookBooksController`:** **`PUT /api/notebooks/{notebook}/book/ranges/{bookRange}/reading-record`** → **204**; **`assertReadAuthorization`** aligned with reading-position endpoints.
- **No** new **read** endpoint in **2.12**; **no** change to **`GET …/book`**. Prefer bundling **OpenAPI** updates with **2.13** when the **reading-records** **`GET`** is added.

**Tests**

- **Spring** controller (and/or **`@Transactional`**) tests: row persisted; **404** wrong notebook / no book / range not in book; **403** read access; **idempotent** second **PUT** behavior.

**Explicit non-goals**

- **`GET …/book/reading-records`** or embedding read state on **`BookRange`** (that is **2.13**).

---

### Phase 2.13 — Backend: **`GET …/book/reading-records`** + **OpenAPI** + **TypeScript**

**User-visible value:** **No** from the browser until **2.14** (contract ready for the client).

**Deliverables**

- **`BookService`:** **`listReadingRecordsForBook(notebook, user)`** — resolve the notebook’s **book**, then **bulk-load** all **`BookRangeReadingRecord`** rows for **(user, book)** in **one query**; map to **`BookRangeReadingRecordListItem`** (`bookRangeId`, `status`, `completedAt`).
- **`NotebookBooksController`:** **`GET /api/notebooks/{notebook}/book/reading-records`** → **200** + JSON array; **`assertReadAuthorization`** same as book read.
- **Do not** add **transient** or **embedded** read fields on **`BookRange`**; **`GET …/book`** remains layout-only.
- Regenerate **OpenAPI**; **`pnpm generateTypeScript`**; **OpenAPI** approval test golden updated as needed.

**Tests**

- **Spring:** list includes **only** the **current user’s** rows; **only** ranges in that book; **empty array** when none; **does not** leak another user’s rows.
- **OpenAPI** snapshot test passes after intentional doc update.

**Explicit non-goals**

- Frontend wiring (**2.14**).

---

### Phase 2.14 — Frontend: **API-backed** read state (**last** persistence sub-phase)

**User-visible value:** **Yes** — **Mark as read** persists; read border and panel logic can use **server** state; reload can show read state once **`getBook`** + **`getNotebookBookReadingRecords`** run on load (still **no** new Cypress requirement to **force** reload — optional future scenario).

**Deliverables**

- **`BookReadingPage`:** remove **in-memory** read **`Set`**; derive border / panel visibility / sr-only copy from **`getNotebookBookReadingRecords`** (e.g. build a **`Set`** of **`bookRangeId`** with **`status === READ`**) merged with **`getBook`** layout.
- **`markSelectedRangeAsRead`:** **`putNotebookBookRangeReadingRecord`** then **`getNotebookBookReadingRecords`** (and **`getBook`** only if needed); **`apiCallWithLoading`** (or equivalent project pattern); preserve **selection → successor** behavior from **2.10** tests.
- Extend **`makeMe` / builders** only if needed so tests stay on **`@generated`** types.

**Tests**

- **Vitest (mounted):** mock **PUT** + second **`getNotebookBookReadingRecords`** returning an item for the marked **`bookRangeId`**; keep existing reading-control assertions green.
- **Fixture test:** merged **`BookFull`** + reading-records list → assert **read** border / hooks match **bookRangeId** in the list.

**Explicit non-goals**

- Changing **2.1–2.10** Gherkin to **require** reload (optional **future** scenario).

---

## Dependency graph (summary)

```text
2.1 red ──► 2.2 green ──► 2.3 red ──► 2.4 green ──► 2.5 red ──► 2.6 green
    ──► 2.7 red ──► 2.8 green ──► 2.9 red ──► 2.10 green (full E2E)
                                                              │
                        ┌─────────────────────────────────────┼─────────────────────────────────────┐
                        ▼                                     ▼                                     ▼
                    2.11 Flyway                           2.12 PUT                           2.13 GET+OpenAPI
                    (schema only)                      mark read write              reading-records list + TS
                                                              │                                     │
                                                              └──────────────────┬──────────────────┘
                                                                                 ▼
                                                                            2.14 Frontend
                                                                            (persisted UX)
```

**2.11–2.14** are strictly **after** **2.10**. **Order:** **2.11** → **2.12** → **2.13** → **2.14** (each may be its own PR). **No parallelization** of the **2.1–2.10** red/green sequence without breaking the **one failing step** rule.

---

## Phase discipline (each sub-phase)

1. **Tests first or alongside** — red → green for the **observable** surface of **that** sub-phase.
2. **No dead production code** — no unused endpoints, components, or DB columns; **remove** scaffolding before merge. After **2.14**, **remove** interim client-only read state so the book tree and read flags use **generated** types from **`GET …/book`** and **`GET …/book/reading-records`** (decision **5**).
3. **No stray feature flags** — behavior is **on** when merged.
4. **Deploy gate** — parent plan: commit/push/CD between **parent** phases; for **sub-phases**, follow team habit (often **one PR per red/green beat** or **pair 2.n+2.n+1** when practical).
5. **Update documents** — when shipped, trim this file’s speculative options; align [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) Phase 2 bullet list with **completed** sub-phases; refresh **Current directional choices** in the architecture roadmap only if a **default** changed (e.g. reading-order walk).

---

## Relationship to parent Phase 2 text

[`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) Phase 2 described **predecessor-range** copy and **2.2 / 2.3** example titles. **This document** aligns Phase 2 with [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md): **selected range 2.1**, **current 2.2**, **panel at successor boundary**, **read border on 2.1**, **selection stays on 2.2**. When implementing, **either** match this story **literally** or **update the user story in the same PR** — no drift between Gherkin and behavior.

---

## Open points

1. **Exact HTTP paths and DTO names** — **`PUT …/book/ranges/{bookRange}/reading-record`**, **`GET …/book/reading-records`** (`BookRangeReadingRecordListItem`); **`GET …/book`** unchanged.
2. **Idempotency** — second “mark read” is **no-op** or **updates timestamp**; document chosen behavior in API tests (**2.12**).
3. **Selection vs current range styling** — if the scenario’s “selected” is **ambiguous** with **current range** after scroll, **product + a11y** pick one mapping; encode in **one** place (page object + live region strategy if any) — lock by **2.10** so hooks stay stable.
