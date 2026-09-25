/// <reference types="Cypress" />
// @ts-check
import type {
  FolderListing,
  FolderRealm,
  NoteRealm,
  NotebookRealm,
} from '@generated/donut-backend-api'
import {
  NoteController,
  NotebookController,
  NotebookFolderController,
} from '@generated/donut-backend-api/sdk.gen'
import { e2eAppBaseUrl } from '../support/e2eAppUrl'
import testability from './testability'
import { unwrapData } from './unwrapApi'

/**
 * What Donut itself holds at a notebook path ("Notebook/Folder/.../Title"), read through the
 * backend API. Features whose subject is not the web UI (e.g. CLI publication) observe results
 * here instead of navigating pages.
 */

type FolderLocation = { folderId?: number; listing: FolderListing }

const pathText = (path: string[]) => path.join('/')

/** Markdown body with any leading YAML frontmatter removed. */
const bodyOf = (markdown: string | undefined) =>
  (markdown ?? '').replace(/^---\n[\s\S]*?\n---\n?/, '').trim()

function folderListing(notebookId: number, parentFolderId?: number) {
  return cy
    .wrap(
      NotebookFolderController.listNotebookFolderListing({
        path: { notebook: notebookId },
        query: { parent: parentFolderId },
      }),
      { log: false }
    )
    .then((response) => unwrapData<FolderListing>(response))
}

function locateFolder(
  notebookId: number,
  folderNames: string[],
  parentFolderId?: number
): Cypress.Chainable<FolderLocation> {
  return folderListing(notebookId, parentFolderId).then((listing) => {
    const [name, ...rest] = folderNames
    if (name === undefined) {
      return cy.wrap<FolderLocation>(
        { folderId: parentFolderId, listing },
        { log: false }
      )
    }
    const folder = listing.folders?.find((f) => f.name === name)
    expect(
      folder,
      `folder "${name}" in Donut; found folders ${JSON.stringify(listing.folders?.map((f) => f.name) ?? [])}`
    ).to.exist
    return locateFolder(notebookId, rest, folder!.id)
  })
}

function locateContainer(path: string[]) {
  const [notebookName, ...folderNames] = path
  return testability()
    .getNotebookIdByName(notebookName!)
    .then((notebookId) =>
      locateFolder(notebookId, folderNames).then((location) => ({
        notebookId,
        ...location,
      }))
    )
}

function showNote(noteId: number) {
  return cy
    .wrap(NoteController.showNote({ path: { note: noteId } }), { log: false })
    .then((response) => unwrapData<NoteRealm>(response))
}

function expectNoteRealm(
  realm: NoteRealm,
  notePath: string[],
  expectedBody: string
) {
  const actualPath = [
    realm.notebookRealm.notebook.name,
    ...(realm.ancestorFolders ?? []).map((f) => f.name),
    realm.note.noteTopology.title,
  ]
  expect(pathText(actualPath), 'note path in Donut').to.equal(
    pathText(notePath)
  )
  expect(
    bodyOf(realm.note.content),
    `content of "${pathText(notePath)}" in Donut (frontmatter excluded)`
  ).to.equal(expectedBody)
}

export const donutNotebookContent = {
  expectNoteContent(notePath: string[], expectedBody: string) {
    const title = notePath[notePath.length - 1]
    locateContainer(notePath.slice(0, -1)).then(({ listing }) => {
      const note = listing.noteTopologies?.find((n) => n.title === title)
      expect(
        note,
        `note "${pathText(notePath)}" in Donut; found notes ${JSON.stringify(listing.noteTopologies?.map((n) => n.title) ?? [])}`
      ).to.exist
      showNote(note!.id).then((realm) =>
        expectNoteRealm(realm, notePath, expectedBody)
      )
    })
    return this
  },

  expectNoteByIdAt(noteId: number, notePath: string[], expectedBody: string) {
    showNote(noteId).then((realm) =>
      expectNoteRealm(realm, notePath, expectedBody)
    )
    return this
  },

  /** Notebook readme for a one-segment path, otherwise the folder's readme. */
  expectReadme(containerPath: string[], expectedBody: string) {
    locateContainer(containerPath).then(({ notebookId, folderId }) => {
      const readme =
        folderId === undefined
          ? cy
              .wrap(
                NotebookController.get({ path: { notebook: notebookId } }),
                {
                  log: false,
                }
              )
              .then(
                (response) => unwrapData<NotebookRealm>(response).readmeContent
              )
          : cy
              .wrap(
                NotebookFolderController.getFolderPage({
                  path: { notebook: notebookId, folder: folderId },
                }),
                { log: false }
              )
              .then(
                (response) => unwrapData<FolderRealm>(response).readmeContent
              )
      readme.then((markdown) =>
        expect(
          bodyOf(markdown),
          `readme of "${pathText(containerPath)}" in Donut (frontmatter excluded)`
        ).to.equal(expectedBody)
      )
    })
    return this
  },

  expectRootFileDownload(
    notebookName: string,
    filename: string,
    content: string
  ) {
    locateContainer([notebookName]).then(({ notebookId, listing }) => {
      const file = listing.attachments?.find((a) => a.filename === filename)
      expect(
        file,
        `root file "${filename}" in Donut notebook "${notebookName}"; found ${JSON.stringify(listing.attachments?.map((a) => a.filename) ?? [])}`
      ).to.exist
      cy.request(
        `${e2eAppBaseUrl()}/api/notebooks/${notebookId}/attachments/${file!.id}/content`
      ).then((response) => {
        expect(response.body, 'downloaded bytes').to.equal(content)
        expect(response.headers['content-disposition']).to.contain(
          `attachment; filename="${filename}"`
        )
      })
    })
    return this
  },
}
