import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type {
  BookLayoutReorganizationSuggestion,
  BookMutationResponseFull,
} from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"

const BOOK_LAYOUT_APPLY_LOADING_MESSAGE = "Applying book layout changes…"

export async function applyBookLayoutDepths(
  notebookId: number,
  layout: BookLayoutReorganizationSuggestion
): Promise<BookMutationResponseFull | undefined> {
  const { data, error } = await apiCallWithLoading(
    () =>
      NotebookBooksController.applyBookLayoutReorganization({
        path: { notebook: notebookId },
        body: layout,
      }),
    { blockUi: true, message: BOOK_LAYOUT_APPLY_LOADING_MESSAGE }
  )
  return !error && data ? data : undefined
}
