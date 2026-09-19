// Reads and records a work item's identity in the canonical home a backlog
// link points at: one anchored story section inside a seed, or one whole
// document such as an active plan. Recording the identity in the home is what
// lets the same work item be recognised after its title, link, or location
// changes; the backlog line alone could not survive that.
//
// This owns where an identity is written, not which identity a work item has.

import { resolve } from "node:path";
import { composeIdentity, splitHref } from "./product-backlog-identity.mjs";
import { BacklogError } from "./product-backlog-refusal.mjs";
import { joinSource, splitSource } from "./product-backlog-source.mjs";
import { readFile, replaceFile } from "./product-backlog-store.mjs";

const identityPattern = /^\*\*Identity:\*\* +(?<identity>\S.*?) *$/;
const frontMatterIdPattern = /^id: +(?<id>\S+) *$/;
const fence = "---";

function identityLine(identity) {
  return `**Identity:** ${identity}`;
}

// The whole document, or just the story section the anchor names.
function regionFor(lines, relative, anchor) {
  if (anchor === "") {
    const heading = lines.findIndex((line) => line.startsWith("# "));
    if (heading === -1) {
      throw new BacklogError(
        `${relative} has no "# " title to record an identity under.`,
      );
    }
    return { start: 0, end: lines.length, heading };
  }

  const start = lines.findIndex(
    (line) => line.trim() === `<a id="${anchor}"></a>`,
  );
  if (start === -1) {
    throw new BacklogError(
      `${relative} has no story anchored at "${anchor}"; the backlog links ` +
        `to a section that is not there.`,
    );
  }
  let end = lines.length;
  for (let index = start + 1; index < lines.length; index += 1) {
    if (lines[index].startsWith('<a id="') || lines[index].startsWith("## ")) {
      end = index;
      break;
    }
  }
  const heading = lines.findIndex(
    (line, index) => index > start && index < end && line.startsWith("### "),
  );
  if (heading === -1) {
    throw new BacklogError(
      `${relative} has no "### " story heading under the anchor "${anchor}".`,
    );
  }
  return { start, end, heading };
}

// The immutable document ID a seed already carries. Adoption reuses it rather
// than allocating a number of its own, so no shared registry is needed.
function documentIdFor(lines) {
  if (lines[0] !== fence) {
    return "";
  }
  const close = lines.findIndex((line, index) => index > 0 && line === fence);
  const end = close === -1 ? lines.length : close;
  for (let index = 1; index < end; index += 1) {
    const match = frontMatterIdPattern.exec(lines[index]);
    if (match) {
      return match.groups.id;
    }
  }
  return "";
}

function recordedIn(lines, region, relative) {
  const found = [];
  for (let index = region.start; index < region.end; index += 1) {
    const match = identityPattern.exec(lines[index]);
    if (match) {
      found.push({ identity: match.groups.identity, index });
    }
  }
  if (found.length > 1) {
    throw new BacklogError(
      `${relative} records an identity ${found.length} times at lines ` +
        `${found.map((entry) => entry.index + 1).join(" and ")}; a human ` +
        `decides which one this work item keeps.`,
    );
  }
  return found[0];
}

// Opens the canonical home a backlog link names and reports what it already
// records. Throws a BacklogError describing the home when it cannot be used.
export function openHome(backlogDirectory, href) {
  const { path: relative, anchor } = splitHref(href);
  const path = resolve(backlogDirectory, relative);
  const source = readFile(path, `canonical home not found: ${relative}`);
  const document = splitSource(source);
  const region = regionFor(document.lines, relative, anchor);
  return {
    backlogDirectory,
    href,
    relative,
    anchor,
    // Two stories in one seed are separate homes, so the anchor is part of it.
    key: anchor === "" ? relative : `${relative}#${anchor}`,
    path,
    document,
    region,
    documentId: anchor === "" ? "" : documentIdFor(document.lines),
    recorded: recordedIn(document.lines, region, relative),
  };
}

// The identity an anchored home implies before anything is recorded there:
// the seed's own document ID composed with the anchor — the same evidence a
// work item's identity is first taken from. A whole-document home, or an
// anchored home whose seed carries no "id:" of its own, implies none this
// way. Both adoption and the canonical-home check at `add` read this same
// implied identity when nothing has been recorded yet.
export function impliedIdentity(home) {
  if (home.anchor === "" || home.documentId === "") {
    return undefined;
  }
  return composeIdentity(home.documentId, home.anchor);
}

// The identity a home names on its own, without writing anything: an explicit
// `**Identity:**` record when the home carries one, or otherwise the identity
// it implies. A whole-document home that has recorded nothing names none on
// its own; there is no link-derived fallback here, because a link agreeing
// with an identity is not the same as the home naming it.
export function namedIdentity(home) {
  return home.recorded ? home.recorded.identity : impliedIdentity(home);
}

// Writes the identity into the home. The home is read again here, because one
// seed can be the home of several stories and because another run may have
// allocated in the meantime; an identity already recorded is never replaced.
export function recordIdentity(home, identity) {
  const current = openHome(home.backlogDirectory, home.href);
  if (current.recorded) {
    if (current.recorded.identity === identity) {
      return;
    }
    throw new BacklogError(
      `${home.relative} now records identity ` +
        `"${current.recorded.identity}", allocated while this adoption was ` +
        `running, and not "${identity}".`,
    );
  }
  const lines = [...current.document.lines];
  lines.splice(current.region.heading + 1, 0, "", identityLine(identity));
  replaceFile(current.path, joinSource({ ...current.document, lines }));
}

// Whether a document still claims an identity, asked of a reference that is
// about to be dropped. A path that is gone, or that no longer reads as a
// canonical home at all, claims nothing: only a home still recording this
// exact identity would leave two documents claiming one work item.
export function stillRecords(backlogDirectory, href, identity) {
  try {
    const home = openHome(backlogDirectory, href);
    return Boolean(home.recorded) && home.recorded.identity === identity;
  } catch (error) {
    if (error instanceof BacklogError) {
      return false;
    }
    throw error;
  }
}
