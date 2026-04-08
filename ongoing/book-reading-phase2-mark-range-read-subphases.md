# Plan: Phase 2 sub-phases — Mark a book range as read

**Parent delivery plan:** [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) — **Phase 2** (*Mark a book range as read*).

**User story (authoritative scenario):** [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md) — *mark a book range as read*.

**Architecture:** [`ongoing/doughnut-book-reading-architecture-roadmap.md`](doughnut-book-reading-architecture-roadmap.md) — `ReadingRecord` per user + `BookRange`, progress on chunks, not `SourceSpan`.

**UX:** [`ongoing/book-reading-ux-ui-roadmap.md`](book-reading-ux-ui-roadmap.md) — Reading Control Panel placement, coexistence with PDF scroll and book layout.

**Planning rules:** `.cursor/rules/planning.mdc` — one **user-visible** behavior per sub-phase where possible; scenario-first ordering; observable tests; **no dead production code** (each sub-phase leaves only code that current tests or user flows use); at most one intentionally failing test while driving a change.

**Status:** Planning only — **do not execute** until picked up for implementation.

---

## Scenario (product contract)

```gherkin
Given I choose the book range "2.1 xxx"
And I scroll to title "2.2 xxx"
When I mark the book range "2.1 xxx" as read in the Reading Control Panel
Then I should see that book range "2.1 xxx" is marked as read in the book layout
And I should see that book range "2.2 xxx" is selected in the book layout
```

**Interpretation for engineering**

| Phrase | Meaning |
|--------|--------|
| **Choose** the book range | User establishes **selected range** in the book layout (distinct from **current range** driven by PDF viewport where the product already distinguishes them — see shipped Story 2 sync). |
| **Scroll to** title "2.2 …" | PDF viewport + sync logic yield **current range** = the range whose title matches "2.2 …" (same **reading-order walk** as today’s current-range highlight). |
| **Reading Control Panel** | Bottom-anchored control in the **PDF main pane** per UX roadmap; **does not** own document scroll. |
| **Mark "2.1 …" as read** | Records disposition for **the selected range’s direct content** (conceptually the gap **from 2.1’s start through the next range’s start in reading order** — architecture *direct content*). The disposition attaches to **book range 2.1**, not to 2.2. **Before the persistence sub-phase (2.11):** session **in-memory** only. **From 2.11 onward:** server **`ReadingRecord`** (`ReadingRecord` → `BookRange`) via DB + API. |
| **Marked as read in the book layout** | **Observable** styling on the **2.1** row: **right border** in a **fixed semantic color** (exact token: pick one DaisyUI / Tailwind semantic and reuse everywhere). |
| **"2.2 …" is selected** | After the action, the layout still reflects **2.2** as the **active selection** row (the same notion as “chosen” / selection affordance used elsewhere on the book reading page — **not** necessarily the same CSS as *current range* if the product keeps two tracks; the scenario asserts the **selection** track matches 2.2). **Design rule:** Marking read **must not** steal selection back to 2.1 or clear selection in a way that contradicts this Then. |

**Panel visibility rule (from product intent)**

- The **selected range** is the range whose **direct content** the user is treating as “in progress.”
- The **Reading Control Panel** is shown **only when** the **current range** (viewport-derived) is the **immediate successor** of the **selected range** in **book layout reading order** (same linear order as current-range computation: **depth-first preorder** unless an explicit product decision changes it — if so, update architecture roadmap once).
- When the user has **not** yet scrolled to that successor, the panel is **hidden** (or not mounted — implementation detail; tests assert **observable** presence/absence).
- This replaces the older plan wording (“question tied to the **previous** range”) with an equivalent **selection + successor** formulation aligned with the user story.

---

## Design decisions (record)

1. **Frontend-first, in-memory until 2.11** — Sub-phases **2.1–2.10** drive and implement the **in-memory** story **from Cypress** (see **E2E-driven cycle** below). **No** Flyway, **no** new reading-record HTTP surface until **2.11**, and **no** requirement that the scenario **survives reload** or **hydrates from server** — the Cypress path stays in **one session** with state held in the running app.

2. **Identity of “read” (after 2.11)** — One row per **(user, book_range_id)** with a **status** enum; ship only **`READ`** in the Phase 2 backend slice (parent Phase 4 adds skim/skip). **Uniqueness** enforced in DB and application layer.

3. **Timestamps (after 2.11)** — On first “mark read,” set **`completedAt`** (and **`lastReadAt`** only if the schema uses both — keep minimal: one **completion** instant is enough for Phase 2; avoid unused columns).

4. **API surface (2.11 only)** — Prefer **few cohesive endpoints**:
   - **Write:** e.g. `PUT` or `PATCH` under the **notebook book** resource **or** `.../book/ranges/{rangeId}/reading-record` — **choose one** and mirror existing Spring/OpenAPI style; **stable error shapes** for wrong notebook / range not in book / unauthorized.
   - **Read:** Extend **`GET …/book`** so each **range node** carries enough for the layout border (e.g. **`readingRecord`** or **`readingStatus`**) — **avoid a second round-trip** on every paint if possible.

5. **OpenAPI + TypeScript (2.11)** — Land OpenAPI changes **with** the backend in **2.11**, then **`pnpm generateTypeScript`**. The **frontend’s authoritative model** for the loaded book (including per-range reading fields) **must** use the **generated API types** — **no** long-lived parallel hand-written DTO for the same payload. Sub-phases **2.1–2.10** may use a **minimal interim** shape **only** where it reduces churn; **2.11** **refactors** so client updates operate on **the same types** the SDK returns for **`GET …/book`** (compose or narrow generated types; do not duplicate field names in a second schema).

6. **Right border = read** — Single **semantic** color; optional **non-color cue** for a11y in the **green** sub-phase that implements the first **`Then`** (see **2.8** in the table below).

7. **After “Mark as read”** — **2.1–2.10:** update **client** read state immediately; **do not** change **selection** away from **2.2** per scenario. **2.11 onward:** after successful write, **refresh book** from server (or optimistic update + reconcile) so border and **`GET …/book`** stay aligned; same selection rule.

8. **Out of scope for Phase 2** — Auto-mark when no direct content (parent **Phase 3**), skim/skip (parent **Phase 4**), persisting panel expand/minimize preference, fine-grained in-range scroll restore beyond existing Phase 1 work.

---

## Sub-phases (E2E-led in-memory slice, then persistence)

**In-memory work (2.1–2.10)** follows an **E2E-driven cycle** aligned with `.cursor/rules/planning.mdc` (**at most one intentionally failing test** while driving a slice). **Persistence (2.11)** is **after** the full scenario is **green** without server-backed reading state.

### E2E-driven cycle (repeat for each new Gherkin step)

Use this **two-beat** pattern (your **x.1 / x.2**); **x.3** is **“uncomment the next step and repeat”** — i.e. start the next **x.1**.

| Beat | Goal |
|------|------|
| **Red (odd sub-phase: 2.1, 2.3, 2.5, …)** | Add the **full** scenario to [`e2e_test/features/book_reading/book_reading.feature`](e2e_test/features/book_reading/book_reading.feature) (or adjacent feature) with the **exact** user-story Gherkin. **Uncomment only the next step** toward the end state; **comment out** all **later** steps (Gherkin `#` lines) **or** equivalent (separate `@wip` scenario with a single new step — pick **one** approach per repo habit). **Run Cypress** and confirm **one** failure for the **right reason** (**feature not implemented**), with a **clear** step definition / assertion message (fix wording if the failure is ambiguous). Improve step defs / page objects **in this beat** if the harness cannot express the step yet. **Do not** leave multiple new steps failing at once. |
| **Green (even sub-phase: 2.2, 2.4, 2.6, …)** | **Minimum** production change to make the **currently uncommented** scenario prefix **pass**. **No dead code**; remove scaffolding; keep code clean. **Optional** small **mounted** tests only if they reduce duplication without mirroring structure — E2E remains the **driver** for this slice. |

Then **uncomment the next line** → **Red** again → **Green** again, until **all** lines are active and the scenario passes.

**Fixture:** Use a book with **distinct** titles **2.1 …** and **2.2 …** from the first **Red** beat that needs them; step defs per [`e2e_test.mdc`](.cursor/rules/e2e_test.mdc). **No** reload / re-navigation / server read-state requirements for **2.1–2.10**.

### Mapping: Gherkin line → sub-phases

| Order | Gherkin (uncomment up to here) | Red | Green (minimum outcome) |
|-------|--------------------------------|-----|-------------------------|
| 1 | `Given I choose the book range "2.1 xxx"` | **2.1** | **2.2** — selection + stable hook for “chosen” range |
| 2 | `And I scroll to title "2.2 xxx"` | **2.3** | **2.4** — scroll / viewport drives **current range** to **2.2**; assert in E2E |
| 3 | `When I mark the book range "2.1 xxx" as read in the Reading Control Panel` | **2.5** | **2.6** — panel at **successor** boundary + **Mark as read** updates **in-memory** state for **2.1** (no HTTP) |
| 4 | `Then I should see that book range "2.1 xxx" is marked as read in the book layout` | **2.7** | **2.8** — **right border** + same **DOM hook** as Cypress; optional non-color cue (design **6**) |
| 5 | `And I should see that book range "2.2 xxx" is selected in the book layout` | **2.9** | **2.10** — selection **still** **2.2** after mark; **full** scenario **green** |

If a **single** Gherkin step is **too large** (two user-visible outcomes), **split** it in the feature file with **intermediate** steps **still commented** until their Red/Green pair — **never** more than **one** new failing assertion driver per Red beat.

---

### Phase 2.11 — Persistence + `GET …/book` enrichment + API-typed client model (**last** sub-phase)

**User-visible value:** **Yes** — read state **survives** reload and matches server; **authoritative** book tree on the client uses **generated** types.

This sub-phase combines **“persist read”** + **“book layout payload includes read state”**, **after** **2.10** proves the **in-memory** UX end-to-end.

**Deliverables**

- **DB:** Flyway migration — **`ReadingRecord`** (user, book_range_id, status, timestamps); FKs; **unique (user_id, book_range_id)**.
- **Backend:** Entity, repository, service, **write** endpoint (mark read, idempotent upsert acceptable).
- **Backend:** Extend **`GET …/book`** so each **range** includes reading fields needed for the border (single query pattern, no N+1).
- **OpenAPI** updated; **`pnpm generateTypeScript`**.
- **Frontend refactor:** the **loaded book** + per-range reading info held in client state **must** be typed from **generated** SDK/OpenAPI types (the **same** shapes as **`GET …/book`** — use composition/`Pick`/narrowing on generated types; **remove** interim duplicate DTOs).
- Wire **Mark as read** to the **write** API; after success, **refresh book** (or equivalent reconcile) so UI matches server.

**Tests**

- **Spring** controller (and/or `@Transactional`) tests: write + enriched **`GET …/book`**; errors for wrong notebook / range / auth.
- **Vitest (mounted):** load a **fixture object** that is **typed as / satisfies** the **generated** book response type (or build via **`makeMe`** if extended appropriately) → mount book layout (or page with mocked SDK returning that payload) → assert **tree renders correctly** (titles/structure hooks and **read** border for ranges with reading fields set). This proves the **UI matches the API-shaped payload**, not a divergent hand-written mock.

**Explicit non-goals**

- Changing the **2.1–2.10** Gherkin to **require** reload (optional **future** scenario, not **2.11**).

---

## Dependency graph (summary)

```text
2.1 red ──► 2.2 green ──► 2.3 red ──► 2.4 green ──► 2.5 red ──► 2.6 green
    ──► 2.7 red ──► 2.8 green ──► 2.9 red ──► 2.10 green (full E2E)
                                                              │
                                                              ▼
                                                         2.11 persistence
                                                         + GET book + API types
```

**2.11** is strictly **after** **2.10**. **No parallelization** of the red/green sequence without breaking the **one failing step** rule.

---

## Phase discipline (each sub-phase)

1. **Tests first or alongside** — red → green for the **observable** surface of **that** sub-phase.
2. **No dead production code** — no unused endpoints, components, or DB columns; **remove** scaffolding before merge. After **2.11**, **remove** interim client DTOs so the book tree uses **generated** types only (decision **5**).
3. **No stray feature flags** — behavior is **on** when merged.
4. **Deploy gate** — parent plan: commit/push/CD between **parent** phases; for **sub-phases**, follow team habit (often **one PR per red/green beat** or **pair 2.n+2.n+1** when practical).
5. **Update documents** — when shipped, trim this file’s speculative options; align [`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) Phase 2 bullet list with **completed** sub-phases; refresh **Current directional choices** in the architecture roadmap only if a **default** changed (e.g. reading-order walk).

---

## Relationship to parent Phase 2 text

[`ongoing/book-reading-reading-record-plan.md`](book-reading-reading-record-plan.md) Phase 2 described **predecessor-range** copy and **2.2 / 2.3** example titles. **This document** aligns Phase 2 with [`ongoing/book-reading-user-stories.md`](book-reading-user-stories.md): **selected range 2.1**, **current 2.2**, **panel at successor boundary**, **read border on 2.1**, **selection stays on 2.2**. When implementing, **either** match this story **literally** or **update the user story in the same PR** — no drift between Gherkin and behavior.

---

## Open points

1. **Exact HTTP paths and DTO names** — match existing notebook/book controllers; decide in **2.11**.
2. **Idempotency** — second “mark read” is **no-op** or **updates timestamp**; document chosen behavior in API tests (**2.11**).
3. **Selection vs current range styling** — if the scenario’s “selected” is **ambiguous** with **current range** after scroll, **product + a11y** pick one mapping; encode in **one** place (page object + live region strategy if any) — lock by **2.10** so hooks stay stable.
