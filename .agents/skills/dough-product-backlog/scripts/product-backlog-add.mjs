// Adds exactly one already identified entry to "## Backlog list" at an
// explicit relative position. Identities are never allocated here, and nothing
// else in the document changes.

import {
  parseBacklog,
  renderBacklog,
  renderEntry,
  requireUnlistedHome,
} from "./product-backlog-document.mjs";
import { namedIdentity, openHome } from "./product-backlog-home.mjs";
import { recordsOwnIdentity, splitHref } from "./product-backlog-identity.mjs";
import {
  insertEntryLine,
  queueIndexFor,
} from "./product-backlog-placement.mjs";
import { BacklogError } from "./product-backlog-refusal.mjs";

function requireUnlistedWork(document, request) {
  const existing = document.entries.find(
    (entry) => entry.identity === request.identity,
  );
  if (existing) {
    throw new BacklogError(
      `Identity "${request.identity}" is already listed in ` +
        `"## ${existing.list}" at line ${existing.index + 1}. This ` +
        `operation never re-identifies or moves existing work.`,
    );
  }
  requireUnlistedHome(document, request.href);
}

// An entry whose link already spells its identity exactly names nothing
// beyond that link, so there is nothing for the canonical home to confirm:
// this is the ordinary shape for a bounded correction still living at the
// path it was identified by. An entry that claims a name of its own must have
// that name in the canonical home the link points at, read the same way
// adoption reads it — recorded there already, or, for an anchored story that
// has recorded nothing yet, composed from the seed's own document ID and the
// story's anchor. This never asks the link path itself to spell the
// identity: a home whose record matches while its own path has moved on is
// the ordinary case of relocation, not a refusal.
//
// A whole-document home (no anchor, such as a bounded correction's own plan)
// is not opened to check this, matching adoption's own reading of such a
// home: nothing beyond the entry's own claim exists there to confirm or
// dispute an identity, so there is nothing to gain by requiring the file be
// there at all.
function requireNamedHome(backlogDirectory, request) {
  if (!recordsOwnIdentity(request.identity, request.href)) {
    return;
  }
  if (splitHref(request.href).anchor === "") {
    return;
  }
  const home = openHome(backlogDirectory, request.href);
  const named = namedIdentity(home);
  if (named === undefined) {
    throw new BacklogError(
      `${home.key} names no identity of its own, so nothing establishes ` +
        `that it is the canonical home of "${request.identity}". Record ` +
        `the identity there first, or add the entry under the identity the ` +
        `home already names.`,
    );
  }
  if (named !== request.identity) {
    throw new BacklogError(
      `${home.key} names identity "${named}", not "${request.identity}", ` +
        `so it is the canonical home of different work. This operation ` +
        `never lets an entry claim a name its own canonical home does not ` +
        `give it.`,
    );
  }
}

export function addQueueEntry(source, request) {
  const document = parseBacklog(source);
  const line = renderEntry(request);
  requireUnlistedWork(document, request);
  requireNamedHome(request.backlogDirectory, request);

  insertEntryLine(document, queueIndexFor(document, request), line);
  return renderBacklog(document);
}
