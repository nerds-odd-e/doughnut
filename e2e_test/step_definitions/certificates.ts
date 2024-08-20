import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import '../support/string_util'
import start from '../start'

When('I have a notebook with the name {string}', (noteTopic: string) => {
  start.routerToNotebooksPage().creatingNotebook(noteTopic)
})

Then(
  'I should see the default expiration of {string} note to be 1 year',
  (noteTopic: string) => {
    start
      .routerToNotebooksPage()
      .assertNoteHasSettingWithValue(noteTopic, 'Certificate Expiry', '1 y')
  }
)
