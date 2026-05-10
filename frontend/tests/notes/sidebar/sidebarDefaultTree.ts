import makeMe from "doughnut-test-fixtures/makeMe"
import { testFolderStub } from "@tests/helpers"
import {
  FOLDER_FIRST_GEN_CHILDREN_ID,
  FOLDER_TOP_NOTE_CHILDREN_ID,
  structuralFolder,
  type SidebarTreeFixtures,
} from "./sidebarTestSupport"

const topNoteRealm = makeMe.aNoteRealm.title("top").please()
const firstGeneration = makeMe.aNoteRealm
  .title("first gen")
  .under(topNoteRealm)
  .ancestorFolders([
    testFolderStub(
      FOLDER_TOP_NOTE_CHILDREN_ID,
      topNoteRealm.note.noteTopology.title
    ),
  ])
  .please()
const firstGenerationSibling = makeMe.aNoteRealm
  .title("first gen sibling")
  .under(topNoteRealm)
  .ancestorFolders([
    testFolderStub(
      FOLDER_TOP_NOTE_CHILDREN_ID,
      topNoteRealm.note.noteTopology.title
    ),
  ])
  .please()
const secondGeneration = makeMe.aNoteRealm
  .title("2nd gen")
  .under(firstGeneration)
  .ancestorFolders([
    testFolderStub(
      FOLDER_TOP_NOTE_CHILDREN_ID,
      topNoteRealm.note.noteTopology.title
    ),
    testFolderStub(
      FOLDER_FIRST_GEN_CHILDREN_ID,
      firstGeneration.note.noteTopology.title
    ),
  ])
  .please()

const defaultTreeFolderListings: SidebarTreeFixtures["defaultTreeFolderListings"] =
  {
    [String(undefined)]: {
      noteTopologies: [{ ...topNoteRealm.note.noteTopology }],
      folders: [structuralFolder(FOLDER_TOP_NOTE_CHILDREN_ID, topNoteRealm)],
    },
    [String(FOLDER_TOP_NOTE_CHILDREN_ID)]: {
      noteTopologies: [
        { ...firstGeneration.note.noteTopology },
        { ...firstGenerationSibling.note.noteTopology },
      ],
      folders: [
        structuralFolder(FOLDER_FIRST_GEN_CHILDREN_ID, firstGeneration),
      ],
    },
    [String(FOLDER_FIRST_GEN_CHILDREN_ID)]: {
      noteTopologies: [{ ...secondGeneration.note.noteTopology }],
      folders: [],
    },
  }

export const sidebarDefaultTreeFixtures: SidebarTreeFixtures = {
  topNoteRealm,
  firstGeneration,
  firstGenerationSibling,
  secondGeneration,
  defaultTreeFolderListings,
}
