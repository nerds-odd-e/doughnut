/// <reference types="Cypress" />
// @ts-check
import type { AttachBookRequestFull } from '@generated/donut-backend-api'
import { NotebookBooksController } from '@generated/donut-backend-api/sdk.gen'
import { notebookStructureTestabilityMethods } from './testabilityNotebookStructure'

/** Must match page count in `e2e_test/fixtures/book_reading/blank_5_pages.pdf`. */
const BLANK_BOOK_FIXTURE_PAGE_COUNT = 5

/** Cached once per Cypress process — blank PDF bytes for attachBook. */
let blankBookPdfBuffer: ArrayBuffer | undefined

function readBlankBookPdf() {
  if (blankBookPdfBuffer !== undefined) {
    return cy.wrap(blankBookPdfBuffer, { log: false })
  }
  return cy
    .readFile('e2e_test/fixtures/book_reading/blank_5_pages.pdf', null)
    .then((pdfBuffer) => {
      blankBookPdfBuffer = pdfBuffer as ArrayBuffer
      return blankBookPdfBuffer
    })
}

function pageCountFromContentList(contentList: Array<unknown>): number {
  let max = -1
  for (const o of contentList) {
    if (o !== null && typeof o === 'object' && 'page_idx' in o) {
      const p = (o as { page_idx: unknown }).page_idx
      if (typeof p === 'number' && Number.isFinite(p)) {
        max = Math.max(max, p)
      }
    }
  }
  if (max < 0) {
    throw new Error('contentList has no valid page_idx')
  }
  return max + 1
}

export const bookTestabilityMethods = {
  attachBookToNotebook(
    notebookName: string,
    bookName: string,
    contentList: Array<unknown>
  ) {
    expect(
      pageCountFromContentList(contentList),
      `contentList page range must match blank_${BLANK_BOOK_FIXTURE_PAGE_COUNT}_pages.pdf`
    ).to.equal(BLANK_BOOK_FIXTURE_PAGE_COUNT)
    return notebookStructureTestabilityMethods
      .getNotebookIdByName(notebookName)
      .then((notebookId) =>
        readBlankBookPdf().then((pdfBuffer) => {
          const file = new File([pdfBuffer as BlobPart], 'blank.pdf', {
            type: 'application/pdf',
          })
          const metadataBlob = new Blob(
            [JSON.stringify({ bookName, format: 'pdf', contentList })],
            { type: 'application/json' }
          )
          return cy.wrap(
            NotebookBooksController.attachBook({
              path: { notebook: notebookId },
              body: {
                metadata: metadataBlob as unknown as AttachBookRequestFull,
                file,
              },
            }),
            { log: false }
          )
        })
      )
  },
}
