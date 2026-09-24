// Git operations shared by the installed publication commands. Test fixtures
// import these mechanics, but production never imports fixture setup.
import { execFile } from "node:child_process";
import { isAbsolute, join } from "node:path";
import { promisify } from "node:util";

export const exec = promisify(execFile);

export async function git(cwd, ...args) {
  return exec("git", args, { cwd });
}

export async function revParse(cwd, ref) {
  return (await git(cwd, "rev-parse", ref)).stdout.trim();
}

export async function lsRemoteSha(remote, ref) {
  const { stdout } = await exec("git", ["ls-remote", remote, ref]);
  return stdout.trim().split(/\s+/)[0];
}

// Branch name from an authorized heads ref (refs/heads/NAME → NAME).
export function targetBranchName(targetRef) {
  if (!targetRef.startsWith("refs/heads/")) {
    throw new Error(`authorized target must be a branch ref: ${targetRef}`);
  }
  return targetRef.slice("refs/heads/".length);
}

// Authorized publication target as the workspace's remote-tracking ref.
export function originTrackingRef(targetRef, remote = "origin") {
  return `${remote}/${targetBranchName(targetRef)}`;
}

export async function pushExactRef(workspace, sha, remote, targetRef) {
  await git(workspace, "push", remote, `${sha}:${targetRef}`);
}

export async function indexLockPath(checkout) {
  const printed = (
    await git(checkout, "rev-parse", "--git-path", "index.lock")
  ).stdout.trim();
  return isAbsolute(printed) ? printed : join(checkout, printed);
}

export async function captureCheckout(checkout) {
  return {
    head: await revParse(checkout, "HEAD"),
    status: (await git(checkout, "status", "--porcelain")).stdout,
    staged: (await git(checkout, "diff", "--cached")).stdout,
    unstaged: (await git(checkout, "diff")).stdout,
    index: (await git(checkout, "ls-files", "-s")).stdout,
  };
}

// Soft-fail lookup of the workspace's remote-tracking tip for an authorized
// target. Missing tracking refs are treated as an absent remote tip.
export async function fetchedTarget(workspace, targetRef, remote = "origin") {
  try {
    return await revParse(workspace, originTrackingRef(targetRef, remote));
  } catch (error) {
    const text = `${error.stderr ?? ""}\n${error.message ?? ""}`;
    if (
      error.code === 128 ||
      /unknown revision|Needed a single revision|ambiguous argument/.test(text)
    ) {
      return null;
    }
    throw error;
  }
}

function isNonFastForward(error) {
  const text = `${error.message ?? ""}\n${error.stderr ?? ""}`;
  return /rejected|non-fast-forward|fetch first/i.test(text);
}

// Push one exact candidate; non-fast-forward rejection is returned, not thrown.
export async function tryPushExactRef(workspace, candidate, remote, targetRef) {
  try {
    await pushExactRef(workspace, candidate, remote, targetRef);
    return { rejected: false };
  } catch (error) {
    if (!isNonFastForward(error)) {
      throw error;
    }
    return { rejected: true, error };
  }
}

// Inspection-only maintenance result for a default checkout after remote
// acceptance. Does not refresh. A clean checkout already at the accepted tip
// is already current; any other state is deferred.
export function maintenanceFromInspection(checkoutState, remoteSha) {
  if (checkoutState.head === remoteSha && checkoutState.status === "") {
    return "already current";
  }
  return "deferred";
}

export async function inspectDefaultCheckoutMaintenance(
  workspace,
  defaultCheckout,
  remote,
  targetRef,
) {
  if (!defaultCheckout) return null;
  const remoteTip = await revParse(
    workspace,
    originTrackingRef(targetRef, remote),
  );
  const checkout = await captureCheckout(defaultCheckout);
  return maintenanceFromInspection(
    { head: checkout.head, status: checkout.status },
    remoteTip,
  );
}
