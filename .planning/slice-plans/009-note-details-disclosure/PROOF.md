# Note details execution proof

## Aggregate retrospective

Reviewed the original execution-ready story/plan at claim `cb6eb20ba6` and
the uncontaminated published range through `522f3f9f72`: implementation
`8d30de7ea3`, `89a2bb85d1`, `4b9d3792f7`, `f6ba658ab7`, `522f3f9f72`, and
owned CI repair `df295a0d86`. No sibling work is attributed. Original intent,
all five outcomes, retained property/save/route journeys, current whole-product
ownership and Accepted ADRs 0001/0004/0005/0007 agree with the aggregate code,
tests, and maintained docs. No product defect, refactoring residue, or justified
test-cleanup correction remains. The new proof uses real mounted surfaces;
existing E2E journeys supply integration proof rather than driving the initial
implementation. Headless geometry does not establish real-device comfort.

Process review used coordinator history and the concrete implementation/
refactor returns. Existing findings ODF-264, DD-252, DD-253 gained one occurrence
each in DearDough.md (374 physical lines, below the warning threshold): oversized
startup reads, generation/format scheduling, and the missed direct relationship
control consumer. These are process proposals, not new product work. Product
advice is no change to the backlog; remembered disclosure and relative dates
remain deferred without user evidence. No correction plan was created.
Applicable CI remained pending on the retained branch observer at review;
the completion operation owns its final verdict and shutdown.

## Mounted property rows

Accepted: `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test
tests/notes/NoteTextContent.detailsDisclosure.spec.ts
tests/components/form/RichMarkdownEditor.frontmatter.spec.ts
tests/notes/NoteTextContent tests/notes/NoteContextReader.spec.ts
tests/notes/NoteShow.spec.ts` passed 7 files / 50 tests. Both required
frontend typechecks passed. The new real-surface test collapses the provided
state, edits Quill, and observes exact frontmatter plus body in the save.
Providerless editor tests retain revealed rows and composition. Independent
refactoring found no edits; coordinator selective formatting passed. Slice 2
replaces the temporary internal-state setup with the disclosure control.

## Compact disclosure

Accepted: the focused browser command below passed 103 files / 596 tests,
including the real coarse-pointer 44px observation, both typechecks passed,
and local Cypress passed 54 scenarios across the seven property, layout,
wiki-link, editing, reification, memory-tracker, and image features named in
preparation. The same public control now drives the hidden-body-save test.
Valid route arrivals, stale-key notice, collapse on a property route, and
same-key arrival after another note's reset are observed at the note surface.
Independent refactoring moved repeated test helpers without changing their
actions/assertions; accepted proof remains valid. Selective formatting passed.
Budget exception: one isolated E2E startup plus 2:56 of consumer journeys
extends verification beyond the 10-minute implementation checkpoint.

## Verification and delivery

Focused browser command (add the new spec under the notes prefix):

```sh
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes tests/components/form/RichMarkdownEditor tests/pages/NoteShowPage.imageUpload.spec.ts tests/pages/RecallPage.justReview.spec.ts
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

`tests/notes` covers the note surface, title editing, content saving, and
the context reader; the editor prefix covers draft, readme, image, relation,
dialog, and property-location consumers. Both typechecks are required by
`.agents/skills/frontend/SKILL.md`. No backend or API change is planned.

Slices that change selectors or page objects (2 and 3) trace their consumers
and obtain bounded local observations for affected journeys before delivery,
as required by execution proof acceptance; hosted CI also runs the full suite.
This execution correction replaces the preparation's CI-only assumption for
known affected consumers. Do not
infer real device usability from headless Chromium; manual testing is not
requested.

Execution follows `AGENTS.md`: Jidoka, fresh dough-post-change-refactor agent,
coordinator `./scripts/run.sh pnpm format:changed` once, plan update, agent
commit with the check-only hook, and push under `dough-execute-plan`. Do not
implement or execute this sequence in preparation.


## Relationship CI repair

CI repair at slice 3's delivery boundary: run `39388493180`, attempt 1, on
`89a2bb85d151f827b64e6b3e769ab4f6f8eea4ef` found the direct relationship
edit helper. The existing scoped reveal helper repairs that consumer.
`env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm cy:run --spec
e2e_test/features/relationships/relationship_edit_and_remove.feature` first
reproduced 4 passing / 1 inaccessible relation-control failure, then passed
all 5 scenarios after the repair. Independent refactoring required no edits.
Slice 3's completed changes and proof were preserved during repair.

