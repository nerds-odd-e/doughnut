---
id: SEED-045
status: dormant
planted: 2026-09-26
planted_during: owner-requested manual testing of the near-future direction's file and Git work
trigger_when: file and Git integration work for the near-future direction is nearly delivered
scope: small
---

# SEED-045: Manual test of notebook files and Git integration

## Why This Matters

The near-future direction (AI guidance Markdown, images, PDFs and other
non-Markdown files as notebook folder files, working in parallel from local Git
checkouts) is almost delivered, but no one has manually tested the combined
behavior since the direction was set on 2026-09-20.

## Story Decomposition

<a id="story-1"></a>

### Manual test of notebook files and Git integration

**Identity:** SEED-045#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** The owner learns, from a two-hour exploratory session, where the
file and Git integration behavior delivered since 2026-09-20 departs from its
promises, including whether publish, clone and pull of a notebook holding about
100 MB of files each finish within 5 seconds.

**Scope:** web file upload, browsing, download, deletion, pictures, Books,
folder operations with files, name rules, and `donut notebook` clone, pull and
publish with Git LFS. Excludes the in-progress link-rewrite story
(SEED-035#story-25). Testing only: no diagnosis or repair.

**Known expectations:** a notebook with about 100 MB of files publishes in at
most 5 seconds, and clones or pulls in at most 5 seconds; slower is a finding.
