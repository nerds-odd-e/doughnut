import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given(
  'I remember the notebook id of {string} for LFS transfer',
  (notebookName: string) => {
    cli.notebookLfs().rememberNotebookId(notebookName)
  }
)

Given(
  'the notebook {string} has an accepted LFS tip {string} with payload {string} and obsolete payload {string}',
  (
    notebookName: string,
    filename: string,
    payload: string,
    obsoletePayload: string
  ) => {
    cli
      .notebookLfs()
      .acceptLfsAttachmentTip(notebookName, filename, payload, obsoletePayload)
  }
)

Given(
  'an existing destination already contains {string} with {string}',
  (relativePath: string, content: string) => {
    cli.notebookLfs().prepareExistingDestination(relativePath, content)
  }
)

When(
  'I upload and download attachment bytes {string} with the standard Git LFS client',
  (payload: string) => {
    cli.notebookLfs().uploadDownloadAndVerifyExactDigest(payload)
  }
)

When(
  'I attempt to upload attachment bytes {string} with the standard Git LFS client',
  (payload: string) => {
    cli.notebookLfs().attemptUnauthorizedUpload(payload)
  }
)

When(
  'I clone the notebook {string} expecting rejection from the installed CLI into that existing destination',
  (notebookName: string) => {
    cli
      .notebookClone()
      .cloneNotebookExpectingRejectionIntoExisting(notebookName)
  }
)

Then(
  'the standard Git LFS client round trip keeps the exact SHA-256 digest',
  () => {
    cy.get<string>('@lfsOid').should('match', /^[a-f0-9]{64}$/)
  }
)

Then('the standard Git LFS client is refused authorization', () => {
  cy.get<{ output: string; status: number | null }>('@lfsAuthDenial').should(
    (result) => {
      expect(result.status, 'git lfs push exit status').to.not.equal(0)
      expect(result.output, 'git lfs denial output').to.match(
        /denied|403|forbidden|access/i
      )
    }
  )
})

Then('the cloned checkout is a clean main branch', () => {
  cli.notebookLfs().expectClonedCheckoutCleanMain()
})

Then(
  'the cloned checkout file {string} is exactly {string}',
  (relativePath: string, text: string) => {
    cli.notebookLfs().expectClonedCheckoutFileExact(relativePath, text)
  }
)

Then(
  'the cloned checkout LFS object cache holds only the tip digest for {string}',
  (_relativePath: string) => {
    cli.notebookLfs().expectClonedCheckoutLfsCacheHoldsOnlyTip()
  }
)

Then('the cloned checkout does not track credentials', () => {
  cli.notebookLfs().expectClonedCheckoutDoesNotTrackCredentials()
})

Then(
  'the cloned checkout local Git config records the LFS endpoint for {string}',
  (notebookName: string) => {
    cli.notebookLfs().expectClonedCheckoutRecordsLfsEndpoint(notebookName)
  }
)

Then(
  'the existing destination file {string} is still {string}',
  (relativePath: string, content: string) => {
    cli.notebookLfs().expectExistingDestinationFile(relativePath, content)
  }
)

When(
  'I commit the LFS attachment {string} filled with {int} bytes of {string} as {string}',
  (
    relativePath: string,
    byteLength: number,
    fillByteHex: string,
    alias: string
  ) => {
    cli
      .notebookLfs()
      .commitLfsFilledAttachment(relativePath, byteLength, fillByteHex, alias)
  }
)

When(
  'I commit the incompressible LFS attachment {string} of {int} bytes as {string}',
  (relativePath: string, byteLength: number, alias: string) => {
    cli.notebookLfs().commitLfsRandomAttachment(relativePath, byteLength, alias)
  }
)

Then(
  'the LFS commit {string} remains an ancestor of {string}',
  (ancestorAlias: string, descendantAlias: string) => {
    cli.notebookLfs().expectCommitIsAncestor(ancestorAlias, descendantAlias)
  }
)

Then(
  'the fresh clone file {string} is filled with {int} bytes of {string}',
  (relativePath: string, byteLength: number, fillByteHex: string) => {
    cli
      .notebookLfs()
      .expectFreshCloneFileFilledBytes(relativePath, byteLength, fillByteHex)
  }
)

Then(
  'the notebook {string} MySQL attachment {string} holds the LFS pointer for {string}',
  (notebookName: string, filename: string, versionAlias: string) => {
    cli
      .notebookLfs()
      .expectMysqlAcceptedGitContentIsTipPointer(
        notebookName,
        filename,
        versionAlias
      )
  }
)

Then(
  'the notebook {string} content store {word} object for {string} under attachment {string}',
  (
    notebookName: string,
    storedWord: string,
    versionAlias: string,
    filename: string
  ) => {
    expect(storedWord, 'expected "has" or "lacks"').to.be.oneOf([
      'has',
      'lacks',
    ])
    cli
      .notebookLfs()
      .expectObjectStorage(
        notebookName,
        filename,
        versionAlias,
        storedWord === 'has'
      )
  }
)

Then('the fresh clone LFS object cache holds only the tip digest', () => {
  cli.notebookLfs().expectFreshCloneLfsCacheHoldsOnlyTip()
})

Then(
  'bundle traffic stays pointer-scale versus object traffic for versions {string}',
  (aliasesCsv: string) => {
    const aliases = aliasesCsv.split(',').map((s) => s.trim())
    cli.notebookLfs().expectSizeProofTraffic(aliases)
  }
)
