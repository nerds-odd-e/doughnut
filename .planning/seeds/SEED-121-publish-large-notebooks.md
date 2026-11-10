---
id: SEED-121
status: active
planted: 2026-11-10
scope: story
---

# Publish large notebooks without losing history

<a id="publish-large-notebooks-without-losing-history"></a>

### Publish large notebooks without losing history

**Identity:** SEED-121#publish-large-notebooks-without-losing-history
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Beneficiary and goal.** A notebook owner can publish committed edits to an
existing large notebook with the supported Donut CLI. If the live notebook and
accepted Git tree disagree, the owner gets a useful recovery path that
preserves accepted commits, learning data, and their unpublished work.

**Problem and evidence.** On 2026-11-10, a three-file Markdown publish from a
notebook with about 11,000 tracked files failed in CLI `0.5.9`. Its Git tree
listing was 1,394,368 bytes and reproducibly exceeded Node's 1 MiB subprocess
buffer. A temporary CLI workaround reached the server, which refused the
proposal because the live Portable tree differed from accepted `main`. Pull
confirmed the local commit was already based on the server's accepted head;
retrying returned the same refusal. The folder paths, READMEs, attachment
contents, and sampled notes matched; the differing path and cause remain
unconfirmed. The full-tree CLI scan was already replaced in source
(`7c96542978`); the public download inspected that day contained the newer
scan but reported `0.5.5`, below the installed version, so `donut update`
would skip it.

**Scope.** Verify the CLI works when the full Git tree listing exceeds 1 MiB,
and make the fixed build available through the normal update path. Diagnose
and correct the confirmed cause of the live/accepted mismatch. Provide a
supported way to identify the differing paths and reconcile the affected
notebook so the owner can retry publication. A stale local base and a
server-side mismatch must give different recovery guidance. Preserve the
accepted history and the publication checks in
[ADR 0002](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md).
Existing file and upload size policies stay in force.

**Evaluation.**

- From an affected installed CLI, `donut update` installs a correctly
  versioned fixed build. That build publishes a small Markdown commit on a
  healthy notebook whose full Git listing exceeds 1 MiB.
- A notebook whose live tree differs from its accepted head is refused with
  actionable guidance. The supported recovery identifies the discrepancy,
  preserves accepted history and learning data, and lets the owner publish
  their pending changes after pulling and resolving any conflicts if the
  accepted head advances.
