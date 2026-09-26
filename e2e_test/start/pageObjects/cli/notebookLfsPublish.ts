/**
 * Page-object observations for publishing LFS attachments from a CLI checkout
 * and verifying accepted tip / fresh-clone size proof.
 */
import type { CliNotebookCloneDestinationAlias } from './notebookCloneCheckoutDestination'
import { notebookLfsHistoricalFetch } from './notebookLfsHistoricalFetch'
import { inspectNotebookLfsAttachment } from './notebookLfsInspect'

export function notebookLfsPublish() {
  return {
    commitLfsFilledAttachment(
      relativePath: string,
      byteLength: number,
      fillByteHex: string,
      alias: string,
      noteEdit?: { relativePath: string; content: string }
    ) {
      const fillByte = Number.parseInt(fillByteHex.replace(/^0x/i, ''), 16)
      expect(fillByte, `fill byte ${fillByteHex}`).to.be.within(0, 255)
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<{
            head: string
            oid: string
            size: number
            gitBlobSize: number
            pointerText: string
          }>('commitCliNotebookCheckoutLfsFilledAttachment', {
            checkoutDir,
            relativePath,
            byteLength,
            fillByte,
            noteEdit,
          })
          .then((result) => {
            expect(
              result.gitBlobSize,
              'Git blob must be pointer-sized, not payload'
            ).to.be.lessThan(500)
            expect(result.pointerText).to.include(`oid sha256:${result.oid}`)
            cy.wrap(result).as(alias)
            cy.wrap(result.oid).as('lfsTipOid')
            cy.wrap(result.head).as('cliNotebookPublishHead')
          })
      )
    },
    commitLfsRandomAttachment(
      relativePath: string,
      byteLength: number,
      alias: string
    ) {
      return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
        cy
          .task<{
            head: string
            oid: string
            size: number
            gitBlobSize: number
            pointerText: string
          }>('commitCliNotebookCheckoutLfsRandomAttachment', {
            checkoutDir,
            relativePath,
            byteLength,
          })
          .then((result) => {
            expect(
              result.gitBlobSize,
              'Git blob must be pointer-sized, not payload'
            ).to.be.lessThan(500)
            cy.wrap(result).as(alias)
            cy.wrap(result.oid).as('lfsTipOid')
            cy.wrap(result.head).as('cliNotebookPublishHead')
          })
      )
    },
    expectCommitIsAncestor(ancestorAlias: string, descendantAlias: string) {
      return cy.get<{ head: string }>(`@${ancestorAlias}`).then((ancestor) =>
        cy.get<{ head: string }>(`@${descendantAlias}`).then((descendant) =>
          cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
            cy
              .task<number>('gitMergeBaseIsAncestor', {
                checkoutDir,
                ancestor: ancestor.head,
                descendant: descendant.head,
              })
              .should('equal', 0)
          )
        )
      )
    },
    expectCheckoutFileFilledBytes(
      destinationAlias: CliNotebookCloneDestinationAlias,
      relativePath: string,
      byteLength: number,
      fillByteHex: string
    ) {
      const fillByte = Number.parseInt(fillByteHex.replace(/^0x/i, ''), 16)
      return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
        cy
          .task<{ length: number; firstByte: number }>(
            'readCheckoutFileFilledSummary',
            { checkoutDir, relativePath }
          )
          .then((summary) => {
            expect(summary.length, 'checkout file length').to.equal(byteLength)
            expect(summary.firstByte, 'checkout fill byte').to.equal(fillByte)
          })
      )
    },
    expectCheckoutLfsCacheHoldsOnlyTip(
      destinationAlias: CliNotebookCloneDestinationAlias
    ) {
      return cy.get<string>(`@${destinationAlias}`).then((checkoutDir) =>
        cy.get<string>('@lfsTipOid').then((tipOid) =>
          cy
            .task<string[]>('listCliNotebookCheckoutLfsObjectOids', checkoutDir)
            .then((oids) => {
              expect(oids, 'LFS object cache oids').to.deep.equal([tipOid])
            })
        )
      )
    },
    expectMysqlAcceptedGitContentIsTipPointer(
      notebookName: string,
      filename: string,
      versionAlias: string
    ) {
      return cy
        .get<{ oid: string; size: number }>(`@${versionAlias}`)
        .then((version) =>
          inspectNotebookLfsAttachment({
            notebookName,
            filename,
            oid: version.oid,
          }).then((response) => {
            expect(response.status).to.eq(200)
            expect(
              response.body.attachmentPresent,
              'attachment row must be present'
            ).to.equal(true)
            expect(
              response.body.acceptedGitContentLength,
              'MySQL content must be pointer-sized'
            ).to.be.lessThan(500)
            expect(
              response.body.acceptedGitContentLength,
              'MySQL must not hold the payload'
            ).to.be.lessThan(version.size)
            expect(response.body.acceptedGitContentUtf8).to.include(
              `oid sha256:${version.oid}`
            )
            expect(response.body.objectStored, 'tip object stored').to.equal(
              true
            )
            expect(response.body.storedObjectSize).to.equal(version.size)
          })
        )
    },
    expectObjectStored(
      notebookName: string,
      filename: string,
      versionAlias: string
    ) {
      return cy.get<{ oid: string }>(`@${versionAlias}`).then((version) =>
        inspectNotebookLfsAttachment({
          notebookName,
          filename,
          oid: version.oid,
        }).then((response) => {
          expect(response.body.objectStored).to.equal(true)
        })
      )
    },
    expectSizeProofTraffic(versionAliases: string[]) {
      const versions: { oid: string; size: number }[] = []
      const loadNext = (index: number): Cypress.Chainable<null> => {
        if (index >= versionAliases.length) {
          const tip = versions[versions.length - 1]!
          cy.wrap(tip.oid).as('lfsTipOid')
          const objectTrafficBytes = versions.reduce(
            (sum, v) => sum + v.size,
            0
          )
          return cy.get<string>('@cliCloneDestination').then((checkoutDir) =>
            cy
              .task<number>(
                'measureCliNotebookCheckoutBundleBytes',
                checkoutDir
              )
              .then((bundleTrafficBytes) => {
                expect(
                  bundleTrafficBytes,
                  `bundle traffic ${bundleTrafficBytes} vs object traffic ${objectTrafficBytes}`
                ).to.be.lessThan(objectTrafficBytes / 10)
                return cy
                  .get<string>('@cliCloneFreshDestination')
                  .then((freshDir) =>
                    cy
                      .task<string[]>(
                        'listCliNotebookCheckoutLfsObjectOids',
                        freshDir
                      )
                      .then((oids) => {
                        expect(
                          oids,
                          'fresh clone downloads only tip'
                        ).to.deep.equal([tip.oid])
                        return cy
                          .task<number | null>('lfsObjectFileSize', {
                            checkoutDir: freshDir,
                            oid: tip.oid,
                          })
                          .then((cloneObjectTraffic) => {
                            expect(
                              cloneObjectTraffic,
                              'clone object traffic is tip payload only'
                            ).to.equal(tip.size)
                            cy.wrap({
                              bundleTrafficBytes,
                              objectTrafficBytes,
                              cloneObjectTrafficBytes: cloneObjectTraffic,
                            }).as('lfsTrafficReport')
                            return cy.wrap(null)
                          })
                      })
                  )
              })
          )
        }
        return cy
          .get<{ oid: string; size: number }>(`@${versionAliases[index]}`)
          .then((version) => {
            versions.push(version)
            return loadNext(index + 1)
          })
      }
      return loadNext(0)
    },
    ...notebookLfsHistoricalFetch(),
  }
}
