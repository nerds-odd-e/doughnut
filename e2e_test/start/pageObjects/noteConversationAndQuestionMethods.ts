import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  assumeAssimilationPage,
  assimilateButtonSelector,
} from './assimilationPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import { form } from '../forms'
import { addQuestionPage } from './addQuestionPage'
import { questionListPage } from './questionListPage'
import { toolbarButton } from './toolbarButton'

export const noteConversationAndQuestionMethods = () => ({
  deleteNote() {
    this.moreOptions().deleteNote()
  },
  deleteRelationshipNote() {
    this.moreOptions().deleteRelationshipNote()
  },
  deleteNoteAndLeaveReferencesAsDeadWikiLinks() {
    this.moreOptions().deleteNoteAndLeaveReferencesAsDeadWikiLinks()
  },
  deleteNoteAndRemoveFromReferenceProperties() {
    this.moreOptions().deleteNoteAndRemoveFromReferenceProperties()
  },
  deleteNoteAndReduceToSourceProperty() {
    this.moreOptions().deleteNoteAndReduceToSourceProperty()
  },
  openQuestionList() {
    return this.moreOptions().openQuestionList()
  },
  addQuestion(row: Record<string, string>) {
    this.openQuestionList().addQuestionPage().addQuestion(row)
  },
  refineQuestionInForm(row: Record<string, string>) {
    addQuestionPage().refineQuestion(row)
    return this
  },
  expectQuestionInForm(expected: Record<string, string>) {
    addQuestionPage().expectQuestionInForm(expected)
    return this
  },
  expectQuestionsInList(expectedQuestions: Record<string, string>[]) {
    cy.get('body').then(($body) => {
      if ($body.find('.question-table').length > 0) {
        questionListPage().expectQuestions(expectedQuestions)
      } else {
        this.openQuestionList().expectQuestions(expectedQuestions)
      }
    })
  },
  startAConversationAboutNote() {
    toolbarButton('Start a conversation about this note').click()
    waitUntilAppIsNotBusy()
    return assumeConversationAboutNotePage()
  },

  closeConversation() {
    assumeConversationAboutNotePage().closeConversation()
    return this
  },

  sendMessageToAI(message: string) {
    this.startAConversationAboutNote().replyToConversationAndInviteAiToReply(
      message
    )
  },

  openAssimilationSettings() {
    this.moreOptions().openAssimilationSettings()
    waitUntilAppIsNotBusy()
    return assumeAssimilationPage()
  },
  setLevel(level: number) {
    this.openAssimilationSettings()
    form.getField('Level').within(() => {
      cy.findByRole('button', { name: `${level}` }).click()
    })
    waitUntilAppIsNotBusy()
    return this
  },
  expectWithoutAssimilationPanel() {
    cy.url({ timeout: 15000 }).should('match', /\/d\/n\/\d+|\/n\/\d+|\/n\d+/)
    cy.get(assimilateButtonSelector).should('not.exist')
    return this
  },
})
