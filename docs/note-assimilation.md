# Note assimilation

Note-level assimilation shows the note title before its actions. When the note
column has the full reading width (48rem), the panel sits between the title and
the note content: Understanding is the default mode, with one primary
**Assimilate understanding** action and a secondary **Skip from sequence…**, or
**Return to sequence** for a skipped note. Spelling (**Verify spelling**) and
Commissioned (**Add to sessions**) are optional modes, each with its icon, a
short description and its own action; an active mode shows its tracker link and
next recall date instead.

When the note column is narrower, as on a phone, an iPad in portrait or a
window narrowed by the notebook sidebar, the note reads first: title,
properties and content come before a sticky bottom bar that holds the
Understanding actions and an **Optional modes** popover opening upward.
`NoteShow` measures its note column and renders one placement at a time; both
use the note's shared tracker actions.

After Understanding succeeds, the panel shows a checkmark and **Understanding
added to recall** with **Next item** as its primary action, and optional modes
stay available. The result belongs to the current note visit: it survives
optional additions and presentation changes and resets when another note
opens. Only Next item requests the next unit, through the named note and
property locations of [ADR 0005](adrs/0005-web-routes-accepted.md); when none
remains it shows **No more notes to assimilate**, closes the presentation and
keeps the note. Assimilating from a property row still advances on its own.

**Skip from sequence…** confirms that only the note's Understanding entry stays
out of the assimilation sequence until **Return to sequence**; its property
entries and optional modes are unaffected, so a property of the same note may
come next. A property row's skip confirms leaving that property out.
Assimilating Understanding clears the note's skip; adding an optional mode
keeps it. Spelling is added only after the title or an alias is verified from
memory, with the note hidden while verifying.
