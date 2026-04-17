import type { BookBlockFull } from "@generated/doughnut-backend-api"

/**
 * The last direct-content locator in a block, shared by PDF and EPUB readers for
 * reading-panel anchoring and snap-back. Returns `null` when the block has only
 * its start anchor (no direct content locators follow).
 */
export function lastDirectContentLocator(
  block: BookBlockFull
): BookBlockFull["contentLocators"][number] | null {
  if (block.contentLocators.length <= 1) return null
  return block.contentLocators[block.contentLocators.length - 1]!
}
