---
id: SEED-048
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture
trigger_when: selected from the product backlog
scope: large
---

# SEED-048: Close the local AI notebook effort

## Why This Matters

The effort to support local AI IDEs, images, and other unknown file types in
Donut notebooks is mostly concluded. The product owner and maintainers need a
final review and cleanup before treating the related work as complete.

## Story Decomposition

<a id="story-1"></a>

### Review and close the local AI notebook effort

**Identity:** SEED-048#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** The product owner and maintainers can close this effort with a clear
assessment of its architecture, architectural process, and feature coverage,
and with its related story seeds cleaned up.

**Scope for future refinement and execution:** Use all changes from the commit
that last changed the related near-future direction in the product backlog
through the time of the review as context. Then:

1. Thoroughly review the structural impact of the effort. Identify improvements
   needed for a healthy, cohesive architecture that maps directly to the model.
2. Review the lightweight architectural process guided by Open Dough, especially
   plan instructions and the North Star. Consult relevant Claude Code AI thread
   history where useful, and produce feedback for improving the Open Dough
   architectural process.
3. Review the retained features as organized, documented system behavior rather
   than as ad hoc stories. Assess high-level coverage through E2E tests and
   lower-level coverage through unit tests; identify improvements.
4. Clean up stories in seeds related to these features. Keep only extremely
   concise, urgent and important work that should be done soon; drop deferred
   stories, aiming to delete emptied related seeds. Also list seeds that have
   not been looked at for more than 15 days for the owner's keep-or-delete
   decision.

## When to Surface

Third priority in the product backlog.

## Breadcrumbs

- Owner request, 2026-09-27. This seed records the requested review and cleanup
  for future refinement and execution; no investigation has been done yet.
