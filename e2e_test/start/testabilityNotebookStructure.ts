/// <reference types="Cypress" />
// @ts-check
import type {
  Folder,
  FolderCreationRequest,
  NotebooksViewedByUser,
} from '@generated/donut-backend-api'
import {
  NotebookController,
  NotebookFolderController,
} from '@generated/donut-backend-api/sdk.gen'
import { unwrapData } from './unwrapApi'

/** Seeds a notebook's structure (folders, readmes, root notes) through the backend API. */

type InjectedNoteIds = {
  getInjectedNoteIdByTitle(noteTitle: string): Cypress.Chainable<number>
}

const getNotebookIdByName = (notebookName: string) =>
  cy.wrap(NotebookController.myNotebooks(), { log: false }).then((response) => {
    const data = unwrapData<NotebooksViewedByUser>(response)
    const notebookRealm = data.notebooks.find(
      (realm) => realm.notebook.name === notebookName
    )
    expect(
      notebookRealm,
      `notebook "${notebookName}" was not found for the current user`
    ).to.not.be.undefined
    return notebookRealm!.notebook.id
  })

const getFolderIdInNotebook = (notebookId: number, folderName: string) =>
  cy
    .wrap(
      NotebookFolderController.listNotebookFolderIndex({
        path: { notebook: notebookId },
      }),
      { log: false }
    )
    .then((response) => {
      const folders = unwrapData<Folder[]>(response)
      const folder = folders.find((f) => f.name === folderName)
      expect(
        folder,
        `folder "${folderName}" was not found in notebook id ${notebookId}`
      ).to.exist
      return folder!.id
    })

const createFolder = (notebookId: number, body: FolderCreationRequest) =>
  cy.wrap(
    NotebookFolderController.createFolder({
      path: { notebook: notebookId },
      body,
    }),
    { log: false }
  )

const updateFolderReadme = (
  notebookId: number,
  folderId: number,
  readme: string
) =>
  cy.wrap(
    NotebookFolderController.updateFolderReadmeContent({
      path: { notebook: notebookId, folder: folderId },
      body: { content: readme },
    }),
    { log: false }
  )

export const notebookStructureTestabilityMethods = {
  getNotebookIdByName,
  getFolderIdInNotebook,

  updateNotebookIndex(notebookName: string) {
    return getNotebookIdByName(notebookName).then((notebookId) =>
      cy.wrap(
        NotebookController.updateNotebookIndex({
          path: { notebook: notebookId },
        }),
        { log: false }
      )
    )
  },

  createEmptyFolder(
    this: InjectedNoteIds,
    notebookName: string,
    folderName: string,
    underNoteTitle?: string
  ) {
    return getNotebookIdByName(notebookName).then((notebookId) => {
      if (underNoteTitle) {
        return this.getInjectedNoteIdByTitle(underNoteTitle).then((noteId) =>
          createFolder(notebookId, { name: folderName, underNoteId: noteId })
        )
      }
      return createFolder(notebookId, { name: folderName })
    })
  },

  createReadmeOnlyFolder(
    notebookName: string,
    folderName: string,
    readme: string
  ) {
    return getNotebookIdByName(notebookName).then((notebookId) =>
      createFolder(notebookId, { name: folderName }).then((response) =>
        updateFolderReadme(notebookId, unwrapData<Folder>(response).id, readme)
      )
    )
  },

  setFolderReadmeContent(
    notebookName: string,
    folderName: string,
    readme: string
  ) {
    return getNotebookIdByName(notebookName).then((notebookId) =>
      getFolderIdInNotebook(notebookId, folderName).then((folderId) =>
        updateFolderReadme(notebookId, folderId, readme)
      )
    )
  },

  createTitleOnlyRootNote(notebookName: string, title: string) {
    return getNotebookIdByName(notebookName).then((notebookId) =>
      cy.wrap(
        NotebookController.createNoteAtNotebookRoot({
          path: { notebook: notebookId },
          body: { newTitle: title },
        }),
        { log: false }
      )
    )
  },

  setNotebookReadmeContent(notebookName: string, content: string) {
    return getNotebookIdByName(notebookName).then((notebookId) =>
      cy.wrap(
        NotebookController.updateNotebookReadmeContent({
          path: { notebook: notebookId },
          body: { content },
        }),
        { log: false }
      )
    )
  },
}
