import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { noteExport } from '../start/pageObjects/noteExport'

When('I export the highlighted note context', () => noteExport().open())
When('I include recent questions in the note export', () =>
  noteExport().includeRecentQuestions()
)
Then('the note export should contain {string}', (text: string) =>
  noteExport().expectMarkdownContains(text)
)
