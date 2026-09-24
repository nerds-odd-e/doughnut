---
id: SEED-040
status: dormant
planted: 2026-09-24
planted_during: refinement of SEED-035 story 3 (local image display)
trigger_when: now; queued first by the owner as a bug
scope: small
---

# SEED-040: Uploaded note images stay private to their notebook's readers

## Why This Matters

Anyone can fetch uploaded note images from private notebooks. The finding came
up while SEED-035 story 5 (existing-image conversion) was being refined, and the
owner queued it first on 2026-09-24.

- **Expected:** An uploaded note image is readable only by users who may read
  the note's notebook, the same rule as the notebook file download.
- **Actual:** `GET /attachments/images/{id}/{fileName}`
  (`backend/src/main/java/com/odde/donut/controllers/AttachmentController.java`)
  returns the image with no authorization check, ignores `fileName`, and image
  IDs are sequential, so private images can be listed by counting IDs.

## Alternatives and Decision

- **Wait for SEED-035 story 5 conversion:** rejected; conversion keeps legacy
  rows and this endpoint, so the exposure would remain.
- **Recommended:** check notebook read authorization on the existing endpoint.

## Story Decomposition

<a id="story-1"></a>

### Keep uploaded note images private to readers of their notebook
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

- **Identity:** SEED-040#story-1
- **Kind:** Bug, queued first priority by the owner (2026-09-24).
- **For / why:** Owners of private notebooks, whose uploaded pictures are
  currently public to anyone who guesses an ID.
- **Evaluation:** A user who cannot read the notebook requests one of its note
  images and is refused; a reader still sees it on the note page and in recall.
- **Key examples:**
  1. Private notebook note with image 42; another logged-in user requests
     `/attachments/images/42/x.png` → refused, no bytes.
  2. The same request without login → refused.
  3. The owner, and a Bazaar reader of a shared notebook, still see the image.
- **Effort hypothesis:** S, medium confidence; route through dough-bug-fixing.
- **Depends on:** none.
- **Safe stopping point:** Private images are no longer public; display for
  readers is unchanged.
- **Open decision for refinement:** Whether any current feature relies on the
  public URL without a login, such as images for a logged-out Bazaar visitor or
  images in content copied outside Donut. If one does, decide whether that use
  keeps working.

## When to Surface

Now; first in the product backlog.

## Breadcrumbs

- [SEED-035 story 5](SEED-035-ai-workspace-supporting-files.md#story-5) records
  the original finding.
- [SEED-035 story 3](SEED-035-ai-workspace-supporting-files.md#story-3) adds a
  new picture-reading path that must use the same read authorization.
