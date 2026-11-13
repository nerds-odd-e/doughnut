/// <reference types="Cypress" />
// @ts-check
import type {
  NoteTestData,
  NotesTestDataWritable,
} from '@generated/donut-backend-api'
import {
  TestabilityRestController,
  TextContentController,
} from '@generated/donut-backend-api/sdk.gen'
import { noteIdFromUrl } from './noteIdFromUrl'
import { unwrapData } from './unwrapApi'

export const injectedNoteIdMapAliasName = 'injectedNoteIdMap'

export type InjectedNoteIds = {
  getInjectedNoteIdByTitle(noteTitle: string): Cypress.Chainable<number>
}

export type NoteInjectionContext = {
  injectNotes(
    noteTestData: NoteTestData[],
    externalIdentifier: string,
    notebookName: string,
    circleName?: string | null
  ): Cypress.Chainable<Record<string, number>>
}

export const noteTestabilityMethods = {
  injectNotes(
    noteTestData: NoteTestData[],
    externalIdentifier: string,
    notebookName: string,
    circleName: string | null = null
  ) {
    const requestBody: NotesTestDataWritable = {
      externalIdentifier,
      notebookName,
      circleName: circleName ?? undefined,
      noteTestData,
    }

    return cy
      .wrap(externalIdentifier)
      .as('injectNotesExternalIdentifier')
      .then(() =>
        cy
          .get<Record<string, number>>(`@${injectedNoteIdMapAliasName}`)
          .then((existingMap) => {
            return cy
              .wrap(
                TestabilityRestController.injectNotes({
                  body: requestBody,
                }).then((response) => {
                  const data = unwrapData<Record<string, number>>(response)
                  const expectedCount = noteTestData.length
                  const actualCount = Object.keys(data).length
                  expect(
                    actualCount,
                    `Expected ${expectedCount} notes to be created, but backend only created ${actualCount} notes`
                  ).to.equal(expectedCount)
                  return { ...existingMap, ...data }
                }),
                { log: false }
              )
              .as(injectedNoteIdMapAliasName)
          })
      )
  },

  injectNoteWithContent(
    this: NoteInjectionContext,
    noteTitle: string,
    content: string,
    externalIdentifier: string,
    notebookName: string,
    folder?: string
  ) {
    const note: NoteTestData = { Title: noteTitle, Content: content }
    if (folder !== undefined) {
      note.Folder = folder
    }
    return this.injectNotes([note], externalIdentifier, notebookName)
  },

  rememberUiCreatedNote(noteTitle: string) {
    return cy
      .get<Record<string, number>>(`@${injectedNoteIdMapAliasName}`)
      .then((existingMap) => {
        return cy.url().then((url) => {
          const m =
            url.match(/\/n(\d+)/) ??
            url.match(/\/n\/(\d+)/) ??
            url.match(/\/d\/n\/(\d+)/)
          expect(
            m,
            `could not parse note id from URL (expected /n<id>, /n/<id>, or legacy /d/n/<id>): ${url}`
          ).to.not.be.null
          const noteId = Number(m![1])
          const map = existingMap as Record<string, number>
          return cy
            .wrap({ ...map, [noteTitle]: noteId })
            .as(injectedNoteIdMapAliasName)
        })
      })
  },

  getInjectedNoteIdByTitle(noteTopology: string) {
    return cy
      .get<Record<string, number>>(`@${injectedNoteIdMapAliasName}`)
      .then((injectedNoteIdMap) => {
        expect(
          injectedNoteIdMap,
          `"${noteTopology}" is not in the injected note. Did you created during the test?`
        ).to.have.property(noteTopology)
        return injectedNoteIdMap[noteTopology]
      })
  },

  setInjectedNoteContent(
    this: InjectedNoteIds,
    noteTitle: string,
    content: string
  ) {
    return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
      cy.wrap(
        TextContentController.updateNoteContent({
          path: { note: noteId },
          body: { content },
        }),
        { log: false }
      )
    )
  },

  renameInjectedNoteTitleForNoteOnPage(newTitle: string) {
    return cy.url().then((url) => {
      const noteId = noteIdFromUrl(url)
      return cy
        .get<Record<string, number>>(`@${injectedNoteIdMapAliasName}`)
        .then((existingMap) => {
          const map = existingMap as Record<string, number>
          const oldKey = Object.keys(map).find((key) => map[key] === noteId)
          if (!oldKey || oldKey === newTitle) {
            return
          }
          const { [oldKey]: _removed, ...rest } = map
          return cy
            .wrap({ ...rest, [newTitle]: noteId })
            .as(injectedNoteIdMapAliasName)
        })
    })
  },
}
