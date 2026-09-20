---
id: SEED-035
status: dormant
planted: 2026-09-20
planted_during: owner shift toward parallel AI IDE work alongside Web Donut
trigger_when: selecting work for the shared local and web notebook direction
scope: large
---

# SEED-035: Keep AI working context and learning material together

## Why This Matters

For notebook owners using local AI IDEs alongside Web Donut, keeping instructions
and visual material outside the synchronized notebook should change to carrying
that context with their notes, while preserving authored work, note identity,
and learning history. Web Donut remains the place for assimilation and recall.

Supporting files are Portable data. Recall history and memory-tracker state
remain server-side; preserving learning history here means retaining those
associations when Portable content changes, not synchronizing learning data.

The immediate value is continuity: reopen a checkout with its working context,
retrieve a supporting file from the web, or use an existing note image locally
without copying it by hand. Research, note completion, linking, and linting are
uses of the AI IDE, not evidence that Donut needs four new automation features.
File acceptance alone does not establish better research or learning outcomes.

The owner selected this direction because the previous local/web note workflow
is largely implemented. No frequency or time-saving measurements were supplied.
The ordering below is a value hypothesis: unblock IDE context first, then make
accumulated visual knowledge portable, ahead of less frequent file cleanup.

## Alternatives and Decision

- **Defer:** reasonable for an owner who only needs today's note workflow; it
  leaves the explicitly requested shared context and file access unresolved.
- **Use IDE-level or untracked instructions:** the cheapest way to use AI now.
  It may be sufficient for personal, machine-wide preferences. It cannot carry
  notebook-specific instructions through Donut to another checkout or browser.
- **Use a separate repository and manual image downloads:** supports versioned
  instructions and occasional local image use, but requires owners to assemble
  and keep two sets of context consistent. This is the strongest workaround,
  not an unusable one; the proposed stories remove that repeated handoff.
- **Preserve supporting files through synchronization first:** a smaller useful
  delivery than building all web file controls. Owners can continue their
  existing local workflow with tracked IDE guidance. Browser retrieval and
  image use remain separate, independently evaluable outcomes.

Recommend the last option as the first story, followed by web retrieval and
existing-image portability. Do not convert IDE guidance into learning notes or
build an IDE-specific rule importer. The owner requested one file mechanism;
separate stories express user journeys, not separate file stores or codecs.

## ADR Principles and Consequences

| Principle | Consequence for these stories |
| --- | --- |
| [ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md): a note, Readme, and Portable path have specific meanings | A supporting file is not automatically a note or a semantic wiki-link destination. Use ordinary file paths without redefining Portable paths. Lint checks; fixes mutate. “Delete” must not silently mean note/folder Trash. |
| [ADR 0002 — Git-native synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md): one accepted tree and publication boundary | Supporting bytes must survive both local publication and subsequent web changes. Image bytes and authored references must describe the same accepted result. No second synchronization protocol, mandatory local manifest, history rewriting, or storage-only success claim. |
| [ADR 0004 — OKF-compatible Markdown](../../docs/adrs/0004-okf-compatible-notebook-markdown-accepted.md): interoperable notes and lossless authored content | Accepting supporting Markdown needs a defined boundary, not “catch a parse error and call it a file.” Unknown concept types remain valid notes under the current profile. Skills and their own Readmes must retain the form their tools expect. |
| [ADRs 0001 and 0003 — Learning semantics](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md): assimilation and grades own learning transitions | File publication is not assimilation or recall. Existing note trackers, grades, and schedules survive file changes; supporting files are not offered as learning units. |
| [ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md): notebook addresses and web/API locations are distinct | Local image references must work outside Donut without depending on private server IDs; their web presentation must respect access rules. This does not authorize a new wiki-link meaning or arbitrary Markdown rewriting. |
| [ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md): handle failures for an explicit outcome | Never conceal rejected publication, lost attachments, or an invalid existing note as successful file acceptance. Do not add speculative retries or recovery machinery. |

The index and record statuses agree: these ADRs are Accepted. ADR 0001's filename
lacks the usual `-accepted` suffix; its explicit status remains authoritative.
ADR 0004's current durable-write rule conflicts with accepting arbitrary
untyped Markdown as current notebook content. The owner has selected the product
outcome; the exact note/file boundary and durable ADR clarification remain open.
This seed neither approves an ADR change nor prescribes the boundary.

All stories preserve notebook access permissions, authored bytes, unrelated
files, and learning history. Existing web operations must not silently drop
supporting files, including files in a folder with no learning notes. Removal
from the current tree does not erase append-only Git history. File-only changes
must be real accepted content changes, even when no learning note changes.

The owner clarified parallel work on 2026-09-20: Donut serializes accepted
content changes into one linear history. When local and web changes diverge,
the local side acquires the latest accepted head, rebases its unpublished work,
resolves conflicts, and pushes a fast-forward result. The remote never merges
or rebases; stale or divergent proposals are refused without loss. This is
the workflow boundary in the amended ADR 0002, not a new remote reconciliation
story. Recall-only activity does not create Portable content divergence.

## Story Decomposition

S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery. Estimates
are hypotheses with the assumptions stated below, not implementation readiness.
Story 6 separates synchronization continuity from story 1's browser retrieval;
existing story identities and anchors are retained. Stories are shown in queue
order. Each crosses whatever product layers its outcome requires.

<a id="story-6"></a>

### Keep IDE guidance with notes through local and web changes

- **Identity:** SEED-035#story-6
- **For / why:** A notebook owner can keep notebook-specific AI guidance in a
  tracked checkout and continue publishing notes without stripping that guidance.
- **Evaluation:** Given a valid note and ordinary `AGENTS.md` or nested skill
  files, publish local changes, edit the note in Web Donut, then acquire the
  accepted notebook in another checkout: notes and supporting bytes are intact,
  and the guidance remains usable in its original form.
- **Scope:** Accept and preserve supporting files and local changes to them,
  including a file-only publication. Separate file storage from note semantics;
  non-OKF Markdown and binary bytes exercise the same file behavior. Browser
  file controls and image rendering are owned by later stories, not prerequisites.
- **Value / learning:** Removes an actual publication barrier to IDE work and
  tests the highest-risk assumption: mixed files can coexist with strict notes
  without hidden loss or accidental learning participation.
- **Effort hypothesis:** L, low confidence until the classification rule is
  settled; assumes reuse of the current Git workflow rather than a new protocol.
- **Depends on:** No new story. Resolve the format decision before executable
  planning; this is a product/ADR decision, not a technical preparation story.
- **Safe stopping point:** Owners can use and transfer their tracked guidance
  with their notes. They still manage files locally and keep web learning intact.

<a id="story-1"></a>

### Browse and download local supporting files in Web Donut

- **Identity:** SEED-035#story-1
- **For / why:** An owner away from the checkout can find and retrieve the
  notebook's supporting material without acquiring the entire repository.
- **Evaluation:** Given accepted supporting files, including a nested skill and
  an image, browse their folder locations and download the exact accepted bytes.
  A folder containing only supporting files is still discoverable.
- **Scope:** One find-and-retrieve journey for files regardless of their source
  or extension. Image preview, text editing, and dedicated IDE screens are not
  required. No rule/skill allowlist or independent binary-file browser.
- **Value / learning:** Delivers the owner's explicit web visibility requirement
  and tests whether file location provides enough context for retrieval.
- **Effort hypothesis:** M, medium confidence; assumes ordinary folder navigation
  can expose the files made available by story 6.
- **Depends on:** Story 6's accepted supporting content.
- **Safe stopping point:** Web retrieval is useful without web deletion or
  specialized rendering; local management remains available.

<a id="story-5"></a>

### Access existing note images as notebook folder files

- **Identity:** SEED-035#story-5
- **For / why:** An existing notebook owner can use accumulated visual knowledge
  in a local AI session without downloading and reattaching each image manually.
- **Evaluation:** Given a notebook with existing uploaded note images, acquire
  its accepted checkout: the referenced image bytes are ordinary folder files
  usable with the notes locally, while existing web image display and learning
  identities remain intact. Story 1 also exposes those files on the web.
- **Scope:** Existing notebook-owned uploaded images. Preserve accepted history;
  represent the transition as new accepted content. Discover the actual ownership
  and reference cases before planning. This promises user access, not merely a
  storage migration. Do not fetch arbitrary remote image URLs or absorb Books.
- **Value / learning:** Benefits current notebooks immediately and tests the
  image/reference continuity that any shared attachment model must provide.
- **Effort hypothesis:** L, low confidence pending existing-image ownership and
  reference cases. A fleet-wide conversion is not presumed to fit this estimate;
  split by independently usable owner journeys if discovery exceeds the band.
- **Depends on:** Story 6. Story 1 is earlier for value; local-added image
  rendering and new web uploads are not user prerequisites. Required web/local
  continuity for these existing images belongs to this story itself.
- **Safe stopping point:** Existing images become usable locally without breaking
  web use. Later new-image workflows can remain deferred; no cleanup may remove
  the only accessible copy or introduce a separate permanent image-file model.

<a id="story-3"></a>

### See locally added image files in Web Donut notes

- **Identity:** SEED-035#story-3
- **For / why:** A learner can use a locally collected or created diagram when
  returning to the note in Web Donut, without uploading it a second time.
- **Evaluation:** Given an image and a note reference authored locally, publish
  them and open the note on the web: the intended image displays from the same
  notebook file and the note retains its learning history.
- **Scope:** Notebook-local image references in note content. Preserve existing
  note-image presentation wherever already supported; do not promise new image
  prompts, new recall modes, or changes to question generation. Reference rules
  must work locally and on the web without changing the meaning of note links.
- **Value / learning:** Completes the local visual-authoring handoff into learning.
- **Effort hypothesis:** M, low confidence until reference spelling and currently
  supported image presentation are established.
- **Depends on:** Story 6. Story 5 may supply reusable behavior; it is not a
  separate user prerequisite and does not justify another image mechanism.
- **Safe stopping point:** Local diagrams work in notes even if new web-upload
  portability is deferred. File retrieval remains the fallback for unsupported
  preview formats, not a claim that they render as images.

<a id="story-4"></a>

### Use newly web-uploaded note images from a local checkout

- **Identity:** SEED-035#story-4
- **For / why:** An owner adding an image in Web Donut can continue working with
  that note and image locally without a second download-and-relink task.
- **Evaluation:** Upload a note image through an existing web authoring flow,
  then receive the accepted result locally: the note references an available
  image file, and its web display still works.
- **Scope:** Newly uploaded note images use the same accepted files and reference
  behavior as the other image stories. The file and its note reference appear
  together. Generic web file upload and new image-authoring features are not
  promised; inventory existing upload flows before selecting execution scope.
- **Value / learning:** Prevents new web-authored visual material from recreating
  the portability gap after existing images have become accessible.
- **Effort hypothesis:** M, low confidence assuming shared image behavior from
  the preceding deliveries; reassess if existing upload flows have distinct
  user outcomes rather than hiding them under “all uploads.”
- **Depends on:** Story 6. Earlier image stories reduce uncertainty and effort;
  neither publication direction is intrinsically a prerequisite for the other.
- **Safe stopping point:** New uploaded images are usable in both contexts with
  no separate attachment-management workflow.

<a id="story-2"></a>

### Delete unwanted supporting files from Web Donut

- **Identity:** SEED-035#story-2
- **For / why:** An owner away from the checkout can remove obsolete guidance or
  attachments from the current notebook without switching tools.
- **Evaluation:** Delete a file from its web folder, then receive the accepted
  result locally: the file is absent and unrelated notes, files, and learning
  histories remain intact.
- **Scope:** The same deletion behavior covers guidance and image files. Define
  the referenced-file outcome during refinement; do not infer note deletion,
  automatic reference rewriting, or recoverable Trash from the word “delete.”
- **Value / learning:** Removes a tool switch for cleanup. Its frequency is
  unmeasured and local deletion is a viable workaround, so it follows the
  authoring/learning handoffs rather than completing CRUD first.
- **Effort hypothesis:** M, low confidence until referenced-file behavior is
  chosen; no history-erasure or new restoration feature is assumed.
- **Depends on:** Stories 6 and 1; specialized image display is not a prerequisite.
- **Safe stopping point:** Current-tree cleanup works without deleting referring
  notes or learning data. Historical Git content retains its existing semantics.

## Ordering and Scope Reduction

Queue continuity → browser retrieval → existing visual knowledge → local visual
authoring → new web-image portability → web cleanup. This prioritizes a concrete
blocked workflow, then user value and learning, rather than CRUD completeness or
technical migration order. Existing-image portability precedes new uploads
because accumulated knowledge is already present; this priority remains a
hypothesis until actual use says otherwise. Code reuse is not a product dependency.

Every delivery uses one supporting-file concept. Recognition as a learning note
adds note semantics; image rendering adds a presentation capability. Story
boundaries must not create per-IDE acceptance rules, competing file authorities,
or separate old/new image stores as permanent product models. Temporary coexistence
with existing attachments ends when stories 5 and 4 have delivered their outcomes.

If scope shrinks, defer web deletion first, then new web-upload portability,
then local-added image display, while preserving existing behavior. Browser
retrieval and existing-image portability can each stand on story 6; do not make
one an artificial prerequisite for the other. Retain deferred promises in this
seed. Current Taken work stays in place and the existing note-presentation
cleanup stays last, as explicitly requested.

## Open Decisions and Refinement Boundaries

- **Note/file classification:** Agree on untyped Markdown, unknown typed concepts,
  tool-owned `README.md` files, and an existing note whose frontmatter becomes
  invalid. The last case must not silently lose its identity or learning history.
  Apply the agreed distinction consistently to publication, export, and lint;
  mandatory classification sidecars would conflict with ADR 0002. Record the
  human-owned ADR 0004 clarification/exception before dependent executable work.
- **Existing images:** Determine ownership and reference forms, including any
  sharing, before promising a conversion scope. Refinement owns concrete image
  locations, current display contexts, and an independently usable split if the
  estimate proves larger than L; no fleet migration design is selected here.
- **File removal:** Choose the observable result for references to a removed
  file. History retention is already settled; warning, refusal, dangling
  reference presentation, and Trash are not interchangeable promises.

No executable plans or implementation are authorized. The first recommendation
is story 6. The new scope preserves all previously requested outcomes; the
additional story separates two independently useful parts of the original first
story rather than adding a new feature ambition.

## When to Surface

Now, under the owner's revised near-future direction; resolve classification
when refining story 6. Ready-made AI skills, arbitrary document previews, Books,
file editing, and broader Git integration require their own selected outcomes.

## Breadcrumbs

- Owner direction and backlog request, 2026-09-20: AI IDEs alongside Web Donut;
  rules, skills, and unsupported files; web browsing/download/deletion; images
  as ordinary folder files. Follow-up requests a critical ADR-grounded review.
- [Near-future direction](../PRODUCT-BACKLOG.md#near-future-direction).
- [SEED-009](SEED-009-git-backed-local-notebook-workflow.md): existing local/web
  note workflow; this seed owns the selected supporting-file and image outcomes.
- [Git synchronization contract](../../docs/notebook-git-synchronization.md):
  accepted tree, publication, identity preservation, and local responsibility
  for rebasing divergent unpublished work before fast-forward publication.
