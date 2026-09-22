// Git mechanics for publishing one Taken claim from an owned workspace.
// Select or reuse the workspace, commit the claim there, publish that SHA,
// then run project-command readiness when required. Implementation runs only
// after the claim is on the remote. Installed guidance is the agent's contract.
import { runReadinessGate } from "./execution-worktree-preparation-readiness-gate.mjs";
import { lsRemoteSha } from "./publication-test-fixtures.mjs";
import {
  claimMembership,
  conflictResult,
  publishClaimSha,
} from "./workspace-publication-push.mjs";
import {
  commitWorkspaceClaim,
  selectOwnedWorkspace,
} from "./workspace-publication-select.mjs";
import {
  isAncestor,
  stopped,
  targetOf,
} from "./workspace-publication-ownership.mjs";

export { commitWorkspaceClaim, publishClaimSha, selectOwnedWorkspace };

async function finishAfterPublication(request) {
  const tip = await lsRemoteSha(
    request.origin,
    `refs/heads/${targetOf(request)}`,
  );
  const present =
    request.publishedSha === tip ||
    (await isAncestor(request.workspace, request.publishedSha, tip));
  if (!present) {
    return stopped("unpublished", {
      publishedSha: request.publishedSha,
      recovery: {
        workspace: request.workspace,
        publishedSha: request.publishedSha,
      },
    });
  }
  if (request.prepareCommands) {
    const readiness = await runReadinessGate(
      request.workspace,
      request.env ?? process.env,
    );
    request.trace?.push("prepared");
    if (!readiness.ok) {
      return stopped("preparation-failed", {
        publishedSha: request.publishedSha,
        workspace: request.workspace,
        branch: request.branch,
        readiness,
        recovery: {
          workspace: request.workspace,
          branch: request.branch,
          publishedSha: request.publishedSha,
          report: readiness.report,
        },
      });
    }
  }
  request.trace?.push("implementation");
  await request.onImplement?.(request);
  return {
    ok: true,
    status: request.status,
    publishedSha: request.publishedSha,
    workspace: request.workspace,
    branch: request.branch,
    created: request.created,
    startingRevision: request.startingRevision,
    implemented: true,
    trace: request.trace,
  };
}

export async function acquireWorkspaceClaim(request) {
  const trace = [];
  const selected = await selectOwnedWorkspace(request);
  if (!selected.ok) return { ...selected, trace };
  trace.push("selected");
  const checked = await claimMembership({
    ...request,
    ...selected,
    candidateSha: request.retained?.candidateSha,
  });
  if (checked.ownership === "owned") {
    trace.push("resumed");
    return finishAfterPublication({
      ...request,
      ...selected,
      status: "resumed",
      publishedSha: checked.provenance.sha,
      trace,
    });
  }
  if (checked.ownership !== "absent") {
    return {
      ...conflictResult(
        { ...request, ...selected },
        checked.ownership,
        checked.provenance,
        request.retained?.candidateSha,
      ),
      trace,
    };
  }
  const committed = await commitWorkspaceClaim({ ...request, ...selected });
  if (!committed.ok) return { ...committed, trace };
  trace.push("committed");
  const published = await publishClaimSha({
    ...request,
    ...selected,
    candidateSha: committed.candidateSha,
  });
  if (!published.ok) return { ...published, trace };
  return finishAfterPublication({
    ...request,
    ...selected,
    ...published,
    trace,
  });
}
