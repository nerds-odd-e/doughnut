/** Match Tailwind `md` and `BookReadingBookLayout` aside visibility. */
export const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768

/**
 * Whether the book layout aside should be open on first paint. Must match the md+ branch so
 * the main pane has final width before `EpubBookViewer` mounts (avoids a late resize).
 */
export function bookLayoutAsideInitiallyOpen(innerWidth: number): boolean {
  return innerWidth >= BOOK_READING_LAYOUT_BREAKPOINT_PX
}
