// The installed start operation reports remote publication and local maintenance separately.
import { remoteOf } from "./workspace-publication-ownership.mjs";

export function acceptedReceipt(
  request,
  selected,
  source,
  fetched,
  publication,
  beforeMaintenance,
  afterMaintenance,
) {
  return {
    ok: true,
    status: publication.status,
    mode: request.mode,
    identity: request.identity,
    publisherId: request.publisherId,
    remote: remoteOf(request),
    target: `refs/heads/${request.target}`,
    fetched,
    publishedSha: publication.publishedSha,
    candidateSha: publication.candidateSha,
    workspace: selected.workspace,
    branch: selected.branch,
    startingRevision: selected.startingRevision,
    created: publication.created ?? selected.created,
    plan: source.planTarget,
    preparation: "ready",
    beforeMaintenance,
    afterMaintenance,
    projectSetupRequired: true,
  };
}
