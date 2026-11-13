import { waitUntilAppIsNotBusy } from '../pageBase'
import {
  assumeAssimilationPage,
  assimilationModesSelector,
} from './assimilationPage'
import { assumeConversationAboutNotePage } from './conversationAboutNotePage'
import { toolbarButton } from './toolbarButton'

export const noteActionsMethods = () => ({
  trashNote() {
    this.moreOptions().trashNote()
  },
  permanentlyDeleteNote() {
    this.moreOptions().permanentlyDeleteNote()
  },
  deleteRelationshipNote() {
    this.moreOptions().deleteRelationshipNote()
  },
  trashNoteAndLeaveReferencesAsDeadWikiLinks() {
    this.moreOptions().trashNoteAndLeaveReferencesAsDeadWikiLinks()
  },
  trashNoteAndRemoveFromReferenceProperties() {
    this.moreOptions().trashNoteAndRemoveFromReferenceProperties()
  },
  deleteNoteAndReduceToSourceProperty() {
    this.moreOptions().deleteNoteAndReduceToSourceProperty()
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

  openAssimilationPanel() {
    this.moreOptions().openAssimilationPanel()
    waitUntilAppIsNotBusy()
    return assumeAssimilationPage()
  },
  setLevel(level: number) {
    this.addRichNoteProperty('note_level', `${level}`)
    return this
  },
  expectWithoutAssimilationPanel() {
    cy.url({ timeout: 15000 }).should('match', /\/d\/n\/\d+|\/n\/\d+|\/n\d+/)
    cy.get(assimilationModesSelector).should('not.exist')
    return this
  },
})
