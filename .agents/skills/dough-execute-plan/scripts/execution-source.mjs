// Published queued or continued Taken source and local unpublished
// selected-source checks.
import { readFileSync } from "node:fs";
import { dirname, join, posix, relative, resolve, sep } from "node:path";
import {
  parseBacklog,
  queueHeading,
  takenHeading,
} from "../../dough-product-backlog/scripts/product-backlog-document.mjs";
import { readHome } from "../../dough-product-backlog/scripts/product-backlog-home-reader.mjs";
import { splitHref } from "../../dough-product-backlog/scripts/product-backlog-identity.mjs";
import { readStoryState } from "../../dough-product-backlog/scripts/product-backlog-story-state.mjs";
import { BacklogError } from "../../dough-product-backlog/scripts/product-backlog-refusal.mjs";
import { git } from "./publication-git.mjs";
import {
  backlogPath,
  claimProvenance,
} from "./workspace-publication-ownership.mjs";

export async function show(cwd, rev, path) {
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

export function selectedRegion(source, href) {
  if (source === null) return null;
  const home = readHome(source, href);
  const lines = home.document.lines.slice(home.region.start, home.region.end);
  // A sibling section may add separator lines at this boundary. Preserve
  // whitespace within the selected section, including trailing spaces on text.
  while (lines.at(-1) === "") lines.pop();
  return lines.join("\n");
}

// The originating checkout's working-tree copy, or null when it has none.
export function worktreeSource(root, path) {
  try {
    return readFileSync(join(root, path), "utf8");
  } catch {
    return null;
  }
}

export async function mergeBase(integration, remoteRef) {
  return (
    await git(integration, "merge-base", "HEAD", remoteRef)
  ).stdout.trim();
}

// The project path of the canonical home a backlog link names.
export function canonicalHomePath(integration, href) {
  return within(
    integration,
    posix.join(dirname(backlogPath), splitHref(href).path),
  );
}

// The project path of the plan a canonical home's preparation declares.
export function declaredPlanPath(integration, homePath, plan) {
  return within(integration, posix.join(dirname(homePath), plan));
}

// The selected region, or undefined when the source lacks that story.
function regionOf(source, href) {
  if (!href) return source;
  try {
    return selectedRegion(source, href);
  } catch (error) {
    if (error instanceof BacklogError) return undefined;
    throw error;
  }
}

// Whether the originating checkout's HEAD, index or worktree holds a version
// of the selected source that was never published: one matching neither its
// merge base, fetched trunk, nor a `published` revision such as the claim
// that admitted it and left its draft there.
async function unpublishedSource(
  integration,
  remoteRef,
  path,
  href,
  published,
) {
  const base = await mergeBase(integration, remoteRef);
  const known = new Set();
  for (const rev of [base, remoteRef, ...published])
    known.add(regionOf(await show(integration, rev, path), href));
  const local = [
    await show(integration, "HEAD", path),
    await show(integration, "", path),
    worktreeSource(integration, path),
  ];
  return local.some((source) => !known.has(regionOf(source, href)));
}

export async function readPublishedExecutionSource(request, remoteRef) {
  const backlog = await show(request.integration, remoteRef, backlogPath);
  if (backlog === null) throw new Error("fetched trunk has no product backlog");
  const entry = parseBacklog(backlog).entries.find(
    (item) => item.identity === request.identity,
  );
  if (!entry || (entry.list !== queueHeading && entry.list !== takenHeading))
    throw new Error("selected identity is not queued on fetched trunk");
  // Outside claim recovery, Taken work is a continuation: only the claim's
  // own publisher continues it, and only from ready published preparation.
  let claim;
  if (entry.list === takenHeading && !request.retained) {
    claim = await claimProvenance(
      request.integration,
      remoteRef,
      request.identity,
      backlogPath,
    );
    if (!claim?.publisher || claim.publisher !== request.publisherId)
      return { existing: entry, claim };
  }
  const backlogDir = dirname(backlogPath);
  const homePath = canonicalHomePath(request.integration, entry.href);
  const home = await show(request.integration, remoteRef, homePath);
  if (home === null)
    throw new Error("selected canonical home is absent on fetched trunk");
  const preview = readStoryState(home, entry.href);
  if (preview.identity !== request.identity || preview.status !== "recorded")
    throw new Error("selected canonical preparation or identity is unresolved");
  let planPath, plan, planTarget;
  if (preview.approach.kind === "planned") {
    planPath = declaredPlanPath(
      request.integration,
      homePath,
      preview.approach.plan,
    );
    plan = await show(request.integration, remoteRef, planPath);
    if (plan === null) throw new Error("published plan is absent");
    const planHref = posix.relative(backlogDir, planPath);
    // Take refuses a plan link naming the entry's own href as a duplicate.
    if (planHref !== entry.href) planTarget = planHref;
    if (entry.plan && entry.plan.target !== planTarget)
      throw new Error("queued plan link disagrees with preparation");
    if (request.plan && request.plan !== planHref)
      throw new Error("requested plan disagrees with published preparation");
  } else if (preview.approach.kind === "unselected") {
    throw new Error("published approach is unselected");
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
  const published = claim ? [claim.sha] : [];
  if (
    await unpublishedSource(
      request.integration,
      remoteRef,
      homePath,
      entry.href,
      published,
    )
  )
    throw new Error(
      "unpublished selected story source in originating checkout",
    );
  if (
    planPath &&
    (await unpublishedSource(
      request.integration,
      remoteRef,
      planPath,
      null,
      published,
    ))
  )
    throw new Error("unpublished selected plan in originating checkout");
  return {
    ...(claim ? { existing: entry, claim } : {}),
    entry,
    homePath,
    planPath,
    planTarget,
    preparation,
    selectedSource: selectedRegion(home, entry.href),
    planSource: plan,
  };
}
