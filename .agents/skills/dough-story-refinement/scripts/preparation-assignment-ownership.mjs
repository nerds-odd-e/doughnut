// Which preparation assignment a workspace owns, and what became of it. An
// assignment is identified by its profile path plus the commit that added it
// (its allocation), never by story identity, timestamps, or tool/model alone.
// The workspace remembers its own announcement commit under a per-worktree
// ref, recorded before the announcement is pushed. Trunk history alone cannot
// tell a workspace's own allocation from a later one of the same name that a
// fast-forward brought into the workspace, so only that record counts.
import { dirname, join, resolve } from "node:path";
import {
  agentIdentity,
  agentReportError,
} from "../../dough-product-backlog/scripts/product-backlog-agent-profile.mjs";
import {
  addedProfile,
  profileAllocation,
} from "../../dough-execute-plan/scripts/execution-start-agent.mjs";
import { git } from "../../dough-execute-plan/scripts/publication-git.mjs";
import {
  backlogPath,
  fileAt,
  isAncestor,
} from "../../dough-execute-plan/scripts/workspace-publication-ownership.mjs";

export function stop(status, fields) {
  return { ok: false, status, ...fields };
}

export const errorText = (error) => error.stderr || error.message;

// Repository path of a rotation name's profile beside the backlog.
export const profilePathOf = (name) =>
  join(dirname(backlogPath), agentIdentity(name).path);

// Operations that publish to trunk need authority and a separate workspace.
const publishing = new Set(["start", "abandon"]);

// The validated request for an operation, or a stop saying why not.
export function requestOf(operation, input) {
  const required = ["workspace", "identity", "target"];
  if (publishing.has(operation)) required.push("integration");
  for (const field of required)
    if (!input[field])
      return stop("invalid-request", { error: `missing ${field}` });
  const request = {
    ...input,
    remote: input.remote ?? "origin",
    workspace: resolve(input.workspace),
    ...(input.integration ? { integration: resolve(input.integration) } : {}),
  };
  const reportError = agentReportError(request);
  if (reportError) return stop("invalid-request", { error: reportError });
  if (publishing.has(operation)) {
    if (request.pushAuthorized !== true)
      return stop("authority-required", {
        error: "trunk publication authority must be established",
      });
    if (request.integration === request.workspace)
      return stop("invalid-request", {
        error: "preparation requires a separate owned workspace",
      });
  }
  return { ok: true, request };
}

const recordRef = "refs/worktree/dough/preparation-assignment";

// Remembers `sha` as this workspace's own announcement commit.
export async function recordAllocation(workspace, sha) {
  await git(workspace, "update-ref", recordRef, sha);
}

// Puts back the record a workspace held before (`sha`), or none.
export async function restoreAllocation(workspace, sha) {
  if (sha) await recordAllocation(workspace, sha);
  else await git(workspace, "update-ref", "-d", recordRef);
}

export async function recordedAllocation(workspace) {
  try {
    const { stdout } = await git(
      workspace,
      "rev-parse",
      "--verify",
      "-q",
      `${recordRef}^{commit}`,
    );
    return stdout.trim();
  } catch {
    return undefined;
  }
}

// The preparation profile the announcement commit `sha` added, as an
// assignment: its name, path, allocation, and recorded facts.
async function announcedAssignment(workspace, sha) {
  const added = await addedProfile(
    workspace,
    sha,
    backlogPath,
    (profile, name) =>
      profile.activity === "preparation" && profile.name === name,
  );
  return added && { ...added, allocation: sha };
}

// What became of the assignment this workspace announced, read from the
// fetched trunk `ref`:
// - `held`: trunk still records that exact allocation;
// - `ended`: trunk history removed it, naming the removing commit and any
//   later allocation of the same name, which is never this workspace's;
// - `unconfirmed`: trunk never took the recorded announcement;
// - `none`: this workspace recorded no announcement for `identity`.
// `assigned` names a still-held assignment the workspace records for another
// story than the request names.
export async function workspaceAssignment(request, ref) {
  const { workspace, identity } = request;
  const sha = await recordedAllocation(workspace);
  const own = sha && (await announcedAssignment(workspace, sha));
  if (!own) return { state: "none" };
  const named = own.profile.identity === identity;
  if (!(await isAncestor(workspace, sha, ref)))
    return named ? { state: "unconfirmed", own } : { state: "none" };
  const current =
    (await fileAt(workspace, ref, own.path)) === null
      ? undefined
      : await profileAllocation(workspace, ref, own.path);
  if (!named)
    return current === sha
      ? { state: "none", assigned: own }
      : { state: "none" };
  if (current === sha) return { state: "held", own };
  const { stdout } = await git(
    workspace,
    "log",
    "--reverse",
    "--diff-filter=D",
    "--format=%H",
    `${sha}..${ref}`,
    "--",
    own.path,
  );
  return {
    state: "ended",
    own,
    endedBy: stdout.split("\n")[0] || undefined,
    ...(current === undefined ? {} : { successor: current }),
  };
}

export function assignmentFields(request, { name, path, allocation, profile }) {
  return {
    activity: "preparation",
    identity: request.identity,
    agent: agentIdentity(name).agent,
    profile: path,
    allocation,
    ...(profile.host === undefined ? {} : { host: profile.host }),
    ...(profile.model === undefined ? {} : { model: profile.model }),
  };
}

// The stop for a workspace with no assignment of the identity to act on.
export function noAssignment(request, ref, found, action) {
  const reason =
    found.state === "unconfirmed"
      ? `its announcement ${found.own.allocation} never reached ${ref}`
      : `${ref} holds no preparation assignment of ${request.identity} announced from this workspace`;
  return stop("no-assignment", {
    workspace: request.workspace,
    error: `${reason}; nothing was ${action}`,
  });
}

// The receipt for an assignment trunk already ended: nothing more to do.
export function alreadyReleased(request, found) {
  return {
    ok: true,
    status: "already-released",
    ...assignmentFields(request, found.own),
    endedBy: found.endedBy,
    ...(found.successor ? { successor: found.successor } : {}),
    workspace: request.workspace,
  };
}
