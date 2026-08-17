/**
 * Book layout reorganize / new-block scenarios: thin glue to `bookReadingPage`.
 */
import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { BookFull, NoteRealm } from '@generated/doughnut-backend-api'
import {
  NoteController,
  NotebookBooksController,
} from '@generated/doughnut-backend-api/sdk.gen'
import bookReadingPage from '../start/pageObjects/bookReadingPage'
import { mock_services } from '../start'
import testability from '../start/testability'
import { unwrapData, validatePreorderDepths } from './book_reading_helpers'

Given(
  'OpenAI returns a book layout suggestion that indents block {string} for notebook {string}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (blockTitle: string, notebookName: string) => {
    return testability()
      .getInjectedNoteIdByTitle(notebookName)
      .then((noteId) =>
        cy.wrap(NoteController.showNote({ path: { note: noteId } }), {
          log: false,
        })
      )
      .then((showResponse) => {
        const realm = unwrapData<NoteRealm>(showResponse)
        const notebookId = realm.notebookRealm.notebook.id
        expect(notebookId, 'note must belong to a notebook').to.be.a('number')
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
                .responses()
                .requestMessageMatches({
                  role: 'developer',
                  content: '.*You reorganize the outline nesting.*',
                })
                .stubOutputText(reply)
            })
          })
      })
  }
)

When(
  'I request AI reorganization of the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().requestAiReorganizationOfBookLayout()
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

Given(
  'the book layout shows block {string} at depth {int}',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string, depth: number) => {
    return bookReadingPage().expectBookBlockAtDepth(title, depth)
  }
)

When(
  'I indent the focused book block with Tab',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().indentFocusedBookBlockWithTab()
  }
)

When(
  'I outdent the focused book block with Shift+Tab',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().outdentFocusedBookBlockWithShiftTab()
  }
)

When(
  'I cancel the focused book block with Backspace',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().cancelFocusedBookBlockWithBackspace()
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
  'the book block {string} should be focused in the book layout',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  (title: string) => {
    return bookReadingPage().expectBookBlockIsFocusedByTitle(title)
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
  'I create a book block from a content block on the PDF',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().createBookBlockFromContentBlockOnPdf()
  }
)

When(
  'I create a book block from a long-text content block on the PDF',
  // @ts-expect-error Cucumber preprocessor typings omit Cypress.Chainable; runtime supports returning the chain
  () => {
    return bookReadingPage().createBookBlockFromLongTextContentBlockOnPdf()
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
