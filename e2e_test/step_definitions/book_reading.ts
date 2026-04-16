/**
 * Book-reading scenarios: thin glue to `e2e_test/start/pageObjects/bookReadingPage`.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import type { BookFull, NoteRealm } from '@generated/doughnut-backend-api'
import {
  NoteController,
  NotebookBooksController,
} from '@generated/doughnut-backend-api/sdk.gen'
import bookReadingPage, {
  type BookLayoutRow,
} from '../start/pageObjects/bookReadingPage'
import { cli } from '../start/pageObjects/cli'
import notebookPage from '../start/pageObjects/notebookPage'
import start, { mock_services } from '../start'
import testability from '../start/testability'

function unwrapData<T>(result: T | { data: T } | undefined): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as { data: T }).data
  }
  return result as T
}

function parseBookLayoutTable(data: DataTable): BookLayoutRow[] {
  return data.raw().map((row) => {
    const depth = parseInt(row[0] ?? '0', 10)
    const title = (row[1] ?? '').trim()
    return { depth, title }
  })
}

function pdfFixtureStem(fixtureFilename: string): string {
  return fixtureFilename.replace(/\.pdf$/i, '')
}

const MAX_BOOK_LAYOUT_DEPTH = 64

function validatePreorderDepths(depths: number[]): void {
  if (depths.length === 0) {
    return
  }
  if (depths[0] !== 0 || depths[0] > MAX_BOOK_LAYOUT_DEPTH) {
    throw new Error('Suggested depths do not form a valid outline')
  }
  for (let i = 1; i < depths.length; i++) {
    const d = depths[i]!
    if (d < 0 || d > MAX_BOOK_LAYOUT_DEPTH || d > depths[i - 1]! + 1) {
      throw new Error('Suggested depths do not form a valid outline')
    }
  }
}

Given(
  'I set the book reading viewport to {int} by {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (width: number, height: number) => {
    return bookReadingPage().setBookReadingViewport(width, height)
  }
)

When(
  'I attach book {string} to the notebook {string} via the CLI',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureFilename: string, notebookTitle: string) => {
    const stem = pdfFixtureStem(fixtureFilename)
    cy.wrap(stem).as('attachedBookPdfStem')
    return cli
      .useNotebook(notebookTitle)
      .then((ctx) => ctx.attachPdfBook(fixtureFilename))
      .then((ctx) => {
        ctx.pastCliAssistantMessages().expectContains(`Attached "${stem}"`)
        ctx
          .pastCliAssistantMessages()
          .expectContains('Protecting Intention in Working Software')
        ctx.pastCliAssistantMessages().expectContains('Easier to Change')
      })
  }
)

When(
  'I attach a fake blank pdf book with layout of {string} to the notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (fixtureStem: string, notebookTitle: string) => {
    const bookName = fixtureStem
    cy.wrap(bookName).as('attachedBookPdfStem')
    return cy
      .fixture(`book_reading/mineru_output_for_${fixtureStem}.json`)
      .then((contentList: unknown) => {
        return testability().attachBookToNotebook(
          notebookTitle,
          bookName,
          contentList as Array<unknown>
        )
      })
  }
)

When(
  'I open the book attached to notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (notebookTitle: string) => {
    return cy.get<string>('@attachedBookPdfStem').then((stem) => {
      start.navigateToNotebookPage(notebookTitle).readBook(stem)
    })
  }
)

When('I open the notebook settings for {string}', (notebookTitle: string) => {
  start.navigateToNotebookPage(notebookTitle)
})

When(
  'I attach the EPUB file {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (relativePath: string) => {
    return notebookPage().attachEpubFixture(relativePath)
  }
)

When(
  'I attempt to attach the EPUB file {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (relativePath: string) => {
    return notebookPage().attemptAttachEpubFixture(relativePath)
  }
)

When(
  'I open the reading view for the attached book {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (bookTitle: string) => {
    return notebookPage().readBook(bookTitle)
  }
)

Then(
  'I should see the EPUB reading view with book name {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (name: string) => {
    return bookReadingPage().expectEpubReadingViewShowsBookName(name)
  }
)

Then(
  'I should see the text {string} in the EPUB reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (text: string) => {
    return bookReadingPage().expectEpubContentTextVisible(text)
  }
)

Then(
  'I should see an EPUB attach error containing {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (messageSubstring: string) => {
    return notebookPage().expectEpubAttachErrorContaining(messageSubstring)
  }
)

Given(
  'OpenAI returns the current book block depths as the layout suggestion for notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (notebookTitle: string) => {
    return testability()
      .getInjectedNoteIdByTitle(notebookTitle)
      .then((noteId) =>
        cy.wrap(NoteController.showNote({ path: { note: noteId } }), {
          log: false,
        })
      )
      .then((showResponse) => {
        const realm = unwrapData<NoteRealm>(showResponse)
        const notebookId = realm.notebook?.id
        expect(notebookId, 'head note must belong to a notebook').to.be.a(
          'number'
        )
        return cy
          .wrap(
            NotebookBooksController.getBook({ path: { notebook: notebookId } }),
            { log: false }
          )
          .then((bookResponse) => {
            const book = unwrapData<BookFull>(bookResponse)
            expect(book.blocks, 'book must have blocks').to.be.an('array')
            const suggestion = {
              blocks: book.blocks.map((b) => ({
                id: b.id,
                depth: b.depth,
              })),
            }
            const reply = JSON.stringify(suggestion)
            return cy.then(async () => {
              await mock_services
                .openAi()
                .chatCompletion()
                .requestMessageMatches({
                  role: 'system',
                  content: '.*You reorganize the outline nesting.*',
                })
                .stubJsonSchemaResponse(reply)
            })
          })
      })
  }
)

Given(
  'OpenAI returns a layout suggestion that indents block {string} for notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string, notebookTitle: string) => {
    return testability()
      .getInjectedNoteIdByTitle(notebookTitle)
      .then((noteId) =>
        cy.wrap(NoteController.showNote({ path: { note: noteId } }), {
          log: false,
        })
      )
      .then((showResponse) => {
        const realm = unwrapData<NoteRealm>(showResponse)
        const notebookId = realm.notebook?.id
        expect(notebookId, 'head note must belong to a notebook').to.be.a(
          'number'
        )
        return cy
          .wrap(
            NotebookBooksController.getBook({ path: { notebook: notebookId } }),
            { log: false }
          )
          .then((bookResponse) => {
            const book = unwrapData<BookFull>(bookResponse)
            expect(book.blocks, 'book must have blocks').to.be.an('array')
            const depths = book.blocks.map((b) => b.depth)
            const idx = book.blocks.findIndex((b) => b.title === blockTitle)
            expect(
              idx,
              `no block with title "${blockTitle}"`
            ).to.be.greaterThan(0)
            depths[idx] = depths[idx]! + 1
            expect(() => validatePreorderDepths(depths)).not.to.throw()
            const suggestion = {
              blocks: book.blocks.map((b, i) => ({
                id: b.id,
                depth: depths[i]!,
              })),
            }
            const reply = JSON.stringify(suggestion)
            return cy.then(async () => {
              await mock_services
                .openAi()
                .chatCompletion()
                .requestMessageMatches({
                  role: 'system',
                  content: '.*You reorganize the outline nesting.*',
                })
                .stubJsonSchemaResponse(reply)
            })
          })
      })
  }
)

When(
  'I request AI reorganization of the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickAiReorganizeLayout()
  }
)

Then(
  'I should see a reorganization preview dialog',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectReorganizationPreviewDialog()
  }
)

Then(
  'the preview should show block {string} with suggested depth {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string, suggestedDepth: number) => {
    return bookReadingPage().expectReorganizationPreviewBlockSuggestedDepth(
      blockTitle,
      suggestedDepth
    )
  }
)

When(
  'I confirm the AI suggestion',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().confirmAiReorganizeSuggestion()
  }
)

Then(
  'I should see the book layout in the browser:',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (data: DataTable) => {
    const expected = parseBookLayoutTable(data)
    return bookReadingPage().expectBookLayoutRows(expected)
  }
)

Then(
  'I should see the beginning of the PDF book {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (_fixtureFilename: string) => {
    return bookReadingPage().expectPdfBeginningVisible()
  }
)

When(
  'I scroll the PDF book reader to bring page 2 into primary view',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollPdfBookReaderToBringPage2IntoPrimaryView()
  }
)

When(
  'I scroll the PDF book reader down within the same page to move viewport past the next book block bbox',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().scrollPdfBookReaderDownWithinSamePageForNextBbox()
  }
)

When(
  'I choose the book block {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().clickBookBlockByTitle(title)
  }
)

When(
  'I scroll the PDF until the book block {string} is the current block in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().scrollPdfBookReaderToMakeBookBlockCurrent(
      blockTitle
    )
  }
)

When(
  'I scroll the PDF book reader until the Reading Control Panel shows for {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (selectedBlockTitle: string) => {
    return bookReadingPage().scrollPdfUntilReadingControlPanelVisible(
      selectedBlockTitle
    )
  }
)

When(
  'I mark the book block {string} as read in the Reading Control Panel',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string) => {
    return bookReadingPage().markBookBlockAsReadInReadingControlPanel(
      blockTitle
    )
  }
)

Then(
  'I should see that book block {string} is marked as read in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockMarkedAsReadInBookLayout(title)
  }
)

Then(
  'I should see that book block {string} is selected in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentSelectionByTitle(title)
  }
)

Then(
  'I should see in the book reader visible PDF viewport on page {int} text including {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (pageNumber: number, marker: string) => {
    return bookReadingPage()
      .expectCurrentPage(pageNumber)
      .expectVisibleOCRContains(marker)
  }
)

Then(
  'the book block {string} should be focused in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsFocusedByTitle(title)
  }
)

Then(
  'the book block {string} should be the current selection in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentSelectionByTitle(title)
  }
)

Then(
  'the book block {string} should be the current block in the book reader',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsCurrentBlockByTitle(title)
  }
)

Then(
  'the book block {string} should be the current block and visible in the book layout aside',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectCurrentBlockVisibleInBookLayoutAside(title)
  }
)

Then(
  'I should see the current block navigation bar showing {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectCurrentBlockNavigationBar(title)
  }
)

When(
  'I click "Read from here" in the current block navigation bar',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickReadFromHere()
  }
)

When(
  'I click "Back to selected" in the current block navigation bar',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickBackToSelected()
  }
)

Then(
  'the current block navigation bar should not be visible',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectCurrentBlockNavigationBarNotVisible()
  }
)

Given(
  'the book layout shows block {string} at depth {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string, depth: number) => {
    return bookReadingPage().expectBookBlockAtDepth(title, depth)
  }
)

When(
  'I press {string} on the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (key: string) => {
    if (key === 'Shift+Tab') {
      return bookReadingPage().pressShiftTabOnBookLayout()
    }
    if (key === 'Backspace') {
      return bookReadingPage().pressBackspaceOnBookLayout()
    }
    return bookReadingPage().pressTabOnBookLayout()
  }
)

Then(
  'the book block {string} should no longer appear in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockNotPresent(title)
  }
)

Then(
  'the book block {string} should be at depth {int} in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string, depth: number) => {
    return bookReadingPage().expectBookBlockAtDepth(title, depth)
  }
)

Then(
  'I should see content block bbox overlays on the PDF',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectContentBlockBboxOverlaysVisible()
  }
)

When(
  'I click on a content block bbox overlay in the PDF',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickContentBlockBboxOverlay()
  }
)

When(
  'I click on a long-text content block bbox overlay in the PDF',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().clickLongTextContentBlockBboxOverlay()
  }
)

Then(
  'I should see the {string} callout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (_label: string) => {
    return bookReadingPage().expectNewBlockCallout()
  }
)

When(
  'I confirm creating a new block',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().confirmNewBlockCallout()
  }
)

Then(
  'the book layout should contain a new block as a child of the selected block',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectNewChildBlockInLayout()
  }
)

Then(
  'I should be prompted to enter a title defaulting to truncated content',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().expectTitlePromptWithDefaultTitle()
  }
)

When(
  'I confirm the title',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().confirmTitlePrompt()
  }
)
