// What an entry's active plan link says and where it has to point. Claiming
// work writes that link and refreshing a reference repoints it, so both settle
// the question here rather than each spelling out what a plan path means.
//
// This owns no judgment about a plan's content: a link this accepts can still
// be refused by an operation that needs more of the document than its path.

import { resolve } from "node:path";
import { readFile } from "./product-backlog-store.mjs";

// How the established backlog spells an active plan link.
export const planLabel = "plan";

// The check is mechanical and existence-only: the target must resolve to a
// file relative to the backlog's own directory. `hint` says what the caller of
// that particular operation can do about a plan that is not there, because
// claiming work and repointing an established link differ in that.
export function requireResolvedPlan(backlogDirectory, target, hint) {
  readFile(
    resolve(backlogDirectory, target),
    `Unresolved plan: ${target} is not there, relative to the backlog. ${hint}`,
  );
}
