import { execFileSync } from "node:child_process";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  realpathSync,
  writeFileSync,
} from "node:fs";
import { basename, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const checkoutRoot = fileURLToPath(
  new URL("../../../../", import.meta.url),
);
// Native hooks and Nix launchers share this directory; it must not follow TMPDIR.
export const mailboxRoot =
  process.env.DOUGH_CI_MAILBOX_ROOT ??
  join("/tmp", `dough-ci-${process.getuid?.() ?? "user"}`);
export const receiptPrefix = "CI_OBSERVER ";

function gitCommonDir(root) {
  try {
    const checkout = realpathSync(root);
    const toplevel = execFileSync(
      "git",
      ["-C", checkout, "rev-parse", "--show-toplevel"],
      { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] },
    ).trim();
    if (realpathSync(toplevel) !== checkout) return;
    const common = execFileSync(
      "git",
      ["-C", checkout, "rev-parse", "--git-common-dir"],
      { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] },
    ).trim();
    return realpathSync(resolve(checkout, common));
  } catch {
    return;
  }
}

function sameRepositoryCheckout(requestRoot, hookRoot) {
  if (resolve(requestRoot) === resolve(hookRoot)) return true;
  const requestCommon = gitCommonDir(requestRoot);
  const hookCommon = gitCommonDir(hookRoot);
  return Boolean(requestCommon && hookCommon && requestCommon === hookCommon);
}

export function readMailbox(
  directory,
  root = checkoutRoot,
  storage = mailboxRoot,
) {
  if (
    resolve(directory, "..") !== resolve(storage) ||
    !/^watch-/.test(basename(directory))
  ) {
    throw new Error("CI mailbox is outside the observer directory");
  }
  const request = JSON.parse(
    readFileSync(join(directory, "request.json"), "utf8"),
  );
  if (!sameRepositoryCheckout(request.root, root))
    throw new Error("CI mailbox belongs to another checkout");
  return request;
}

export function createMailbox(
  request,
  { root = checkoutRoot, storage = mailboxRoot } = {},
) {
  mkdirSync(storage, { recursive: true, mode: 0o700 });
  const directory = mkdtempSync(join(storage, "watch-"));
  writeFileSync(
    join(directory, "request.json"),
    JSON.stringify({ ...request, root }),
    { mode: 0o600 },
  );
  mkdirSync(join(directory, "events"), { mode: 0o700 });
  return directory;
}
