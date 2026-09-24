// Published queued source and local unpublished selected-source checks.
import { readFileSync } from "node:fs";
import { dirname, join, posix, relative, resolve, sep } from "node:path";
import {
  parseBacklog,
  queueHeading,
  takenHeading,
} from "../../dough-product-backlog/scripts/product-backlog-document.mjs";
import { readHome } from "../../dough-product-backlog/scripts/product-backlog-home-reader.mjs";
import { readStoryState } from "../../dough-product-backlog/scripts/product-backlog-story-state.mjs";
import { git } from "./publication-git.mjs";
import { backlogPath } from "./workspace-publication-ownership.mjs";

async function show(cwd, rev, path) {
  try {
    return (await git(cwd, "show", `${rev}:${path}`)).stdout;
  } catch {
    return null;
  }
}

function within(root, path) {
  const project = resolve(root);
  const absolute = resolve(project, path);
  if (absolute !== project && !absolute.startsWith(project + sep))
    throw new Error(`source path escapes project: ${path}`);
  return relative(project, absolute).split(sep).join("/");
}

function selectedRegion(source, href) {
  if (source === null) return null;
  const home = readHome(source, href);
  const lines = home.document.lines.slice(home.region.start, home.region.end);
  // A sibling section may add separator lines at this boundary. Preserve
  // whitespace within the selected section, including trailing spaces on text.
  while (lines.at(-1) === "") lines.pop();
  return lines.join("\n");
}

async function unpublishedSource(integration, remoteRef, path, href) {
  const base = (
    await git(integration, "merge-base", "HEAD", remoteRef)
  ).stdout.trim();
  const head = await show(integration, "HEAD", path);
  const index = await show(integration, "", path);
  let worktree;
  try {
    worktree = readFileSync(join(integration, path), "utf8");
  } catch {
    worktree = null;
  }
  const versions = [await show(integration, base, path), head, index, worktree];
  const selected = versions.map((source) =>
    href ? selectedRegion(source, href) : source,
  );
  return selected.some(
    (value, index) => index > 0 && value !== selected[index - 1],
  );
}

export async function readPublishedExecutionSource(request, remoteRef) {
  const backlog = await show(request.integration, remoteRef, backlogPath);
  if (backlog === null) throw new Error("fetched trunk has no product backlog");
  const entry = parseBacklog(backlog).entries.find(
    (item) => item.identity === request.identity,
  );
  if (
    !entry ||
    (entry.list !== queueHeading &&
      !(request.retained && entry.list === takenHeading))
  )
    throw new Error("selected identity is not queued on fetched trunk");
  const backlogDir = dirname(backlogPath);
  const homePath = within(
    request.integration,
    posix.join(backlogDir, entry.href.split("#")[0]),
  );
  const home = await show(request.integration, remoteRef, homePath);
  if (home === null)
    throw new Error("selected canonical home is absent on fetched trunk");
  const preview = readStoryState(home, entry.href);
  if (preview.identity !== request.identity || preview.status !== "recorded")
    throw new Error("selected canonical preparation or identity is unresolved");
  let planPath, plan, planTarget;
  if (preview.approach.kind === "planned") {
    planPath = within(
      request.integration,
      posix.join(dirname(homePath), preview.approach.plan),
    );
    plan = await show(request.integration, remoteRef, planPath);
    if (plan === null) throw new Error("published plan is absent");
    // A whole-document correction's plan is its canonical home: no plan link.
    if (planPath !== homePath)
      planTarget = posix.relative(backlogDir, planPath);
    if (entry.plan && entry.plan.target !== planTarget)
      throw new Error("queued plan link disagrees with preparation");
    if (request.plan && request.plan !== planTarget)
      throw new Error("requested plan disagrees with published preparation");
  } else if (
    preview.approach.kind !== "planless" ||
    request.plan ||
    entry.plan
  ) {
    throw new Error("queued planless authority or plan link is inconsistent");
  }
  const preparation = readStoryState(
    home,
    entry.href,
    planPath ? { planSource: plan } : {},
  );
  if (preparation.assessment.status !== "ready")
    throw new Error(
      `published preparation is ${preparation.assessment.status}`,
    );
  if (
    await unpublishedSource(
      request.integration,
      remoteRef,
      homePath,
      entry.href,
    )
  )
    throw new Error(
      "unpublished selected story source in originating checkout",
    );
  if (
    planPath &&
    (await unpublishedSource(request.integration, remoteRef, planPath, null))
  )
    throw new Error("unpublished selected plan in originating checkout");
  return {
    entry,
    homePath,
    planPath,
    planTarget,
    preparation,
    selectedSource: selectedRegion(home, entry.href),
    planSource: plan,
  };
}
