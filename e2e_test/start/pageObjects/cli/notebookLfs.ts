/**
 * Standard Git LFS client against a notebook's authenticated LFS endpoint.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import testability from '../../testability'

export function notebookLfs() {
  return {
    rememberNotebookId(notebookName: string) {
      return testability()
        .getNotebookIdByName(notebookName)
        .then((notebookId) => {
          cy.wrap(notebookId).as('lfsNotebookId')
        })
    },
    uploadDownloadAndVerifyExactDigest(payload: string) {
      return cy.get<number>('@lfsNotebookId').then((notebookId) =>
        cy.get<string>('@savedAccessToken').then((token) =>
          cy
            .task<{
              gitLfsVersion: string
              oid: string
              size: number
            }>('notebookLfsStandardClientRoundTrip', {
              lfsUrl: `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/lfs`,
              token,
              payload,
            })
            .then((result) => {
              cy.wrap(result.oid).as('lfsOid')
              expect(
                result.size,
                'uploaded/downloaded byte length'
              ).to.be.greaterThan(0)
            })
        )
      )
    },
    attemptUnauthorizedUpload(payload: string) {
      return cy.get<number>('@lfsNotebookId').then((notebookId) =>
        cy.get<string>('@savedAccessToken').then((token) =>
          cy
            .task<{
              gitLfsVersion: string
              oid: string
              output: string
              status: number | null
            }>('notebookLfsStandardClientAuthDenial', {
              lfsUrl: `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/lfs`,
              token,
              payload,
            })
            .then((result) => {
              cy.wrap(result).as('lfsAuthDenial')
            })
        )
      )
    },
  }
}
