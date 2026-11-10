import { When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I open occurrence {int} of item {string} in property {string} of note {string} from its URL',
  (occurrence: number, value: string, key: string, title: string) => {
    start
      .assumeNotePage()
      .visitRichNotePropertyItem(title, key, value, occurrence)
  }
)
When('I reload the property item URL', () => {
  start.assumeNotePage().reloadRichNotePropertyItem()
})
Then(
  'occurrence {int} of item {string} in property {string} should be highlighted',
  (occurrence: number, value: string, key: string) => {
    start
      .assumeNotePage()
      .expectFocusedRichNotePropertyItem(key, value, occurrence)
  }
)
