---
id: SEED-051
status: dormant
planted: 2026-09-28
planted_during: owner request to retire semantic search after a brief alternatives review
trigger_when: prioritizing non-urgent feature cleanup and reducing ongoing operating cost
scope: medium
---

# SEED-051: Retire costly, underused note semantic search

## Why This Matters

The owner reports that hosted note semantic search performs poorly and provides
little practical value for its recurring embedding work and maintenance. Users
can now check out notebooks locally and work in modern IDEs. After the brief
alternatives review, the owner confirmed complete retirement without a
replacement.

## Decision

The owner's acceptance rule is deletion: remove the feature's code, tests,
documentation, production data, and data structures completely. Do not replace
them with disabled implementations, tests asserting the feature's absence, or
documentation saying it was removed. Git supplies the history.

The alternatives decision is closed. No further rescue investigation or
replacement implementation is included. This seed is temporary preparation
input: remove it and other spent execution records at wrap-up instead of
turning them into permanent historical or research documentation.

## Story Decomposition

<a id="story-2"></a>

### Web search shows each response without accumulating earlier ones

**Identity:** SEED-051#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/010-search-shows-each-response/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"080610786fefe842f9ca473ed6c6c99eafe2d807a410ded556f960ca3c0c764d","plan":"a7e4bddc90fc0c3c1ed02fcff01854fd65675ae7f318bb13fa1b581572ba6281"}}
```

**Slice plan:** [Web search shows each response without accumulating earlier ones](../slice-plans/010-search-shows-each-response/PLAN.md)

**Goal:** Donut users searching for notes, folders, and notebooks on the web see the
results of their current search, not a union with hits kept from earlier responses.
This bounded retrospective correction removes the multi-response merging that
existed only to combine literal and semantic batches.

**Scope:** Frontend search results caching and ordering only; server search,
MCP, and ranking rules are unchanged.
