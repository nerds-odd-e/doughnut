# Compact activity and pinned commands

Owned CI failure on published `bb3b9b8916c2418e194c791dd121163b9ac03676`:
[run 39436678082, attempt 1](https://github.com/nerds-odd-e/doughnut/actions/runs/39436678082),
Frontend Unit Tests (1/2), job `118675398935`, observed 2026-10-20.
`NotebookWorkspaceHeader.dictation.spec.ts:241` failed bar containment with open
Assimilate, recording Stop, and More at 320px; the other pinned-state assertions passed.

The full menu caption on the collapsed Assimilate control consumed too much
width. A compact `NavigationItem` variant hides only the optional visible caption
for the collapsed control and retains 40px height, icon, badge, progress,
accessible identity, Resume styling, direct action and independent menu opener.
Expanded menu, Account and home consumers keep the default presentation.

At the clean published base, this exact command failed before the repair and
passed after it, without assertion changes (1 passed, 4 skipped):

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceHeader.dictation.spec.ts -t 'fits an open Assimilate'
```

Additional proof passed 157 tests across 25 files, all terminal exit 0:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/layouts/NotebookWorkspaceHeader.dictation.spec.ts tests/layouts/NotebookWorkspaceHeader.spec.ts tests/layouts/NotebookWorkspaceAssimilation.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/NoteToolbar.voiceInputOverflow.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/pages/HomePage.welcome.spec.ts tests/pages/HomePage.theme.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
```

Real CSS/browser geometry observes all pinned controls and ellipsis inside the
bar. Workspace assimilation observes compact direct next and count updates;
menu/home tests retain labels, counts, progress, Resume and navigation.
Native software-keyboard behavior remains unverified under the owner's waiver.

Slice 5 was paused and its candidate preserved with the sanctioned CI stash
record `/var/folders/65/16p4k5qj42qg7l46k2j0nhj40000gn/T/dough-ci-repair-stash-OpiaIW/record.json`,
OID `0cd6cd115f2f2bb3bcd4c9f43d3b39eb90717bbd`. All writers and the owned held
stack were quiescent first; implementation touched no stashed slice-5 files.
Repair delivery and exact restoration precede continued slice-5 acceptance.

Repair `ddd19a5e330d0f21bc52f50cf2f9f482cc9b0eee` was accepted on the execution
branch and registered on the retained observer. Exact sanctioned restoration
returned `resumed`, `applied: true`, and dropped only the recorded owned entry.
Slice 5 resumed with combined geometry/typechecks and repeated visual acceptance;
earlier partial 1280/768 observations are not reused for the changed allocation.
