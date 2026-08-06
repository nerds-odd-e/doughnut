import type {
  FolderListing,
  NoteRealm,
  Options,
  ShowNoteData,
} from "@generated/doughnut-backend-api"
import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import {
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import { expect } from "vitest"

type ListNotebookFolderListingOptions = Parameters<
  typeof NotebookController.listNotebookFolderListing
>[0]

type NoteStorageAccessor = ReturnType<
  typeof import("@/composables/useStorageAccessor")["useStorageAccessor"]
>

export const EMPTY_FOLDER_LISTING: FolderListing = {
  noteTopologies: [],
  folders: [],
}

export function folderListingForQueryParent(
  options: unknown,
  table: Record<string, FolderListing>
): FolderListing {
  const query = (options as ListNotebookFolderListingOptions).query
  return table[String(query?.parent)] ?? EMPTY_FOLDER_LISTING
}

export function countFolderListingCallsForParent(
  listingSpy: { mock: { calls: unknown[][] } },
  parent: number | undefined
) {
  const want = String(parent)
  return listingSpy.mock.calls.filter(
    (call) =>
      String((call[0] as ListNotebookFolderListingOptions).query?.parent) ===
      want
  ).length
}

/** Distinct from note ids — folder listing API uses folder entity ids. */
export const FOLDER_TOP_NOTE_CHILDREN_ID = 77001
export const FOLDER_FIRST_GEN_CHILDREN_ID = 77002

export function structuralFolder(folderId: number, noteRealm: NoteRealm) {
  return testFolderStub(folderId, noteRealm.note.noteTopology.title)
}

export type SidebarTreeFixtures = {
  topNoteRealm: NoteRealm
  firstGeneration: NoteRealm
  firstGenerationSibling: NoteRealm
  secondGeneration: NoteRealm
  defaultTreeFolderListings: Record<string, FolderListing>
}

export function stubNotebookFolderListings(
  defaultTreeFolderListings: Record<string, FolderListing>
) {
  return mockSdkServiceWithImplementation(
    NotebookController,
    "listNotebookFolderListing",
    (options) => folderListingForQueryParent(options, defaultTreeFolderListings)
  )
}

export function mockShowNoteForRealms(realms: NoteRealm[]) {
  const byId = Object.fromEntries(realms.map((r) => [r.id, r])) as Record<
    number,
    NoteRealm
  >
  mockSdkServiceWithImplementation(NoteController, "showNote", (options) => {
    const id = (options as Options<ShowNoteData>).path.note
    const realm = byId[id]
    expect(
      realm,
      `sidebar tests: unmocked showNote for note id ${id}`
    ).toBeDefined()
    return realm!
  })
}

export const DEFAULT_ROOT_PEER_ORDER = [
  "folder:banana",
  "folder:mango",
  "note:apple",
  "note:zebra",
] as const

export function setupRootPeersWithFolders(options: {
  storageAccessor: NoteStorageAccessor
  topNoteRealm: NoteRealm
  realmZ: NoteRealm
  realmA: NoteRealm
  folderExtras?: {
    mango?: Partial<ReturnType<typeof testFolderStub>>
    banana?: Partial<ReturnType<typeof testFolderStub>>
  }
}) {
  const { storageAccessor, topNoteRealm, realmZ, realmA, folderExtras } =
    options
  const nbId = topNoteRealm.notebookRealm.notebook.id
  storageAccessor.value.refOfNoteRealm(realmZ.id).value = realmZ
  storageAccessor.value.refOfNoteRealm(realmA.id).value = realmA
  storageAccessor.value.refOfNoteRealm(topNoteRealm.id).value = topNoteRealm

  const folderBanana = {
    ...testFolderStub(9001, "banana"),
    ...folderExtras?.banana,
  }
  const folderMango = {
    ...testFolderStub(9002, "mango"),
    ...folderExtras?.mango,
  }

  const rootPeersFolderListings: Record<string, FolderListing> = {
    [String(undefined)]: {
      noteTopologies: [realmZ.note.noteTopology, realmA.note.noteTopology],
      folders: [folderMango, folderBanana],
    },
  }
  mockSdkServiceWithImplementation(
    NotebookController,
    "listNotebookFolderListing",
    (options) => folderListingForQueryParent(options, rootPeersFolderListings)
  )
  mockShowNoteForRealms([topNoteRealm, realmZ, realmA])
  return { nbId, realmA, realmZ }
}
