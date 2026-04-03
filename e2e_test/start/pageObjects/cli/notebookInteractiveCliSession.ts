import { interactiveCli } from './interactiveCli'

const NOTEBOOK_ACTIVE_TIMEOUT_MS = 20_000

/**
 * Fluent notebook-stage session after `/use`: attach PDFs under
 * `e2e_test/fixtures/book_reading/` and assert on past assistant text.
 */
export type NotebookInteractiveCliCtx = {
  attachPdfBook(
    fixtureFilename: string
  ): Cypress.Chainable<NotebookInteractiveCliCtx>
  pastCliAssistantMessages(): NotebookInteractiveCliPastAssistant
}

export type NotebookInteractiveCliPastAssistant = {
  expectContains(
    expected: string,
    options?: { timeoutMs?: number }
  ): Cypress.Chainable<NotebookInteractiveCliCtx>
  expectContainsBookReadingMineruStubLayoutExcerpt(options?: {
    timeoutMs?: number
  }): Cypress.Chainable<NotebookInteractiveCliCtx>
}

function notebookInteractiveCtx(): NotebookInteractiveCliCtx {
  return {
    attachPdfBook(fixtureFilename: string) {
      return cy
        .task<string>(
          'getE2eFixtureAbsolutePath',
          `book_reading/${fixtureFilename}`
        )
        .then((absPath) =>
          interactiveCli().enterSlashCommandInInteractiveCli(
            `/attach ${absPath}`
          )
        )
        .then(() => cy.wrap(notebookInteractiveCtx()))
    },
    pastCliAssistantMessages(): NotebookInteractiveCliPastAssistant {
      return {
        expectContains(expected, options) {
          return interactiveCli()
            .pastCliAssistantMessages()
            .expectContains(expected, options)
            .then(() => cy.wrap(notebookInteractiveCtx()))
        },
        expectContainsBookReadingMineruStubLayoutExcerpt(options) {
          return interactiveCli()
            .pastCliAssistantMessages()
            .expectContainsBookReadingMineruStubLayoutExcerpt(options)
            .then(() => cy.wrap(notebookInteractiveCtx()))
        },
      }
    },
  }
}

export function useNotebook(
  notebookTitle: string
): Cypress.Chainable<NotebookInteractiveCliCtx> {
  const ic = interactiveCli()
  return ic
    .enterSlashCommandInInteractiveCli(`/use ${notebookTitle}`)
    .then(() =>
      ic
        .pastCliAssistantMessages()
        .expectContains(`Active notebook: ${notebookTitle}`, {
          timeoutMs: NOTEBOOK_ACTIVE_TIMEOUT_MS,
        })
    )
    .then(() => cy.wrap(notebookInteractiveCtx()))
}
