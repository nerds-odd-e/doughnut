# Consistent frontmatter after local publication

## Source and goal

Follow-up to [SEED-009 Story 4](../../seeds/SEED-009-git-backed-local-notebook-workflow.md#story-4)
and the manual UAT on 2026-09-06. The user has authorized execution of this plan.
The temporary detailed report is `/private/tmp/donut-manual-uat-20260906/REPORT.md`;
the reproductions needed for execution are embedded below so that report and
the current test database are not prerequisites.

**Outcome:** A notebook owner can publish valid authored metadata and use the
resulting note on the web without being told to repair valid YAML. Invalid
duplicate-key proposals stop before remote state changes.

## Scope and current decisions

- Reject duplicate YAML mapping keys during Git proposal validation, for
  additions and existing-note edits. Report the offending Portable path and
  duplicate-key problem. Preserve the accepted head, bundle, and notes, and
  retain the local proposal for correction.
- Preserve valid nested author-owned metadata. It is valid YAML, unlike
  duplicate keys. Do not solve the mismatch by rejecting all nested metadata
  or flattening/dropping it.
- For a note whose valid nested metadata cannot be represented by the current
  Properties controls, render only its body as rich content and direct metadata
  editing to Markdown. Rich body editing and pasting preserve the original
  frontmatter prefix, including comments, quoting, order, and line endings.
  The limited fallback applies to valid nested metadata, not every existing
  `unsupported_value` error or invalid structural property.
- In this fallback, the whole metadata block is edited in Markdown. Building
  nested Properties controls or partially editing scalar rows alongside hidden
  nested values is excluded. Existing flat Properties editing remains supported.
- Malformed YAML, duplicate keys, and malformed fences retain the existing
  protective rich-editor behavior; they must not enter the valid-metadata
  fallback. Explicit `readonly` and image-upload locking still apply.
- **Accepted execution scope:** preserve nested metadata and enable rich body
  editing, as proposed in the plan the user instructed us to execute. This
  follows [Accepted ADR 0004](../../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md),
  which requires valid YAML and preservation of author-owned/unknown metadata.
  A later user correction changes this contract before dependent execution.
- No schema, API wire-shape, Git identity, history, or concurrency redesign.
  No migration or automatic repair of already accepted invalid notes. An author
  can correct duplicate keys in Markdown before the next publication.
- Exclude the two UAT recovery-message findings, authorization diagnostics,
  structural drift repair, folder creation, and multiple-change publication.
  The separate quick plan 016 and the concurrently edited home seed are not
  owned by this plan.

## Reproductions and expected observations

**Duplicate keys:** add and commit `Duplicate Keys.md` in a clean eligible
checkout:

```markdown
---
type: Note
author: first
author: second
---
Body.
```

Current UAT: publication accepted `5e125e7`; web note n12 reports “Map keys must
be unique.” Target: actionable rejection, unchanged remote, intact local commit.

**Valid nested metadata:** publish `Nested Metadata.md`:

```markdown
---
type: Note
# Preserve this author's annotation.
custom:
  source: 'local'
---
Original body.
```

Current UAT: publication accepted `b96f680`; web note n10 reports unsupported
frontmatter values, renders YAML as a body heading, and freezes rich editing.
Target: body rendered normally; metadata guidance refers to Markdown without
calling this valid document invalid; a rich body edit saves with the original
metadata prefix intact and appears in accepted Git history.

**Controls:** unknown scalar `type: UatCustomType` with flat metadata remains
usable; scalar/list Properties editing keeps its current behavior; truly
malformed YAML remains protected. Test fixtures must be created afresh rather
than depending on UAT note IDs.

## Execution context

- `NotebookGitProposalMarkdownFormat` walks proposed Markdown and checks UTF-8,
  fenced YAML, top-level map, and type. Its current `new Yaml().load(...)` path
  accepted both UAT examples. Keep strict syntax policy here; do not change the
  permissive `Frontmatter.parse` behavior globally as an incidental fix.
- `NotebookGitProposalAdditionValidationControllerTest` already asserts a bad
  addition leaves the binding and note list unchanged. Extend its existing
  data-driven rejection case. `NotebookGitProposalMarkdownFormatControllerTest`
  provides the existing-note edit variation.
- `e2e_test/features/cli/cli_notebook_clone.feature` already owns installed-CLI
  add/commit/publish/open behavior. Its command task currently insists on exit
  zero, so a narrow expected-rejection observation is immediate preparation.
- `RichMarkdownEditor.properties.spec.ts` currently labels nested YAML as
  “invalid YAML” and explicitly expects frozen Quill. Replace that conflation:
  preserve malformed-input coverage with a genuinely malformed fixture and
  supply the nested-metadata behavior separately.
- `noteContentFrontmatterParse.ts` combines YAML document validation and
  conversion into the limited Properties model. Separate those concerns only
  as far as the fallback needs. Root shape, syntax, and duplicate-key errors
  must remain distinguishable from a valid map with nested metadata.
- The existing `verbatimFrontmatterPrefixAndBody` helper reconstructs an LF
  prefix; its name alone is not proof of exact prefix preservation. Use the
  authored substring when editing the body rather than dumping YAML again.
- `AuthoredNoteContent.prepareDocumentForSave` validates selected properties and
  normalizes stored type. `NoteLeadingFrontmatter.ensureTypeKey` returns original
  content when its canonical type is unchanged. Reuse this save path; prove
  nested-prefix preservation at the existing controller/visible save boundary.

## Ordered slices

### 1. Observe an expected installed-CLI rejection
Type: Structure
Status: done
Proof: Existing installed-CLI success scenarios and the focused CLI task tests
remain green; the next slice can assert an expected nonzero result and its text.

Internal change: introduce a narrowly named expected-failure invocation through
the existing managed CLI process runner, retaining output after expected exit 1.
Share only the invocation/lifecycle code needed by success and failure callers;
keep their expectations explicit at the caller. Do not catch arbitrary failures
or weaken the existing success runner. Thin Cucumber glue delegates to a named
page-object operation.

Unchanged external behavior: normal installed CLI invocations still require
exit zero and retain their existing output assertions. This enables only the
duplicate-key publication rejection in slice 2. No new PTY framework.

Sizing: approximately five minutes of implementation, focused verification,
and cleanup; medium confidence. Reuse `waitForPtyExit` and managed-session
ownership. If new lifecycle behavior is needed, refine before proceeding.

### 2. Reject a duplicate-key publication without remote changes
Type: Behavior
Status: done
Proof: An installed-CLI duplicate-key addition fails with the path and reason,
the note is absent, and the original local proposal survives. Existing
controller rejection observations establish unchanged accepted state; an
existing-note edit is a data variation of the same validation rule.

Behavior: an eligible proposal contains duplicate YAML keys → publish → reject
before creating/updating notes or advancing accepted Git history.

First reproduce the UAT through the existing installed-CLI feature and the
addition controller's rejection table. Confirm failure is acceptance of the
invalid document, not command plumbing. Make the smallest parser-policy change
in `NotebookGitProposalMarkdownFormat` that rejects duplicate mapping keys,
including duplicates inside a nested mapping. Preserve valid nested mappings
and unknown types; do not conflate duplicate mapping keys with repeated list
items. Exercise one existing-note edit variation through the format controller.

Use existing binding snapshots and note observations. Do not assert against a
mutable entity reference as the only before/after head evidence; capture the
pre-publication values. Keep CLI head/files intact after rejection. No repairs,
automatic commits, or automatic retries.

Stopping point: invalid new publications are rejected; valid nested notes
retain the current web limitation until slices 3–4.

Sizing: approximately five minutes excluding required suite/E2E runtime;
medium confidence after slice 1 removes the command-observation preparation.

### 3. Read a valid nested-metadata note as ordinary body content
Type: Behavior
Status: done
Proof: A mounted rich editor given the nested reproduction renders only the
body, offers Markdown metadata guidance, and does not show a malformed-YAML
alert. Genuinely invalid YAML retains the protective fallback.

Behavior: a saved note has valid nested metadata → open rich view → read its
body without YAML being rendered as body text or being called invalid.

Replace the conflated nested-as-invalid component fixture. Introduce the
smallest document classification needed to distinguish syntax-invalid content
from valid nested metadata outside the Properties model. Keep metadata outside
the rich body and preserve its raw prefix. Until slice 4, metadata/body changes
for this limited view continue through Markdown; say so in the visible guidance
instead of silently presenting an editable surface that cannot save.

Do not broadly reinterpret every `unsupported_value` result: non-map roots,
invalid structural values, malformed fences, and duplicate keys remain invalid.
Existing flat Properties tests remain authoritative. Test the actual mounted
component; helper-only parser assertions do not establish this visible outcome.

Stopping point: valid nested notes are readable without a false corruption
warning, with Markdown editing still available.

Sizing: approximately five minutes; medium confidence. If classification changes
spread through unrelated property consumers, narrow the fallback or refine this
leaf before introducing a new generalized property representation.

### 4. Save rich body edits without changing nested metadata
Type: Behavior
Status: in-progress
Proof: Publish a valid nested note through the installed CLI, edit its body in
the web rich editor, and observe the saved body plus unchanged metadata after
reload. Accepted downloaded Markdown retains the metadata and note identity.
Mounted-editor data variations cover typing/paste and a CRLF prefix.

Behavior: a valid nested note is open → edit or paste into its rich body → save
the new body on the same note with the authored metadata prefix preserved.

Enable body editing in slice 3's valid-metadata case. Compose body updates and
`pasteComplete` using the raw prefix, covering both existing update paths in
one preservation rule. Replace slice 3's interim body-edit guidance with
“Edit metadata in Markdown.” Keep actual readonly and image-upload locks.
Metadata controls stay absent for this fallback; do not allow a partial property
edit to discard unrepresented nested values.

Extend the installed-CLI feature's publication-to-web scenario with the nested
fixture and existing rich-edit steps. Use the mounted component for exact
prefix observations (comments, quote choice, ordering, LF/CRLF); reuse/adapt
`NotebookGitMixedEditingControllerTest.savesAWebEditOnTheSameLocallyCreatedNote`
for the persisted/downloadable metadata variation instead of repeating its
canonical ancestry proof. Keep publication → web save one coherent example;
do not add another general multi-checkout synchronization framework.

Stopping point: the selected UAT mismatch is resolved. Nested values remain
author-editable in Markdown and survive ordinary body editing.

Sizing: approximately five minutes excluding focused E2E/runtime; medium
confidence with classification already delivered by slice 3. Most changes are
the existing rich editor's two composition paths and fixture variations. If
save normalization changes the metadata despite unchanged canonical type,
preserve that failing evidence and refine this leaf before changing codecs.

## Promise ownership

| Promise | Owner and observable evidence |
|---|---|
| Duplicate keys reject with actionable filename/reason | 2: installed CLI + addition controller |
| Accepted state and local proposal survive rejection | 2: captured binding/bundle/note values and local Git/file observations |
| Same rule for edits and nested duplicate keys | 2: controller data variations |
| Unknown types, valid nested maps, repeated list items remain valid | 2: positive control/retained acceptance proof; 4: actual nested publication |
| Valid nested YAML is distinct from syntax errors | 3: mounted editor rendering and malformed-input control |
| Body view excludes frontmatter; metadata remains accessible via Markdown | 3: visible body and guidance; 4 replaces interim guidance |
| Rich typing/pasting preserves authored nested prefix | 4: mounted component exact payloads, LF/CRLF variations |
| Save persists on same note and accepted Git history | 4: installed-CLI/web scenario + existing controller variation |
| Flat Properties behavior and readonly locks remain valid | 3–4: related mounted-editor suite |

## Verification and delivery

Follow the bug-fixing workflow within each Behavior: demonstrate the right red,
make the smallest fix, then confirm green. Do not commit a red E2E example;
use the repository's `@wip` convention while implementing a multi-beat case.

- Backend: `CURSOR_DEV=true nix develop -c pnpm backend:test_only` — backend
  rules require the whole backend unit suite, including during reproduction.
- Frontend iteration: `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/form/RichMarkdownEditor.properties.spec.ts`.
  Include affected parser/list/editor suites; finish frontend changes with
  `CURSOR_DEV=true nix develop -c pnpm frontend:test`.
- Installed-CLI proof: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_notebook_clone.feature`.
  Run focused existing CLI task checks if the runner has a test owner; do not
  add a second harness merely to test the structural extraction.
- Respect `data-app-busy` via the existing page-object waiter. Testability
  resnapshot is allowed for initial fixtures only, never after publication or
  the web save being proved.
- On execution, required per-slice wrap-up: Jidoka → fresh post-change-refactor
  agent → API generation only if a wire contract actually changed → coordinator
  runs `./scripts/run.sh pnpm format:changed` once → update this plan → commit
  with independent lint hook → push, with asynchronous CI observation. Preserve
  other work, especially quick plan 016 and the home seed changes.
- Five minutes is a sizing hypothesis, not a guarantee. Scrutinize an overrun;
  at ten minutes refine the same plan unless the focused suite or external wait
  alone explains the elapsed time. Record that exception at the time.

## Readiness

Slice 3: valid nested maps/lists now render only body with accurate Markdown
editing guidance; Properties remain absent and the interim body is read-only.
Malformed YAML, duplicates, and structural-value failures remain protected.
Mounted reproductions failed first, then all 338 frontend files / 1,836 tests
passed. Independent refactor required no changes; selective formatting and type
checking passed. Raw prefixes retain authored LF/CRLF/BOM substrings for slice 4.
Slice 4 now owns replacing interim read-only behavior and exact saved payload
observations. Slice 3 took approximately seven minutes including suite/startup
runtime and remained one coherent reading outcome.

Slice 2: strict proposal YAML rejects duplicate keys at any mapping depth.
Controller rejection observations capture immutable head/bundle/timestamp values;
installed CLI verifies actionable path/reason, intact proposal, and absent remote
note. Full backend suite and four focused CLI scenarios pass after the expected
red. Independent refactor required no changes; selective formatting passed.
The active implementation exceeded the five-minute hypothesis (~8–9 minutes);
required full-suite and E2E runtime plus the CI repair pause explain the longer
wall time. The behavior and ownership remained bounded.

CI repair: run 34029294495 attempt 1 failed Other Unit Tests because its shared
fake GitHub process announced readiness before installing SIGTERM handling.
A forced 500ms scheduling window reproduced the exact missing shutdown marker;
registering the handler first passed the same test and all six host lifecycle
tests. Repair af65fb4d5f was independently reviewed, formatted, committed, and
pushed. All paused work (including pre-existing seed/016 changes) was restored
and verified, then the exact recovery stash was dropped.

Slices 2 and 3 can be implemented concurrently: slice 2 owns backend/E2E,
slice 3 owns frontend parsing/rendering and mounted tests; neither depends on
the other's implementation. Coordinator serializes wrap-up and commits. Slice 4
waits until both are delivered. Other planning work remains unowned.

Execution evidence: slice 1's installed-CLI clone/add/edit feature passed all
three scenarios before and after independent refactor. Extracted
`cliE2eInstalledCli.ts` to retain the 250-line file limit. Selective formatting
passed without changes. CLI expected-rejection observation is ready for slice 2.

CI observer: coordinator `frontmatter-root`, checkout `/Users/terryyin/git/doughnut`,
repository `nerds-odd-e/doughnut`, branch `main`; yielded cell `52`, PTY session
`64878`. Receipt directory/PID were not exposed at initial yield; retain this
exact live session for shutdown. Pending CI is not a success assertion.

**Ready for direct execution under the stated nested-metadata assumption.**
The refinement-trigger check found one immediate structural preparation and
three bounded Behavior leaves; classification is delivered before editing, and
expected CLI failure capture is delivered before its rejection proof. No product
code was changed and no tests were run during planning. Execution is a separate
step. Retain this plan while active; clean up spent history after completion.
