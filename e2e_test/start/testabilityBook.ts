/// <reference types="Cypress" />
// @ts-check
import type { AttachBookRequestFull } from '@generated/donut-backend-api'
import { NotebookBooksController } from '@generated/donut-backend-api/sdk.gen'
import { notebookStructureTestabilityMethods } from './testabilityNotebookStructure'

const BLANK_BOOK_FIXTURE = 'blank_5_pages.pdf'
/** Must match page count in `e2e_test/fixtures/book_reading/blank_5_pages.pdf`. */
const BLANK_BOOK_FIXTURE_PAGE_COUNT = 5

/** Cached once per Cypress process — PDF fixture bytes for attachBook, by file name. */
const pdfFixtureBuffers = new Map<string, ArrayBuffer>()

function readPdfFixture(pdfFixture: string) {
  const cached = pdfFixtureBuffers.get(pdfFixture)
  if (cached !== undefined) {
    return cy.wrap(cached, { log: false })
  }
  return cy
    .readFile(`e2e_test/fixtures/book_reading/${pdfFixture}`, null)
    .then((pdfBuffer) => {
      pdfFixtureBuffers.set(pdfFixture, pdfBuffer as ArrayBuffer)
      return pdfBuffer as ArrayBuffer
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
  /** Attaches `pdfFixture` (under `e2e_test/fixtures/book_reading/`) with its MinerU `contentList`. */
  attachBookToNotebook(
    notebookName: string,
    bookName: string,
    contentList: Array<unknown>,
    pdfFixture: string
  ) {
    return notebookStructureTestabilityMethods
      .getNotebookIdByName(notebookName)
      .then((notebookId) =>
        readPdfFixture(pdfFixture).then((pdfBuffer) => {
          const file = new File([pdfBuffer as BlobPart], pdfFixture, {
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

  /** Attaches the blank 5-page PDF with a MinerU `contentList` of the same page range. */
  attachBlankPdfBookToNotebook(
    notebookName: string,
    bookName: string,
    contentList: Array<unknown>
  ) {
    expect(
      pageCountFromContentList(contentList),
      `contentList page range must match ${BLANK_BOOK_FIXTURE}`
    ).to.equal(BLANK_BOOK_FIXTURE_PAGE_COUNT)
    return bookTestabilityMethods.attachBookToNotebook(
      notebookName,
      bookName,
      contentList,
      BLANK_BOOK_FIXTURE
    )
  },
}
