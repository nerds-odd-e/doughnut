// Authoritative queued-start orchestration. The CLI adapter stays in execution-start.mjs.
import { resolve } from "node:path";
import { git, lsRemoteSha, revParse } from "./publication-git.mjs";
import { maintenance } from "./execution-start-maintenance.mjs";
import { readPublishedExecutionSource } from "./execution-source.mjs";
import {
  recovery,
  retainedCandidate,
  sameSelectedSource,
} from "./execution-start-recovery.mjs";
import { acceptedReceipt } from "./execution-start-receipt.mjs";
import {
  commitWorkspaceClaim,
  selectOwnedWorkspace,
} from "./workspace-publication-select.mjs";
import {
  claimMembership,
  publishClaimSha,
} from "./workspace-publication-push.mjs";
import {
  backlogPath,
  claimProvenance,
  isAncestor,
  remoteOf,
  stopped,
} from "./workspace-publication-ownership.mjs";

export async function startQueuedExecution(requestInput) {
  const required = [
    "integration",
    "workspace",
    "branch",
    "identity",
    "publisherId",
    "mode",
    "target",
  ];
  for (const field of required)
    if (!requestInput[field])
      return stopped("invalid-request", { error: `missing ${field}` });
  const request = {
    ...requestInput,
    integration: resolve(requestInput.integration),
    workspace: resolve(requestInput.workspace),
  };
  if (request.startingRevision || request.candidateSha) {
    if (!request.startingRevision || !request.candidateSha)
      return stopped("invalid-request", {
        error: "resume requires starting revision and candidate SHA",
      });
    request.retained = {
      workspace: request.workspace,
      branch: request.branch,
      startingRevision: request.startingRevision,
      candidateSha: request.candidateSha,
    };
  }
  if (
    !["trunk", "story-branch"].includes(request.mode) ||
    request.pushAuthorized !== true ||
    request.workspaceAuthorized !== true
  ) {
    return stopped("authority-required", {
      error:
        "mode, workspace and trunk publication authority must be established",
    });
  }
  if (resolve(request.integration) === resolve(request.workspace))
    return stopped("invalid-request", {
      error: "queued work requires a separate owned workspace",
    });
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
  const beforeMaintenance = await maintenance(request);
  const selected = await selectOwnedWorkspace({ ...request, origin });
  if (!selected.ok) return { ...selected, fetched, beforeMaintenance };
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
  } catch (error) {
    return stopped("unpublished", {
      workspace: selected.workspace,
      branch: selected.branch,
      beforeMaintenance,
      error: error.stderr || error.message,
    });
  }
  if (checked.ownership === "owned" && request.retained) {
    const afterMaintenance = await maintenance(request);
    return acceptedReceipt(
      request,
      selected,
      selectedSource,
      fetched,
      {
        status: "resumed",
        publishedSha: checked.provenance.sha,
        candidateSha: request.retained.candidateSha,
        created: false,
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
      beforeMaintenance,
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
      : await commitWorkspaceClaim(claimRequest);
  } catch (error) {
    return stopped("claim-failed", {
      workspace: selected.workspace,
      branch: selected.branch,
      beforeMaintenance,
      error: error.stderr || error.message,
    });
  }
  if (!committed.ok) return { ...committed, beforeMaintenance };
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
  });
  if (!published.ok)
    return {
      ...published,
      beforeMaintenance,
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
        beforeMaintenance,
        error: "remote containment or claim ownership is unconfirmed",
      });
    }
  } catch (error) {
    return stopped("unpublished", {
      workspace: selected.workspace,
      candidateSha: published.candidateSha,
      recovery: recovery(request, selected, published.candidateSha),
      beforeMaintenance,
      error: error.stderr || error.message,
    });
  }
  const afterMaintenance = await maintenance(request);
  return acceptedReceipt(
    request,
    selected,
    selectedSource,
    fetched,
    {
      ...published,
      created: selected.created,
    },
    beforeMaintenance,
    afterMaintenance,
  );
}
