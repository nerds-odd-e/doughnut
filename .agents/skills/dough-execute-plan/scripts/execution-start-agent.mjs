// The agent a Take names: chosen from profiles held on the trunk its claim is
// built on, chosen again when a rival publishes the same name first, and read
// back from the claim commit when an existing claim resumes.
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

// Agent profiles (path and agent name) a Git command lists under the profile
// directory beside the backlog, in listed order.
async function listedProfiles(cwd, backlogPath, ...args) {
  const directory = join(dirname(backlogPath), agentProfileDirectory);
  const { stdout } = await git(cwd, ...args, "--", `${directory}/`);
  return stdout
    .split("\n")
    .filter(Boolean)
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
async function nextAgentName(cwd, rev, backlogPath) {
  const [mostRecent, held] = await Promise.all([
    mostRecentAgentName(cwd, rev, backlogPath),
    heldAgentNames(cwd, rev, backlogPath),
  ]);
  return { name: selectAgentName(mostRecent, held), held };
}

function unavailable(extra = {}) {
  return stopped("agent-unavailable", {
    ...extra,
    error: "every agent name is held on remote trunk",
  });
}

// The rotation's next name at `rev`, carrying what the agent reported about
// itself; when every name is held, a stop carrying `stopFields`.
export async function selectClaimAgent(request, rev, backlogPath, stopFields) {
  const { name } = await nextAgentName(request.integration, rev, backlogPath);
  if (!name) return unavailable(stopFields);
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
    if (!name) return unavailable();
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
  const added = await listedProfiles(
    workspace,
    backlogPath,
    "diff-tree",
    "--no-commit-id",
    "-r",
    "--name-only",
    "--diff-filter=A",
    claimSha,
  );
  for (const { path } of added) {
    const text = (await git(workspace, "show", `${claimSha}:${path}`)).stdout;
    const read = parseAgentProfile(text);
    if (!read.ok || read.profile.identity !== identity) continue;
    const agent = agentIdentity(read.profile.name);
    await configureAgentAuthorship(workspace, agent);
    return agent.agent;
  }
  return undefined;
}

// The receipt's agent and whether its workspace authors ordinary commits as
// that agent; nothing for a claim made without an agent.
export async function receiptAgent(workspace, agent) {
  if (!agent) return {};
  return { agent, workspaceAuthorship: await workspaceAuthorship(workspace) };
}
