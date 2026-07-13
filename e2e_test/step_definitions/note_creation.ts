import { When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

When(
  'I create a note titled {string} from note {string} with relationship {string}',
  (title: string, fromNote: string, relationship: string) => {
    start
      .jumpToNotePage(fromNote)
      .addingNewNoteFromToolbar()
      .createNoteWithTitleAndParentRelationship(title, relationship)
    start.assumeNotePage(title)
    start.testability().rememberUiCreatedNote(title)
  }
)
