// Git mechanics for Trunk Mode wrap-up closure. Each owned closure commit
// is published through publishExecutionIncrement, or classified through
// resumeInterruptedPublication when it is already the retained candidate.
// The remote execution branch stays unpublished and undeleted. Current-branch
// closure stays in the recorded checkout and follows the caller's publication
// authority through deliverRecordedCheckout. Installed guidance is the agent's
// contract.
import { existsSync } from "node:fs";
import { deliverRecordedCheckout } from "../../dough-execute-plan/scripts/current-branch-publication.mjs";
import { publishExecutionIncrement } from "../../dough-execute-plan/scripts/execution-increment-publication.mjs";
import { refreshDefaultCheckout } from "../../dough-execute-plan/scripts/maintain-default-checkout.mjs";
import {
  git,
  originTrackingRef,
  revParse,
} from "../../dough-execute-plan/scripts/publication-test-fixtures.mjs";
import { resumeInterruptedPublication } from "../../dough-execute-plan/scripts/publication-resume.mjs";
import {
  findWorktree,
  isAncestor,
  removeExecutionResources,
  trunkTarget,
} from "./closure-resources.mjs";

export { removeExecutionResources };

function registerClosureReceipt(observer) {
  return (receipt) => {
    observer.register(receipt.sha, receipt.target);
  };
}

async function refreshAfterAcceptance({
  defaultCheckout,
  declaredOwner,
  requester,
}) {
  return refreshDefaultCheckout({
    checkout: defaultCheckout,
    declaredOwner,
    requester,
    integrationBranch: "main",
  });
}

export async function publishTrunkClosureRevision({
  workspace,
  branch,
  previouslyPublishedBase,
  observer,
  defaultCheckout,
  declaredOwner,
  requester,
}) {
  const published = await publishExecutionIncrement({
    workspace,
    branch,
    previouslyPublishedBase,
    targetRef: trunkTarget,
    register: registerClosureReceipt(observer),
  });
  const maintenance = await refreshAfterAcceptance({
    defaultCheckout,
    declaredOwner,
    requester,
  });
  return {
    ok: true,
    receipt: published.receipt,
    preRebaseSha: published.preRebaseSha,
    maintenance,
  };
}

async function ownedWorkspaceAvailable(integration, ownedWorkspace) {
  if (await findWorktree(integration, ownedWorkspace)) {
    return true;
  }
  return existsSync(ownedWorkspace);
}

async function settleClosureCandidate({
  ownedWorkspace,
  integration,
  defaultCheckout,
  branch,
  sha,
  previouslyPublishedBase,
  supersededShas,
  publishedRevisions,
  observer,
}) {
  const workspaceReady = await ownedWorkspaceAvailable(
    integration,
    ownedWorkspace,
  );
  const inspectionWorkspace = workspaceReady ? ownedWorkspace : integration;
  await git(inspectionWorkspace, "fetch", "origin");
  const tracking = originTrackingRef(trunkTarget);
  const remoteTip = await revParse(inspectionWorkspace, tracking);
  const onRemote = await isAncestor(inspectionWorkspace, sha, tracking);
  if (!workspaceReady && !onRemote) {
    return {
      classification: "stopped",
      stopped: true,
      completedObligation: "publish",
      pushCount: 0,
      acceptedSha: null,
      cleanup: "not-performed",
      reason: "execution worktree is absent before closure is on remote trunk",
    };
  }
  const canFastForward =
    onRemote || (await isAncestor(inspectionWorkspace, remoteTip, sha));
  if (canFastForward) {
    return resumeInterruptedPublication({
      ownedWorkspace: workspaceReady ? ownedWorkspace : integration,
      defaultCheckout,
      candidateSha: sha,
      supersededShas,
      publishedRevisions,
      observer,
      targetRef: trunkTarget,
    });
  }
  const tip = await revParse(ownedWorkspace, branch);
  if (tip !== sha) {
    return {
      classification: "stopped",
      stopped: true,
      completedObligation: "publish",
      pushCount: 0,
      acceptedSha: null,
      cleanup: "not-performed",
      reason: "unpublished closure needs rebase and is not the branch tip",
    };
  }
  const published = await publishExecutionIncrement({
    workspace: ownedWorkspace,
    branch,
    previouslyPublishedBase,
    targetRef: trunkTarget,
    register: registerClosureReceipt(observer),
  });
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

async function finishObligation(result, input) {
  if (result.stopped === true) {
    return { ...result, cleanup: "not-performed" };
  }
  const refresh = await refreshAfterAcceptance(input);
  return { ...result, refresh, cleanup: "not-performed" };
}

export async function resumeTrunkClosure(input) {
  const beforeResult = await settleClosureCandidate({
    ...input,
    sha: input.beforeCleanupSha,
    previouslyPublishedBase: input.previouslyPublishedBase,
  });
  if (beforeResult.completedObligation !== "none") {
    return finishObligation(beforeResult, input);
  }
  if (!input.finalClosureSha) {
    return {
      completedObligation: "final-closure-not-committed",
      pushCount: 0,
      cleanup: "not-performed",
      acceptedSha: beforeResult.acceptedSha,
    };
  }
  const finalResult = await settleClosureCandidate({
    ...input,
    sha: input.finalClosureSha,
    previouslyPublishedBase: input.beforeCleanupSha,
  });
  if (finalResult.completedObligation !== "none") {
    return finishObligation(finalResult, input);
  }
  if (!input.observer.stopped) {
    input.observer.stop();
    return {
      completedObligation: "stop-observer",
      pushCount: 0,
      cleanup: "not-performed",
      acceptedSha: input.finalClosureSha,
    };
  }
  const cleanup = await removeExecutionResources({
    integration: input.integration,
    execution: input.ownedWorkspace,
    branch: input.branch,
    observer: input.observer,
    sessionOwned: input.sessionOwned,
    closureShas: [input.beforeCleanupSha, input.finalClosureSha],
  });
  return {
    completedObligation: "remove-resources",
    pushCount: 0,
    cleanup,
    acceptedSha: input.finalClosureSha,
  };
}

export async function deliverCurrentBranchClosure(request) {
  return deliverRecordedCheckout(request);
}
