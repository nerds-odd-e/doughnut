---
id: SEED-118
status: active
planted: 2026-11-10
scope: story
---

# Export a note with recent quiz questions

<a id="export-a-note-with-recent-quiz-questions"></a>

### Export a note with recent quiz questions

**Identity:** SEED-118#export-a-note-with-recent-quiz-questions
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/089-export-recent-note-questions/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"b7405528f3459dd9286515bb762f773568755563e9d825c926ea74a2888d8129","plan":"35f54b8350824771932fa97d39b58b1dac27b1b35d97ed7f864a6d3ea84d3227"}}
```

**Goal.** A learner can export a note with its recent multiple-choice questions
and their own answers, so they can later share the Markdown with an AI to
analyze the questions and identify ways to improve the note.

**Scope.** Add an unchecked **Include recent multiple-choice questions**
checkbox to **Export Note Data**. When checked, the preview and Markdown available
to copy or download include up to 10 distinct multiple-choice questions belonging
to the exported note, ordered by question creation time descending, then ID
descending. Include each complete stem, its choices, and a separately labeled
**Donut answer key**. For questions the exporting learner answered, include their
latest recorded selected answer (answer time descending, then answer ID
descending). An unanswered question remains included without a learner selection.
Questions without prompts also qualify; selecting questions does not filter on
contested state or an attempt's exclusion state.

The answer lookup belongs to the exporting learner; note visibility does not
grant access to another learner's selections. Preserve existing note-read access,
including ordinary export for a guest, who has no personal selections to attach.
With the checkbox cleared, export the usual note context. Keep the existing
focus/related-note-body token budget; selected questions form a complete appendix
outside that body budget. The story ends at the Markdown export.

Deferred promises: automatic AI analysis or note changes, a separate recall-history
report, historical note-version reconstruction, and additional quiz metadata.

**Key examples.**

- A note has 12 multiple-choice questions. With the checkbox selected, the
  preview, copied, and downloaded Markdown includes the 10 newest by question
  creation time, including unanswered questions and each Donut answer key.
- A note has three multiple-choice questions and the learner answered one.
  The export includes all three questions and shows the learner's selected
  answer for that one question, separately from Donut's answer key.
- A question was created yesterday but presented today, while another was
  created today and never presented → export with inclusion selected → today's
  new question comes first. Equal creation times use descending question IDs.
- The learner answered one question twice; another learner answered it later
  → export → one question entry contains the exporting learner's latest
  selection. Another learner's selection never substitutes for it. A question
  answered only by another learner has no selection for the exporter.
- A readable note has no questions, or is read by a guest → select inclusion
  → its note context still exports; a guest's output contains no personal
  selections. No new login prerequisite is introduced.
- An ordinary preview is loaded → select inclusion → copy/download wait for
  refreshed text; the refreshed preview and both outputs match. Clear inclusion
  → the ordinary context returns. If refresh fails, show the existing API error
  feedback and make the previous text unavailable for export; Refresh retries.

**Refinement decisions — 2026-11-10.** The developer accepted both recommended
answers: newest created note questions with the learner's latest own selection,
and Donut's answer key alongside that selection. The initial checkbox,
ten-question bound, unanswered-question inclusion, and export-only goal remain.

| Candidate | Learner's task and system consequence | Assessment |
| --- | --- | --- |
| Existing export plus manual collection | Collect questions separately and paste them beside the note; uses existing surfaces but risks omissions and requires repeated work | Strongest smaller alternative; does not deliver the selected one-export outcome |
| Opt-in question appendix | Copy or download one artifact with note context and recent questions; compose a learner-scoped appendix at the export boundary | Recommended; matches the selected story |
| Separate personal recall-history report | Review attempts first, then combine that report with note context; introduces another export workflow and makes personal presentations the selection unit | Deferred; the selected story combines recent question content with note context |

Borrowed mechanism: a diagnostic support bundle pairs current source/configuration
with recent observations. Here note context is the source, questions are probes,
and selected answers are observations. An explicitly labeled appendix adapts
that relationship. Its limit: old questions and current note content do not
establish which historical version of the note produced an answer.

Strongest challenge: the newest ten question definitions may be mostly
unanswered. They support question review but can provide little evidence of the
learner's difficulty. Including unanswered questions is already promised; this
finding does not replace that promise with answered attempts. Labeling Donut's
answer key supports inspection without presenting a disputed key as ground truth.

**UI.** Keep the existing Export Note Data task and its shared
preview/copy/download text. Changing the inclusion checkbox
refreshes the preview, with copy/download unavailable while that refresh is
pending, so the output corresponds to the selected option. Keep the existing
token budget's meaning (focus and related note bodies); complete selected
questions form an additional appendix. Explain that relationship beside the
option. A zero-question note can still export its ordinary context. A failed
refresh cannot offer previous text as the current selection's output; retain
the existing Refresh retry and shared API feedback. No layout is prescribed.

**Architecture.** Compose this user-requested export at its existing read
boundary. [ADR 0001, Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md#recall)
distinguishes reusable MCQ content from learner-specific recall prompts and
answers. [ADR 0002, Git-native Portable notebook tree synchronization](../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md#decision)
keeps learning history separate from authored Portable content. This appendix
is a transient export; it does not become authored note content or notebook Git
data. The shared focus-context renderer also serves other AI consumers, so the
export boundary should own this optional composition. Readable note access
does not authorize another learner's answers. No conflict with these Accepted
decisions was found; no new ADR or exception is proposed.

**Investigation evidence.** Read at revision
`439f3fea6888084507d6fe4a022e9b89c911d08b` on 2026-11-10:

- `frontend/src/components/notes/core/NoteExportForm.vue` fetches Markdown on
  open, defaults its note-body token budget to 2,000, and provides manual
  refresh. `JsonExportSection.vue` gives the same returned text to preview,
  copy, and Markdown download and disables export actions while loading.
- `NoteController.getAiContextMarkdown` checks note read access and renders
  current-viewer focus context. `FocusContextMarkdownRenderer` is shared with
  other AI flows. Existing question-generation JSON export and recall-history
  export do not provide this combined artifact.
- `Mcq` stores note, question creation time, choices, solution, and contested
  state. `RecallPrompt` links an MCQ to a memory tracker and optional `Answer`;
  `MemoryTracker.user` identifies the learner. The data model permits multiple
  prompts for reusable question content. Existing data supplies the needed
  fields without a new persistence model.
- `McqController.list` returns note-level MCQs without recency ordering or a
  contested filter, with full note authorization. Personal prompt history is
  separately owner-authorized. Regeneration preserves old question/prompt rows;
  clearing unanswered prompts deletes those prompts while retaining MCQs.
  These readings locate selection rules; they do not prove a future export.
- At 14:22 Singapore, `./scripts/run.sh pnpm frontend:test
  tests/notes/NoteExportForm.spec.ts` passed both existing Chromium tests in
  this workspace: initial preview/download and token-budget refresh. This is
  an existing-export baseline, not proof of the proposed question appendix,
  clipboard contents, or failure recovery. No backend behavior observation
  was run during this investigation.
- At 14:31 Singapore, a disposable edit discarded returned Markdown in
  `NoteExportForm.vue`; the same command failed both tests at their preview
  assertions. The source was restored byte-for-byte. Together with the 14:22
  pass, this establishes discrimination for the existing rendered export
  baseline. It does not establish the new option or learner-history behavior.
