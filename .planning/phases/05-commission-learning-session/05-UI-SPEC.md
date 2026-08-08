---
phase: 5
slug: commission-learning-session
status: draft
shadcn_initialized: false
preset: none
created: 2026-08-08
---

# Phase 5 — UI Design Contract

> Visual and interaction contract for commissioning a Learning Session from the recall progress bar, displaying the copyable Learning Session Request, and surfacing awaiting-report state.
>
> **Mode:** `--auto` — defaults chosen to extend Phase 3 DaisyUI recall chrome, RESEARCH patterns (`AiRequestExportDialog`), and locked COM-01–03 / ADR 0005 constraints. Choice log at end.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (not shadcn) |
| Preset | not applicable |
| Component library | DaisyUI (`daisy-` prefix) + existing Vue recall / commons chrome |
| Icon library | `@lucide/vue` — `ClipboardCheck` via existing `CopyButton` only; **no new icons** on progress-bar row |
| Font | Inherit app default; request textarea uses `font-mono text-xs` (export-dialog pattern) |

**Source:** Phase 3 `03-UI-SPEC.md`; `frontend/src/components/commons/AiRequestExportDialog.vue`, `Modal.vue`, `CopyButton.vue`; `RecallProgressBar.vue`. No `components.json` — shadcn gate N/A.

---

## Spacing Scale

Declared values (must be multiples of 4). Inherit Phase 3 recall strip; add dialog spacing from export-dialog pattern:

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | Inline gap between status banner icon/text if added |
| sm | 8px (`gap-2`) | Gap between potential-session rows; row internal gap between copy and Commission button |
| md | 16px (`px-4`, `mt-4`) | Horizontal inset of potential-session strip; dialog card body padding; gap above textarea |
| lg | 24px | Dialog card outer margin on narrow viewports (Modal default) |
| xl | 32px | Not required this phase |
| 2xl | 48px | Not required this phase |
| 3xl | 64px | Not required this phase |

**Exceptions:** Commission button uses DaisyUI default min-height (touch-friendly) — do not shrink below existing `daisy-btn` sizing.

**Row layout (auto default — RESEARCH Q2):** Each potential-session row is a horizontal flex: glossary copy left (`flex-1`), `daisy-btn daisy-btn-primary` Commission affordance right (`shrink-0`). Strip remains sibling below ordinary progress bar (Phase 3 placement unchanged).

---

## Typography

Exactly four roles — extend Phase 3 recall strip into dialog:

| Role | Size | Weight | Line Height | Usage |
|------|------|--------|-------------|-------|
| Body | 16px (`text-base`) | 400 | 1.5 | Potential-session row glossary copy; dialog explanatory copy; awaiting-report banner |
| Label | 14px (`text-sm`) | 400 | 1.5 | Optional dialog helper under title (e.g. notebook name line) |
| Heading | 20px (`text-xl` / `daisy-card-title`) | 600 | 1.2 | Dialog title `Commission learning session` |
| Display | 28px | 600 | 1.2 | Not used this phase |

**Request markdown display:** `font-mono text-xs` in readonly textarea — matches `AiRequestExportDialog` (`h-96` fixed height, scroll inside textarea).

**Weights in use:** 400 on row copy and body; 600 on dialog title only. Do not bold the entire glossary sentence.

---

## Color

Use DaisyUI semantic tokens (theme-safe light/dark). Phase 3 accent reservation **activates** this phase.

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `var(--color-base-100)` | Page / recall bar surrounding surface; textarea background `bg-base-100` |
| Secondary (30%) | `var(--color-base-200)` | Modal panel (`Modal.vue` default `bg-base-200`); dialog card body |
| Accent (10%) | `var(--color-primary)` / `daisy-btn-primary` | **Commission** button on each potential-session row; pre-commission dialog primary CTA |
| Destructive | `daisy-btn-error` / alert error | Not used this phase |
| Row text | `var(--color-base-content)` | Glossary copy on potential-session strip |
| Awaiting banner | `daisy-alert-info` or `bg-info/10 text-info-content` with `text-base-content` fallback | Post-commission status — informational, not accent |
| Copy control | `daisy-btn-secondary` (CopyButton default) | Secondary to primary commission CTA |

**Accent reserved for:** (1) per-row `Commission` button on recall progress bar, (2) dialog pre-commission `Commission learning session` submit button. Do **not** use primary accent on CopyButton, modal close, or awaiting banner.

**Ordinary recall separation (D-05):** Progress bar fill, `toRepeatCount`, and nav badge colors unchanged. Commission UI lives only on potential-session strip + dialog.

---

## Copywriting Contract

Glossary terms: **potential learning session**, **Learning Session**, **Learning Session Request**, **tutor**, **report** (ADR 0001 §3). Match E2E feature wording.

| Element | Copy |
|---------|------|
| Primary CTA (row) | `Commission` — short label on `daisy-btn-primary` beside glossary copy |
| Primary CTA (dialog, pre-commission) | `Commission learning session` |
| Row populated (canonical — Phase 3) | `1 potential learning session to commission for notebook "{notebookName}"` — singular “session”; unchanged from Phase 3 |
| Dialog title | `Commission learning session` |
| Dialog notebook context | `Notebook: "{notebookName}"` — label line under title (14px) |
| Dialog pre-commission body | `Commissioning creates a Learning Session Request you can copy and send to your tutor.` |
| Awaiting-report banner (post-commission) | `This learning session is awaiting the tutor's report.` |
| Request field label | `Learning session request` — visually associated with readonly textarea |
| Copy button aria-label | `Copy learning session request` |
| Loading (global) | `Commissioning learning session…` — `apiCallWithLoading` `blockUi` message |
| Empty state heading | N/A — dialog always opened with a notebook context; no empty dialog |
| Empty state body | N/A |
| Error state | No dialog-local error chrome. Commission failures use existing `apiCallWithLoading` toast + global error path. User can retry via dialog CTA or close and reopen. |
| Destructive confirmation | None — recommission abandon is backend lifecycle; no extra confirm UI this phase |

**Post-commission:** Dialog stays open showing request + awaiting banner (COM-02, COM-03). User dismisses via Modal close (X or overlay). No “Done” primary CTA required.

---

## Interaction Contract

| Affordance | Behavior |
|------------|----------|
| Potential-session row | Retain `data-test="potential-learning-session"` and glossary copy. Add explicit `Commission` `daisy-btn-primary` — **not** whole-row click (RESEARCH recommendation). |
| Open dialog | Click row `Commission` button → open `CommissionLearningSessionDialog` with `notebookId` + `notebookName`. |
| Pre-commission dialog | Show title, notebook context, body copy, primary `Commission learning session` CTA. No fetch-on-open — commission on CTA click. |
| Commission mutation | `apiCallWithLoading(() => LearningSessionController.commission({ body: { notebookId }, query: { timezone: timezoneParam() } }), { blockUi: true, message: "Commissioning learning session…" })`. On success: populate textarea, show awaiting banner, hide pre-commission CTA. |
| Post-commission dialog | Readonly textarea bound to `response.requestMarkdown`; `CopyButton` copies full markdown; awaiting banner when `response.status === "AWAITING_REPORT"`. |
| Close dialog | Modal `close_request` — no abandon warning (session already persisted on success). |
| After success | Emit `commissioned` → parent calls `requestDueRecallsRefresh()`. Recommended backend excludes awaiting-report trackers from `dueCommissioned` so row may disappear. |
| Keyboard | Commission buttons are focusable; dialog traps focus per `Modal.vue` / `modalStack`. |
| Ordinary progress | `#buttons`, settings cog, progress math — **unchanged**. |

**Anti-patterns:** Do not commission on dialog open; do not `v-html` notebook names or request markdown; do not re-implement Request template in FE.

---

## Component Inventory

| Component / slot | Change |
|------------------|--------|
| `CommissionLearningSessionDialog.vue` | **New** — `Modal` + pre/post commission states + textarea + `CopyButton` |
| `RecallProgressBar.vue` | Add `Commission` button per row; wire dialog open state |
| `RecallPage.vue` | Host dialog; pass notebook props; on `commissioned` → `requestDueRecallsRefresh()` |
| `ProgressBar.vue` | **No change** |
| `Modal.vue`, `CopyButton.vue` | Reuse as-is |
| `AiRequestExportDialog.vue` | Pattern reference only — no modification |

---

## Testability Contract

| Attribute | Value | Notes |
|-----------|-------|-------|
| Row marker | `data-test="potential-learning-session"` | Preserve Phase 3 E2E |
| Commission trigger | `data-test="commission-learning-session"` | On row Commission button (page-object: click by notebook title context) |
| Dialog | `data-test="commission-learning-session-dialog"` | Root dialog body wrapper |
| Pre-commission CTA | `data-test="commission-learning-session-submit"` | Dialog primary button |
| Request textarea | `data-test="learning-session-request"` | Assert markdown substrings (ADR / E2E steps) |
| Awaiting status | `data-test="learning-session-awaiting-report"` | Visible when `status === "AWAITING_REPORT"` |
| Copy button | `data-testid="copy-learning-session-request"` | `CopyButton` `test-id` prop |
| E2E busy pairing | `waitUntilAppIsNotBusy()` | After commission submit, before textarea assertions |

Assert request content via textarea value / visible text — not rendered HTML. Rubric substring: `score from 0 to 5 per item` (backend source of truth).

---

## UI Considerations

> Shape-rooted UI state coverage for commission flow surfaces.

Applicable state considerations resolved: **15 covered, 2 backstop, 0 unresolved**

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| empty | potential-session list | ✅ covered | Zero notebooks with due commissioned trackers → no strip (Phase 3 silent absence). |
| loading | potential-session list | ✅ covered | No strip-local skeleton; rows appear when recalling payload loads (same path as Phase 3). |
| error | potential-session list | ✅ covered | No strip-local error UI; failed recalling uses existing recall page error handling — never show “0 potential sessions” on API failure. |
| populated | potential-session list | ✅ covered | One row per notebook: canonical glossary copy + `Commission` primary button. |
| partial | potential-session list | ✅ covered | API contract supplies `notebookName`; if empty, show `""` in quotes only in test fixtures — production expects name present. |
| overflow | potential-session list | ✅ covered | Many notebooks: vertical stack `gap-2`; strip grows within recall top bar column. |
| zero-one-many | potential-session list | ✅ covered | Zero = hidden; one/many = one row each with singular “session” copy (Phase 3). |
| long-text | notebook title in row | 🧪 backstop | Long `notebookName` wraps (`break-words`); Commission button stays on same row (`flex` + `shrink-0`); E2E substring match on full title must not be broken by ellipsis. |
| empty | commission dialog | ✅ covered | Dialog not mounted without `notebookId` — no empty open state. |
| loading | commission dialog | ✅ covered | Pre-commission: no in-dialog spinner. Commission uses global `blockUi` + `data-app-busy` thin bar (`Commissioning learning session…`). Post-commission: textarea populated synchronously from response. |
| error | commission dialog | ✅ covered | Commission failure: global toast from `apiCallWithLoading`; dialog remains on pre-commission state so user can retry CTA. See Copywriting Contract error row. |
| partial | commission dialog | ✅ covered | N/A for form fields — single notebook context passed in. Request markdown is all-or-nothing from API. |
| overflow | commission dialog | ✅ covered | Modal scrolls; textarea fixed `h-96` with internal scroll for long markdown. |
| long-text | commission dialog title/context | ✅ covered | Notebook name in `Notebook: "{name}"` wraps; title stays single line. |
| overflow | request textarea | ✅ covered | Fixed height textarea; vertical scroll inside field for long Session Item lists. |
| long-text | request textarea | ✅ covered | Monospace `text-xs`; no truncation — full `requestMarkdown` visible and copyable. |
| overflow | awaiting-report banner | ✅ covered | Single-line or short wrap; no clip. |
| long-text | awaiting-report banner | ✅ covered | Fixed copy string — no dynamic long text. |
| loading | copy button | ✅ covered | Disabled when `!requestMarkdown`; no spinner on copy action. |
| error | copy button | ✅ covered | Disabled when no text; clipboard failures propagate (CopyButton does not swallow). |
| long-text | copy button | ✅ covered | Copies full markdown regardless of length — no label truncation. |

**Security / XSS:** Interpolate `notebookName` and display `requestMarkdown` as text only (`textarea` value / mustache). Never `v-html`.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none | not applicable — no shadcn |
| third-party | none | not applicable |

---

## Auto Choice Log

Choices made under `--auto` (no user questions):

1. **Row affordance:** Explicit `Commission` `daisy-btn-primary` button per row — not whole-row click (RESEARCH Q2; clearer than status text alone).
2. **Dialog pattern:** `Modal` + `daisy-card` body mirroring `AiRequestExportDialog` — commission on CTA, not fetch-on-mount.
3. **Accent activation:** First use of Phase 3 reserved `daisy-btn-primary` on row + dialog commission CTA only.
4. **Awaiting state:** Informational banner + `data-test="learning-session-awaiting-report"`; human copy, not raw enum string.
5. **Post-commission:** Dialog stays open with request + copy — no separate “success” screen.
6. **Loading/errors:** Global `apiCallWithLoading` contract — no duplicate dialog spinners or inline error panels.
7. **Typography/spacing:** Inherit Phase 3 strip; dialog title 20px/600; textarea mono xs per export dialogs.
8. **Strip after commission:** UI assumes recommended `dueCommissioned` exclusion + refresh — row may disappear; not asserted in commission E2E.
9. **Registry:** none — DaisyUI project.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending
