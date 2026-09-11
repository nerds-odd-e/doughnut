# Accept pipe characters in note names

Source: [SEED-018 story 1](../../seeds/SEED-018-publish-large-authored-notebooks.md#story-1).
Status: in progress. Execution authorized on 2026-09-11.

## Execution identity

- Originating checkout: `/Users/terryyin/git/doughnut` on `main`
- Execution checkout: `/Users/terryyin/git/doughnut-105-accept-pipe-note-names` on `codex/105-accept-pipe-note-names`
- Integration target: `main`
- CI observation: unavailable for the execution branch; `.github/workflows/ci.yml` (`donut CI`) is push-triggered only for `main`, so branch pushes have `pendingCi: unobserved`.

## Outcome and scope

Notebook owners retain ASCII `|` in note titles and aliases, can follow their
references in Donut, and receive a nonblocking compatibility warning. Preserve
the authored character through editing and Portable notebook tree round trips.
Filename-as-title remains: `A|B` exports as `A|B.md`.

The user approved the concise change to
[ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md):
escape literal pipes as `\|`, backslashes as `\\`, and use the first unescaped
pipe as the label separator. `[[A|B]]` continues to mean target A, label B.
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) still owns note/property
navigation. Existing authorization, ambiguity, path safety, alias shape, identity,
learning history, and atomic-publication guarantees remain intact.

Exclude Windows filename encoding, other forbidden characters, folder/notebook
name expansion, fullwidth-name migration, general Obsidian/table compatibility,
and large-commit performance (story 2).

## Current decisions and evidence

- `WikiLinkMarkdown` and frontend `authoredLinkMarkup` split on the first pipe;
  `WikiLinkMarkdownRewrite` repeats that split in several transformations.
  Use one token rule per runtime for reading, generating, and rewriting links;
  distinguish decoded Portable paths from escaped wiki spelling. Do not add
  separate title/alias/property escape dialects or a cross-runtime framework.
- Keep untouched authored tokens unchanged. Escape decoding is single-pass;
  other backslashes remain literal. YAML decoding precedes wiki parsing.
  Property-key percent encoding is unchanged. These are implementation examples
  under the concise ADR, not additional ADR prose.
- The shared name normalizer currently replaces `|` with `｜`; both alias
  validators reject `|`. Account for both acceptance and preservation, without
  accidentally changing folder/notebook validation through shared helpers.
- `NotebookGitAdvisoryNamePublicationControllerTest` already proves advisory-name
  publication, download, and notebook-health findings. Reuse that workflow.
  Publication returns a plain accepted head; warnings can be read through the
  existing notebook-health lint result after publication. No new publication
  response, CLI warning transport, or dashboard is needed for this story.
- Warn beside title/alias editing and in existing notebook health. Distinguish
  title filename incompatibility from alias-only link incompatibility. Keep
  copy brief; warning findings are not auto-fixes and do not require consent.

## Learnings

- Slice 5 overran at about 12 active minutes. Its controller proof established
  note/learning identity and body rewrite behavior, but the double-quoted YAML
  example failed because writing `\|` directly makes an invalid YAML escape.
  Attempt-owned tests are parked in stash
  `e9cb2f9b099138b9b540a0247214a936d05ecfce`. The disproved sizing assumption
  was that the document rewriter could emit the same wiki spelling in body,
  single-quoted YAML, and double-quoted YAML; slice 5a now isolates that boundary.
- The first slice 5a attempt then reached 10 active minutes without compiling;
  its quote-context scan was reverted. Story-boundary reassessment retains this
  work because usable pipe titles require reference-preserving rename and the
  remaining outcome is still one end-to-end story. Bounded research found the
  smaller seam: use SnakeYAML scalar-node marks to rewrite only supported
  leading-frontmatter value spans, re-rendering only changed double-quoted
  scalars while preserving untouched frontmatter verbatim.

## Ordered slices

All slices are sequential. Sizing includes implementation, proof, and local
cleanup; target approximately 5 minutes. Estimates above target are scrutinized
below. Stop and refine at 10 minutes of active work. Required full-suite runtime
may exceed that limit: record actual wait separately; it does not excuse excess
implementation work. Never deliver a failing intermediate state.

### 1. Centralize wiki target transformations
Type: Structure
Status: done
Proof: Existing wiki parsing, rename, relocation, and property-reference tests
pass with unchanged authored output (backend command below).

Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed after the
post-change refactor. `WikiInnerSplit` now owns target rewrites and preserves
the raw authored inner when the target is unchanged.

Replace repeated delimiter handling in `WikiLinkMarkdownRewrite` with the
existing token abstraction, preserving raw spelling where a transformation
does not change it. Prepare only the token transformations needed by slice 2;
do not change acceptance or add a general Markdown parser.

Enables immediately: slice 2 can change the escape rule without leaving
rewrites on the old delimiter rule. Estimate: 4–5 minutes, medium confidence.

### 2. Read escaped wiki references consistently
Type: Behavior
Status: done
Proof: `WikiLinkMarkdownTest` and mounted Markdown rendering examples distinguish
decoded destination and label; backend/frontend suites pass.

Evidence: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed with
2,392 tests, and `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed with
1,881 tests after the post-change refactor. The backend and frontend scan the
token once, and Markdown lexing protects escapes with a collision-free marker.

Behavior: Given authored escaped wiki text in a body or recognized YAML value,
reading it preserves the intended target and visible label. Use `[[A|B\|C]]`
to prove a live reference to existing A displays `B|C`; `[[A\|B]]` is an
unresolved reference to `A|B`, not a reference to A. Cover double backslashes,
multiple pipes, and equivalent single/double-quoted YAML with compact data at
the stable token contract. Preserve ordinary `[[A|B]]` and unrelated escapes.

Update backend parsing and frontend parsing/rendering together. The pure grammar
proof owns edge combinations; mounted tests own rendered text, not another
complete grammar matrix. Estimate: 5–8 minutes, medium confidence; two runtime
implementations are inseparable for consistent reading, but slice 1 removes
rewrite preparation from this proof loop.

### 3. Generate unambiguous wiki references
Type: Behavior
Status: done
Proof: Mounted `InsertWikiLink.spec.ts` and dead-link repair examples assert the
stored token from an API-provided Portable path; frontend suite passes.

Evidence: `CURSOR_DEV=true nix develop -c pnpm frontend:test` passed with 1,881
tests after the post-change refactor. `wikiLinkTokenFromDecodedParts` is the
single frontend serializer for inserted and repaired wiki tokens, escaping
decoded destinations and labels without changing the separator or property key.

Behavior: Given a selected destination whose authored Portable path contains a
pipe, inserting or repairing its link writes an escaped destination and retains
the chosen display label/property suffix. Exercise `folder/A|B` with optional
display text; do not escape the separator or double-encode the property key.

Use the token rule at wiki serialization boundaries (`wikiLinkAuthoring` and
`wikiLinkMarkup`), keeping the Portable-path API decoded. Tests may use the
existing generated-API fixture boundary before pipe names are accepted by writes.
Estimate: 4–5 minutes, medium confidence.

### 4. Create a pipe-titled note with a warning
Type: Behavior
Status: done
Proof: Existing note-create controller boundary saves/reloads exact `A|B`;
mounted `PathNameEditor.spec.ts` proves nonblocking warning and submitted value.
Backend/frontend suites pass.

Evidence: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript` regenerated
the DTO constraint, `CURSOR_DEV=true nix develop -c pnpm openapi:lint` passed,
and the backend/frontend suites passed after the post-change refactor (2,394
backend tests and 1,882 frontend tests). The note-specific path preserves `|`;
folder and notebook validation still reject it.

Behavior: Given a new otherwise valid note, creating `A|B` preserves that title
and warns about portability. An existing fullwidth `A｜B` remains distinct.
Update the note-name validation/normalization path and editor together. Cover
subsequent body save/reload so storage conversion cannot silently alter the name.
Existing other-name validation tests remain regression proof; do not broaden
the exception to other entity names merely because a helper is shared.

Estimate: 5–8 minutes, medium confidence. Validation plus normalizer plus editor
are necessary for this single observable save; no export or rename work here.

### 5a. Preserve wiki escapes in YAML rewrites
Type: Structure
Status: planned
Proof: Focused document-rewrite examples preserve existing body and
single-quoted YAML spelling while double-quoted YAML stores the extra YAML
escape needed to decode to the same wiki token; backend suite passes.

Make `WikiLinkMarkdownDocumentRewrite` preserve the containing scalar's YAML
syntax when a rewritten target introduces wiki backslashes. Use SnakeYAML
scalar-node marks for the same top-level scalar and direct-list-item shapes the
frontmatter reader recognizes. Keep body, single-quoted YAML, and untouched
frontmatter spelling unchanged; re-render only a changed double-quoted scalar
so SnakeYAML decodes the intended `\|`. Do not introduce a second wiki grammar,
re-dump the whole frontmatter, or add a general YAML rewriter.

Enables immediately: slice 5b can rename references to a pipe title without
persisting invalid YAML. Estimate: 7–10 minutes, medium confidence after the
bounded node-mark investigation; the exact source-splice boundary is one proof
loop and has no remaining unexplained preparation.

### 5b. Preserve references when renaming to a pipe title
Type: Behavior
Status: planned
Proof: Extend `TextContentControllerUpdateNoteTitleTests` and existing rewrite
controller examples; backend suite passes.

Behavior: Given a learned note referenced by another note, renaming it to `A|B`
keeps the same note and learning records, and incoming references still resolve
to it with their intended label. Reuse slice 1–2's transformations, including
recognized YAML references. An unchanged `[[A|B]]` referencing a separate A
must not be captured by the new title. Preserve existing folder, notebook,
ambiguity, and property rewrite tests as regression coverage.

Estimate: 4–6 minutes, high confidence after slice 5a isolates YAML spelling;
existing rewrite controller fixtures own the lifecycle, avoiding a separate
rename mechanism.

### 6. Save and resolve a pipe alias with a warning
Type: Behavior
Status: planned
Proof: Extend text-content controller alias tests, note-show resolution tests,
and mounted property-editor tests; backend/frontend suites pass.

Behavior: Given an ordinary note, saving `aliases: ['A|B']` keeps that alias and
shows a link-compatibility warning; `[[A\|B|Read this]]` resolves to it. The
warning does not claim that the note's filename is Windows-incompatible.
Extend both validators and the existing property editor. Two matching aliases
remain ambiguous under the existing scope rules; invalid alias shape still
rejects. Assert body and recognized YAML references at note-show, reusing the
same parser rather than adding alias-specific interpretation.

Estimate: 5–8 minutes, medium confidence; both validators and the editor are
required for one successful alias edit, while grammar is already delivered.

### 7. Find pipe compatibility warnings in notebook health
Type: Behavior
Status: planned
Proof: `NotebookHealthControllerTest` and mounted health-findings tests expose
affected notes with warning severity; backend/frontend suites pass.

Behavior: Given persisted pipe titles or aliases, running notebook health
identifies the affected notes with the appropriate brief compatibility warning.
Use existing health groups/findings and navigation, without auto-fixing names.
A note with an alias-only finding receives no filename warning. Repeated pipes
need not create repeated findings. Keep unrelated health rules unchanged.

Estimate: 4–5 minutes, medium confidence.

### 8. Publish and round-trip a pipe-titled file
Type: Behavior
Status: planned
Proof: Extend `NotebookGitAdvisoryNamePublicationControllerTest` with a real
proposal bundle and downloaded accepted tree; backend suite passes.

Behavior: Given a valid `folder/A|B.md` with body and YAML references, publishing
then downloading the accepted tree preserves filename, content, and reference
destinations; reimporting the unchanged tree preserves those values. Notebook
health exposes the warning from slice 7. Align any remaining durable format or
path validation with note-title acceptance. Keep unrelated unsafe-path and
reserved-name rejection examples green.

Estimate: 5–8 minutes, medium confidence; reuse the existing JGit bundle fixture
and existing unchanged-publication path. Do not benchmark the motivating notebook.

### 9. Publish a pipe alias without weakening atomic validation
Type: Behavior
Status: planned
Proof: A publication controller proposal changes an existing `Topic.md` to use
`aliases: ['A|B']`; accepted download retains filename/YAML, and note-show resolves
its escaped reference. Existing publication rejection tests plus one mixed
valid-pipe/invalid-alias-shape proposal prove unchanged accepted head and state.
Backend suite passes.

Behavior: Given a local alias edit, publication accepts the pipe-bearing alias
without renaming its note or losing learning history. The corresponding lint
finding is available. A separate invalid condition still rejects the entire
proposal atomically. This is the motivating alias-publication outcome, using
the same validation and warning rules as web editing.

Estimate: 4–6 minutes, medium confidence; no transport changes. The negative
case belongs to this acceptance boundary, not a separate test-only slice.

## Proof ownership

| Story promise | Owner |
| --- | --- |
| Literal pipe/backslash grammar; preserve ordinary label syntax | 2 |
| Product-authored links and separate display text | 3 |
| Exact title preservation; fullwidth remains distinct; create/edit warning | 4 |
| YAML-safe rewrite spelling | 5a |
| Rename identity/learning history and incoming references | 5b |
| Alias edit, warning, unique/ambiguous resolution, body/YAML links | 6 |
| Persistent warnings, including local publication lint | 7 |
| Filename-as-title, authored YAML/body round trip and property references | 8, using 2–3's token/property proof |
| Alias publication, existing learning history, independent atomic rejection | 9 |
| Existing path/scope/property/navigation and access rules | Relevant existing controller suites throughout; 5b–6 own changed resolution cases |

## Verification and delivery

Run repository commands through Nix:

```sh
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

Backend rules require the full backend unit suite at each backend slice;
frontend slices run the frontend suite. Use controller tests with real DB and
lower-level code, and mounted frontend tests mocking only the backend API.
These are the high-level proof boundaries; no manual browser testing or new
Cypress infrastructure is planned. No tests were run during this planning task.

On execution follow dough-execute-plan: claim the queued story, establish the
execution worktree identity, and preserve this plan's single writer. Each slice
requires Jidoka, a fresh dough-post-change-refactor agent, API generation if a
wire contract changes, one coordinator `./scripts/run.sh pnpm format:changed`,
plan update, commit with check-only hook, push, and asynchronous CI handling.
Retain the completed plan for retrospective and story wrap-up. Do not move the
backlog entry during planning.

## Cumulative assessment

One wiki token model carries raw spelling, decoded destination, and display
text; name acceptance does not define another reference language. The order
prepares reading and writing references before accepting new names, then adds
warnings and durable publication. Intermediate slices are green foundations;
the story is not complete until both publication cases and all warnings pass.

The longest hypotheses are the cross-runtime reading and web-edit slices;
their preparation and independent publication outcomes are already separated.
Slice 5's overrun exposed YAML scalar spelling as a separate preparation beat,
now isolated in 5a immediately before the rename behavior in 5b. After the
first 5a approach also reached the limit, story-boundary reassessment retained
the outcome and bounded research replaced the broad quote-context scan with one
node-mark source-splice proof loop. No other remaining slice has an unexplained
path beyond the active-work hard limit. Full
backend-suite elapsed time is the explicit verification exception. Reassess
slice 8 if durable validation contains a separate name model rather than the
inspected shared rules; do not introduce parallel escape or normalization rules.
