// Select or reuse the owned execution workspace for a Take. Committing the
// claim there is workspace-publication-claim.mjs; publication of that SHA is a
// separate step.
import { existsSync } from "node:fs";
import { fastForwardToFetchedTrunk } from "./maintain-default-checkout.mjs";
import { git, revParse } from "./publication-git.mjs";
import {
  createdForRef,
  isAncestor,
  remoteOf,
  remoteRef,
  stopped,
  trailers,
} from "./workspace-publication-ownership.mjs";

const continuable = new Set(["advanced", "already current"]);

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

// The ref recording that a workspace was created for `identity`, or undefined
// when the request names no identity or Git rejects it as a ref name.
async function creationRecord(repository, identity) {
  if (!identity) return undefined;
  const ref = createdForRef(identity);
  try {
    await git(repository, "check-ref-format", ref);
    return ref;
  } catch (error) {
    if (error.code === 1) return undefined;
    throw error;
  }
}

// A supplied `base` is fetched trunk the caller already reset the workspace
// to, such as a carried escalation's park; it is used without fetching again.
// `repository` is the Git context for fetching and creating the workspace; it
// is required so that Git never falls back to the process working directory.
export async function selectOwnedWorkspace(request) {
  if (request.retained?.workspace) return verifyRetained(request);
  if (!request.repository)
    return stopped("setup-failed", {
      recovery: {
        workspace: request.workspace,
        branch: request.branch,
        error: "workspace selection needs a repository",
      },
    });
  try {
    let { base } = request;
    if (!base) {
      await git(request.repository, "fetch", remoteOf(request));
      base = await revParse(request.repository, remoteRef(request));
    }
    if (existsSync(request.workspace)) {
      const actual = await revParse(request.workspace, "--show-toplevel");
      // A reused workspace continues on fetched trunk only through refresh's
      // fast-forward eligibility; its own commits, edits, and any ongoing Git
      // operation stay as they are.
      const reused =
        actual === request.workspace
          ? await fastForwardToFetchedTrunk(
              request.workspace,
              base,
              request.branch,
            )
          : { reason: "not-the-workspace-toplevel" };
      if (!continuable.has(reused.result)) {
        return stopped("setup-failed", {
          recovery: {
            workspace: request.workspace,
            branch: request.branch,
            error: `existing workspace cannot continue on fetched trunk as ${request.branch}: ${reused.reason}`,
          },
        });
      }
      return {
        ok: true,
        created: false,
        workspace: request.workspace,
        branch: request.branch,
        startingRevision: base,
      };
    }
    // A created workspace records the work it was created for, so a later
    // session can tell it from one that work only reused.
    const record = await creationRecord(request.repository, request.identity);
    await git(
      request.repository,
      "worktree",
      "add",
      "-b",
      request.branch,
      request.workspace,
      base,
    );
    if (record) await git(request.workspace, "update-ref", record, base);
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
