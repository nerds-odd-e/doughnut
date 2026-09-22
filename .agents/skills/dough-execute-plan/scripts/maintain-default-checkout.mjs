// Git mechanics for maintain-default-checkout.md "Refresh eligibility".
// Fast-forwards only a clean checkout this caller owns and that is strictly
// behind fetched trunk. Installed guidance is the agent's contract.
import { existsSync } from "node:fs";
import {
  captureCheckout,
  git,
  indexLockPath,
  revParse,
} from "./publication-test-fixtures.mjs";

const IN_PROGRESS_REFS = [
  "MERGE_HEAD",
  "REBASE_HEAD",
  "CHERRY_PICK_HEAD",
  "REVERT_HEAD",
];

function singleOwner(owner) {
  return typeof owner === "string" && owner.trim() !== "";
}

// Refresh eligibility's declared-owner rule, before any fetch or mutation.
// Another caller applies the same refusal when it would mutate this checkout.
export function declaredOwnerRefusal(declaredOwner, requester) {
  if (!singleOwner(declaredOwner) || !singleOwner(requester)) {
    return "unclear-ownership";
  }
  if (declaredOwner !== requester) {
    return "another-writer";
  }
  return null;
}

function decision(result, reason, state, remoteSha) {
  return { result, reason, remoteSha, ...state };
}

async function isAncestor(checkout, ancestor, descendant) {
  try {
    await git(checkout, "merge-base", "--is-ancestor", ancestor, descendant);
    return true;
  } catch {
    return false;
  }
}

async function ongoingOperation(checkout) {
  if (existsSync(await indexLockPath(checkout))) {
    return "index.lock";
  }
  for (const ref of IN_PROGRESS_REFS) {
    try {
      await git(checkout, "rev-parse", "-q", "--verify", ref);
      return ref;
    } catch {
      // This ref is absent.
    }
  }
  return null;
}

// Re-reads the checkout after the declared owner is applied. `declaredOwner`
// comes from explicit coordinator context; this module does not create a lock.
// A missing snapshot argument is intentional: callers cannot supply a stale one.
export async function refreshDefaultCheckout({
  checkout,
  declaredOwner,
  requester,
  integrationBranch = "main",
}) {
  const ongoing = await ongoingOperation(checkout);
  const state = ongoing
    ? {
        head: await revParse(checkout, "HEAD"),
        status: null,
        staged: null,
        unstaged: null,
        index: null,
      }
    : await captureCheckout(checkout);

  const ownerRefusal = declaredOwnerRefusal(declaredOwner, requester);
  if (ownerRefusal) {
    return decision("deferred", ownerRefusal, state, null);
  }
  if (ongoing) {
    return decision("deferred", "ongoing-operation", state, null);
  }

  await git(checkout, "fetch", "origin");
  const current = await captureCheckout(checkout);
  const remoteRef = `origin/${integrationBranch}`;
  const remoteSha = await revParse(checkout, remoteRef);
  const branch = (
    await git(checkout, "branch", "--show-current")
  ).stdout.trim();

  if (branch !== integrationBranch) {
    return decision("stopped", "unexpected-branch", current, remoteSha);
  }
  if (await ongoingOperation(checkout)) {
    return decision("deferred", "ongoing-operation", current, remoteSha);
  }
  const behind = await isAncestor(checkout, current.head, remoteRef);
  const ahead = await isAncestor(checkout, remoteRef, current.head);
  if (!behind && !ahead) {
    return decision("stopped", "diverged", current, remoteSha);
  }
  if (current.status !== "") {
    return decision("deferred", "pending-edit", current, remoteSha);
  }
  if (current.head === remoteSha) {
    return decision("already current", null, current, remoteSha);
  }
  if (behind) {
    await git(checkout, "merge", "--ff-only", remoteRef);
    const advanced = await captureCheckout(checkout);
    if (advanced.head !== remoteSha || advanced.status !== "") {
      throw new Error(
        "fast-forward did not leave a clean checkout at fetched trunk",
      );
    }
    return decision("advanced", null, advanced, remoteSha);
  }
  return decision("deferred", "unpublished-commits", current, remoteSha);
}
