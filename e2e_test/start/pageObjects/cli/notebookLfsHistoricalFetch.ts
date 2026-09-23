/**
 * Explicit historical recovery via standard `git lfs fetch origin <ref>`
 * after clearing the local object cache.
 */
import { inspectNotebookLfsAttachment } from './notebookLfsInspect'

function runHistoricalLfsFetch(
  versionAlias: string,
  alias: 'lfsHistoricalFetch' | 'lfsHistoricalFetchDenial',
  requireSuccess: boolean
) {
  return cy.get<{ head: string }>(`@${versionAlias}`).then((version) =>
    cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
      cy
        .task<{ status: number | null; output: string }>(
          'fetchCliNotebookCheckoutLfsRef',
          { checkoutDir, ref: version.head }
        )
        .then((result) => {
          if (requireSuccess) {
            expect(
              result.status,
              `git lfs fetch origin ${version.head}\n${result.output}`
            ).to.equal(0)
          }
          cy.wrap(result).as(alias)
        })
    )
  )
}

export function notebookLfsHistoricalFetch() {
  return {
    expectNoRootAttachment(notebookName: string, filename: string) {
      return inspectNotebookLfsAttachment({
        notebookName,
        filename,
        // Digest is required by the inspect contract; row presence does not depend on it.
        oid: '0'.repeat(64),
      }).then((response) => {
        expect(response.status).to.eq(200)
        expect(
          response.body.attachmentPresent,
          'current attachment row must be gone'
        ).to.equal(false)
      })
    },
    clearClonedCheckoutLfsObjectCache() {
      return cy
        .get<string>('@cliCloneDestination')
        .then((checkoutDir) =>
          cy.task('clearCliNotebookCheckoutLfsObjectCache', checkoutDir)
        )
    },
    fetchLfsObjectsForCommit(versionAlias: string) {
      return runHistoricalLfsFetch(versionAlias, 'lfsHistoricalFetch', true)
    },
    attemptFetchLfsObjectsForCommit(versionAlias: string) {
      return runHistoricalLfsFetch(
        versionAlias,
        'lfsHistoricalFetchDenial',
        false
      )
    },
    expectHistoricalFetchReportsUnavailable() {
      return cy
        .get<{ status: number | null; output: string }>(
          '@lfsHistoricalFetchDenial'
        )
        .should((result) => {
          expect(
            result.status,
            'git lfs fetch of omitted object must fail'
          ).to.not.equal(0)
          expect(
            result.output,
            'historical fetch should report the object unavailable'
          ).to.match(/does not exist|not found|404|missing|unavailable|error/i)
        })
    },
    expectCachedLfsObjectFilledBytes(
      versionAlias: string,
      byteLength: number,
      fillByteHex: string
    ) {
      const fillByte = Number.parseInt(fillByteHex.replace(/^0x/i, ''), 16)
      return cy
        .get<{ oid: string; size: number }>(`@${versionAlias}`)
        .then((version) =>
          cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
            cy
              .task<{ length: number; firstByte: number; digest: string }>(
                'readLfsObjectFilledSummary',
                { checkoutDir, oid: version.oid }
              )
              .then((summary) => {
                expect(summary.digest, 'cached LFS object digest').to.equal(
                  version.oid
                )
                expect(summary.length, 'cached LFS object length').to.equal(
                  byteLength
                )
                expect(summary.firstByte, 'cached LFS fill byte').to.equal(
                  fillByte
                )
                expect(summary.length, 'cached size matches version').to.equal(
                  version.size
                )
              })
          )
        )
    },
    expectLfsCacheHoldsDigest(versionAlias: string) {
      return cy.get<{ oid: string }>(`@${versionAlias}`).then((version) =>
        cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
          cy
            .task<string[]>('listCliNotebookCheckoutLfsObjectOids', checkoutDir)
            .then((oids) => {
              expect(
                oids,
                `LFS cache should include ${version.oid}`
              ).to.include(version.oid)
            })
        )
      )
    },
    commitLfsAttachmentRemoval(relativePath: string) {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<{ head: string }>(
            'commitCliNotebookCheckoutAttachmentRemoval',
            {
              checkoutDir,
              relativePath,
            }
          )
          .then((result) => {
            cy.wrap(result.head).as('cliNotebookPublishHead')
          })
      )
    },
  }
}
