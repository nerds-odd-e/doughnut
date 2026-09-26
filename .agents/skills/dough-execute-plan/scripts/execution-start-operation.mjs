// Authoritative queued-start orchestration. The CLI adapter stays in execution-start.mjs.
import { git, lsRemoteSha, revParse } from "./publication-git.mjs";
import {
  maintenance,
  reportedMaintenance,
} from "./execution-start-maintenance.mjs";
import { readPublishedExecutionSource } from "./execution-source.mjs";
import {
  recovery,
  retainedCandidate,
  sameSelectedSource,
} from "./execution-start-recovery.mjs";
import { acceptedReceipt } from "./execution-start-receipt.mjs";
import { agentIdentity } from "../../dough-product-backlog/scripts/product-backlog-agent-profile.mjs";
import { startRequest } from "./execution-start-request.mjs";
import {
  receiptAgent,
  reselectClaimAgent,
  resumeClaimAgent,
  selectClaimAgent,
} from "./execution-start-agent.mjs";
import {
  commitWorkspaceClaim,
  selectOwnedWorkspace,
} from "./workspace-publication-select.mjs";
import {
  claimMembership,
  publishClaimSha,
  publishStoryBranch,
} from "./workspace-publication-push.mjs";
import {
  backlogPath,
  claimProvenance,
  isAncestor,
  remoteOf,
  stopped,
} from "./workspace-publication-ownership.mjs";

export async function startQueuedExecution(requestInput) {
  const started = startRequest(requestInput);
  if (!started.ok) return started;
  const { request } = started;
  const remote = remoteOf(request);
  const ref = `${remote}/${request.target}`;
  let selectedSource, fetched, origin;
  try {
    origin = (
      await git(request.integration, "remote", "get-url", remote)
    ).stdout.trim();
    await git(request.integration, "fetch", remote);
    fetched = await revParse(request.integration, ref);
    selectedSource = await readPublishedExecutionSource(request, ref);
    if (request.retained) {
      const original = await readPublishedExecutionSource(
        request,
        request.retained.startingRevision,
      );
      if (!sameSelectedSource(original, selectedSource))
        throw new Error(
          "selected published source changed since retained claim basis",
        );
    }
  } catch (error) {
    return stopped("source-refused", { error: error.stderr || error.message });
  }
  let agent;
  if (!request.retained) {
    const chosen = await selectClaimAgent(request, ref, backlogPath, {
      fetched,
    });
    if (!chosen.ok) return chosen;
    agent = chosen.agent;
  }
  const beforeMaintenance = await maintenance(request);
  // Stops report only this compact local outcome; acceptance reports both.
  const stopMaintenance = reportedMaintenance(beforeMaintenance);
  const selected = await selectOwnedWorkspace({ ...request, origin });
  if (!selected.ok) return { ...selected, fetched, ...stopMaintenance };
  // Trunk can move between the source fetch and the workspace's base; the
  // claim names the rotation's next agent on the trunk it is built on.
  if (agent && selected.startingRevision !== fetched) {
    const { startingRevision: base, workspace, branch } = selected;
    const stop = { fetched, workspace, branch, ...stopMaintenance };
    const chosen = await selectClaimAgent(request, base, backlogPath, stop);
    if (!chosen.ok) return chosen;
    agent = chosen.agent;
  }
  const claimRequest = {
    ...request,
    ...selected,
    origin,
    plan: selectedSource.planTarget,
    backlogPath,
    candidateSha: request.retained?.candidateSha,
  };
  let checked;
  try {
    checked = await claimMembership(claimRequest);
    if (checked.ownership === "owned" && request.retained)
      await publishStoryBranch(claimRequest, checked.provenance.sha);
  } catch (error) {
    return stopped("unpublished", {
      workspace: selected.workspace,
      branch: selected.branch,
      ...stopMaintenance,
      error: error.stderr || error.message,
    });
  }
  if (checked.ownership === "owned" && request.retained) {
    const afterMaintenance = await maintenance(request);
    return acceptedReceipt(
      request,
      selected,
      selectedSource,
      {
        status: "resumed",
        publishedSha: checked.provenance.sha,
        candidateSha: request.retained.candidateSha,
        created: false,
        ...(await receiptAgent(
          selected.workspace,
          await resumeClaimAgent(
            selected.workspace,
            checked.provenance.sha,
            request.identity,
            backlogPath,
          ),
        )),
      },
      beforeMaintenance,
      afterMaintenance,
    );
  }
  if (checked.ownership !== "absent") {
    return stopped("conflict", {
      ownership: checked.ownership,
      workspace: selected.workspace,
      branch: selected.branch,
      ...stopMaintenance,
      provenance: checked.provenance,
      recovery: {
        ...recovery(request, selected, request.retained?.candidateSha),
        provenance: checked.provenance,
      },
    });
  }
  let committed;
  try {
    committed = request.retained
      ? await retainedCandidate(request, selected)
      : await commitWorkspaceClaim({ ...claimRequest, agent });
  } catch (error) {
    return stopped("claim-failed", {
      workspace: selected.workspace,
      branch: selected.branch,
      ...stopMaintenance,
      error: error.stderr || error.message,
    });
  }
  if (!committed.ok) return { ...committed, ...stopMaintenance };
  const published = await publishClaimSha({
    ...claimRequest,
    candidateSha: committed.candidateSha,
    async recheckSource() {
      await git(request.integration, "fetch", remote);
      const refreshed = await readPublishedExecutionSource(request, ref);
      if (!sameSelectedSource(refreshed, selectedSource)) {
        throw new Error(
          "selected published source changed during claim publication",
        );
      }
    },
    reselectClaim:
      agent &&
      reselectClaimAgent(claimRequest, agent, (next) => {
        agent = next;
      }),
  });
  if (!published.ok)
    return {
      ...published,
      ...stopMaintenance,
      recovery: {
        ...recovery(
          request,
          selected,
          published.recovery?.candidateSha ?? committed.candidateSha,
        ),
        ...published.recovery,
      },
    };
  // Independent remote acceptance: containment plus current provenance.
  try {
    await git(selected.workspace, "fetch", remote);
    const remoteTip = await lsRemoteSha(origin, `refs/heads/${request.target}`);
    const contained = await isAncestor(
      selected.workspace,
      published.publishedSha,
      remoteTip,
    );
    const provenance = await claimProvenance(
      selected.workspace,
      ref,
      request.identity,
      backlogPath,
    );
    if (
      !contained ||
      provenance?.publisher !== request.publisherId ||
      provenance?.identity !== request.identity
    ) {
      return stopped("unpublished", {
        workspace: selected.workspace,
        candidateSha: published.candidateSha,
        recovery: recovery(request, selected, published.candidateSha),
        ...stopMaintenance,
        error: "remote containment or claim ownership is unconfirmed",
      });
    }
    await publishStoryBranch(claimRequest, published.publishedSha);
  } catch (error) {
    return stopped("unpublished", {
      workspace: selected.workspace,
      candidateSha: published.candidateSha,
      recovery: recovery(request, selected, published.candidateSha),
      ...stopMaintenance,
      error: error.stderr || error.message,
    });
  }
  const afterMaintenance = await maintenance(request);
  return acceptedReceipt(
    request,
    selected,
    selectedSource,
    {
      ...published,
      created: selected.created,
      ...(await receiptAgent(
        selected.workspace,
        agent
          ? agentIdentity(agent.name).agent
          : await resumeClaimAgent(
              selected.workspace,
              published.publishedSha,
              request.identity,
              backlogPath,
            ),
      )),
    },
    beforeMaintenance,
    afterMaintenance,
  );
}
