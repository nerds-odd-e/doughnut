// Git mechanics for bug remaining-work retention. Removes only the caller's
// named disposable paths, then either leaves the durable record as a local
// draft or commits and publishes that record from the owned workspace.
// Preparation disposition remains the publication owner.
import { rmSync } from "node:fs";
import { isAbsolute, relative, resolve } from "node:path";
import {
  git,
  pushCandidate,
  revParse,
} from "../../dough-execute-plan/scripts/publication-test-fixtures.mjs";

function insideWorkspace(workspace, candidate) {
  const root = resolve(workspace);
  const target = resolve(workspace, candidate);
  const rel = relative(root, target);
  return rel !== "" && !rel.startsWith("..") && !isAbsolute(rel);
}

function removeNamedDisposablePaths(workspace, disposablePaths, durablePath) {
  const durable = resolve(workspace, durablePath);
  for (const disposablePath of disposablePaths) {
    if (!insideWorkspace(workspace, disposablePath)) continue;
    const target = resolve(workspace, disposablePath);
    if (target === durable) continue;
    rmSync(target, { force: true });
  }
}

export async function retainBugTriageArtifacts({
  workspace,
  disposablePaths,
  durablePath,
  keep = false,
}) {
  removeNamedDisposablePaths(workspace, disposablePaths, durablePath);
  if (!keep) {
    return {
      disposition: "pending",
      detail:
        "pending disposition: local draft is not explicitly retained and stays in the owned workspace",
      published: false,
    };
  }
  await git(workspace, "add", "--", durablePath);
  await git(
    workspace,
    "commit",
    "-m",
    "Retain the bug-triage record",
    "--only",
    "--",
    durablePath,
  );
  const sha = await revParse(workspace, "HEAD");
  await pushCandidate(workspace, sha);
  await git(workspace, "fetch", "origin");
  return {
    disposition: "retained",
    detail: "retained record is on the authorized remote target",
    published: true,
    sha,
    workspace,
  };
}
