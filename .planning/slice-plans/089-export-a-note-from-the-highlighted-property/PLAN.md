# Export a note from the highlighted property

**Source:** [selected story](../../seeds/SEED-120-export-highlighted-property.md#export-a-note-from-the-highlighted-property)
**Source review:** [source-review.md](source-review.md)

**Identity:** SEED-120#export-a-note-from-the-highlighted-property

## Goal and scope

Let a learner share the highlighted property or list item, surrounding focus
context, and optionally its recent quiz evidence with an external AI. The
seed owns the examples and the developer's two accepted scope decisions.
Preparation authorizes no implementation or Take.

One selection rule covers the report: exact authored property key, optional
exact raw list value, and optional occurrence for describing/validating the
visible subject. Without a selected item, a list's entire property is the
subject. Equal occurrences share property/value history. Saved recall links
establish attribution, including unanswered prompts; MCQs without such links
remain eligible for whole-note export only. Shared MCQ content and private
current-learner answers retain their separate access rules.

Keep the existing checkbox default, preview/copy/download, token-budget
control, complete appendix outside the note-body budget, newest-first order,
answer keys, and latest own selection. No focus picker, question generation,
metadata backfill, new learning identity, migration, or notebook-format work.
If the selected key/value/occurrence is unresolved, report that subject as
unresolved and return no export for a substituted subject. Recovery is to
close export, select an existing focus or clear it, then reopen.

## Architecture and existing solutions

- Reuse `Frontmatter.getPropertyValueExact` and its scalar/list value model
  for current-note resolution; add no YAML parser or duplicate route state.
- Extend `FocusContextMarkdownAugmenter` as the shared owner of the property
  instruction and wiki-link preservation. Support whole-list and occurrence
  descriptions there while preserving question-generation consumers. Keep
  surrounding bounded focus-context retrieval/rendering.
- Extend the existing `/api/notes/{note}/ai-context-markdown` read contract
  with optional property/item selectors; preserve its response shape and
  focus-free callers. Reuse the MCQ repository, saved prompt/tracker links,
  `RecentNoteQuestionsMarkdownAppendix`, and latest-own-answer lookup.
  Use an existence predicate or equivalent deduplication before ordering and
  limiting, with exact string comparison independent of MySQL collation.
- Match history regardless of learner or current scheduling eligibility;
  answer selection alone is current-user scoped. Do not add active-tracker,
  answered-only, or contested-question filters to historical evidence.
- Consume `useFocusedNoteProperty`/named route helpers from the actual note
  export surface. Occurrence does not enter learning identity. Generated SDK
  and OpenAPI artifacts are regenerated, never edited by hand.
- Follow [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
  (learning data stays outside the Portable tree),
  [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) (one route owner),
  and [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md)
  (deliberate unresolved-focus feedback, ordinary unexpected failure handling).
  Existing [North Star](../../NORTH-STAR.md) topics need no change.

## Outside-in proof ownership

| Promise | Owner and observation |
| --- | --- |
| Scalar property, whole-list property, and exact item/duplicate perspective retain surrounding context and relevant links | 1: MockMvc export responses with real note content; preserve question-generation request assertions. 2: real mounted note/toolbar opens export from each named route and verifies the generated SDK request and displayed focus |
| Only attributable MCQs, including unanswered prompts; distinct newest ten after filtering; exact keys/raw values; no-history focus remains useful | 1: controller/MockMvc tests over real MySQL with more than ten matching questions, newer unrelated and unlinked MCQs, repeated prompts, equal creation times, and case-distinct values; assert the selected ordered stems |
| Shared-value history for equal occurrences; whole-property history across list values | 1: response examples distinguish occurrence description from property/value history and whole-property selection. 2: new integrated browser journey exports the second duplicate's real recall-linked question |
| Choices, Donut key, latest own answer, guest/privacy and read authorization | 1: retain canonical appendix/learner-selection controller regressions and add focused-request coverage, including another learner's newer answer and an unreadable note |
| Default unchecked option; token-budget refresh and checkbox refresh keep the focus; preview/copy/download share the returned Markdown | 2: mounted form/toolbar interactions using real router/popups and the permitted SDK, clipboard and download boundaries; include both toolbar and overflow-menu entry points |
| No highlight preserves whole-note export, including unprompted questions; no matching history still returns focused context | 1: retained whole-note controller tests and focused empty-history response. 2: focus-free export regressions and returning from focused to ordinary export |
| Stale key, missing value or duplicate occurrence cannot yield a misleading export; learner can recover | 1: bound HTTP requests return explicit unresolved-focus feedback. 2: mounted failed export has actionable feedback and unavailable copy/download, followed by successful export after selecting a valid subject or clearing focus |

Use concise MakeMe fixtures and stable controller/mounted boundaries, mocking
only external dependencies under the stack skills. The temporary preparation
probe is evidence about feasibility, not the future implementation's tests.

## Decisive premises and observations

All observations below used this owned worktree at
`05a4e226df34f06c88fb7f50b78767b4a39ab14a`, on 2026-11-10.

| Premise / consuming operation | Form, result, and limitation |
| --- | --- |
| Exact raw-value attribution can use existing saved links without answers, duplicating MCQs or consuming the limit too early | At 09:33:57 UTC, a disposable Spring/EntityManager probe ran the native query below against isolated MySQL 8.4.11, database `doughnut_wt_f7c0188e28194b0488b417912d30e054_test`, test profile. Twelve run MCQs had two unanswered prompts each; newer unlinked, walk, differently cased value and differently cased key MCQs competed. Exact item returned `run-11` through `run-2`; whole-property returned `case-different, walk, run-11, run-10, run-9, run-8, run-7, run-6, run-5, run-4`. No answer join or learner filter was needed. Future HTTP binding and report construction remain slice 1's proof, not an observation of completed product behavior |
| Route-owned focus supplies exact occurrences and distinguishes property-only lists and stale destinations | `./scripts/run.sh pnpm frontend:test tests/notes/NoteExportForm.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts`: 3 files / 18 cases passed in headless Chromium. Setup used production named routes and real mounted note/editor. A scratch change replacing route item consumption with `undefined` made 9 of the 11 item-location cases fail; `./scripts/run.sh pnpm frontend:test tests/components/form/RichMarkdownEditor.propertyItemLocation.spec.ts` passed all 11 again after restoration. This proves focus acquisition, not its future export consumption |
| Existing export and question-generation boundaries are runnable in the local profile | `./scripts/run.sh pnpm backend:test_only` ran all backend tests including the one disposable probe: 3,074 cases, zero failures/errors, two skipped; test phase 3m26s after migration. Existing export/privacy and property-focus generation request tests passed. The 18-case frontend run exercised current preview/copy/download/default behavior; it does not prove the new selectors |

Representative query actually executed (omit the final value predicate for a
whole-property report; bind the authored key and raw value without normalization):

```sql
SELECT m.* FROM mcq m
WHERE m.note_id = :noteId AND EXISTS (
  SELECT 1 FROM recall_prompt rp
  JOIN memory_tracker mt ON mt.id = rp.memory_tracker_id
  WHERE rp.mcq_id = m.id AND mt.note_id = :noteId
    AND CAST(mt.property_key AS BINARY) = CAST(:propertyKey AS BINARY)
    AND CAST(mt.property_value AS BINARY) = CAST(:propertyValue AS BINARY)
)
ORDER BY m.created_at DESC, m.id DESC LIMIT 10
```

The probe and frontend mutation were reverted before recording this plan.
No production code change, paid call, or shared/production database observation
was made. These observations settle the reuse/data-shape premises; the new
export outcomes remain planned proof.

## Ordered slices

### 1. Request a focused note report with matching quiz evidence

Type: Behavior
Status: done
Proof: Real MockMvc requests on `NoteExportController` with MakeMe data and
real MySQL establish the focused Markdown, optional matching appendix,
request binding, authorization, current-user privacy, and unresolved-focus
outcome. Run the full backend suite, and preserve generation request proof.

Behavior: Given a readable note and valid optional property/item selectors →
request its AI-context Markdown → receive the selected perspective and, when
requested, up to ten distinct matching MCQs with current-learner answers.
An omitted selector gives the existing whole-note report. An unresolved
selector produces explicit feedback instead of another subject's report.

Extend the existing report path and shared focus augmenter using one resolved
selection model. Property-only list selection includes all values; item
selection adds raw-value matching and validates/describes the occurrence.
Preserve current no-focus callers and the existing appendix format. Regenerate
the API client after the controller contract changes; update all direct Java
callers rather than adding a test-only overload. Keep focus cases in cohesive
test files and retain the existing broad export and learner-selection cases.

Sizing: about 8 minutes of implementation, focused proof preparation and
cleanup, plus mandatory full-backend runtime (observed 3m26s). The larger
target is justified by one indivisible report invariant: its perspective and
question set must describe the same subject. The HTTP report is independently
usable before UI wiring, and isolates request/database risk. At ten minutes
of active work, stop and refine the remaining work rather than extending it;
test-runtime waiting is the stated exception.

Safe stopping point: the HTTP report is complete and backwards compatible;
the note-page export still uses its prior whole-note request until slice 2.

### 2. Export the note page's highlight through the existing action

Type: Behavior
Status: done
Proof: Mounted note/toolbar/form tests drive selection, opening, refresh,
checkbox, preview, copy/download, errors and recovery through the generated
SDK boundary. One new unpaid Cypress journey in
`e2e_test/features/note_topology/export_highlighted_property.feature` uses real
recall-linked question data, the second duplicate's inbound URL, the export
action and checkbox, and observes the resulting focus and quiz appendix.
Mock only external OpenAI generation; reuse the existing list-property recall
and item-location steps, adding thin export actions/assertions as necessary.

Behavior: Given a note-page property or item highlight → open Export Note
Data → receive that perspective; refresh and question inclusion keep it,
and copy/download use the same Markdown. A plain note exports its whole-note
report. A stale focus reports its problem and is recoverable through existing
note selection controls without copying an unrelated report.

Read the existing reactive focus without shadow state, preserving the full
raw value and occurrence. Integrate both toolbar and overflow-menu entries.
Keep the checkbox, loading/error handling and copy/download owner. Add focused
form/page specs separately from the current 250-line `NoteExportForm.spec.ts`;
reuse its existing helpers when they fit, without a second fixture framework.
The main Cypress journey must traverse the real HTTP report, not stub the
export SDK or pre-supply the Markdown it claims to produce.

Sizing: about 5–8 active minutes plus test/runtime waits. One interaction
proof loop crosses selection, request and export output; it is not split by
component or by verification activity. Apply the same ten-minute stop rule.

Safe stopping point: the selected story's full user journey is observable.

## Verification and delivery

- Slice 1: `./scripts/run.sh pnpm backend:test_only` (all backend tests,
  required by the backend skill); `./scripts/run.sh pnpm generateTypeScript`
  after the API change. No Flyway work is expected.
- Slice 2: `./scripts/run.sh pnpm frontend:test`, then
  `./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit` and
  `./scripts/run.sh pnpm -C frontend exec tsc --noEmit`, required by the
  frontend skill; run
  `./scripts/run.sh pnpm cy:run --spec e2e_test/features/note_topology/export_highlighted_property.feature`.
  Sequence backend and Cypress in one checkout because they share build output.
- Trace changed effects with the installed consumer-trace script during
  execution: `getAiContextMarkdown`, property-focus output, new query fields,
  export default/refresh behavior, and new E2E helper names. Inspected existing
  consumers include both Java export test classes, the generation request
  tests, `NoteExportForm.spec.ts`, `NoteToolbar.panels.spec.ts`, and the two
  `NoteMoreOptionsActions` entries. Keep their meaningful proof; add new
  focused proof where old fixtures only supply an SDK response. No CLI/MCP
  caller of this export endpoint was found in the product search.
- Expected production edit sizes are 93 lines (export controller), 75 (focus
  augmenter), 115 (export form), and at most the 238-line toolbar action owner;
  no expected production file currently exceeds 250. Avoid unnecessary edits
  to that toolbar owner by using the established focus seam. Keep every edited
  file within the refactor skill's cohesive 250-line rule, including tests.
- Execution follows Jidoka, a fresh independent post-change-refactor agent,
  API generation when needed, one coordinator `./scripts/run.sh pnpm format:changed`,
  plan update, agent commit with independent check-only hook, and push under
  Execute Plan. Preparation itself does not invoke those execution roles.

## Concern review

Slice-plan refinement was not needed: two coherent report/interaction proof
loops use one selection rule and existing owners; the larger HTTP slice has
an explicit sizing reason and measured mandatory-test runtime exception.
No remaining slice-specific, source-scope, ADR, or premise concern was found
in this review. No pre-Take human decision or early probe is required.

Preparation host decision: Territory.local, 16 CPUs / 48 GiB RAM, load about
8.6, roughly 10 GiB inactive memory at launch; other local work was visible.
One full isolated backend run and three small headless frontend files were
permitted concurrently; both completed without resource failure. No E2E stack
ran. These are local observations, not remote-runner evidence. Execution owns
a fresh headroom check before its larger workloads.

## Execution context

Story Branch Mode, this owned workspace/branch; publisher `dashboard-territory.local-doughnut`, agent Hitomi-chan, origin/main. Claim `884a4fe8e463a1ea32120c7589cf808a82162285` accepted on trunk and execution branch; starting revision `68fd5b9e4759faa8d239c9a707116e0a12f7bbc6`.
Setup: `./scripts/run.sh bash scripts/worktree_setup.sh` completed frozen installation; `./scripts/run.sh pnpm exec node --version` passed (v26.13.0). Replanning authorized within scope; ten active minutes per slice, mandatory runtime waits excepted.
Observer: Codex `hitomi-export`, cell `g1041:6`, session7913, PID15125, `/tmp/dough-ci-501/watch-J9bp5P`, this checkout, nerds-odd-e/doughnut execution branch, ci.yml (5208694, donut CI). Claim trunk CI is unobserved.
Capacity: Territory.local16CPUs/48GiB; startup load39/21/15, ~10GiB inactive with compression and other sessions. Sequence owned heavy jobs and reconsider before launch. Slice1 serial frontend/GC mode passed after parallel browser crash; slice2 reused it. Latest pre-Cypress load13.1/9.2/10.8, prior1.5GiBfree+10.8inactive; one owned heavy job. Each agent owned proof through terminal.
Slice1 published `401bc9ff4a2bf02f4c4e922f2b87b1fb8b4e030f`, suffix base claim; remote execution branch accepted and observer reused.

## Accepted execution proof

Slice 1: focused HTTP report accepted against all 58 allocated commitments.
Backend 3,090 / frontend 2,521 tests passed; both typechecks and OpenAPI passed.
Exact setup/assertions, corrected fixture outcomes, consumer trace, refactor
disposition and literal commands are retained in [source review](source-review.md).
Freshness unchanged on origin/main `d17580ea6cee7f8eb3d348728721fdb9633e2f95`;
no incoming named-supplier behavior change.

Slice 2: all 20 remaining commitments accepted; full frontend plus two later
recovery cases, both typechecks, registry tests and real HTTP Cypress passed.
Exact commands, setup/assertions, consumer trace and limits are in source review.
Refactor accepted: duplicate route-only recovery proof consolidated into the
stronger note-control cases; both replayed successfully, other proof reused.

## Execution complete

Product advice: no change. The selected focus and attributable quiz evidence support the existing learning-to-refinement direction; no additional product promise or backlog change is needed.
