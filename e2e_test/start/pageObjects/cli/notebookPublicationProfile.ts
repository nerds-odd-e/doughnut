import { notebookCloneCheckout } from './notebookCloneCheckout'
import { expectCheckoutFileAt } from './notebookCloneCheckoutReceiver'

export const notebookPublicationProfile = {
  prepare() {
    const files = Array.from({ length: 20 }, (_, i) => ({
      relativePath: `Recipes/Added-${String(i).padStart(5, '0')}.md`,
      content: `---\ntype: Note\naliases: ['Profile alias ${i}']\nmeaning: 'Profile concept ${i}'\nsource: '[[Overview]]'\nrelated:\n  - '[[Pasta]]'\n  - '[[Overview]]'\n---\nProfile concept ${i}. ${'Deterministic authored prose. '.repeat(32)}\nSee [[Pasta]] and [[Added-${String((i + 1) % 20).padStart(5, '0')}]].\n`,
    }))
    return notebookCloneCheckout().commitRelatedNoteChanges(files)
  },
  start() {
    return cy.task('startNotebookPublicationProfile')
  },
  stop() {
    return cy.task('stopNotebookPublicationProfile')
  },
  expectReceived() {
    return cy
      .get<{ relativePath: string; content: string }[]>(
        '@cliNotebookProposalFiles'
      )
      .each(({ relativePath, content }) =>
        expectCheckoutFileAt(
          'cliCloneReceiverDestination',
          relativePath,
          content
        )
      )
      .then(() =>
        cy.get<string>('@cliNotebookPublishHead').then((acceptedHead) =>
          cy
            .get<{ relativePath: string; content: string }[]>(
              '@cliNotebookProposalFiles'
            )
            .then((files) =>
              cy.task('confirmNotebookPublicationProfile', {
                acceptedHead,
                documentCount: files.length,
              })
            )
        )
      )
  },
}
