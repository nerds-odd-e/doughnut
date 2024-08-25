import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start from '../start'
import { DataTable } from '@cucumber/cucumber'

When('I have a notebook with the name {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().creatingNotebook(noteTopic)
})

Then(
  'I should see the default expiration of {string} note to be 1 year',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .assertNoteHasSettingWithValue(noteTopic, 'Certificate Expiry', '1y')
  }
)

Given(
  'I set the certificate expiry of the notebook {string} to {string}',
  (notebook: string, period: string) => {
    start
      .routerToNotebooksPage()
      .updateAssessmentSettings(notebook, { certificateExpiry: period })
    start
      .routerToNotebooksPage()
      .assertNoteHasSettingWithValue(notebook, 'Certificate Expiry', period)
  }
)

When(
  'I complete an assessment for the notebook {string}',
  (notebook: string) => {
    start
      .navigateToBazaar()
      .selfAssessmentOnNotebook(notebook)
      .answerYesNoQuestionsToScore(2, 2)
  }
)

Then(
  'I should see that the certificate of {string} assesment expires on {string}',
  (notebook: string, expires: string) => {
    start
      .assumeAssessmentPage(notebook)
      .expectCertificate()
      .expectExpiryDate(expires)
  }
)

Then('list should contain certificates', (datatable: DataTable) => {
  for (let i = 0; i < datatable.rows().length; i++) {
    const row = datatable.rows()[i]
    if (row) {
      const notebook = row[0]
      const expires = row[1]
      if (notebook && expires) {
        start.navigateToAssessmentHistory().viewCertificateAt(i)
        cy.findByTestId('expired-date').contains(expires)
      }
    }
  }
})
