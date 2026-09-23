import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { cli } from '../start/pageObjects/cli'

Given(
  'I remember the notebook id of {string} for LFS transfer',
  (notebookName: string) => {
    cli.notebookLfs().rememberNotebookId(notebookName)
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
