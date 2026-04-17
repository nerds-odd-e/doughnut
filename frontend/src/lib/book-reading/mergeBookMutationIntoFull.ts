import type {
  BookBlockFull,
  BookFull,
  BookMutationResponse,
  EpubLocator,
  PdfLocator,
} from "@generated/doughnut-backend-api"

function contentLocatorsAfterMutation(
  rowLocators: Array<EpubLocator | PdfLocator> | undefined,
  prev: BookBlockFull
): BookBlockFull["contentLocators"] {
  if (!rowLocators) {
    return prev.contentLocators
  }
  return rowLocators.map((loc) =>
    loc.type === "epub"
      ? {
          href: loc.href,
          fragment: loc.fragment,
          type: "EpubLocator_Full" as const,
        }
      : {
          pageIndex: loc.pageIndex,
          bbox: loc.bbox,
          contentBlockId: loc.contentBlockId,
          derivedTitle: loc.derivedTitle,
          type: "PdfLocator_Full" as const,
        }
  )
}

export function mergeBookMutationIntoFull(
  previous: BookFull,
  mutation: BookMutationResponse
): BookFull {
  const prevById = new Map(previous.blocks.map((b) => [b.id, b]))
  return {
    ...previous,
    id: mutation.id,
    bookName: mutation.bookName,
    format: mutation.format,
    createdAt: mutation.createdAt,
    updatedAt: mutation.updatedAt,
    notebookId: mutation.notebookId,
    blocks: mutation.blocks.map((row) => {
      const prev = prevById.get(row.id)
      if (!prev) {
        throw new Error(`mergeBookMutationIntoFull: unknown block id ${row.id}`)
      }
      return {
        ...prev,
        id: row.id,
        depth: row.depth,
        title: row.title,
        contentLocators: contentLocatorsAfterMutation(
          row.contentLocators,
          prev
        ),
      }
    }),
  }
}
