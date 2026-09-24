// Whether and how an owned workspace's ordinary commits name its agent as
// author through per-worktree Git config.
import { join } from "node:path";
import { git } from "./publication-git.mjs";

// "configured" when the workspace's ordinary commits can be authored through
// per-worktree config. A bare repository's worktrees rely on `core.bare` or
// `core.worktree` in the shared config; enabling `extensions.worktreeConfig`
// there breaks every checkout, so such a workspace is "not-configured".
export async function workspaceAuthorship(workspace) {
  const read = async (...args) => (await git(workspace, ...args)).stdout.trim();
  const common = await read(
    "rev-parse",
    "--path-format=absolute",
    "--git-common-dir",
  );
  const shared = (...args) =>
    read("config", "--file", join(common, "config"), ...args);
  const bare = await shared("--type=bool", "--default=false", "core.bare");
  const worktree = await shared("--default=", "core.worktree");
  return bare === "true" || worktree !== "" ? "not-configured" : "configured";
}

// The agent authors every ordinary commit in its owned workspace; the
// configured Git user stays the committer, and other checkouts keep their
// usual author. Leaves a bare repository's shared config untouched.
export async function configureAgentAuthorship(workspace, { agent, email }) {
  if ((await workspaceAuthorship(workspace)) !== "configured") return;
  await git(workspace, "config", "extensions.worktreeConfig", "true");
  await git(workspace, "config", "--worktree", "author.name", agent);
  await git(workspace, "config", "--worktree", "author.email", email);
}
