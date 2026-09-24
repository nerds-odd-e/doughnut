// Filesystem boundary for story-state preparation and assessment records:
// opens a canonical home, serializes cooperating writers per file, loads a
// planned path relative to that home when needed for digests, and applies the
// pure record/read operations. Backlog queue bytes are never rewritten here.

import { dirname, resolve } from "node:path";
import { splitHref } from "./product-backlog-identity.mjs";
import { BacklogError } from "./product-backlog-refusal.mjs";
import { applyToFile, readFile } from "./product-backlog-store.mjs";
import { preparationPayload } from "./product-backlog-story-state-preparation.mjs";
import {
  readStoryState,
  recordStoryState,
} from "./product-backlog-story-state.mjs";

function homePath(backlogDirectory, href) {
  const { path: relative } = splitHref(href);
  return {
    relative,
    path: resolve(backlogDirectory, relative),
  };
}

function planPathBesideHome(canonicalPath, plan) {
  return resolve(dirname(canonicalPath), plan);
}

// Loads plan text beside the canonical home when digests need it. Same-file
// plans are marked canonical so the basis digests the document once.
function planLoadOptions(canonicalPath, approach, plan) {
  if (approach !== "planned") {
    return { planSource: undefined, planIsCanonical: false };
  }
  const resolved = planPathBesideHome(canonicalPath, plan);
  if (resolve(resolved) === resolve(canonicalPath)) {
    return { planSource: undefined, planIsCanonical: true };
  }
  const planSource = readFile(
    resolved,
    `Unresolved plan: ${plan} is not there, relative to the canonical file. ` +
      `Create the plan first, or supply the path the story should associate.`,
  );
  return { planSource, planIsCanonical: false };
}

// Reads preparation facts and assessment view from the canonical home a link
// names, including the current content basis.
export function readPreparation(backlogDirectory, href) {
  const { relative, path } = homePath(backlogDirectory, href);
  const source = readFile(path, `canonical home not found: ${relative}`);
  const preview = readStoryState(source, href);
  if (preview.status === "recorded" && preview.approach.kind === "planned") {
    const options = planLoadOptions(path, "planned", preview.approach.plan);
    return readStoryState(source, href, options);
  }
  return preview;
}

// Records preparation and optional assessment for one story under a
// cooperating per-file lock. Other stories in the same seed, identity lines,
// and the backlog file are left untouched. A planned approach must resolve
// beside the canonical file; planless needs no plan file.
export async function recordPreparation(backlogDirectory, request) {
  const { relative, path } = homePath(backlogDirectory, request.href);
  let outcome;
  await applyToFile(
    path,
    (source) => {
      const preparation = preparationPayload(request);
      const options = planLoadOptions(
        path,
        preparation.approach,
        preparation.plan,
      );
      outcome = recordStoryState(source, request, options);
      return outcome.source;
    },
    `canonical home not found: ${relative}`,
  );
  return outcome;
}

// Re-export the pure reader for callers that already hold source text.
export { readStoryState };

// Surface lock/refusal cases that are about the home, not the backlog queue.
export function preparationRefusal(error) {
  if (!(error instanceof BacklogError)) {
    return undefined;
  }
  return error.refusal.replace(
    /\nThe backlog was not changed\.$/,
    "\nNothing was written.",
  );
}
