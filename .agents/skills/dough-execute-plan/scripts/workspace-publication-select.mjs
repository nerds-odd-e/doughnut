// Select or reuse the owned execution workspace, then commit the Taken claim
// there. Publication of that SHA is a separate step.
import { dirname, join } from "node:path";
import { existsSync } from "node:fs";
import { applyToBacklog } from "../../dough-product-backlog/scripts/product-backlog-store.mjs";
import { takeEntry } from "../../dough-product-backlog/scripts/product-backlog-take.mjs";
import { git, revParse } from "./publication-git.mjs";
import {
  claimCommitMessage,
  isAncestor,
  pathOf,
  remoteOf,
  stopped,
  targetOf,
  trailers,
} from "./workspace-publication-ownership.mjs";

async function verifyRetained(request) {
  const { retained } = request;
  const recovery = {
    workspace: retained.workspace,
    branch: retained.branch,
    startingRevision: retained.startingRevision,
  };
  try {
    const branch = (
      await git(retained.workspace, "rev-parse", "--abbrev-ref", "HEAD")
    ).stdout.trim();
    if (branch !== retained.branch) {
      return stopped("setup-failed", {
        recovery: { ...recovery, error: `HEAD is ${branch}` },
      });
    }
    const matches = await isAncestor(
      retained.workspace,
      retained.startingRevision,
      "HEAD",
    );
    const head = await revParse(retained.workspace, "HEAD");
    const message = (await git(retained.workspace, "log", "-1", "--format=%B"))
      .stdout;
    const owned = trailers(message);
    // A successful semantic replay rewrites the candidate onto newer trunk.
    // In that case its original base need not remain an ancestor of HEAD.
    const replayedCandidate =
      retained.candidateSha === head &&
      owned.publisher === request.publisherId &&
      owned.identity === request.identity;
    if (!matches && !replayedCandidate) {
      return stopped("setup-failed", {
        recovery: {
          ...recovery,
          error: "starting revision is not contained in HEAD",
        },
      });
    }
    return {
      ok: true,
      created: false,
      workspace: retained.workspace,
      branch,
      startingRevision: retained.startingRevision,
    };
  } catch (error) {
    return stopped("setup-failed", {
      recovery: { ...recovery, error: error.stderr || error.message },
    });
  }
}

export async function selectOwnedWorkspace(request) {
  if (request.retained?.workspace) return verifyRetained(request);
  const remote = remoteOf(request);
  const target = targetOf(request);
  try {
    await git(request.integration, "fetch", remote);
    const base = await revParse(request.integration, `${remote}/${target}`);
    if (existsSync(request.workspace)) {
      const actual = await revParse(request.workspace, "--show-toplevel");
      const branch = (
        await git(request.workspace, "branch", "--show-current")
      ).stdout.trim();
      const head = await revParse(request.workspace, "HEAD");
      const status = (await git(request.workspace, "status", "--porcelain"))
        .stdout;
      if (
        actual !== request.workspace ||
        branch !== request.branch ||
        head !== base ||
        status !== ""
      ) {
        return stopped("setup-failed", {
          recovery: {
            workspace: request.workspace,
            branch: request.branch,
            error:
              "existing workspace does not match clean fetched trunk and owned branch",
          },
        });
      }
      return {
        ok: true,
        created: false,
        workspace: request.workspace,
        branch,
        startingRevision: base,
      };
    }
    await git(
      request.integration,
      "worktree",
      "add",
      "-b",
      request.branch,
      request.workspace,
      base,
    );
    return {
      ok: true,
      created: true,
      workspace: request.workspace,
      branch: request.branch,
      startingRevision: base,
    };
  } catch (error) {
    return stopped("setup-failed", {
      recovery: {
        workspace: request.workspace,
        branch: request.branch,
        error: `${request.workspace}: ${error.stderr || error.message}`,
      },
    });
  }
}

export async function commitWorkspaceClaim(request) {
  const { workspace, identity, publisherId, startingRevision } = request;
  const file = pathOf(request);
  const message = (await git(workspace, "log", "-1", "--format=%B")).stdout;
  const owned = trailers(message);
  const head = await revParse(workspace, "HEAD");
  if (
    head !== startingRevision &&
    owned.publisher === publisherId &&
    owned.identity === identity
  ) {
    return { ok: true, candidateSha: head, committed: false };
  }
  if (
    head !== startingRevision ||
    (await git(workspace, "status", "--porcelain")).stdout !== ""
  ) {
    return stopped("setup-failed", {
      recovery: {
        workspace,
        branch: request.branch,
        error: "claim workspace has unpublished commits or pending changes",
      },
    });
  }
  let outcome;
  await applyToBacklog(join(workspace, file), (source) => {
    outcome = takeEntry(source, {
      identity,
      ...(request.plan === undefined ? {} : { plan: request.plan }),
      backlogDirectory: dirname(join(workspace, file)),
    });
    return outcome.source;
  });
  if (outcome.result === "unchanged") {
    return stopped("unchanged", { workspace, branch: request.branch });
  }
  await git(workspace, "add", "--", file);
  await git(
    workspace,
    "commit",
    "-m",
    claimCommitMessage(identity, publisherId),
  );
  return {
    ok: true,
    candidateSha: await revParse(workspace, "HEAD"),
    committed: true,
  };
}
