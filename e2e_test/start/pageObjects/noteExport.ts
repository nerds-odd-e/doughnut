import { waitUntilAppIsNotBusy } from '../pageBase'
import { toolbarButton } from './toolbarButton'

export const noteExport = () => ({
  open() {
    toolbarButton('Export... (e)').click()
    waitUntilAppIsNotBusy()
    cy.findByTestId('ai-context-markdown-textarea').should('be.visible')
    return this
  },
  includeRecentQuestions() {
    cy.findByTestId('include-recent-questions-checkbox').check()
    waitUntilAppIsNotBusy()
    return this
  },
  expectMarkdownContains(text: string) {
    cy.findByTestId('ai-context-markdown-textarea').should(($textarea) => {
      expect($textarea.val(), 'exported Markdown').to.contain(text)
    })
    return this
  },
})
