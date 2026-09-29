---
id: SEED-058
status: dormant
planted: 2026-09-29
planted_during: owner request to suppress the empty-content rich Markdown warning
trigger_when: improving the editing experience for empty notes
scope: small
---

# SEED-058: Handle the empty-content Markdown discrepancy quietly

## Why This Matters

For an empty note, the rich Markdown editor and Donut's Markdown rules can
disagree about the regenerated Markdown. Accepting that regenerated form can
trigger a content change even though the learner changed nothing. The existing
behavior protects against this, but displays a warning the owner wants hidden.

## Alternatives and Decision

Keep the current handling of the discrepancy and suppress only the warning
for empty note content. Accepting the regenerated Markdown would alter behavior
and reintroduce false changes. The owner explicitly approves this limited
exception to the convention of keeping behavior explicit to the user.

## Story Decomposition

One story changes the warning presentation for this existing empty-content case.

<a id="story-1"></a>

### Hide the empty-content rich Markdown mismatch warning

**Identity:** SEED-058#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["An execution approach has not been selected; this request captures backlog work only."],"basis":{"document":"75eb9a86cc482b886a1a6f6c2d0565a4d7cdbb0af4a5aa5ae50a50d8e7fa57cc"}}
```

**Goal**

Learners opening an empty note see no warning for the known rich Markdown
discrepancy, and do not trigger a content change merely by opening the editor.

**Scope**

- Suppress the warning caused by the discrepancy between the rich editor's
  regenerated Markdown and Donut's Markdown rules when note content is empty.
- Preserve all existing handling of that discrepancy, including the protection
  against accepting regenerated Markdown as a change when nothing was edited.
  Preserve any existing editing restrictions; change only warning visibility.
- Keep warnings for other cases, including non-empty content the rich editor
  cannot preserve and invalid frontmatter.
- Repairing the conversion discrepancy or changing Markdown acceptance and
  change-detection rules is deferred.

**Key examples**

- An empty note encounters the known discrepancy when opened in the rich editor
  → no warning is shown, the existing handling remains in effect, and opening
  the editor alone does not emit or save a content change.
- A non-empty note contains content the rich editor cannot preserve → the
  existing warning and protective behavior remain.

- **For / why:** Learners avoid a distracting warning on an empty note while
  retaining protection against false edits.
- **Evaluation:** Open an empty note that triggers the discrepancy and observe
  no warning or spurious content change, with existing editor behavior preserved.
- **Effort hypothesis:** S (30–60 minutes), medium confidence; distinguish warning
  visibility from the existing protective state before planning.
- **Depends on:** No queued prerequisite is established; independent of SEED-057.
- **Safe stopping point:** The empty-content warning is hidden while discrepancy
  handling and warnings for other content remain intact.

## Ordering and Scope Reduction

Append after the relationship sentence reduction story, preserving earlier
priorities. This is a warning-only change for the empty-content case.

## Approved Product Exception

Terry Yin explicitly requested on 2026-09-29 that the empty-content discrepancy
remain handled as it is today, without showing its warning. This is an approved
exception to keeping such behavior explicit, limited to this case.

## When to Surface

When selecting empty-note editing and warning presentation improvements.

## Breadcrumbs

- Owner request on 2026-09-29: keep the current behavior for the empty-content
  discrepancy between rich Markdown and Donut's Markdown rules, but hide the
  warning; the owner explicitly accepts this exception to the explicitness rule.
- Current warning and protective state: `frontend/src/components/form/RichMarkdownEditor.vue`.
- Existing non-empty preservation examples: `frontend/tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts`.
