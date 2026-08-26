import type { PdfLocatorFull } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { vi } from "vitest"
import {
  bookId,
  getTopMathsPdfBytes,
  mockNotebookBookFilePdfOk,
  mountBookReadingPage,
  notebookId,
  stubGetBookWithTopMathsBlocks,
  waitForPdfViewer,
} from "./bookReadingPageTestSupport"

export function stubReadingPositionSnapshot(options: {
  pageIndex?: number
  bboxTop?: number
  selectedBookBlockId?: number
}) {
  return vi
    .spyOn(NotebookBooksController, "getNotebookBookReadingPosition")
    .mockResolvedValue(
      wrapSdkResponse({
        id: 1,
        locator: {
          type: "PdfLocator_Full",
          pageIndex: options.pageIndex ?? 2,
          bbox: [0, options.bboxTop ?? 750, 100, 600],
        } satisfies PdfLocatorFull,
        ...(options.selectedBookBlockId !== undefined
          ? { selectedBookBlockId: options.selectedBookBlockId }
          : {}),
      }) as Awaited<
        ReturnType<
          typeof NotebookBooksController.getNotebookBookReadingPosition
        >
      >
    )
}

export async function mountPatchDebounceScenario() {
  stubGetBookWithTopMathsBlocks(notebookId)
  mockNotebookBookFilePdfOk(bookId, getTopMathsPdfBytes())
  const patchSpy = mockSdkService(
    NotebookBooksController,
    "patchNotebookBookReadingPosition",
    undefined
  )
  const wrapper = mountBookReadingPage(notebookId)
  await waitForPdfViewer(wrapper)
  return { wrapper, patchSpy }
}
