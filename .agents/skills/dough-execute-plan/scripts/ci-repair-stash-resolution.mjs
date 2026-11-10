// Validates an operator-reviewed merge that differs from both saved and
// repaired trees. Reading and writing its recovery record stays with the CLI.
import { resolve } from "node:path";
import { conflictPaths } from "./ci-repair-stash-applied.mjs";
import {
  gitOutputOrNull,
  inventory,
  stashEntryByOid,
} from "./ci-repair-stash-git.mjs";

export async function reviewRepairStashResolution(record, reviewed) {
  const result = { ok: false, oid: record.oid };
  if (!reviewed || reviewed !== record.oid || record.restore?.status !== "conflict")
    return { ...result, status: "review-required" };
  const root = await gitOutputOrNull(
    record.checkout,
    "rev-parse",
    "--show-toplevel",
  );
  const branch = await gitOutputOrNull(
    record.checkout,
    "symbolic-ref",
    "--short",
    "-q",
    "HEAD",
  );
  if (!root || resolve(root) !== resolve(record.checkout) || branch !== record.branch)
    return { ...result, status: "checkout-mismatch" };
  const paths = await conflictPaths(record.checkout, "");
  if (paths.length) return { ...result, status: "unresolved", paths };
  const current = await inventory(record.checkout);
  const stagedNotRestored = record.staged.filter(
    (path) => !current.staged.includes(path),
  );
  if (stagedNotRestored.length)
    return { ...result, status: "partial", stagedNotRestored };
  if (!(await stashEntryByOid(record.checkout, record.oid)))
    return { ...result, status: "missing" };
  return { ...result, ok: true, status: "resolved", applied: "reviewed" };
}
