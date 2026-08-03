---
phase: 08-resolve-pull-export-story-1
plan: 01
subsystem: api
tags: [export, zip, doughnut_id, wiki-links, attachments, markdown]

requires:
  - phase: 07-publish-triage-decisions
    provides: Story 1 strengthen verdict and TRIAGE gap list (identity, links, attachments)
provides:
  - Backend zip contract with merged doughnut_id, wiki→relative MD links, absolute attachment URLs
  - ExportNoteMarkdown helper shared by NotebookZipBuilder
  - publicOrigin plumbing from NotebookController through NotebookExportService
affects:
  - 08-02 (CLI /export E2E proofs against strengthened zip)
  - Phases 9–10 (Stories 2–3 consume same zip identity/link contract)

actuals:
  tokens: 7851
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Verbatim frontmatter identity merge via textual doughnut_id inject (no Frontmatter.fenced dump)"
    - "Two-pass noteId→zipPath map before zip write for wiki relativize"
    - "Request publicOrigin (scheme+host+non-default port) for absolute attachment URLs"

key-files:
  created:
    - backend/src/main/java/com/odde/doughnut/services/notebookExport/ExportNoteMarkdown.java
    - backend/src/test/java/com/odde/doughnut/services/notebookExport/ExportNoteMarkdownTest.java
  modified:
    - backend/src/main/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilder.java
    - backend/src/main/java/com/odde/doughnut/services/NotebookExportService.java
    - backend/src/main/java/com/odde/doughnut/controllers/NotebookController.java
    - backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java
    - backend/src/test/java/com/odde/doughnut/services/NotebookExportServiceTest.java
    - backend/src/test/java/com/odde/doughnut/controllers/NotebookExportControllerTest.java

key-decisions:
  - "Identity merge is textual inject into splitVerbatim fences; never Frontmatter.fenced (D-02/HYG-02)"
  - "Wiki resolve is same-notebook title→lowest-id→path map; unresolved keeps [[wiki]] (D-04/A1)"
  - "Attachment rewrite only matches root-relative /attachments/images/{digits}/… prefixed with publicOrigin (D-05)"

patterns-established:
  - "ExportNoteMarkdown.assemble owns identity + wiki + attachment rewrite; ZipBuilder owns path map + zip I/O"
  - "NotebookController.publicOriginFrom derives origin without trailing slash"

requirements-completed: []  # EXP-01 remains open until 08-02 E2E proofs close the phase

coverage:
  - id: D1
    description: Exported notes merge doughnut_id into author frontmatter (or emit minimal fence)
    requirement: EXP-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java#mergesDoughnutIdIntoAuthorFrontmatterWithoutStrippingProperties
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/notebookExport/ExportNoteMarkdownTest.java#emitsMinimalIdentityFenceWhenNoFrontmatter
        status: pass
    human_judgment: false
  - id: D2
    description: Resolvable same-notebook wiki links become relative Markdown links; unresolved stay double-bracket
    requirement: EXP-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java#rewritesResolvableWikiLinkToRelativeMarkdownAndAttachmentToAbsoluteUrl
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java#leavesUnresolvedWikiAsDoubleBracketText
        status: pass
    human_judgment: false
  - id: D3
    description: Attachment refs become absolute remote URLs; zip has no attachment blobs
    requirement: EXP-01
    verification:
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/notebookExport/NotebookZipBuilderTest.java#rewritesResolvableWikiLinkToRelativeMarkdownAndAttachmentToAbsoluteUrl
        status: pass
      - kind: unit
        ref: backend/src/test/java/com/odde/doughnut/services/NotebookExportServiceTest.java#forwardsPublicOriginIntoExportedAttachmentUrls
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-08-03
status: complete
---

# Phase 08 Plan 01: Resolve pull/export story 1 — backend zip strengthen Summary

**Backend Markdown zip now merges `doughnut_id`, rewrites resolvable wiki links to relative MD paths, and prefixes attachment refs with request `publicOrigin` — shared by HTTP export and CLI `/export`.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-08-03T06:31:57Z
- **Completed:** 2026-08-03T06:40:00Z
- **Tasks:** 3/3
- **Files modified:** 8 backend (+ STATE touch in tracer commit)

## Accomplishments

- Added `ExportNoteMarkdown` for verbatim identity merge + wiki/attachment rewrite without touching `Frontmatter.java`
- Extended `NotebookZipBuilder.build` with notebook name + publicOrigin and a two-pass noteId→path map
- Threaded origin from `NotebookController.exportNotebook(HttpServletRequest, …)` through `NotebookExportService`
- Unit proofs for all three Story 1 gaps plus edges (no-FM, unresolved wiki, nested relativize, collisions)

## Task Commits

1. **Task 1: End-to-end export zip happy path** - `9c0648f95b` (feat)
2. **Task 2: Expand zip unit coverage** - `018a01b668` (test)
3. **Task 3: Compile-fix callers / HYG-02 allowlist** - `9d7c13969a` (test)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated NotebookExportControllerTest for new arity**
- **Found during:** Task 1 GREEN verify
- **Issue:** `exportNotebook` gained `HttpServletRequest`; controller tests still called old arity
- **Fix:** Pass `MockHttpServletRequest` with scheme/host/port 9081
- **Files modified:** `NotebookExportControllerTest.java`
- **Commit:** `9c0648f95b`

**2. [Rule 2 - Missing critical] Service/controller tests for origin forwarding**
- **Found during:** Task 3
- **Issue:** Plan asked for observable origin forwarding assertion at boundary
- **Fix:** Added service attachment-origin assert + `publicOriginFrom` unit assert
- **Files modified:** `NotebookExportServiceTest.java`, `NotebookExportControllerTest.java`
- **Commit:** `9d7c13969a`

### Other notes

- Pre-commit formatting hook also staged a small `.planning/STATE.md` touch into the tracer commit (unrelated planning metadata drift) — no product impact.
- TDD RED/GREEN were combined into one feat commit for the tracer (signature change made separate RED compile awkward); edge coverage landed as dedicated test commits afterward.

## Threat Flags

None — export remains auth-gated (`assertReadAuthorization`); wiki resolve stays within exported notebook note set; attachment rewrite limited to `/attachments/images/{digits}/…`.

## Self-Check: PASSED

- FOUND: `ExportNoteMarkdown.java`
- FOUND: `NotebookZipBuilder.java` with `publicOrigin`
- FOUND: commits `9c0648f95b`, `018a01b668`, `9d7c13969a`
- FOUND: `pnpm backend:test_only` green
- FOUND: diff excludes `Frontmatter.java`, `cli/src/sync/applyPull.ts`, `writeNotebookExport.ts`
