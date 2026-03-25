# CLI interactive UI — ghost input chrome after terminal shrink

## Problem

When the user **narrows the terminal** (fewer columns), the interactive TTY can show **leftover input-box chrome** above the current live block: partial top/side borders and repeated-looking prompt content, stacked vertically, instead of a single clean input box.

The active input box at the bottom may be correct; the issue is **stale rows** from earlier paints that were not fully erased in the viewport.

## Context (product, not implementation detail)

Interactive mode uses Ink with incremental redraws. Shrinking the window changes how many rows the live region needs; if the runtime does not clear or repaint the full vertical span that a previous frame used, fragments of the old frame can remain visible on screen.

Earlier behavior that performed a **full-screen clear** on resize tended to push such debris into **scrollback** rather than leaving it in the visible area, which masked the issue for users who do not scroll up.

This note is only to track the **user-visible defect** until it is addressed.
