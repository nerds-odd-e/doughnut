// Explicit abandonment of a preparation: publishes the end of this
// workspace's own assignment as a coordination-only commit on fetched trunk
// that removes exactly its profile. The workspace's HEAD, index, draft and
// commits are untouched, so the result stays recoverable for keep or discard.
// Each attempt rereads trunk first, so a repeated or delayed abandonment
// never ends a later allocation of the same name.
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { agentIdentity } from "../../dough-product-backlog/scripts/product-backlog-agent-profile.mjs";
import { maintenance } from "../../dough-execute-plan/scripts/execution-start-maintenance.mjs";
import {
  exec,
  git,
  revParse,
  tryPushExactRef,
} from "../../dough-execute-plan/scripts/publication-git.mjs";
import {
  alreadyReleased,
  assignmentFields,
  errorText,
  noAssignment,
  requestOf,
  stop,
  workspaceAssignment,
} from "./preparation-assignment-ownership.mjs";

// A commit on `tip` whose only change removes `own`'s profile, built in a
// scratch index so the workspace's own index and files stay as they are.
async function endingCommit(workspace, tip, own, identity) {
  const scratch = mkdtempSync(join(tmpdir(), "dough-abandon-"));
  const { agent, email } = agentIdentity(own.name);
  const env = {
    ...process.env,
    GIT_INDEX_FILE: join(scratch, "index"),
    GIT_AUTHOR_NAME: agent,
    GIT_AUTHOR_EMAIL: email,
  };
  const run = async (...args) =>
    (await exec("git", args, { cwd: workspace, env })).stdout.trim();
  try {
    await run("read-tree", tip);
    await run("update-index", "--force-remove", "--", own.path);
    const tree = await run("write-tree");
    return await run(
      "commit-tree",
      tree,
      "-p",
      tip,
      "-m",
      `End preparation: ${identity}\n\nPreparation-Identity: ${identity}\n`,
    );
  } finally {
    rmSync(scratch, { recursive: true, force: true });
  }
}

// Up to two publication attempts; each is preceded by a fresh read of trunk,
// which also settles whether a push with a lost response was accepted.
const attempts = 2;

export async function abandonPreparation(input) {
  const requested = requestOf("abandon", input);
  if (!requested.ok) return requested;
  const { request } = requested;
  const { workspace, remote, target, identity } = request;
  const ref = `${remote}/${target}`;
  let candidate;
  for (let pass = 0; ; pass += 1) {
    let found;
    try {
      await git(workspace, "fetch", "--quiet", remote);
      found = await workspaceAssignment(request, ref);
    } catch (error) {
      if (candidate === undefined)
        return stop("source-refused", { workspace, error: errorText(error) });
      return stop("unconfirmed", {
        workspace,
        candidateSha: candidate,
        error: `whether trunk accepted the end of the assignment is unknown; rerun abandon: ${errorText(error)}`,
      });
    }
    if (found.state === "ended") {
      if (candidate === undefined || found.endedBy !== candidate)
        return alreadyReleased(request, found);
      return {
        ok: true,
        status: "abandoned",
        ...assignmentFields(request, found.own),
        publishedSha: candidate,
        workspace,
        refresh: await maintenance(request),
      };
    }
    if (found.state !== "held")
      return noAssignment(request, ref, found, "published");
    if (pass === attempts)
      return stop("unpublished", {
        ...assignmentFields(request, found.own),
        workspace,
        candidateSha: candidate,
        error:
          "remote trunk did not accept the end of the assignment; it is still published",
      });
    const tip = await revParse(workspace, ref);
    candidate = await endingCommit(workspace, tip, found.own, identity);
    try {
      await tryPushExactRef(
        workspace,
        candidate,
        remote,
        `refs/heads/${target}`,
      );
    } catch {
      // The response is lost or refused: the next read of trunk decides.
    }
  }
}
