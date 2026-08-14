/// <reference types="Cypress" />
// @ts-check
import type { AssimilationRequestDto } from '@generated/doughnut-backend-api'
import {
  AssimilationController,
  AssimilationSequenceSkipController,
} from '@generated/doughnut-backend-api/sdk.gen'

type InjectedNoteIds = {
  getInjectedNoteIdByTitle(noteTitle: string): Cypress.Chainable<number>
}

function assimilateInjectedNote(
  this: InjectedNoteIds,
  noteTitle: string,
  body: Omit<AssimilationRequestDto, 'noteId'>
) {
  return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
    cy.wrap(
      AssimilationController.assimilate({
        body: { noteId, ...body },
      }),
      { log: false }
    )
  )
}

export const assimilateTestabilityMethods = {
  assimilateNote(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {})
  },

  skipNoteFromAssimilationSequence(this: InjectedNoteIds, noteTitle: string) {
    return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
      cy.wrap(
        AssimilationSequenceSkipController.create({
          body: { noteId },
        }),
        { log: false }
      )
    )
  },

  assimilateNoteAsCommissioned(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {
      assimilateAsCommissioned: true,
    })
  },

  assimilateNoteAsSpelling(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {
      assimilateAsSpelling: true,
    })
  },

  assimilateNoteProperty(
    this: InjectedNoteIds,
    noteTitle: string,
    propertyKey: string
  ) {
    return assimilateInjectedNote.call(this, noteTitle, { propertyKey })
  },
}
