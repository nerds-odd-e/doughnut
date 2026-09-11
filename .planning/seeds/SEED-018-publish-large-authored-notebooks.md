---
id: SEED-018
status: dormant
planted: 2026-09-11
planted_during: jap3 notebook publication failure diagnosis and pipe-name discussion
trigger_when: notebook owners publish authored names or large local commits
scope: large
---

# SEED-018: Publish large authored notebooks without renaming knowledge or excessive waiting

## Why This Matters

For notebook owners editing in Obsidian or an AI IDE, publishing an authored
notebook should preserve intended names and finish in a practical time, while
preserving the accepted history and learning data on failure.

The reported jap3 commit added 9,277 Markdown files to an existing notebook.
The CLI reported `fetch failed`; the backend continued processing and eventually
reported an invalid alias at 16:28:51 on 2026-09-11. The alias contained ASCII
`|`. The accepted head remained `d9ce5fb`, whereas local main was `6a2df06`, and
the database retained 1,127 live notes. The available log contained 13,703 SQL
statements and 1,214 note inserts before rejection. These are observations from
the available log, not a complete timed profile. A request thread accumulated
about 807 seconds of CPU time. Node's default 300-second response-header timeout
is consistent with the CLI symptom, but the original error cause and elapsed
request time were not captured, so the exact transport failure remains inferred.

## Alternatives and Decision

- Deferring leaves both the rejected authored alias and the expensive publication
  attempt unresolved.
- Manually replacing pipes with fullwidth characters is a smaller workaround,
  but changes the owner's authored names. The user selected acceptance with a
  warning instead.
- Splitting the notebook into small commits may reduce waiting per request but
  imposes manual work and does not establish acceptable large-commit performance.
- Extending the timeout alone neither reduces server work nor prevents a late
  validation rejection. The user requested profiling followed by performance
  improvement.

The user subsequently split the performance work into an evidence-producing
investigation followed by optimization. Static inspection alone can identify
repeated work but cannot establish its runtime importance. Use a focused baseline
profile to select areas worth improving, without requiring an optimization plan
as an investigation deliverable.

Capture three independently valuable stories in the explicit user priority order.
This split does not authorize executable planning or implementation.

## Story Decomposition

Effort bands follow SEED-009: S = 30–60 minutes, M = 1–2 hours,
L = 2–4 hours. Estimates are hypotheses, not commitments.

<a id="story-1"></a>

### 1. Accept pipe characters in note names with a compatibility warning

#### Goal

Notebook owners can create, edit, and publish notes whose intended titles or
aliases contain ASCII `|`, and follow references to those notes in Donut without
renaming their knowledge. This removes the name-related publication obstacle;
large-commit completion time remains story 3's outcome, informed by story 2.

#### Scope

- Accept otherwise valid pipe-bearing note titles and recognized aliases through
  web editing and Portable notebook tree publication/import. Preserve ASCII `|`
  through save, subsequent editing, export, and reimport; do not silently convert
  it to fullwidth `｜`. Existing fullwidth names remain distinct and unchanged.
- Keep filename-equals-title: title `A|B` exports as `A|B.md`. Aliases remain
  authored YAML strings, not filenames. Preserve author-owned frontmatter and
  existing note identity and learning history under the existing update contract.
- References to these titles and aliases must work in the body and recognized
  YAML link values, including relationship endpoints and one-level list items.
  Product-generated links must use the agreed spelling. Existing path,
  notebook-scope, ambiguity, property-selector, and navigation rules still apply.
- Preserve `[[A|B]]` as destination `A`, display `B`, even if a note titled
  `A|B` also exists. Never guess a literal-pipe destination from available notes.
- Warn without blocking save or requiring acknowledgement in the title/alias
  editor and notebook health. The existing notebook-health lint result also
  exposes warnings after local publication; no new CLI warning transport is
  promised. Findings identify the
  affected note and whether the issue is its title or alias; repeated occurrences
  need not produce repeated messages.
- A title warning explains Windows filename incompatibility and possible broken
  references in Obsidian/other Markdown tools. An alias-only warning explains
  link compatibility without falsely claiming that the note's filename is
  Windows-incompatible. Include a usable Donut reference example.
- Retain unrelated validity requirements: unsafe paths, reserved Readme names,
  title length/uniqueness, and recognized-alias YAML shape. A pipe warning does
  not turn other validation failures into successful writes. Existing atomic
  publication rejection and authorization guarantees continue to apply.

**Deferred promises:** Windows filename encoding or checkout support, other
currently forbidden characters, folder/notebook-name expansion, automatic
migration of fullwidth names, general Markdown interoperability, a new warning
dashboard, and bulk-publication performance. These are delivery exclusions,
not new rejection rules.

#### Reference contract

The user approved the concise contract written directly in
[ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
and requested slice planning. The following implementation examples elaborate
that rule without expanding the ADR. Implementation is not yet requested.

Use backslash escaping inside wiki destinations: `\|` means a literal pipe,
`\\` means a literal backslash, and the first unescaped `|` starts the optional
display label. Other backslashes remain literal; this adds no permission for
backslashes in note names or aliases. Scan escapes once, so two backslashes
before a pipe represent a literal backslash followed by the label separator.
Apply the same two escape spellings in a display label; later unescaped pipes
remain display text, consistent with the existing first-separator rule.

- `[[A\|B]]` targets title or alias `A|B` and displays `A|B`.
- `[[A\|B|Read this]]` targets the same name and displays `Read this`.
- `[[A\|B\|C]]` targets `A|B|C`; a pipe count is not a product limit.
- `[[folder/A\|B#prop:meaning|Read this]]` selects the existing `meaning`
  property on that note using the existing property contract.
- YAML is decoded before wiki escapes: `source: '[[A\|B]]'` and
  `source: "[[A\\|B]]"` carry the same link value. An alias declaration such
  as `aliases: ['A|B']` contains the actual name, with no wiki escaping.
- Existing ordinary links and literal backslashes unrelated to these escapes
  retain their meaning. Apply the grammar uniformly to existing
  tokens, without automatic migration or lookup-dependent fallback. Authors
  intending a literal backslash must double it where it would form an escape.

This is a Donut extension, not a claim of Obsidian compatibility.
Obsidian documents `|` as the display-label separator and warns that names
containing it may not work as links. Its table guidance also uses `\|` for
table syntax, so the new destination meaning must not be advertised as an
interchangeable Obsidian table-link spelling. General table interoperability is
not promised by this story.

#### Key examples

1. **Create and edit:** Given a valid new note, enter title `A|B` and save → the
   note retains that exact title, shows a nonblocking compatibility warning, and
   still has that title after reopening and editing its body. Renaming an
   existing note to `A|B` likewise preserves its identity and learning history.
2. **Alias publication:** Given a valid local note named `Topic.md` with
   `aliases: ['A|B']`, publish → acceptance succeeds, the alias is unchanged,
   and the author can see a link-compatibility warning. Export retains
   `Topic.md`; it does not rename the file to the alias.
3. **Distinct destinations:** Given separate notes `A` and `A|B`, follow
   `[[A|B]]` → note `A` with label `B`. Under the agreed contract,
   `[[A\|B]]` and `[[A\|B|Read this]]` both navigate to note `A|B`.
4. **Alias and scope:** Given exactly one matching alias `A|B`, its escaped
   reference resolves to that note. If the existing scope rules find multiple
   candidates, it remains ambiguous and asks for disambiguation; accepting a
   pipe does not select an arbitrary note.
5. **Authored round trip:** Given `folder/A|B.md` with valid frontmatter and
   references in both body and YAML, publish and export → the title, alias
   values, authored link spelling, and destinations survive. YAML quoting must
   preserve the decoded values in the reference-contract examples above.
6. **Independent rejection:** Given a proposal containing an allowed pipe and
   an unrelated invalid alias shape or unsafe path, publish → the independent
   validation error still rejects atomically, leaving accepted state unchanged.

#### Architecture and compatibility evidence

[Accepted ADR 0004](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md)
governs filename-as-title, lossless codec behavior, wiki body/YAML semantics,
and resolution. The user approved the concise pipe-acceptance and escaping
addition after reviewing the ADR diff; detailed implementation examples stay here.
[Accepted ADR 0005](../../docs/adrs/0005-web-routes-accepted.md) continues to
govern resolved note/property navigation and ordinary Markdown URL semantics.
No alternative URL-reference contract is implied.

Evidence checked during refinement, 2026-09-11:

- [Microsoft filename rules](https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file)
  reserve `|` in Windows filenames.
- [Obsidian internal links](https://obsidian.md/help/links) document the label
  separator and warn about pipe-bearing names.
- [Obsidian table syntax](https://obsidian.md/help/advanced-syntax) uses a
  backslash before the display-label pipe inside a table.
- The current Donut name normalizer converts ASCII pipes to fullwidth; alias
  validation rejects them, and wiki parsing splits on the first pipe. Both
  preservation and reference semantics therefore belong to this outcome.

#### Open decisions

None blocking slice planning. Exact warning copy and implementation details
follow the concise ADR and the examples above.

#### Delivery assessment

- **Effort hypothesis:** L, low confidence; assumes one coherent shared link
  rule can support the affected workflows. Refine or resplit if compatibility
  investigation shows this exceeds the band.
- **Depends on:** No queued product prerequisite. ADR alignment belongs inside
  this story; its concise wording has been approved by the user.
- **Safe stopping point:** Pipe-bearing names can be used end to end with warnings
  and unambiguous links even if publication performance work is deferred.

Executable plan: [Accept pipe note names](../quick/105-accept-pipe-note-names/PLAN.md).

<a id="story-2"></a>

### 2. Identify large-publication bottlenecks with a reproducible baseline

- **For / why:** Donut maintainers can decide which areas of large-notebook
  publication deserve improvement using runtime evidence, rather than selecting
  repeated work solely because it looks expensive in the code.
- **Evaluation:** A maintainer can repeat a representative publication workload
  from a documented starting state using the delivered profiling infrastructure,
  inspect the baseline data, and understand which improvement areas the evidence
  supports and which remain uncertain.
- **Scope:** Perform a focused static inspection first to identify candidate
  costs, then baseline profiling of roughly 10,000 valid note additions and a
  late-validation failure case. Capture end-to-end timing and server measurements,
  workload and environment details, starting-state/reset instructions, and the
  observed acceptance or rejection outcome. Preserve the reusable profiling
  infrastructure and baseline data with reproducible instructions.
- **Handoff:** Record the findings and links to the infrastructure and baseline
  data in this story. Populate story 3's statement with evidence-backed areas to
  improve and references to that same infrastructure and data. Distinguish static
  hypotheses from measured bottlenecks. An executable optimization plan is not a
  deliverable; story 3 is refined later using these outputs.
- **Value / learning:** Establish where publication time is spent and provide a
  repeatable basis for evaluating improvements. The evidence remains useful for
  prioritization even if optimization is deferred or cancelled.
- **Effort hypothesis:** L, low confidence; assumes a bounded investigation and
  reusable workload setup, not a general performance-monitoring platform.
- **Depends on:** No dependency on pipe support for a valid benchmark. Using the
  original pipe-bearing jap3 content unchanged depends on story 1.
- **Safe stopping point:** Maintainers have reproducible baseline evidence and
  supported improvement areas without changing publication semantics or claiming
  a performance improvement. Profiling preserves accepted data and history;
  invalid proposals still reject atomically.

<a id="story-3"></a>

### 3. Publish large notebook commits within a practical measured time

- **For / why:** Notebook owners can publish a commit of roughly 10,000 new notes
  and receive a definitive outcome without excessive waiting or manual splitting.
- **Evaluation:** A representative valid large commit completes and its accepted
  head and notes are visible in Donut. Record comparable before/after end-to-end
  timing and server measurements. Invalid proposals still leave accepted history
  and stored notebook state unchanged and return a useful rejection.
- **Scope:** Improve the areas supported by story 2's findings and repeat its
  workloads using the delivered profiling infrastructure and baseline data to
  demonstrate the benefit for success and rejection. Refine this story after
  story 2 supplies that evidence; specific improvements are not selected yet.
  Preserve authorization, authored content,
  note identity, learning history, and atomic acceptance. Increasing transport
  timeouts alone does not satisfy the story.
- **Value / learning:** Determine what drives large-publication time and reduce
  that work, rather than assuming SQL counts alone establish the bottleneck.
- **Improvement areas and evidence:** Pending story 2. Its handoff will add the
  supported areas and links to profiling infrastructure and baseline data here
  before later story refinement.
- **Effort hypothesis:** L, low confidence pending story 2; if the measured
  work exceeds a few hours, refine the story into independently useful outcomes
  before execution planning rather than committing to a broad optimization rewrite.
- **Depends on:** Story 2's findings, reusable profiling infrastructure, and
  baseline data. Prior related notebook publication work supplies the existing
  functionality, not a new queue item.
- **Safe stopping point:** The measured workload publishes faster with existing
  correctness guarantees intact; this does not require general synchronization,
  background jobs, resumable upload, or all other large-data operations.

## Ordering and Scope Reduction

The user selected name acceptance first, then split the performance work into
story 2's static inspection and baseline profiling followed by story 3's
optimization. Preserve that order. Refine story 3 later from story 2's outputs.
If work must be deferred, defer story 3 first; story 2 retains its measured
learning and reusable profiling workflow. Do not deliver name acceptance without
working reference semantics.

## Open Decisions

- Story 1's remaining decisions are recorded in its section above.
- Story 2: representative workload and reproducible benchmark environment remain
  for refinement. Approximately 10,000 additions is the motivating workload, not
  a product size limit.
- Story 3: acceptable completion-time target and bounded improvement scope remain
  for later refinement based on story 2's evidence.

## When to Surface

Name acceptance is taken; investigation and optimization are the next two queued
stories following the jap3 diagnosis and the user-requested split.

## Breadcrumbs

- User discussion and explicit backlog ordering, 2026-09-11.
- User-requested split, 2026-09-11: static inspection and baseline profiling
  deliver infrastructure, data, and improvement areas; refine optimization later.
- [ADR 0004 — OKF-compatible notebook Markdown profile](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md).
- [SEED-009 — Git-backed local notebook workflow](SEED-009-git-backed-local-notebook-workflow.md).
- [SEED-016 — Initial notebook and folder Readmes](SEED-016-initial-notebook-and-folder-readmes.md).
- Diagnostic input: `/Users/terryyin/git/notebooks/jap3`, notebook 66873;
  local commit `6a2df06aafcea5caa5182ede426bb899712f7653`.
