import type {
  BookBlockFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"

export function asPdfLocator(
  loc: BookBlockFull["contentLocators"][number]
): PdfLocatorFull | null {
  const tag = loc.type as string
  if (tag === "PdfLocator_Full" || tag === "pdf") {
    return loc as PdfLocatorFull
  }
  return null
}

export function pdfLocatorsFromBlock(block: BookBlockFull): PdfLocatorFull[] {
  return block.contentLocators
    .map(asPdfLocator)
    .filter((x): x is PdfLocatorFull => x !== null)
}
