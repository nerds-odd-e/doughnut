# Menu spacing candidate — 2026-10-20

Identity: `SEED-080#story-1`, slice 6. Candidate over published
`c8523d0428e88b1da203a6f935decf7c1a888b26` in the established execution checkout.

## Implementation and consumer trace

`DonutMenu.vue` keeps the shared dropdown's adaptive geometry and scroll owner.
Panel padding is 12 px on phones and 16 px above the small breakpoint. Its
navigation cells gain vertical padding and 12 px row gaps; narrower cell side
padding preserves full captions at 320 px. Search starts 16 px below navigation;
the footer gains 24 px separation. The style is scoped to popup navigation,
including Account, so the compact page-bar activity and shared NavigationItem
callers keep their existing geometry.

Reading `rg -n 'DonutMenu|menu-navigation' frontend/src -g '*.vue' -g '*.ts'`
found GlobalBar as the menu consumer and the new local selector only in DonutMenu.
Affected rendered output reaches AccountMenuItem, both search launchers, the
footer and the parent page bar. The full toolbar suite and workspace/header
overflow suites below cover those consumers; shared dropdown or routing contracts
are unchanged. No component or API generation trigger applies.

## Automated observation

Command:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/layouts/NotebookWorkspaceHeader.spec.ts tests/layouts/NotebookWorkspaceHeader.dictation.spec.ts tests/layouts/NotebookWorkspaceContext.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/NoteToolbar.voiceInputOverflow.spec.ts
```

Terminal result: 171 tests passed in 24 files, exit 0, 7.09 seconds reported by
Vitest. This includes actual coarse-pointer launcher tests, search dialog and
shortcut journeys, compact activity/Resume, Account destinations, signed-out
entry, active Assimilation, pinned dictation/open controls and measured overflow.

`DonutMenu.layout.spec.ts` mounts the real GlobalBar with a provided signed-in
user, production router, real dropdown and production CSS; only backend menu
data is mocked by the existing setup. At 320/390/768/1280 px:

- `keeps the account actions inside the panel` expands Account for a builder
  admin and observes every action rectangle within the normal-height panel.
- `spaces navigation and separates the footer` observes at least 12 px panel
  inset and row separation, every caption's text range within its cell, and
  at least 24 px between the search launcher and footer text.
- `fits a short window and reaches expanded commands and footer by keyboard`
  uses a 260 px-high viewport and expanded admin Account. The panel fits the
  viewport and overflows vertically. Real Tab traversal observes focus on each
  Account destination, Logout, both search launchers, Donut by and the brand
  link; browser scrolling brings every focused rectangle wholly into the panel.

An initial focused run failed only because the expected Recent target was its
inner span while actual keyboard focus correctly landed on the enclosing link.
Selecting that existing link fixed the observation; the complete run above
passed all 14 layout cases. No product workaround was introduced.

Both current-candidate typechecks reached terminal exit 0:

```sh
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

## Remaining observation

The coordinator owns bounded Chrome visual acceptance of spacing, footer
separation and combined controls at the four widths, including expanded Account
in a short window. It remains pending at this implementation handoff. Native
iPad software-keyboard behavior remains unverified under the owner's acceptance
decision; no further owner testing is required. No Cypress or app stack was
started by this implementation agent; the coordinator's held stack was preserved.

## Coordinator visual observation

On 2026-10-20, Chrome tab 137234654 used the isolated held app at
`http://127.0.0.1:50012/n1`, signed in as seeded `old_learner`. The real
"Command layout" note in "Layout check" had Assimilation open. At 768x700,
320x700, 390x700 and 1280x700, screenshots showed the expanded Account menu,
legible separate navigation captions, comfortable row spacing, search targets
and extra footer separation. The header retained its compact activity,
independent opener, separators and pinned active operation. No panel clipping
or command overlap was observed.

At 320x260 with Account expanded, eight real Tab presses from Account reached
Recent, Settings, Manage Access Tokens, Logout, Open search, Search, Donut by
and the brand link in order. Read-only DOM rectangles showed every focused
target within the viewport; the final screenshot showed the footer and search
after automatic panel scrolling. Pointer scrolling returned to navigation and
then reached the footer again. The panel stayed inside the short viewport.
The yellow Testability control belongs to the held E2E app.

Visual acceptance is complete. Screenshots/AX results are in the execution
conversation; this is desktop Chrome viewport observation, not native iPad
software-keyboard proof. The owner waived that observation as a dependency.
