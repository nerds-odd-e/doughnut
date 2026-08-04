import type { Notebook } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageView from "@/pages/NotebookPageView.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { wrapSdkError } from "@tests/helpers"
import { beforeEach, vi } from "vitest"

export const noopFetchNotebookPage = async (): Promise<void> => {
  await Promise.resolve()
}

export function stubNotebookPageViewBookAbsent() {
  beforeEach(() => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkError("Not found") as Awaited<
        ReturnType<typeof NotebookBooksController.getBook>
      >
    )
  })
}

export function mountNotebookPageView(
  notebook: Notebook,
  props: Record<string, unknown> = {}
) {
  return helper
    .component(NotebookPageView)
    .withRouter()
    .withProps({
      notebook,
      fetchNotebookPage: noopFetchNotebookPage,
      ...props,
    })
    .mount()
}

export function aNotebook(overrides: Partial<Notebook> = {}): Notebook {
  return {
    ...makeMe.aNotebook.please(),
    ...overrides,
  }
}
