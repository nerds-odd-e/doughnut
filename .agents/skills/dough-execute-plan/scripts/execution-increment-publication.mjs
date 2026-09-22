// Git mechanics for one validated execution increment or owned repair.
// The caller supplies the owned workspace, the owned unpublished suffix,
// the authorized remote target, and how the accepted result is registered.
// This pushes only that target. Stash, checkout refresh, and observer startup
// stay with their own owners. Installed guidance is the agent's contract.
import {
  git,
  lsRemoteSha,
  originTrackingRef,
  pushExactRef,
  revParse,
} from "./publication-test-fixtures.mjs";

async function fetchedTarget(workspace, targetRef) {
  try {
    return await revParse(workspace, originTrackingRef(targetRef));
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

export async function publishExecutionIncrement({
  workspace,
  branch,
  previouslyPublishedBase,
  targetRef,
  register,
}) {
  await git(workspace, "fetch", "origin");
  const remoteTip = await fetchedTarget(workspace, targetRef);
  const preRebaseSha = await revParse(workspace, branch);
  let candidate = preRebaseSha;
  if (remoteTip && remoteTip !== previouslyPublishedBase) {
    await git(
      workspace,
      "rebase",
      "--onto",
      remoteTip,
      previouslyPublishedBase,
      branch,
    );
    candidate = await revParse(workspace, branch);
    if (candidate === preRebaseSha) {
      throw new Error("rebase left the pre-rebase SHA as the candidate");
    }
  }
  await pushExactRef(workspace, candidate, targetRef);
  await git(workspace, "fetch", "origin");
  const origin = (
    await git(workspace, "remote", "get-url", "origin")
  ).stdout.trim();
  const acceptedTip = await lsRemoteSha(origin, targetRef);
  if (acceptedTip !== candidate) {
    throw new Error("remote did not accept the candidate");
  }
  const receipt = { sha: candidate, target: targetRef };
  register?.(receipt);
  return { ok: true, receipt, preRebaseSha };
}
