# `/push --dry-run` — known issues

**Status:** Recorded, not yet fixed. Local note only — not filed as a GitHub issue yet.

**Where the feature lives:** `upstream/main` / tag `cli-v0.5.0` (`cli/src/sync/previewPush.ts`,
`cli/src/sync/pushBaseline.ts`, `cli/src/sync/pushArgument.ts`,
`cli/src/commands/notebook/pushSlashCommand.tsx`). This local checkout's `main` is currently
42 commits behind `upstream/main` and does not have `/push` at all — check against
`upstream/main` when working on either issue below, not this checkout's `main`.

## 1. `~` is not expanded in the workspace path — reads/writes the wrong directory

`parsePushArgument()` (`cli/src/sync/pushArgument.ts`) returns the raw argument string as
`workspacePath` after only `stripSurroundingQuotes()`:

```ts
return { workspacePath: stripSurroundingQuotes(workspacePart) }
```

Compare with `/export`'s `parseExportDestination()` (`cli/src/sync/exportDestination.ts`),
which goes through the shared `parseDirectoryArgument()` (`cli/src/sync/directoryArgument.ts`):

```ts
const expanded = expandTilde(stripSurroundingQuotes(argument.trim()))
const directory = resolve(process.cwd(), expanded.path)
```

`/push --dry-run` never calls `expandTilde()` or `resolve()`. Typing
`/push --dry-run ~/downloads/ettatest` passes the literal string `~/downloads/ettatest` down to
`readWorkspace()` and `pushBaseline.ts`, which `join()` it as a plain relative path from the
process's cwd — `~` is treated as an ordinary directory name, not the home directory.

**Reproduced 2026-07-30:** running that exact command with cwd `$HOME` created and read
`$HOME/~/Downloads/ettatest/` (case-insensitive match on `downloads`/`Downloads`) instead of
`$HOME/downloads/ettatest/` — a real directory on disk, confirmed via `find $HOME -iname '~'`.
That shadow directory happened to hold a stale `tetete.md` from an earlier test and no
`qqqq.md`, which produced a doubly-confusing symptom: the note the user had actually just
edited (`qqqq.md`) never appeared in the report at all (`workspace.get(path) === undefined` for
that path skips it silently — `previewPush.ts`'s `if (local === undefined) return []`), while an
unrelated, unedited note (`tetete.md`) was reported as changed, because its stale shadow-copy
differs from the notebook's current content.

**Fix:** have `parsePushArgument()` route the workspace path through the same
`parseDirectoryArgument()` `/export` and `/sync` already use, instead of its own
`stripSurroundingQuotes`-only handling.

**Workaround:** pass an already-expanded absolute path, e.g.
`/push --dry-run /Users/me/downloads/ettatest`.

## 2. A note only gets a `(push)`/`(pull)`/`(CONFLICT)` label after it has agreed with Doughnut at least once

**Status: fixed 2026-07-31.** `writeNotebookExport.ts` (`/export`) now calls `savePushBaseline()`
right after writing files, seeding `.doughnut-sync/baseline.json` with every exported note's
content — the moment export finishes, workspace and Doughnut content agree by construction, so
this is a legitimate baseline point. A `/push --dry-run` right after an edit made post-export now
correctly labels the note on its very first run, no priming run needed. See
`cli/tests/writeNotebookExport.test.ts` ("seeds the push baseline …", "overwrites the push
baseline …") and `cli/tests/previewPush.test.ts` ("labels a note (push) on the very first preview
when /export primed the workspace") for the regression coverage.

**Accepted remaining gap:** a workspace never created by this CLI's `/export` (hand-authored
files, files copied some other way) still has no history to seed from, so its very first
`/push --dry-run` still reports an unlabeled `difference` when local and remote already differ.
This is intentional — `classify()` still refuses to guess a direction with no baseline at all,
since guessing risks mislabeling a `pull` as a `push`. Decided with the user 2026-07-31; not
pursuing this further.

<details>
<summary>Original write-up (kept for context)</summary>


By design (`previewPush.ts`, `pushBaseline.ts`): each note is labeled by comparing local and
remote against a recorded *baseline* — the content the two sides were last seen to agree on.
`classify()` only attempts a `push`/`pull`/`conflict` verdict `if (baseline !== undefined)`;
otherwise it falls back to a plain, unlabeled `'difference'`:

```ts
function classify(baseline, local, remote) {
  if (baseline !== undefined) {
    ...  // only path that can return 'push' | 'pull' | 'conflict'
  }
  return local === remote ? 'nothing' : 'difference'
}
```

`nextBaseline()` only records an entry for a note when `workspace.get(path) === remote` *in that
same run* (or carries forward an existing entry) — so a note whose local and remote have never
yet matched during any `/push --dry-run` run has no baseline entry, and stays an unlabeled
`difference` no matter how many times it's compared.

**Consequence:** the natural sequence — `/export`, edit a file locally, `/push --dry-run` — never
shows a label on the very first run, because no baseline was ever recorded for the edited note.
To get a labeled result, the workspace needs a "priming" run of `/push --dry-run` *before* any
local edits (right after `/export`, while local still equals remote for every note) — that priming
run reports `No changes to push.` and appears to do nothing, but it silently writes the baseline
every later run depends on. This priming step is not mentioned in `/push`'s `CommandDoc` or any
user-visible output.

**Reproduced 2026-07-30:** editing `qqqq.md` immediately after `/export`, with no priming run in
between, then running `/push --dry-run` against the correct (post-fix-1) path produced:

```
qqqq.md
  --- workspace
  +++ Doughnut

    # qqqq

  - qqq dddd
  -
  + qqq

1 note would change.
```

with no `(push)`/`(pull)`/`(CONFLICT)` label — confirmed by inspecting
`.doughnut-sync/baseline.json` in that workspace, which had no entry for `qqqq.md`
(`{"notebookId":617,"notes":{"tetete.md":"..."}}`, `tetete.md` only, because that note's local and
remote *did* match on this run).

**Open question, not yet decided:** whether the fix is documentation (mention the priming run in
`pushDoc`/help text) or behavior (e.g. treat "no baseline yet" as an implicit priming point and
still attempt a same-run label when there is exactly one differing side to compare against — this
is a design decision, not typed up here).

</details>
