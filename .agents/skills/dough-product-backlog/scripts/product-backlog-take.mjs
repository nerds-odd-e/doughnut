// Moves one explicitly identified work item out of "## Backlog list" and to
// the end of "## Taken" in a single backlog update, carrying the identity it
// already has and the plan link the caller selected. An item already in
// "## Taken" is a resume: it keeps its place in that list and only gains a
// plan link it was missing.
//
// This applies a claim the caller has already decided on. It does not decide
// whether execution may start, who may execute the work, or where the caller
// commits the claim, and it gives no run exclusive ownership of an item.

import {
  parseBacklog,
  renderBacklog,
  renderEntry,
  takenHeading,
} from "./product-backlog-document.mjs";
import {
  appendIndex,
  findEntry,
  moveEntryLine,
} from "./product-backlog-placement.mjs";
import { planLabel, requireResolvedPlan } from "./product-backlog-plan.mjs";
import { BacklogError } from "./product-backlog-refusal.mjs";

// The plan link the taken entry carries, from the caller's explicit choice.
// A quick story and a bounded correction take none: a correction's canonical
// home already is its plan, so a second link would name it twice.
function resolvePlan(entry, request) {
  if (request.plan === undefined) {
    if (entry.plan) {
      throw new BacklogError(
        `"${entry.identity}" already links the plan ` +
          `"${entry.plan.target}", so --no-plan contradicts the backlog. ` +
          `Supply --plan ${entry.plan.target} to keep that link.`,
      );
    }
    return undefined;
  }

  const target = request.plan;
  if (entry.plan && entry.plan.target !== target) {
    throw new BacklogError(
      `"${entry.identity}" already links the plan "${entry.plan.target}", ` +
        `not "${target}". Taking work never repoints a recorded link; ` +
        `refreshing the reference is a separate decision.`,
    );
  }
  if (target === entry.href) {
    throw new BacklogError(
      `The plan "${target}" is already the canonical home of ` +
        `"${entry.identity}", which needs no duplicate plan link. Take it ` +
        `with --no-plan.`,
    );
  }
  requireResolvedPlan(
    request.backlogDirectory,
    target,
    `Take the work once its plan is resolved, or take a quick story with ` +
      `--no-plan.`,
  );
  return { label: planLabel, target };
}

// Applies the claim to one backlog document and returns the backlog to
// publish, alongside which of the three outcomes it reached: the work was
// "taken" into "## Taken", or resumed there and "linked" to its plan, or
// resumed with the entry already as it should be and so "unchanged".
export function takeEntry(source, request) {
  const document = parseBacklog(source);
  const entry = findEntry(
    document,
    request.identity,
    `This operation never writes an absent entry: queue the work first, or ` +
      `supply the identity the backlog carries.`,
  );
  const plan = resolvePlan(entry, request);
  const line = renderEntry({
    identity: entry.identity,
    title: entry.title,
    href: entry.href,
    plan,
  });

  if (entry.list === takenHeading) {
    // Resume: the entry keeps the place it already holds in "## Taken", and
    // gains only the plan link it was missing.
    const result =
      document.lines[entry.index] === line ? "unchanged" : "linked";
    document.lines[entry.index] = line;
    return { source: renderBacklog(document), entry, result };
  }

  moveEntryLine(
    document,
    entry.index,
    appendIndex(document, document.taken),
    line,
  );
  return { source: renderBacklog(document), entry, result: "taken" };
}
