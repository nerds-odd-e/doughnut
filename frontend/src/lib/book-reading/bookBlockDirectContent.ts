import type { BookBlockFull } from "@generated/donut-backend-api"

/**
 * The last direct-content locator in a block, shared by PDF and EPUB readers for
 * the reading panel's target and anchoring. Returns `null` when the block has no
 * text of its own (see `hasNoTextOfItsOwn`) or no locator at all.
 */
export function lastDirectContentLocator(
  block: BookBlockFull
): BookBlockFull["contentLocators"][number] | null {
  if (hasNoTextOfItsOwn(block)) return null
  return block.contentLocators.at(-1) ?? null
}

/**
 * A block with nothing to read of its own: its only locator is a PDF heading,
 * or an EPUB start-only entry (its only content block is a `beginning_anchor`).
 */
export function hasNoTextOfItsOwn(block: BookBlockFull): boolean {
  if (block.contentLocators.length !== 1) return false
  if (block.contentLocators[0]!.type === "PdfLocator_Full") return true
  return (
    block.contentBlocks.length === 1 &&
    block.contentBlocks[0]!.type === "beginning_anchor"
  )
}
