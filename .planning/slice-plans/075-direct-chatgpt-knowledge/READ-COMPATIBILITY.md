# Full-read compatibility observation

SEED-002#story-3 · 2026-10-28 · owned execution checkout unchanged at 4b507045.

The normal ChatGPT conversation `6ae156ec-cb44-83ec-a418-2cedd6bbc17b`
called `/Donut/link_6ae1517f977c8191946eabf2493e676a/read_note` with
`noteId: 28467` three times. Read-only CDP inspection of its recorded conversation
response independently confirmed tool messages containing `{}`. Search and graph
returned JSON text. Sanitized calls/result evidence is retained at
`/tmp/donut-chatgpt-read-empty-evidence.json`; screenshot at
`/tmp/donut-chatgpt-read-empty.jpg`. No assistant reasoning was extracted.

The connected Donut plugin in this chat returns full text content, source fields
and acceptedRevision `4220da3ee0994d2f0c464a3464c2d7e51c2a72d8` for the same
owner-selected note, 実話. It has structuredContent null and isError false.
Refreshing the existing tools succeeded (HTTP 200); the subsequent fresh browser
read still reported `{}`. This does not establish a backend retrieval defect.

A fresh read-only investigator, `/root/read_result_diagnosis`, confirmed read and
graph share the same TextContent serialization and transport, and SDK 2.1.0
supports structuredContent and outputSchema. No server-side cause was found.
Dual output is a bounded compatibility experiment; the HTTP red/green will prove
its wire contract, while the actual browser recheck alone can establish repair.
Official reference: https://developers.openai.com/plugins/reference#tool-results.

The original slice 2 assumed configuration and observation without product
repair. Active diagnosis exceeded its ten-minute bound; no product edits existed
and the checkout was clean before refinement. Split out new slice 2 for structured
full-read attribution; original slices 2–8 become 3–9. Original dated evidence
keeps its numbering. Remaining boundaries are retained, with lifecycle waits
still explicit. Nine slices; execution authority and the Taken identity persist.
The same service/DTO remains the retrieval owner; no architecture change or
new PFE candidate is introduced. Browser acceptance remains an early probe.

## Slice 2 accepted wire-contract proof

Fresh implementation agent `/root/structured_read` owned only `ReadNoteTool.java`
and `ReadNoteMcpEndpointTest.java`. The canonical authenticated HTTP test uses a
persisted owner/token, a Git-bound Physics notebook and an actual accepted
snapshot. It asserts structured content equals parsed text, all attribution
fields including the snapshot commit, and the discovered output schema.

Literal command in the owned checkout:
`CURSOR_DEV=true nix develop -c pnpm backend:test_only`.
Red before production edits: exit 1, 2,961 tests, exactly the new structured-value
assertion failed, two skips (`/tmp/donut-chatgpt-structured-red.log`).
Final green: exit 0, 598 suites, 2,961 tests, zero failures/errors, two skips,
1m12s backend phase plus 10s migration (`/tmp/donut-chatgpt-structured-green.log`,
`/tmp/donut-chatgpt-structured-green-summary.json`). Sequential host proof ran on
the 16-CPU/48-GiB Mac; launch load 6.34/7.03/7.30, no competing owned suite.

An intervening attempt passed result equality/attribution but failed immutable
List value coercion in the JSONPath assertion. Its recorded schema already
contained the correct required array. Hamcrest `contains` preserves the same
five expected names; the subsequent full suite above passed. Logs and bounded
schema are `/tmp/donut-chatgpt-structured-green-attempt-1.log` and
`/tmp/donut-chatgpt-structured-schema-attempt-1.json`.

Consumer trace `/tmp/donut-chatgpt-structured-consumer-trace.md` disposes all 43
listed consumers. Existing full-suite authorization, OAuth identity, revision
freshness and REST cases pass. REST DTO/service, CLI and generated API are
unchanged; no API generation trigger applies. Existing E2E consumers retain
the same text contract; E2E and actual browser repair are not claimed here.

Fresh source-only reviewer `/root/read_source_promises` was given the canonical
story plus human decisions, without implementation/plan/diff/evidence. It returned
individual Goal/Scope/constraint/deferred/example promises after reading the seed,
supplier guide and ADRs 0002/0005/0007. It confirms the source does not prescribe
structured output: ordinary ChatGPT attribution still needs live proof, and all
remaining lifecycle/isolation/freshness/device promises remain open. The human
waived only live cancellation. Current slice 2 establishes a coherent wire
contract; slice 3 must evaluate the compatibility experiment after release.

Independent `/root/structured_read_refactor` returned `## REFACTOR COMPLETE`:
no Java/test edits; source prose wraps shortened without semantic changes,
guide punctuation corrected, and G2's original dated quote restored. All nine
changed files are at most 250 lines. The accepted backend boundary/setup/assertions
remain identical, so no test rerun applies. Repository whitespace check passed.
Refactor consumer trace `/tmp/donut-chatgpt-structured-refactor-trace.md` disposes
all 54 listed consumers. No generated API trigger or new proof gap was introduced.
