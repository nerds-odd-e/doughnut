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
      .editNotebookSettings(noteTopic)
      .assertNoteHasSettingWithValue('Certificate Expiry', '1y')
  }
)

Given(
  'I set the certificate expiry of the notebook {string} to {string}',
  (notebook: string, period: string) => {
    start
      .routerToNotebooksPage()
      .editNotebookSettings(notebook)
      .updateAssessmentSettings({ certificateExpiry: period })
    start
      .routerToNotebooksPage()
      .editNotebookSettings(notebook)
      .assertNoteHasSettingWithValue('Certificate Expiry', period)
  }
)

When(
  'I complete an assessment for the notebook {string}',
  (notebook: string) => {
    start
      .navigateToBazaar()
      .beginAssessmentOnNotebook(notebook)
      .answerYesNoQuestionsToScore(2, 2)
  }
)

Then('I should have the following certificates:', (datatable: DataTable) => {
  datatable.rows().forEach((row, i) => {
    const [notebook, expires] = row
    if (notebook && expires) {
      start
        .navigateToAssessmentAndCertificatePage()
        .viewCertificateAt(i)
        .expectExpiryDate(expires)
    }
  })
})
