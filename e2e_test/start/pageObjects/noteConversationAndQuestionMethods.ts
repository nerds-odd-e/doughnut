import { pageIsNotLoading } from '../pageBase'
import {
  assumeAssimilationPage,
  assimilateButtonSelector,
} from './assimilationPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import { form } from '../forms'
import { makeSureNoteMoreOptionsFormIsOpen } from './noteMoreOptionsForm'
import { questionListPage } from './questionListPage'
import { toolbarButton } from './toolbarButton'

export const noteConversationAndQuestionMethods = () => ({
  deleteNote() {
    this.moreOptions().deleteNote()
  },
  deleteRelationshipNote() {
    this.moreOptions().deleteRelationshipNote()
  },
  deleteNoteAndLeaveReferencesAsDeadLinks() {
    this.moreOptions().deleteNoteAndLeaveReferencesAsDeadLinks()
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
  refineQuestion(row: Record<string, string>) {
    this.openQuestionList().addQuestionPage().refineQuestion(row)
  },
  expectQuestionsInList(expectedQuestions: Record<string, string>[]) {
    cy.get('body').then(($body) => {
      if ($body.find('.question-table').length > 0) {
        questionListPage().expectQuestion(expectedQuestions)
      } else {
        this.openQuestionList().expectQuestion(expectedQuestions)
      }
    })
  },
  deleteQuestion(questionStem: string) {
    this.openQuestionList().deleteQuestion(questionStem)
  },
  expectNoQuestionsInList() {
    cy.get('body').then(($body) => {
      if (!$body.text().includes('No questions')) {
        this.openQuestionList()
      }
      questionListPage().expectNoQuestions()
    })
  },
  sendMessageToNoteOwner(message: string) {
    cy.intercept('POST', '**/api/conversation/note/**').as(
      'startNoteConversation'
    )
    toolbarButton('Star a conversation about this note').click()
    cy.findByRole('textbox').type(message)
    cy.findByRole('button', { name: 'Send message' }).click()
    cy.wait('@startNoteConversation').should(({ response }) => {
      expect(response?.statusCode, 'start conversation about note').to.equal(
        200
      )
    })
  },

  startAConversationAboutNote() {
    toolbarButton('Star a conversation about this note').click()
    return assumeConversationAboutNotePage()
  },

  sendMessageToAI(message: string) {
    this.startAConversationAboutNote().replyToConversationAndInviteAiToReply(
      message
    )
  },

  openAssimilationSettings() {
    makeSureNoteMoreOptionsFormIsOpen().openAssimilationSettings()
    pageIsNotLoading()
    return assumeAssimilationPage()
  },
  setLevel(level: number) {
    this.openAssimilationSettings()
    form.getField('Level').within(() => {
      cy.findByRole('button', { name: `${level}` }).click()
    })
    pageIsNotLoading()
    return this
  },
  setRememberSpelling() {
    this.openAssimilationSettings()
    form.getField('Remember Spelling').check()
    pageIsNotLoading()
    return this
  },
  expectWithoutAssimilationPanel() {
    cy.url({ timeout: 15000 }).should('match', /\/d\/n\/\d+|\/n\/\d+|\/n\d+/)
    cy.get(assimilateButtonSelector).should('not.exist')
    return this
  },
})
