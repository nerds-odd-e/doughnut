# Release and closure boundary

SEED-002#story-3 · 2026-10-28 · Asia/Singapore

Terry requested: “Release the landed changes once CI passes, then wrap up without
more live tests.” The source completion agreement retires remaining live rounds;
no omitted result is represented as passed. Existing read-only authorization
contracts remain in force. Normal-browser repair, null-revision/disambiguation,
withdrawal/reconnection, isolation and computer-off freshness were not rechecked.

## Release receipt

- Main CI 40625799063, attempt 1: success for
  `982d32c62c268e200d1372c681ef24e881ec7e84`; backend jar, frontend-dist and
  cli-dist available and unexpired before tagging.
- Immutable `v1.3.57`, raw ref `fc1859cd894223f47222d72513dd2604584ed7bd`;
  release and build SHA both `982d32c62c268e200d1372c681ef24e881ec7e84`.
- Application Release 40627376887, attempt 1: success. Actual GCP publication
  job 122615592497 succeeded; admission job 122615439640 succeeded.
- `gs://dough-01/deploy/application-release.json` records the identities above
  and `outcome: succeeded`. Last-successful-deploy records the build at
  `2026-10-28T03:39:55Z`, jar SHA-256
  `af73dc486bc8d478885180ca236b406a658c3faaee75845528bc6de9a544ae74`.
  MIG isStable and versionTarget.isReached both true, no actions pending.
- Operational verification: root HTML 200, actual JS asset
  `/assets/main-CDCSIgl-.js` 200 JavaScript, CLI HEAD 200 JavaScript, backend
  health 200 prod with the exact build SHA. No additional user-journey,
  OAuth lifecycle, note-read or browser acceptance test was run.
- Schema prerequisites were already verified before v1.3.56; this increment
  changes MCP output only and introduces no schema change.

## Outcome and process review

Attributable implementation commits: 46c1729c (read-only descriptors) and
b5f3df02 (structured full-read attribution plus setup/evidence docs). 4b507045
is records-only provenance; 926b0d57 is Take publication. Integration 982d32c
preserves those histories and unrelated planning/ADR changes, which are excluded
from this implementation assessment. Original contract and plan are recoverable
before this source-authorized completion agreement.

The existing retrieval, permission, OAuth and accepted-revision owners remain
cohesive; no new store, endpoint or runtime was added. HTTP-boundary regression
proof is appropriate for this descriptor/result change; 2,961 backend tests
passed with zero failures/errors and two skips, and exact main CI passed all
checks including E2E. No new refactoring or test-cost correction is supported.
North Star accepted-content ownership and ADRs 0002/0005/0007 remain aligned.
The browser repair is a released compatibility experiment, not a demonstrated
external root-cause fix. Further live proof is owner-waived, not silently inferred.
Product advice: no additional product change or follow-up plan is recommended
within the owner's completed delivery scope. Process review used available
conversation and agent returns; earlier compacted history limits process claims.
No new supported process finding was identified; no process-log change is needed.

## Recovery and observation

Original execution worktree/branch remain unchanged pending closure:
`/Users/terryyin/git/doughnut/.worktrees/direct-chatgpt-knowledge`,
`codex/direct-chatgpt-knowledge`; created for this work, management directory
`/Users/terryyin/git/doughnut/.git`. The stopped branch stream's terminal receipt
has unread zero and confirmed process exit; it is not a passing CI receipt.

Closure main observer: GitHub ci.yml, workflow 5208694 (donut CI), coordinator
`codex-direct-chatgpt-20261028`, cell 198, session 7611, PID 8103,
directory `/tmp/dough-ci-501/watch-muCd45`. Register actual accepted closure SHAs;
complete this stream only after the final main publication.

Consumer discovery found no supplier agreements for SEED-002#story-3, with one
unrelated discovery problem: SEED-066 manual-discovery-findings is a level-four
heading. Targeted reading found voice-input observations and no dependency block
or reference to this supplier; it remains untouched. Two contextual SEED-073
links will move to maintained assistant setup documentation during cleanup.

An older execution-branch run, 40625749778 at b5f3df02, failed in the existing
real-OpenAI audio scenario: it expected “Please be quiet.” or “請安靜”, but the
returned text was “請保持安靜”. This is an external transcription variant rejected
by that scenario's literal alternatives; the MCP-only production diff does not
change recording/transcription. Its log is
`/tmp/donut-chatgpt-branch-ci-failure.log`. The selected main run independently
passed that shard and every other required job. Closure observes main and its
ignored-only descendants; the failed branch run is not relabelled successful.
