import type {
  BookBlockFull,
  ContentLocatorFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"

export function asPdfLocator(loc: ContentLocatorFull): PdfLocatorFull | null {
  if (loc.type !== "PdfLocator_Full") {
    return null
  }
  return loc as PdfLocatorFull
}

export function pdfLocatorsFromBlock(block: BookBlockFull): PdfLocatorFull[] {
  return block.contentLocators
    .map(asPdfLocator)
    .filter((x): x is PdfLocatorFull => x !== null)
}
