#!/usr/bin/env node
// The Git repository primitives the Git-aware backlog adapters share:
// resolving the real repository root a caller means, running real `git`
// commands against it, and registering the shared resolver as a custom merge
// driver for one attributed path. Nothing here knows about the backlog's own
// content or invariants; `product-backlog-git-merge.mjs` and its driver own
// that.
import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { BacklogError } from "./product-backlog-refusal.mjs";

const driverName = "dough-product-backlog";
const driverScript = fileURLToPath(
  new URL("./product-backlog-git-driver.mjs", import.meta.url),
);

// Every real Git call this adapter makes, in one place: `cwd` is always the
// resolved repository root, never the caller's own working directory, so a
// launch from a subdirectory of the repository still names paths Git and
// this tool agree on. `env` is only ever additional variables layered onto
// this process's own environment — never a replacement of it — for the rare
// call (resuming a rebase) that must not stop to open an editor.
export function git(args, cwd, env) {
  return execFileSync("git", args, {
    cwd,
    encoding: "utf8",
    env: env ? { ...process.env, ...env } : process.env,
  });
}

export function gitLine(args, cwd, env) {
  return git(args, cwd, env).trim();
}

// The same call, reporting failure instead of throwing, for the calls whose
// non-zero exit is an ordinary outcome this adapter reads rather than a
// crash: a merge that left conflicts, or a commit Git refuses because
// something unrelated is still unresolved.
export function gitOutcome(args, cwd, env) {
  try {
    return { code: 0, stdout: git(args, cwd, env) };
  } catch (error) {
    return {
      code: typeof error.status === "number" ? error.status : 1,
      stdout: error.stdout ?? "",
      stderr: error.stderr ?? "",
    };
  }
}

export function repositoryRoot(cwd) {
  try {
    return gitLine(["rev-parse", "--show-toplevel"], cwd);
  } catch {
    throw new BacklogError(`${cwd} is not inside a Git repository.`);
  }
}

// Where Git itself keeps one named piece of its own state for this checkout.
// `<repoRoot>/.git` is a directory only in a primary checkout; in a linked
// worktree (`git worktree add`) it is a file, shared state such as
// `info/attributes` lives in the common Git directory, and in-progress
// operation state (`MERGE_HEAD`, `rebase-merge`, `sequencer`, ...) is that
// worktree's own. Git already knows which is which, so it is asked rather
// than assumed; a relative answer is relative to the checkout it was asked
// from.
export function gitPath(repoRoot, name) {
  return resolve(
    repoRoot,
    gitLine(["rev-parse", "--git-path", name], repoRoot),
  );
}

// Registers this one path to be reconciled by the shared resolver for every
// future content merge Git attempts on it in this checkout. Both the
// attribute and the driver command are written to this checkout's own Git
// directory — never to a file a project tracks or ships — because
// registering the driver for every checkout a project might have, on every
// install, is a separate, later concern; this adapter only needs the
// mechanism to be in effect for the merge it is about to run.
export function ensureDriverRegistered(repoRoot, file) {
  const attributesPath = gitPath(repoRoot, "info/attributes");
  const line = `/${file} merge=${driverName}`;
  const existing = existsSync(attributesPath)
    ? readFileSync(attributesPath, "utf8")
    : "";
  if (!existing.split("\n").includes(line)) {
    const separator = existing !== "" && !existing.endsWith("\n") ? "\n" : "";
    writeFileSync(attributesPath, `${existing}${separator}${line}\n`, "utf8");
  }
  git(
    [
      "config",
      `merge.${driverName}.name`,
      "Reconcile the product backlog through its shared resolver",
    ],
    repoRoot,
  );
  git(
    [
      "config",
      `merge.${driverName}.driver`,
      `${process.execPath} ${driverScript} %O %A %B %P`,
    ],
    repoRoot,
  );
}

// The recoverable shape of a stop neither adapter itself decided: something
// other than this path is still unresolved, so nothing about this path's own
// content is judged. `product-backlog-git-rebase.mjs` and
// `product-backlog-git-cherry-pick-stop.mjs` independently reached this exact
// wording, differing only in the operation noun, once cherry-pick's own
// "blocked" case existed alongside rebase's; expressed once here rather than
// duplicated a second time.
export function blockedStopMessage(operationNoun, file, stderr) {
  return (
    `The ${operationNoun} stopped, but ${file} is not the unresolved path; ` +
    `something unrelated needs human resolution first. This gate leaves ` +
    `the ${operationNoun} exactly as it is.\n${stderr}`
  );
}

// Reads one small state file Git itself maintains for an in-progress
// operation (inside a rebase's or the sequencer's own directory, or
// cherry-pick's top-level marker), trimmed, or `undefined` when absent or
// empty — the same "nothing here means no operation in progress" shape every
// caller below shares.
function readOperationStateFile(path) {
  try {
    const contents = readFileSync(path, "utf8").trim();
    return contents === "" ? undefined : contents;
  } catch {
    return undefined;
  }
}

// Which of Git's two rebase state directories, if either, is actually on
// disk right now. Modern Git defaults ordinary (non-`-i`) `git rebase` to the
// "merge" backend (`.git/rebase-merge`), verified directly against this
// project's installed Git rather than assumed; `.git/rebase-apply` is the
// older "apply" backend, still reachable through `git rebase --apply` or a
// project's own configuration. Both name the commit currently failing to
// apply, just under different filenames, so one caller-facing shape covers
// either.
function rebaseStateDirectory(repoRoot) {
  const merge = gitPath(repoRoot, "rebase-merge");
  if (existsSync(merge)) {
    return { directory: merge, replayedFile: "stopped-sha" };
  }
  const apply = gitPath(repoRoot, "rebase-apply");
  if (existsSync(apply)) {
    return { directory: apply, replayedFile: "original-commit" };
  }
  return undefined;
}

// The real revisions a stopped rebase is actually replaying, read from Git's
// own rebase state rather than trusted from the "ours"/"theirs" conflict
// labels the merge machinery reuses for rebase too. During a rebase those
// labels are reversed from what they mean during a merge: "ours" is the
// destination this step is replaying onto, and "theirs" is the commit being
// replayed off the branch actually being rebased — the opposite of whose
// side is whose. `destination` is this step's actual parent (current HEAD,
// which only equals `onto` for the first replayed commit; later commits
// replay onto the previous step's own new commit). `onto` and `origHead` are
// the rebase's overall destination and the branch's pre-rebase tip, which
// stays reachable there for as long as the rebase remains unfinished or is
// abandoned, so a stopped rebase never puts the unpublished suffix at risk.
// Returns `undefined` when no rebase is in progress.
export function rebaseState(repoRoot) {
  const state = rebaseStateDirectory(repoRoot);
  if (!state) {
    return undefined;
  }
  const replayedCommit = readOperationStateFile(
    join(state.directory, state.replayedFile),
  );
  if (!replayedCommit) {
    return undefined;
  }
  return {
    replayedCommit,
    replayedParent: gitLine(["rev-parse", `${replayedCommit}^`], repoRoot),
    destination: gitLine(["rev-parse", "HEAD"], repoRoot),
    onto: readOperationStateFile(join(state.directory, "onto")),
    origHead: readOperationStateFile(join(state.directory, "orig-head")),
    headName: readOperationStateFile(join(state.directory, "head-name")),
  };
}

// The commit a stopped `todo` line names, however Git chose to abbreviate it
// ("pick <sha> <subject>"), read only when `CHERRY_PICK_HEAD` itself is
// absent — confirmed empirically to happen for one real, in-between state: a
// human resolved Git's own "this step is now empty" stop by committing it
// directly (`git commit --allow-empty`) rather than through this tool.
// That commit clears `CHERRY_PICK_HEAD` immediately, before the sequencer has
// advanced its own `todo` past that same line, so a cherry-pick genuinely
// still in progress (confirmed by `git cherry-pick --continue` still working
// correctly there) would otherwise be misreported as finished.
function pendingPickedCommit(sequencerDirectory) {
  const todo = readOperationStateFile(join(sequencerDirectory, "todo"));
  const line = todo?.split("\n").find((entry) => entry.startsWith("pick "));
  return line?.split(" ")[1];
}

// The real revisions a stopped cherry-pick is actually applying, read from
// Git's own state under `.git/sequencer/` and the top-level
// `CHERRY_PICK_HEAD` marker — confirmed empirically to live there, never
// under `.git/rebase-merge`/`.git/rebase-apply` the way a rebase's own state
// does. A cherry-pick (or a sequence still holding further commits) is
// genuinely in progress whenever either marker exists; `CHERRY_PICK_HEAD`
// alone is not a reliable "finished" signal (see `pendingPickedCommit`).
// Unlike a rebase, cherry-pick's index stages are never reversed: stage 2
// ("ours") is always the current branch (the destination), and stage 3
// ("theirs") is always the commit being picked, the same convention an
// ordinary merge uses. `mainline` is the same 1-based parent number a caller
// would pass to `-m` for picking a merge commit (defaulting to 1, the same
// default `git cherry-pick -m 1` would use, which is also correct for an
// ordinary, non-merge picked commit). Returns `undefined` when no cherry-pick
// is in progress.
export function cherryPickState(repoRoot, mainline = 1) {
  const sequencerDirectory = gitPath(repoRoot, "sequencer");
  const pickedCommit =
    readOperationStateFile(gitPath(repoRoot, "CHERRY_PICK_HEAD")) ??
    (existsSync(sequencerDirectory)
      ? pendingPickedCommit(sequencerDirectory)
      : undefined);
  if (!pickedCommit) {
    return undefined;
  }
  return {
    pickedCommit,
    pickedParent: gitLine(
      ["rev-parse", `${pickedCommit}^${mainline}`],
      repoRoot,
    ),
    destination: gitLine(["rev-parse", "HEAD"], repoRoot),
  };
}
