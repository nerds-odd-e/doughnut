# Keep the recall queue across the same half-day

**Status:** done.

Opening Recall, or returning to it in the same half-day, keeps the already-shown unanswered prompt and the in-progress queue. Remount stays for a new due-list load only (real window rollover, “load more from next N days,” `dueRecallsRefreshNonce`).

First visit uses menu/DB order. There is no first-visit session shuffle.

Origin: split from [001-morning-cognitive-index](../001-morning-cognitive-index/PLAN.md) slice 14.6 (`48763b341d`). Do not append more slices to 001 for this queue bug.
