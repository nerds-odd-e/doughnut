/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  type DataTable,
  Given,
  Then,
  When,
} from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

function omitBlankOptionalInjectionFields(rows: Record<string, string>[]) {
  return rows.map((row) => {
    const next = { ...row }
    for (const [key, value] of Object.entries(next)) {
      if (key === 'Title') {
        continue
      }
      if (typeof value === 'string' && value.trim() === '') {
        delete next[key]
      }
    }
    return next
  })
}

function injectNoteWithContentForCurrentUser(
  notebookName: string,
  noteTitle: string,
  content: string,
  folder?: string
) {
  cy.get<string>('@currentLoginUser').then((username) =>
    start
      .testability()
      .injectNoteWithContent(noteTitle, content, username, notebookName, folder)
  )
}

Given(
  'I have a notebook {string} with notes:',
  (notebookName: string, data: DataTable) => {
    const notes = omitBlankOptionalInjectionFields(data.hashes())
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, notebookName)
    )
  }
)

Given('I have a notebook {string}', (notebookName: string) => {
  cy.get<string>('@currentLoginUser').then((username) =>
    start.testability().injectNotes([], username, notebookName)
  )
})

Given(
  'I have a notebook {string} with a note {string}',
  (notebookName: string, noteTitle: string) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start
        .testability()
        .injectNotes([{ Title: noteTitle }], username, notebookName)
    )
  }
)

Given(
  'I have a notebook {string} with a note {string} and content {string}',
  (notebookName: string, noteTitle: string, content: string) => {
    injectNoteWithContentForCurrentUser(notebookName, noteTitle, content)
  }
)

Given(
  'I have a note {string} under notebook {string} with content:',
  (noteTitle: string, notebookName: string, content: string) => {
    injectNoteWithContentForCurrentUser(notebookName, noteTitle, content)
  }
)

Given(
  'I have a note {string} under notebook {string} in folder {string} with content:',
  (
    noteTitle: string,
    notebookName: string,
    folder: string,
    content: string
  ) => {
    injectNoteWithContentForCurrentUser(
      notebookName,
      noteTitle,
      content,
      folder
    )
  }
)

Given('note {string} has content:', (noteTitle: string, content: string) => {
  start.testability().setInjectedNoteContent(noteTitle, content)
})

/** A change made in Donut itself, as opposed to one made in an exported markdown file. */
When(
  'the note {string} is changed in Donut to {string}',
  (noteTitle: string, content: string) => {
    start.testability().setInjectedNoteContent(noteTitle, content)
  }
)

Then(
  'the note {string} in Donut should still hold {string}',
  (noteTitle: string, expected: string) => {
    start
      .testability()
      .getInjectedNoteContent(noteTitle)
      .should('equal', expected)
  }
)

Given(
  'the notebook {string} has an empty folder {string}',
  (notebookName: string, folderName: string) => {
    start.testability().createEmptyFolder(notebookName, folderName)
  }
)

Given(
  'the notebook {string} has a folder {string} under note {string}',
  (notebookName: string, folderName: string, underNoteTitle: string) => {
    start
      .testability()
      .createEmptyFolder(notebookName, folderName, underNoteTitle)
  }
)

Given(
  'the notebook {string} has a readme-only folder {string} with readme {string}',
  (notebookName: string, folderName: string, readme: string) => {
    start.testability().createReadmeOnlyFolder(notebookName, folderName, readme)
  }
)

Given(
  'the notebook {string} has readme content {string}',
  (notebookName: string, content: string) => {
    start.testability().setNotebookReadmeContent(notebookName, content)
  }
)

Given(
  'there are some notes for existing user {string} in notebook {string}',
  (externalIdentifier: string, notebookName: string, data: DataTable) => {
    const hashes = data
      .hashes()
      .map((row) =>
        Object.fromEntries(
          Object.entries(row).map(([key, value]) => [key.trim(), value])
        )
      )
    return start
      .testability()
      .injectNotes(hashes, externalIdentifier, notebookName)
  }
)

Given(
  'there is a notebook {string} with a note {string} from user {string} shared to the Bazaar',
  (notebookName: string, noteTitle: string, externalIdentifier: string) => {
    start
      .testability()
      .injectNotes([{ Title: noteTitle }], externalIdentifier, notebookName)
      .then(() => {
        return start.testability().shareToBazaar(notebookName)
      })
  }
)

Given(
  'there are notes from Note {int} to Note {int}',
  (from: number, to: number) => {
    const notes = Array(to - from + 1)
      .fill(0)
      .map((_, i) => {
        return { Title: `Note ${i + from}` }
      })
    cy.get<string>('@currentLoginUser').then((username) =>
      start.testability().injectNotes(notes, username, `Note ${from}`)
    )
  }
)
