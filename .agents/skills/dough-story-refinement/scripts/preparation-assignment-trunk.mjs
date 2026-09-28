// Fetched remote trunk as a queued story's preparation starting point: the
// story must be queued there, and a workspace that does not exist yet is
// created at it, never at the integration checkout's local revision.
import { existsSync } from "node:fs";
import { queueHeading } from "../../dough-product-backlog/scripts/product-backlog-document.mjs";
import {
  git,
  revParse,
} from "../../dough-execute-plan/scripts/publication-git.mjs";
import { remoteRef } from "../../dough-execute-plan/scripts/workspace-publication-ownership.mjs";
import { selectOwnedWorkspace } from "../../dough-execute-plan/scripts/workspace-publication-select.mjs";
import {
  errorText,
  stop,
  storyListAt,
} from "./preparation-assignment-ownership.mjs";

// Fetches the target from `cwd` and returns its revision when the story is
// queued there, or the stop that refuses the request.
export async function fetchQueuedTrunk(cwd, request) {
  const { workspace, remote, identity } = request;
  const ref = remoteRef(request);
  let fetched;
  try {
    await git(cwd, "fetch", "--quiet", remote);
    fetched = await revParse(cwd, ref);
  } catch (error) {
    return stop("source-refused", { workspace, error: errorText(error) });
  }
  if ((await storyListAt(cwd, ref, identity)) !== queueHeading)
    return stop("not-queued", {
      workspace,
      fetched,
      error: `${identity} is not queued on ${ref}`,
    });
  return { ok: true, fetched };
}

// Creates the owned workspace on its branch at queued fetched trunk when its
// path does not exist yet, so a refused request leaves nothing behind. The
// integration checkout supplies the repository to create it from. An
// existing path is the caller's owned workspace, verified by the announcement.
export async function selectPreparationWorkspace(request) {
  const { integration, workspace, branch } = request;
  if (existsSync(workspace)) return { ok: true };
  if (!branch || !integration)
    return stop("invalid-request", {
      workspace,
      error:
        "a workspace that does not exist yet needs --branch and --integration to create it",
    });
  const trunk = await fetchQueuedTrunk(integration, request);
  if (!trunk.ok) return trunk;
  const selected = await selectOwnedWorkspace({
    ...request,
    repository: integration,
    base: trunk.fetched,
  });
  if (!selected.ok)
    return stop("workspace-selection-failed", {
      workspace,
      branch,
      error: selected.recovery.error,
    });
  return {
    ok: true,
    selection: {
      created: true,
      branch: selected.branch,
      startingRevision: selected.startingRevision,
    },
  };
}
