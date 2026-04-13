import type {
  BookFull,
  BookMutationResponse,
} from "@generated/doughnut-backend-api"

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
        allBboxes: row.allBboxes ?? prev.allBboxes,
      }
    }),
  }
}
