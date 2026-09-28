// Settle one unpublished Trunk closure SHA: resume when already on remote,
// publish through publishExecutionIncrement when the owned tip still holds it,
// or stop with recoverable context. Inspection runs from the owned workspace,
// or from the repository's management context once that workspace is gone,
// against the authorized remote target. Resume orchestration stays in
// closure-publication.mjs.
import { existsSync } from "node:fs";
import { publishExecutionIncrement } from "../../dough-execute-plan/scripts/execution-increment-publication.mjs";
import {
  git,
  originTrackingRef,
  revParse,
} from "../../dough-execute-plan/scripts/publication-git.mjs";
import { resumeInterruptedPublication } from "../../dough-execute-plan/scripts/publication-resume.mjs";
import { findWorktree, isAncestor, trunkTarget } from "./closure-resources.mjs";

function registerClosureReceipt(observer) {
  return (receipt) => {
    observer.register(receipt.sha, receipt.target);
  };
}

function stoppedClosurePublish(published) {
  return {
    classification: "stopped",
    stopped: true,
    completedObligation: "publish",
    pushCount: 0,
    acceptedSha: null,
    preRebaseSha: published.preRebaseSha,
    candidate: published.candidate,
    status: published.status,
    cleanup: "not-performed",
    reason: published.status,
  };
}

async function ownedWorkspaceAvailable(repository, ownedWorkspace) {
  if (repository && (await findWorktree(repository, ownedWorkspace))) {
    return true;
  }
  return existsSync(ownedWorkspace);
}

function stoppedBeforeRemote(reason) {
  return {
    classification: "stopped",
    stopped: true,
    completedObligation: "publish",
    pushCount: 0,
    acceptedSha: null,
    cleanup: "not-performed",
    reason,
  };
}

export async function settleClosureCandidate({
  ownedWorkspace,
  repository,
  defaultCheckout,
  branch,
  sha,
  previouslyPublishedBase,
  supersededShas,
  publishedRevisions,
  observer,
  validate,
  validatedCandidate,
  backlogPath,
  remote = "origin",
  targetRef = trunkTarget,
}) {
  const workspaceReady = await ownedWorkspaceAvailable(
    repository,
    ownedWorkspace,
  );
  if (!workspaceReady && !repository) {
    return stoppedBeforeRemote(
      "execution worktree and management context are both absent",
    );
  }
  const inspectionWorkspace = workspaceReady ? ownedWorkspace : repository;
  await git(inspectionWorkspace, "fetch", remote);
  const tracking = originTrackingRef(targetRef, remote);
  const remoteTip = await revParse(inspectionWorkspace, tracking);
  const onRemote = await isAncestor(inspectionWorkspace, sha, tracking);
  if (!workspaceReady && !onRemote) {
    return stoppedBeforeRemote(
      "execution worktree is absent before closure is on remote trunk",
    );
  }
  const canFastForward =
    onRemote || (await isAncestor(inspectionWorkspace, remoteTip, sha));
  if (canFastForward) {
    return resumeInterruptedPublication({
      ownedWorkspace: inspectionWorkspace,
      defaultCheckout,
      candidateSha: sha,
      supersededShas,
      publishedRevisions,
      observer,
      targetRef,
      remote,
    });
  }
  const tip = await revParse(ownedWorkspace, branch);
  if (tip !== sha) {
    return stoppedBeforeRemote(
      "unpublished closure needs rebase and is not the branch tip",
    );
  }
  const published = await publishExecutionIncrement({
    workspace: ownedWorkspace,
    branch,
    previouslyPublishedBase,
    targetRef,
    remote,
    register: registerClosureReceipt(observer),
    validate,
    validatedCandidate,
    backlogPath,
  });
  if (!published.ok) {
    return stoppedClosurePublish(published);
  }
  if (!publishedRevisions.includes(published.receipt.sha)) {
    publishedRevisions.push(published.receipt.sha);
  }
  return {
    classification: "not-on-remote",
    completedObligation: "publish",
    pushCount: 1,
    acceptedSha: published.receipt.sha,
    preRebaseSha: published.preRebaseSha,
    receipt: published.receipt,
    acceptedPublicationCount: publishedRevisions.length,
    cleanup: "not-performed",
  };
}

export { registerClosureReceipt, trunkTarget };
