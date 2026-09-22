// Publish one claim SHA from the owned workspace. A rejected push rechecks
// remote membership before any replay. Another publisher, or missing
// provenance, stays a conflict. Implementation is not started here.
import { execFile } from "node:child_process";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";
import {
  git,
  lsRemoteSha,
  pushCandidate,
  revParse,
} from "./publication-test-fixtures.mjs";
import {
  claimProvenance,
  classifyOwnership,
  isAncestor,
  pathOf,
  remoteOf,
  remoteRef,
  stopped,
  targetOf,
} from "./workspace-publication-ownership.mjs";

const exec = promisify(execFile);
const rebaseCli = fileURLToPath(
  new URL(
    "../../dough-product-backlog/scripts/product-backlog-git-rebase.mjs",
    import.meta.url,
  ),
);

async function replaySuffix(request, onto) {
  try {
    const { stdout, stderr } = await exec(process.execPath, [
      rebaseCli,
      "rebase",
      "--onto",
      onto,
      "--ref",
      request.startingRevision,
      "--branch",
      request.branch,
      "--cwd",
      request.workspace,
      "--file",
      pathOf(request),
    ]);
    return { code: 0, stdout, stderr };
  } catch (error) {
    return {
      code: error.code ?? 1,
      stdout: error.stdout ?? "",
      stderr: error.stderr ?? "",
    };
  }
}

export async function claimMembership(request) {
  await git(request.workspace, "fetch", remoteOf(request));
  const ref = remoteRef(request);
  const provenance = await claimProvenance(
    request.workspace,
    ref,
    request.identity,
    pathOf(request),
  );
  const candidateSha = request.candidateSha;
  const candidateIsAncestor = candidateSha
    ? await isAncestor(request.workspace, candidateSha, ref)
    : false;
  return {
    provenance,
    candidateIsAncestor,
    ownership: classifyOwnership({
      publisherId: request.publisherId,
      candidateSha,
      candidateIsAncestor,
      provenance,
    }),
  };
}

export function conflictResult(request, ownership, provenance, candidateSha) {
  return stopped("conflict", {
    ownership,
    workspace: request.workspace,
    branch: request.branch,
    candidateSha,
    recovery: {
      workspace: request.workspace,
      branch: request.branch,
      startingRevision: request.startingRevision,
      candidateSha,
      provenance,
    },
  });
}

export async function publishClaimSha(request) {
  let sha = request.candidateSha;
  let rejected = false;
  try {
    await pushCandidate(request.workspace, sha);
  } catch (error) {
    const text = `${error.message}\n${error.stderr ?? ""}`;
    if (!/rejected|non-fast-forward/.test(text)) {
      return stopped("unpublished", {
        recovery: {
          workspace: request.workspace,
          branch: request.branch,
          candidateSha: sha,
          error: text,
        },
      });
    }
    rejected = true;
  }
  if (rejected) {
    const checked = await claimMembership({ ...request, candidateSha: sha });
    if (checked.ownership === "owned") {
      return {
        ok: true,
        status: "resumed",
        ownership: checked.ownership,
        publishedSha: checked.provenance.sha,
        workspace: request.workspace,
        branch: request.branch,
      };
    }
    if (checked.ownership !== "absent") {
      return conflictResult(
        request,
        checked.ownership,
        checked.provenance,
        sha,
      );
    }
    const onto = await revParse(request.workspace, remoteRef(request));
    const replay = await replaySuffix(request, onto);
    if (replay.code !== 0) {
      return stopped("conflict", {
        ownership: "replay-failed",
        recovery: {
          workspace: request.workspace,
          branch: request.branch,
          candidateSha: sha,
          replay,
        },
      });
    }
    sha = await revParse(request.workspace, "HEAD");
    try {
      await pushCandidate(request.workspace, sha);
    } catch (error) {
      return stopped("unpublished", {
        recovery: {
          workspace: request.workspace,
          branch: request.branch,
          candidateSha: sha,
          error: `${error.message}\n${error.stderr ?? ""}`,
        },
      });
    }
  }
  await git(request.workspace, "fetch", remoteOf(request));
  const tip = await lsRemoteSha(
    request.origin,
    `refs/heads/${targetOf(request)}`,
  );
  const present =
    sha === tip || (await isAncestor(request.workspace, sha, tip));
  if (!present) {
    return stopped("unpublished", {
      recovery: {
        workspace: request.workspace,
        branch: request.branch,
        candidateSha: sha,
      },
    });
  }
  return {
    ok: true,
    status: "published",
    publishedSha: sha,
    workspace: request.workspace,
    branch: request.branch,
    candidateSha: sha,
  };
}
