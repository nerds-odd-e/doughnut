import { bazaarOrCircle } from './BazaarOrCircle'
import router from '../router'

const SPA_DOCUMENT_MARKER = '__donutSpaDocumentMarker'

type WindowWithSpaDocumentMarker = Cypress.AUTWindow & {
  [SPA_DOCUMENT_MARKER]?: true
}

const isSpaAlreadyLoaded = (firstVisited: unknown) =>
  (firstVisited as { valueOf(): string }).valueOf() === 'yes'

export const assumeBazaarPage = () => {
  cy.findByText('Welcome To The Bazaar')

  return bazaarOrCircle()
}

export const navigateToBazaar = () => {
  let expectDocumentToSurvive = false
  cy.get('@firstVisited').then((firstVisited) => {
    expectDocumentToSurvive = isSpaAlreadyLoaded(firstVisited)
    if (expectDocumentToSurvive) {
      cy.window().then((win: WindowWithSpaDocumentMarker) => {
        win[SPA_DOCUMENT_MARKER] = true
      })
    }
  })
  router().push('bazaar')
  cy.get('h2', { timeout: 3000 }).should('contain', 'Welcome To The Bazaar')
  cy.then(() => {
    if (expectDocumentToSurvive) {
      cy.window().should((win: WindowWithSpaDocumentMarker) => {
        expect(
          win[SPA_DOCUMENT_MARKER],
          'Bazaar navigation after login should keep the SPA document (named push, not remount)'
        ).to.eq(true)
      })
    }
  })

  return assumeBazaarPage()
}
