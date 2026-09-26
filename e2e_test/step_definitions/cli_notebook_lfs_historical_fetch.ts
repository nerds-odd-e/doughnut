import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

When('I clear the cloned checkout LFS object cache', () => {
  cli.notebookLfs().clearClonedCheckoutLfsObjectCache()
})

When(
  'I fetch LFS objects for commit {string} with the standard Git LFS client',
  (versionAlias: string) => {
    cli.notebookLfs().fetchLfsObjectsForCommit(versionAlias)
  }
)

When(
  'I remove the LFS attachment {string} from the cloned checkout',
  (relativePath: string) => {
    cli.notebookLfs().commitLfsAttachmentRemoval(relativePath)
  }
)

Then(
  'the cloned checkout LFS object cache holds the digest for {string}',
  (versionAlias: string) => {
    cli.notebookLfs().expectLfsCacheHoldsDigest(versionAlias)
  }
)

Then(
  'the cached LFS object for {string} is filled with {int} bytes of {string}',
  (versionAlias: string, byteLength: number, fillByteHex: string) => {
    cli
      .notebookLfs()
      .expectCachedLfsObjectFilledBytes(versionAlias, byteLength, fillByteHex)
  }
)

Then(
  'the notebook {string} has no root attachment named {string}',
  (notebookName: string, filename: string) => {
    cli.notebookLfs().expectNoRootAttachment(notebookName, filename)
  }
)

Then('the cloned checkout names no Git remote', () => {
  cli.notebookLfs().expectClonedCheckoutNamesNoGitRemote()
})
