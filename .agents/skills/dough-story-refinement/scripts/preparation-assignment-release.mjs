// Ending a preparation assignment with the kept result: stages removal of
// exactly the profile this workspace's own announcement added, beside the
// retained result, so the landing that publishes the result also ends it.
// Rerunning after a rejected or already accepted landing reports what trunk
// shows, and refuses to land a removal that would end a later allocation.
import { existsSync } from "node:fs";
import { join } from "node:path";
import { git } from "../../dough-execute-plan/scripts/publication-git.mjs";
import {
  alreadyReleased,
  assignmentFields,
  errorText,
  noAssignment,
  requestOf,
  stop,
  workspaceAssignment,
} from "./preparation-assignment-ownership.mjs";

async function tracked(workspace, ...args) {
  try {
    await git(workspace, ...args);
    return true;
  } catch {
    return false;
  }
}

// Where the workspace holds `path`: its working tree, index, and HEAD.
async function presence(workspace, path) {
  return {
    worktree: existsSync(join(workspace, path)),
    index: await tracked(workspace, "ls-files", "--error-unmatch", "--", path),
    head: await tracked(workspace, "cat-file", "-e", `HEAD:${path}`),
  };
}

export async function releasePreparation(input) {
  const requested = requestOf("release", input);
  if (!requested.ok) return requested;
  const { request } = requested;
  const { workspace, remote, target } = request;
  const ref = `${remote}/${target}`;
  try {
    await git(workspace, "fetch", "--quiet", remote);
  } catch (error) {
    return stop("source-refused", { workspace, error: errorText(error) });
  }
  const found = await workspaceAssignment(request, ref);
  if (found.state === "ended") {
    const held = await presence(workspace, found.own.path);
    if (found.successor && !(held.worktree && held.index && held.head))
      return stop("release-conflict", {
        ...assignmentFields(request, found.own),
        successor: found.successor,
        workspace,
        error: `${ref} now holds a later allocation (${found.successor}) of ${found.own.path}; landing this workspace's removal of it would end that assignment. Take the removal out of the workspace's unpublished changes and commits before landing`,
      });
    return alreadyReleased(request, found);
  }
  if (found.state !== "held")
    return noAssignment(request, ref, found, "staged");
  const { path } = found.own;
  const held = await presence(workspace, path);
  let staged;
  if (held.index) {
    await git(workspace, "rm", "--quiet", "--", path);
    staged = "staged";
  } else if (held.worktree) {
    return stop("release-conflict", {
      workspace,
      error: `${path} is untracked yet present; inspect it before landing`,
    });
  } else {
    // An earlier release staged the removal, or a landing already committed it.
    staged = held.head ? "already-staged" : "already-committed";
  }
  return {
    ok: true,
    status: "release-staged",
    staged,
    ...assignmentFields(request, found.own),
    workspace,
  };
}
