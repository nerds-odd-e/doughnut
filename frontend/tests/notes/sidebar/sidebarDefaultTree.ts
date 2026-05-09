import makeMe from "doughnut-test-fixtures/makeMe"
import { testFolderStub } from "@tests/helpers"
import {
  FOLDER_FIRST_GEN_CHILDREN_ID,
  FOLDER_TOP_NOTE_CHILDREN_ID,
  noteTopologyInFolder,
  structuralFolder,
  type SidebarTreeFixtures,
} from "./sidebarTestSupport"

const topNoteRealm = makeMe.aNoteRealm.title("top").please()
const firstGeneration = makeMe.aNoteRealm
  .title("first gen")
  .under(topNoteRealm)
  .please()
const firstGenerationSibling = makeMe.aNoteRealm
  .title("first gen sibling")
  .under(topNoteRealm)
  .please()
const secondGeneration = makeMe.aNoteRealm
  .title("2nd gen")
  .under(firstGeneration)
  .please()

const defaultTreeFolderListings: SidebarTreeFixtures["defaultTreeFolderListings"] =
  {
    [String(undefined)]: {
      noteTopologies: [
        {
          ...topNoteRealm.note.noteTopology,
          folderId: topNoteRealm.note.noteTopology.folderId ?? 0,
        },
      ],
      folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
    },
    [String(FOLDER_TOP_NOTE_CHILDREN_ID)]: {
      noteTopologies: [
        noteTopologyInFolder(firstGeneration, FOLDER_TOP_NOTE_CHILDREN_ID),
        noteTopologyInFolder(
          firstGenerationSibling,
          FOLDER_TOP_NOTE_CHILDREN_ID
        ),
      ],
      folders: [
        structuralFolder(FOLDER_FIRST_GEN_CHILDREN_ID, firstGeneration),
      ],
    },
    [String(FOLDER_FIRST_GEN_CHILDREN_ID)]: {
      noteTopologies: [
        noteTopologyInFolder(secondGeneration, FOLDER_FIRST_GEN_CHILDREN_ID),
      ],
      folders: [],
    },
  }

const folderContextByRealmId: SidebarTreeFixtures["folderContextByRealmId"] = {
  [topNoteRealm.id]: { ancestorFolders: [] },
  [firstGeneration.id]: {
    ancestorFolders: [
      testFolderStub(
        FOLDER_TOP_NOTE_CHILDREN_ID,
        topNoteRealm.note.noteTopology.title
      ),
    ],
    folderId: FOLDER_TOP_NOTE_CHILDREN_ID,
  },
  [firstGenerationSibling.id]: {
    ancestorFolders: [
      testFolderStub(
        FOLDER_TOP_NOTE_CHILDREN_ID,
        topNoteRealm.note.noteTopology.title
      ),
    ],
    folderId: FOLDER_TOP_NOTE_CHILDREN_ID,
  },
  [secondGeneration.id]: {
    ancestorFolders: [
      testFolderStub(
        FOLDER_TOP_NOTE_CHILDREN_ID,
        topNoteRealm.note.noteTopology.title
      ),
      testFolderStub(
        FOLDER_FIRST_GEN_CHILDREN_ID,
        firstGeneration.note.noteTopology.title
      ),
    ],
    folderId: FOLDER_FIRST_GEN_CHILDREN_ID,
  },
}

export const sidebarDefaultTreeFixtures: SidebarTreeFixtures = {
  topNoteRealm,
  firstGeneration,
  firstGenerationSibling,
  secondGeneration,
  defaultTreeFolderListings,
  folderContextByRealmId,
}
