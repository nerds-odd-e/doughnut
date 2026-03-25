/**
 * Ink clears focus on Esc before child `useInput` runs. While the live column has only
 * one `useFocus` region at a time, restore focus so the next key still routes correctly.
 *
 * **Phase 5:** delete this file and remove the `useLayoutEffect` refocus blocks (and their
 * imports of this constant) from **`CommandLineLivePanel`** and **`ConfirmLivePanel`** —
 * once the list `Select` is a peer focus target, Esc / focus must follow the multi-region
 * model instead of snapping focus back.
 */
export const INK_LIVE_SOLE_FOCUS_REGION_REFLEX = true
