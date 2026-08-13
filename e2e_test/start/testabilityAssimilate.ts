/// <reference types="Cypress" />
// @ts-check
import type { AssimilationRequestDto } from '@generated/doughnut-backend-api'
import { AssimilationController } from '@generated/doughnut-backend-api/sdk.gen'

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
    return assimilateInjectedNote.call(this, noteTitle, {
      skipMemoryTracking: false,
    })
  },

  assimilateNoteSkippingRecall(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {
      skipMemoryTracking: true,
    })
  },

  assimilateNoteAsCommissioned(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {
      skipMemoryTracking: false,
      assimilateAsCommissioned: true,
    })
  },

  assimilateNoteAsSpelling(this: InjectedNoteIds, noteTitle: string) {
    return assimilateInjectedNote.call(this, noteTitle, {
      skipMemoryTracking: false,
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
