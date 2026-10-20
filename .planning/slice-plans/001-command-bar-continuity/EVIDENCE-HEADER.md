# Command group alignment evidence

**Identity:** SEED-080#story-1, slice 5
**Observed:** 2026-10-20, implementation candidate over
`bb3b9b8916c2418e194c791dd121163b9ac03676`

## Change and consumer trace

The existing measured `NoteToolbar` button row now justifies its visible
commands to the right. `NoteToolbarMoreOptions` keeps its original row-width
measurement, ordered overflow computation, pinned-action state and ellipsis.
Its page-bar teleport, note-owned panel, voice input and editing state stay
with the same owners. The existing `GlobalBar` allocates a shrinkable central
area and supplies a theme-neutral vertical divider after navigation and a
one-pixel bottom separator.

The preparation observation in the source context recorded a 352.5 px command
gap at 1280 px and discriminated the same row alignment change. Current proof
uses the expanded activity controls, including actual visible Assimilate
presentation, rather than relying on that older control width.

`notebookWorkspaceHeaderTestSupport.commandRightGap` compares the final
visible command's right edge with the actual allocated toolbar's right edge.
`NotebookWorkspaceHeader.spec.ts` checks that gap within one pixel at
320/390/768/1280 px, alongside bar containment and the existing command order.
Its open-panel journey clicks the actual Assimilate action and resizes through
390/768/1280/320 px, observing the visible settings, pressed pinned action,
Assimilate activity, right edge and retained order. It supplies no presentation
result through store setup.

The same allocation serves the notebook layout and book reader through their
existing page-bar targets. Whole-toolbar and workspace-context coverage retains
menu access, note editing, read-only controls, undo, notebook context and drawer
geometry. Existing overflow and voice-input suites retain startup, recording,
stop/retry and simultaneous open Assimilate/voice/ellipsis coverage.

## Observed results

Exact command from the execution checkout, with headless Chromium:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/layouts/NotebookWorkspaceHeader.spec.ts tests/layouts/NotebookWorkspaceHeader.dictation.spec.ts tests/layouts/NotebookWorkspaceContext.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/NoteToolbar.voiceInputOverflow.spec.ts
```

All **162 tests in 24 files passed**, terminal exit 0. The active-panel header
test emitted ResizeObserver-loop notifications; the same notification was
already retained in the original and scratch preparation runs. It did not
prevent the final measured layout and interaction assertions from passing.

Both frontend typechecks passed against the final frontend candidate, exit 0:

```bash
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

The first affected-suite run exposed a real separator allocation issue:
adding a physical bottom border inside the fixed 55 px bar put the mobile
dropdown at 54 px, overlapping its bottom by one pixel. The existing
`DonutMenu.layout` containment assertion failed (`54 >= 55`). Using an inset
bottom separator preserves the bar's geometry; the final complete affected
run above passed that same dropdown containment assertion. The failed run's
other 151 cases passed.

No API contract or component inventory change occurred, so neither API nor
component-declaration generation applies. No formatter, commit, push or app
stack was run by the implementation agent. Changes remain uncommitted for
independent refactoring and coordinator delivery.

## Implementation handoff — before coordinator observation

Bounded visual observation of alignment and both separators at
320/390/768/1280 px remains with the coordinator. Automated geometry does not
establish their visual subtlety. Native iPad software-keyboard behavior remains
unverified under the owner's acceptance decision and is unrelated to this
alignment change.

## Coordinator visual observation — 2026-10-20

Budget: bounded header observation; preparation/fixture setup, breadth across all
four widths, then depth on active-panel retention and phone overflow, with reserve
for containment surprises. The original partial 1280/768 observation preceded the
CI compact-control repair and was discarded for acceptance.

Repeated on the combined candidate over `ddd19a5e330d0f21bc52f50cf2f9f482cc9b0eee`:
`CURSOR_DEV=true nix develop -c pnpm e2e:hold` held the isolated unpaid app at
`http://127.0.0.1:50012` (session `74278`). Native Chrome tab `137234654`, production
dark CSS, seeded `old_learner`, notebook “Layout check”, note “Command layout”,
all created/signed in through the real UI. Opened the actual note Assimilate
panel before resizing. CUA screenshots/AX observations cover 1280×700, 320×700,
390×700 and 768×700; hid contents at phone width to inspect the retained panel.

Commands visibly align at the right of their allocated header area. The open
Assimilate action stays pinned, More/ellipsis is reachable on phones, and the
panel remains active across resizing. Navigation/group and lower content
separators are subtle but visible. No header overlap or clipping was observed.
Automation owns recording/startup/busy voice cases; this manual observation did
not request microphone access. It is desktop viewport testing, not native iPad
or software-keyboard evidence. Native input remains unverified under owner waiver.

## Combined candidate after CI repair

On 2026-10-20, execution resumed over the published compact-navigation repair
`ddd19a5e330d0f21bc52f50cf2f9f482cc9b0eee`. Its compact current-activity
presentation hides the optional caption and keeps the existing icon, badge,
progress and accessible action. That changes the toolbar's available width,
so the earlier candidate's layout proof was rerun rather than reused.

The exact affected-suite command and both typechecks above passed again on
the restored slice-5 candidate combined with that repair: **162 tests in 24
files**, and each command reached terminal exit 0. The active-panel geometry
and simultaneous open Assimilate/recording Stop/ellipsis journey passed.
The retained ResizeObserver notifications were also observed in this run.
No additional implementation edit was needed. The coordinator's repeated visual
observation above completes this candidate's header visual acceptance.
