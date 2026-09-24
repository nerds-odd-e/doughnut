/**
 * Standard Git LFS client against a notebook's authenticated LFS endpoint,
 * plus CLI clone observations for LFS tips.
 */
import { e2eAppBaseUrl } from '../../../support/e2eAppUrl'
import testability from '../../testability'
import { expectCheckoutFileExactTextAt } from './notebookCloneCheckoutDestination'
import { notebookLfsPublish } from './notebookLfsPublish'

export function notebookLfs() {
  return {
    rememberNotebookId(notebookName: string) {
      return testability()
        .getNotebookIdByName(notebookName)
        .then((notebookId) => {
          cy.wrap(notebookId).as('lfsNotebookId')
        })
    },
    acceptLfsAttachmentTip(
      notebookName: string,
      filename: string,
      payload: string,
      obsoletePayload: string
    ) {
      return testability()
        .acceptLfsAttachmentTipForTestability(
          notebookName,
          filename,
          payload,
          obsoletePayload
        )
        .then((oid) => {
          expect(oid).to.match(/^[a-f0-9]{64}$/)
          cy.wrap(oid).as('lfsTipOid')
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
    expectClonedCheckoutCleanMain() {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<{
            branch: string
            status: string
          }>('readCliNotebookCheckoutState', checkoutDir)
          .then((state) => {
            expect(state.branch, 'cloned branch').to.equal('main')
            expect(state.status, 'cloned checkout should be clean').to.equal('')
          })
      )
    },
    expectClonedCheckoutFileExact(relativePath: string, text: string) {
      return expectCheckoutFileExactTextAt(
        'cliCloneDestination',
        relativePath,
        text
      )
    },
    expectClonedCheckoutDoesNotTrackCredentials() {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy.get<string>('@savedAccessToken').then((token) =>
          cy
            .task<{ tracked: boolean; matchedPath?: string }>(
              'cliNotebookCheckoutTracksToken',
              { checkoutDir, token }
            )
            .then((result) => {
              expect(
                result.tracked,
                result.matchedPath
                  ? `token found in tracked ${result.matchedPath}`
                  : 'token must not be tracked'
              ).to.equal(false)
            })
        )
      )
    },
    expectClonedCheckoutRecordsLfsEndpoint(notebookName: string) {
      return testability()
        .getNotebookIdByName(notebookName)
        .then((notebookId) =>
          cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
            cy
              .task<string>('readCliNotebookCheckoutGitConfig', {
                checkoutDir,
                key: 'lfs.url',
              })
              .should(
                'equal',
                `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/lfs`
              )
          )
        )
    },
    prepareExistingDestination(relativePath: string, content: string) {
      return cy
        .task<string>('createCliNotebookCloneDestinationWithFile', {
          relativePath,
          content,
        })
        .then((destination) => {
          cy.wrap(destination).as('cliCloneDestination')
          cy.wrap(destination).as('cliExistingCloneDestination')
        })
    },
    expectExistingDestinationFile(relativePath: string, content: string) {
      return cy
        .get<string>('@cliExistingCloneDestination')
        .then((destination) => {
          cy.readFile(`${destination}/${relativePath}`).should('equal', content)
        })
    },
    ...notebookLfsPublish(),
  }
}
