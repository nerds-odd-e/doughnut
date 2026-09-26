// The agent a Take names: chosen from profiles held on the trunk its claim is
// built on, chosen again when a rival publishes the same name first, and read
// back from the claim commit when an existing claim resumes. Preparation
// assignments share the same rotation and allocation provenance.
import { basename, dirname, join } from "node:path";
import {
  agentIdentity,
  agentProfileDirectory,
  parseAgentProfile,
  profileAgentName,
  selectAgentName,
} from "../../dough-product-backlog/scripts/product-backlog-agent-profile.mjs";
import { git, revParse } from "./publication-git.mjs";
import { commitWorkspaceClaim } from "./workspace-publication-select.mjs";
import {
  configureAgentAuthorship,
  workspaceAuthorship,
} from "./workspace-agent-authorship.mjs";
import { stopped } from "./workspace-publication-ownership.mjs";

// Every path a Git command lists under the profile directory beside the
// backlog, in listed order, whether or not it names a rotation agent.
async function listedPaths(cwd, backlogPath, ...args) {
  const directory = join(dirname(backlogPath), agentProfileDirectory);
  const { stdout } = await git(cwd, ...args, "--", `${directory}/`);
  return stdout.split("\n").filter(Boolean);
}

// Agent profiles (path and agent name) a Git command lists under the profile
// directory beside the backlog, in listed order.
async function listedProfiles(cwd, backlogPath, ...args) {
  const paths = await listedPaths(cwd, backlogPath, ...args);
  return paths
    .map((path) => ({ path, name: profileAgentName(basename(path)) }))
    .filter(({ name }) => name);
}

async function listedAgentNames(cwd, backlogPath, ...args) {
  const profiles = await listedProfiles(cwd, backlogPath, ...args);
  return profiles.map(({ name }) => name);
}

// Agent names whose profiles exist beside the backlog at `rev`.
function heldAgentNames(cwd, rev, backlogPath) {
  return listedAgentNames(cwd, backlogPath, "ls-tree", "--name-only", rev);
}

// The agent name whose profile was most recently added beside the backlog in
// `rev`'s first-parent history, released or not; undefined when none ever was.
async function mostRecentAgentName(cwd, rev, backlogPath) {
  const added = await listedAgentNames(
    cwd,
    backlogPath,
    "log",
    "--first-parent",
    "--diff-filter=A",
    "--name-only",
    "--format=",
    rev,
  );
  return added[0];
}

// The rotation's next name at `rev`, or undefined when every name is held.
// Every profile file occupies its name, whatever activity it records.
async function nextAgentName(cwd, rev, backlogPath) {
  const [mostRecent, held] = await Promise.all([
    mostRecentAgentName(cwd, rev, backlogPath),
    heldAgentNames(cwd, rev, backlogPath),
  ]);
  return { name: selectAgentName(mostRecent, held), held };
}

// The allocation a published profile records: the commit that most recently
// added the file at `path` in `rev`'s history. A later reuse of the same name
// is a different allocation even when its text is identical. Undefined when
// `rev` never added it.
export async function profileAllocation(cwd, rev, path) {
  const { stdout } = await git(
    cwd,
    "log",
    "-1",
    "--diff-filter=A",
    "--format=%H",
    rev,
    "--",
    path,
  );
  return stdout.trim() || undefined;
}

// Every file in the profile directory at `rev`, for diagnosing a full
// rotation: each held name's recorded work and allocation, and any file that
// is not a readable profile. Nothing here is released or reclaimed; age and
// absence of a local process are not reasons to free a name.
export async function occupiedAssignments(cwd, rev, backlogPath) {
  const paths = await listedPaths(
    cwd,
    backlogPath,
    "ls-tree",
    "--name-only",
    rev,
  );
  return Promise.all(
    paths.map(async (path) => {
      const allocation = await profileAllocation(cwd, rev, path);
      const text = (await git(cwd, "cat-file", "-p", `${rev}:${path}`)).stdout;
      const read = parseAgentProfile(text);
      if (!read.ok || profileAgentName(basename(path)) !== read.profile.name)
        return {
          path,
          allocation,
          unrecognized: read.ok ? "profile names another agent" : read.error,
        };
      const { name, ...work } = read.profile;
      return { path, agent: agentIdentity(name).agent, ...work, allocation };
    }),
  );
}

async function unavailable(cwd, rev, backlogPath, extra = {}) {
  return stopped("agent-unavailable", {
    ...extra,
    occupied: await occupiedAssignments(cwd, rev, backlogPath),
    error: "every agent name is held on remote trunk",
  });
}

// The rotation's next name at `rev`, carrying what the agent reported about
// itself; when every name is held, a stop carrying `stopFields`.
export async function selectClaimAgent(request, rev, backlogPath, stopFields) {
  const { name } = await nextAgentName(request.integration, rev, backlogPath);
  if (!name)
    return unavailable(request.integration, rev, backlogPath, stopFields);
  return {
    ok: true,
    agent: {
      name,
      ...(request.host === undefined ? {} : { host: request.host }),
      ...(request.model === undefined ? {} : { model: request.model }),
    },
  };
}

// publishClaimSha's reselection hook. When the rejected push's trunk already
// holds the selected name, rebuild the still-isolated Take on that trunk under
// the rotation's next name instead of replaying it; otherwise leave the replay alone.
// `onAgent` learns the name that was actually committed.
export function reselectClaimAgent(claimRequest, agent, onAgent) {
  const { workspace, backlogPath, startingRevision } = claimRequest;
  return async ({ onto, candidateSha }) => {
    const { name, held } = await nextAgentName(workspace, onto, backlogPath);
    if (!held.includes(agent.name)) return undefined;
    const isolated =
      (await revParse(workspace, "HEAD")) === candidateSha &&
      (await revParse(workspace, `${candidateSha}^`)) === startingRevision &&
      (await git(workspace, "status", "--porcelain")).stdout === "";
    if (!isolated) return undefined;
    if (!name) return unavailable(workspace, onto, backlogPath);
    await git(workspace, "reset", "--hard", onto);
    const next = { ...agent, name };
    const recreated = await commitWorkspaceClaim({
      ...claimRequest,
      startingRevision: onto,
      agent: next,
    });
    if (recreated.ok) onAgent(next);
    return recreated;
  };
}

// The first readable profile commit `sha` added beside the backlog that
// `accepts(profile, name)` recognizes, where `name` is the agent its file
// name holds: `{ path, name, profile }`, or undefined. This is how a claim or
// preparation announcement names its own assignment.
export async function addedProfile(cwd, sha, backlogPath, accepts) {
  const added = await listedProfiles(
    cwd,
    backlogPath,
    "diff-tree",
    "--no-commit-id",
    "-r",
    "--name-only",
    "--diff-filter=A",
    sha,
  );
  for (const { path, name } of added) {
    const text = (await git(cwd, "show", `${sha}:${path}`)).stdout;
    const read = parseAgentProfile(text);
    if (read.ok && accepts(read.profile, name))
      return { path, name, profile: read.profile };
  }
  return undefined;
}

// A resumed claim keeps the agent its claim commit named: the profile that
// commit added for `identity`. Restores that agent's authorship in the reused
// workspace and returns its name, or undefined for a claim made without a
// profile. No new name is chosen and no profile is written.
export async function resumeClaimAgent(
  workspace,
  claimSha,
  identity,
  backlogPath,
) {
  const added = await addedProfile(
    workspace,
    claimSha,
    backlogPath,
    (profile) => profile.identity === identity,
  );
  if (!added) return undefined;
  const agent = agentIdentity(added.profile.name);
  await configureAgentAuthorship(workspace, agent);
  return agent.agent;
}

// The receipt's agent and whether its workspace authors ordinary commits as
// that agent; nothing for a claim made without an agent.
export async function receiptAgent(workspace, agent) {
  if (!agent) return {};
  return { agent, workspaceAuthorship: await workspaceAuthorship(workspace) };
}
