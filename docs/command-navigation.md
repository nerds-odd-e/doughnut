# Command navigation

The page bar shows the current activity's icon and applicable count. Ordinary
context opens navigation; Resume and Assimilate perform their existing direct
actions. The adjacent Donut menu control opens navigation independently.
Signed-out visitors reach Login through that menu.

`useNavigationItems` owns activity identity, precedence and counts. Eligible
Resume takes precedence over page context: a paused cursor with loaded trackers,
or progress beyond the first item away from recall with work remaining. Resume
returns to the preserved question and progress through `useResumeRecall`.

Assimilate identifies a learning presentation visible for the current note.
Both note assimilation and a property's learning panel register their actual
presentation lifetime through `useAssimilationView`. Property entry can be
direct or through the next-unit sequence. Hidden details, another note,
missing/read-only properties and panels without learning retain ordinary
context. Route identity alone does not imply learning activity. Named note
and property locations follow [ADR 0005](adrs/0005-web-routes-accepted.md).

Both menu search buttons and Ctrl/Cmd+F open one common global search dialog.
Touch activation prepares focus synchronously before the dialog opens. The
shared search component owns history, scopes, results and result navigation;
selecting the current note closes the dialog and exposes that note.

`NoteToolbar` owns command state, order and measured adaptive overflow. Its
commands align to the right of their allocated space; busy/open controls and
More stay reachable. The page bar separates navigation, commands and content.
The popup has its own navigation spacing, search separation and footer gap;
its scroll area keeps expanded Account actions, search and footer reachable
in narrow or short windows. Compact activity uses a separate presentation
variant without changing other navigation-item callers.
