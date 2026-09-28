// Git mechanics for wrap-up's execution-resource cleanup: Dough Land
// "Retire the worktree" behind wrap-up's gate (confirmed completion receipt,
// no checkout-bound observer), run from the repository management context and
// judged against the authorized remote target; no default checkout is needed.
// Trunk Mode passes no remote execution branch.
import { existsSync, realpathSync } from "node:fs";
import {
  git,
  lsRemoteSha,
  originTrackingRef,
  resolveManagementContext,
} from "../../dough-execute-plan/scripts/publication-git.mjs";

export const trunkTarget = "refs/heads/main";

function canonical(path) {
  return existsSync(path) ? realpathSync(path) : path;
}

export async function isAncestor(workspace, ancestor, descendant) {
  try {
    await git(workspace, "merge-base", "--is-ancestor", ancestor, descendant);
    return true;
  } catch (error) {
    if (error.code === 1) {
      return false;
    }
    throw error;
  }
}

async function refExists(repo, ref) {
  try {
    await git(repo, "show-ref", "--verify", "--quiet", ref);
    return true;
  } catch (error) {
    if (error.code === 1) {
      return false;
    }
    throw error;
  }
}

export async function findWorktree(repository, execution) {
  const { stdout } = await git(repository, "worktree", "list", "--porcelain");
  const wanted = canonical(execution);
  const blocks = stdout.split("\n\n").filter((block) => block.trim() !== "");
  for (const block of blocks) {
    const lines = block.split("\n");
    const pathLine = lines.find((line) => line.startsWith("worktree "));
    if (!pathLine) {
      continue;
    }
    const path = pathLine.slice("worktree ".length);
    if (canonical(path) !== wanted) {
      continue;
    }
    const branchLine = lines.find((line) => line.startsWith("branch "));
    return {
      path,
      branch: branchLine ? branchLine.slice("branch refs/heads/".length) : null,
    };
  }
  return null;
}

function preserved(reason, execution, branch) {
  return {
    removed: false,
    partial: false,
    worktree: "preserved",
    branch: "preserved",
    reason,
    path: execution,
    branchName: branch,
  };
}

function cleaned(worktree, branch, repository) {
  return {
    removed: true,
    partial: false,
    worktree,
    branch,
    reason: null,
    repository,
  };
}

function unverifiedRemoval(reason, execution, branch, worktree, branchResult) {
  return {
    removed: false,
    partial: true,
    worktree,
    branch: branchResult,
    reason,
    path: execution,
    branchName: branch,
  };
}

function hostsThisWorktree(observer, execution) {
  if (!observer?.bound || observer.stopped) {
    return false;
  }
  if (!observer.checkout) {
    return true;
  }
  return canonical(observer.checkout) === canonical(execution);
}

function bothRegistered(observer, closureShas, targetRef) {
  return (
    observer?.bound === true &&
    observer.stopped === true &&
    closureShas.every((sha) =>
      observer.receipts.some(
        (receipt) => receipt.sha === sha && receipt.target === targetRef,
      ),
    )
  );
}

function isNonEmpty(sha) {
  return typeof sha === "string" && sha !== "";
}

async function everyAncestor(workspace, shas, descendant) {
  for (const sha of shas) {
    if (!(await isAncestor(workspace, sha, descendant))) {
      return false;
    }
  }
  return true;
}

export async function removeExecutionResources({
  repository,
  execution,
  branch,
  observer,
  sessionOwned,
  closureShas,
  remoteBranch,
  remote = "origin",
  targetRef = trunkTarget,
}) {
  if (hostsThisWorktree(observer, execution)) {
    return preserved("active checkout-bound observer", execution, branch);
  }
  if (sessionOwned !== true) {
    return preserved("another workspace", execution, branch);
  }
  const management = await resolveManagementContext(repository, execution);
  if (!management) {
    return preserved("management context unavailable", execution, branch);
  }
  const listed = await findWorktree(management, execution);
  if ((!listed && existsSync(execution)) || listed?.branch === null) {
    return preserved("ambiguous checkout", execution, branch);
  }
  if (listed && listed.branch !== branch) {
    return preserved("another workspace", execution, branch);
  }
  if (listed) {
    const status = (await git(execution, "status", "--porcelain")).stdout;
    if (status !== "") {
      return preserved("dirty checkout", execution, branch);
    }
  }
  await git(management, "fetch", remote);
  const tracking = originTrackingRef(targetRef, remote);
  const remoteExecutionBranch =
    typeof remoteBranch === "string" && remoteBranch !== "" ? remoteBranch : "";
  const remoteRef = remoteExecutionBranch
    ? `refs/heads/${remoteExecutionBranch}`
    : "";
  const remoteTip = remoteExecutionBranch
    ? await lsRemoteSha(remote, remoteRef, management)
    : "";
  if (
    remoteExecutionBranch &&
    remoteTip &&
    !(await isAncestor(management, remoteTip, tracking))
  ) {
    return preserved(
      "remote execution tip is not integrated",
      execution,
      branch,
    );
  }
  const shas = Array.isArray(closureShas) ? closureShas : [];
  const published =
    (remoteExecutionBranch ? shas.length >= 1 : shas.length === 2) &&
    shas.every((sha) => isNonEmpty(sha)) &&
    (await everyAncestor(management, shas, tracking));
  const branchRef = `refs/heads/${branch}`;
  const branchPresent = await refExists(management, branchRef);
  const branchContained =
    branchPresent && (await isAncestor(management, branch, tracking));
  if (!published || (branchPresent && !branchContained)) {
    return preserved("unique unpublished work", execution, branch);
  }
  if (!bothRegistered(observer, shas, targetRef)) {
    return preserved("observer obligation unfinished", execution, branch);
  }
  if (listed) {
    await git(management, "worktree", "remove", execution);
    if (await findWorktree(management, execution)) {
      return unverifiedRemoval(
        "worktree removal was not verified",
        execution,
        branch,
        "preserved",
        "preserved",
      );
    }
  }
  if (branchPresent) {
    await git(management, "branch", `--set-upstream-to=${tracking}`, branch);
    await git(management, "branch", "-d", branch);
    if (await refExists(management, branchRef)) {
      return unverifiedRemoval(
        "local branch removal was not verified",
        execution,
        branch,
        listed ? "removed" : "already-absent",
        "preserved",
      );
    }
  }
  if (remoteExecutionBranch && remoteTip) {
    await git(management, "push", remote, "--delete", remoteExecutionBranch);
    if (await lsRemoteSha(remote, remoteRef, management)) {
      return unverifiedRemoval(
        "remote branch removal was not verified",
        execution,
        branch,
        listed ? "removed" : "already-absent",
        branchPresent ? "removed" : "already-absent",
      );
    }
  }
  return cleaned(
    listed ? "removed" : "already-absent",
    branchPresent ? "removed" : "already-absent",
    management,
  );
}
