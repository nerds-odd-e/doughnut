/// <reference types="Cypress" />
// @ts-check
import { ConversationMessageController } from '@generated/donut-backend-api/sdk.gen'
import type { InjectedNoteIds } from './testabilityNote'

const conversationIdAlias = (noteTitle: string) =>
  `conversationId_${noteTitle.replace(/\s+/g, '_')}`

export const conversationTestabilityMethods = {
  startConversationAboutNote(
    this: InjectedNoteIds,
    noteTitle: string,
    message: string
  ) {
    return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
      cy
        .wrap(
          ConversationMessageController.startConversationAboutNote({
            path: { note: noteId },
            body: message,
          }),
          { log: false }
        )
        .then((conversation) => {
          cy.wrap(conversation.id).as(conversationIdAlias(noteTitle))
        })
    )
  },

  replyToConversationAboutNote(noteTitle: string, messages: readonly string[]) {
    return cy
      .get<number>(`@${conversationIdAlias(noteTitle)}`)
      .then((conversationId) => {
        cy.wrap(messages).each((message: string) => {
          cy.wrap(
            ConversationMessageController.replyToConversation({
              path: { conversationId },
              body: message,
            }),
            { log: false }
          )
        })
      })
  },
}
