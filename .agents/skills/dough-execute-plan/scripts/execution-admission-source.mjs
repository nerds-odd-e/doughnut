// Admission source: an accepted mission that neither backlog list holds yet.
// Its canonical story, and a plan that story declares, may still be drafts in
// the originating checkout. Admission carries only that owned content: the
// selected story's section of its seed (the whole seed when the seed itself is
// new) and the whole declared plan. Each is reconciled with fetched trunk
// against the revision it was drafted from, so sibling sections on trunk
// survive, other local edits stay local, and an edit trunk also made
// differently stops with both versions intact for a human decision.
// Preparation is read as recorded; admission never records readiness or
// approach of its own.
import { dirname, posix } from "node:path";
import {
  parseBacklog,
  queueHeading,
} from "../../dough-product-backlog/scripts/product-backlog-document.mjs";
import { readHome } from "../../dough-product-backlog/scripts/product-backlog-home-reader.mjs";
import { BacklogError } from "../../dough-product-backlog/scripts/product-backlog-refusal.mjs";
import {
  joinSource,
  splitSource,
} from "../../dough-product-backlog/scripts/product-backlog-source.mjs";
import { readStoryPurpose } from "../../dough-product-backlog/scripts/product-backlog-story-purpose.mjs";
import { readStoryState } from "../../dough-product-backlog/scripts/product-backlog-story-state.mjs";
import {
  canonicalHomePath,
  declaredPlanPath,
  mergeBase,
  selectedRegion,
  show,
  worktreeSource,
} from "./execution-source.mjs";
import { revParse } from "./publication-git.mjs";
import { backlogPath } from "./workspace-publication-ownership.mjs";

// A refusal that names its stop status, such as a reconciliation conflict.
class AdmissionRefusal extends Error {
  constructor(status, message, fields = {}) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

const refused = (message, fields) =>
  new AdmissionRefusal("source-refused", message, fields);

// Where admitted content is drafted, and the revision it was drafted from:
// the originating worktree against its merge base with trunk, or a preserved
// claim candidate against its own parent. Recovery reconciles that candidate
// again, so later local drafting never changes an accepted admission.
async function draftsOf(integration, remoteRef, candidateSha) {
  if (candidateSha)
    return {
      base: await revParse(integration, `${candidateSha}^`),
      read: (path) => show(integration, candidateSha, path),
    };
  return {
    base: await mergeBase(integration, remoteRef),
    read: async (path) => worktreeSource(integration, path),
  };
}

// The file at the drafts' base, on fetched trunk, and as drafted. A file the
// drafts lack is drafted as trunk has it: there is nothing to carry.
async function versionsOf(request, remoteRef, drafts, path) {
  const trunk = await show(request.integration, remoteRef, path);
  return {
    path,
    base: await show(request.integration, drafts.base, path),
    trunk,
    draft: (await drafts.read(path)) ?? trunk,
  };
}

const conflict = (path, message) =>
  new AdmissionRefusal(
    "source-conflict",
    `${path} ${message}; both versions are preserved`,
    { path },
  );

// The selected story's section, or undefined when `source` has none.
function sectionOf(source, href) {
  if (source === null) return undefined;
  try {
    const { document, region } = readHome(source, href);
    return { document, region, text: selectedRegion(source, href) };
  } catch (error) {
    if (error instanceof BacklogError) return undefined;
    throw error;
  }
}

// Where the drafted section goes in a trunk seed that lacks it: before the
// section boundary that follows it in the draft, or at the end together
// with the blank lines that separate it.
function insertionIndex(lines, drafted, path) {
  const { lines: draft } = drafted.document;
  const { start, end } = drafted.region;
  if (end === draft.length) {
    let from = start;
    while (from > 0 && draft[from - 1] === "") from -= 1;
    return { at: lines.length, from };
  }
  const at = lines.indexOf(draft[end]);
  if (at === -1)
    throw conflict(path, "has no place for the new story section on trunk");
  return { at, from: start };
}

// Trunk's seed with only the selected story's section as drafted. Sibling
// sections keep trunk's text, and other local edits stay local. A section
// that trunk and the draft each changed from the merge base, or one removed
// on trunk, stops for a human decision. A seed new on both sides is owned
// whole.
function reconcileStory({ base, trunk, draft, path }, href) {
  if (trunk === null) {
    if (base !== null) throw conflict(path, "was removed on fetched trunk");
    return draft;
  }
  const drafted = sectionOf(draft, href);
  const published = sectionOf(trunk, href);
  const original = sectionOf(base, href);
  if (published?.text === drafted.text) return trunk;
  if (published && published.text !== original?.text) {
    if (drafted.text === original?.text) return trunk;
    throw conflict(path, "has a story section also changed on fetched trunk");
  }
  if (!published && original)
    throw conflict(path, "lost the story section on fetched trunk");
  const document = splitSource(trunk);
  const lines = [...document.lines];
  const { lines: draftLines } = drafted.document;
  if (published) {
    const { start, end } = published.region;
    lines.splice(
      start,
      end - start,
      ...draftLines.slice(drafted.region.start, drafted.region.end),
    );
  } else {
    const { at, from } = insertionIndex(lines, drafted, path);
    lines.splice(at, 0, ...draftLines.slice(from, drafted.region.end));
  }
  return joinSource({ ...document, lines });
}

// A declared plan is owned whole: an edit on only one side wins, and
// different edits on both sides stop.
function reconcilePlan({ base, trunk, draft, path }) {
  if (draft === base || draft === trunk) return trunk;
  if (trunk === base) return draft;
  throw conflict(path, "was also changed on fetched trunk");
}

// The minimal content an admitted story needs: its own identity, recorded
// preparation facts and a recorded Goal the dashboard can show.
function requireAdmissibleStory(source, href, identity, options = {}) {
  const state = readStoryState(source, href, options);
  if (state.identity !== identity)
    throw refused(
      `${state.key} names identity "${state.identity}", not "${identity}"`,
    );
  if (state.status !== "recorded")
    throw refused(
      `${state.key} has no recorded preparation facts; record them first`,
    );
  if (readStoryPurpose(source, href).status !== "recorded")
    throw refused(`${state.key} has no recorded Goal`);
  return state;
}

// With `candidateSha`, the admission that preserved claim candidate carries.
// Once the work is listed, only the listing and its plan, whose owner the
// claim's provenance decides.
export async function readAdmissionSource(request, remoteRef, candidateSha) {
  const { integration, identity, link } = request;
  const backlog = await show(integration, remoteRef, backlogPath);
  if (backlog === null) throw refused("fetched trunk has no product backlog");
  const { entries } = parseBacklog(backlog);
  const entry = entries.find((item) => item.identity === identity);
  if (entry?.list === queueHeading)
    throw refused(
      "selected identity is already queued on fetched trunk; start it as queued work",
    );
  if (entry) return { existing: entry, planTarget: entry.plan?.target };
  const listed = entries.find((item) => item.href === link);
  if (listed)
    throw refused(
      `canonical home ${link} is already listed as "${listed.identity}"`,
    );
  const homePath = canonicalHomePath(integration, link);
  const drafts = await draftsOf(integration, remoteRef, candidateSha);
  const home = await versionsOf(request, remoteRef, drafts, homePath);
  if (home.draft === null)
    throw refused(`selected canonical home ${homePath} is absent`);
  const drafted = requireAdmissibleStory(home.draft, link, identity);
  let plan, planPath, planTarget;
  if (drafted.approach.kind === "planned") {
    planPath = declaredPlanPath(integration, homePath, drafted.approach.plan);
    const planHref = posix.relative(dirname(backlogPath), planPath);
    if (planPath !== homePath) planTarget = planHref;
    if (request.plan && request.plan !== planHref)
      throw refused("requested plan disagrees with recorded preparation");
    if (planPath !== homePath) {
      plan = await versionsOf(request, remoteRef, drafts, planPath);
      if (plan.draft === null)
        throw refused(`declared plan ${planPath} is absent`);
    }
  } else if (request.plan) {
    throw refused(`a ${drafted.approach.kind} story links no plan`);
  }
  const homeSource = reconcileStory(home, link);
  const planSource = plan && reconcilePlan(plan);
  const preparation = requireAdmissibleStory(
    homeSource,
    link,
    identity,
    planPath === homePath
      ? { planIsCanonical: true }
      : planSource === undefined
        ? {}
        : { planSource },
  );
  const files = [
    [home, homeSource],
    [plan, planSource],
  ]
    .filter(([file, content]) => file && content !== file.trunk)
    .map(([file, content]) => ({ path: file.path, content }));
  return {
    homePath,
    planPath,
    planTarget,
    preparation,
    selectedSource: selectedRegion(homeSource, link),
    planSource,
    admission: { title: request.title, href: link, files },
  };
}
