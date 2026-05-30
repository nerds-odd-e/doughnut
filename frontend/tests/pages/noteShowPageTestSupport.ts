import {
  AiController,
  AssimilationController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { resetAssimilationViewForTests } from "@/composables/useAssimilationView"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockNotebookGetForNoteRealm, mockSdkService } from "@tests/helpers"

export function createNoteShowPageRouter() {
  return createRouter({
    history: createWebHistory(),
    routes,
  })
}

export function setupNoteShowPageAssimilationPanelMocks() {
  const noteRealm = makeMe.aNoteRealm.please()
  resetAssimilationViewForTests()
  mockSdkService(NoteController, "showNote", noteRealm)
  mockNotebookGetForNoteRealm(noteRealm, {
    id: 101,
    name: "a circle",
  })
  mockSdkService(NoteController, "getNoteInfo", {})
  mockSdkService(AiController, "generateUnderstandingChecklist", {
    points: [],
  })
  mockSdkService(AssimilationController, "assimilate", [])
  return noteRealm
}

export async function withStubbedInnerWidth<T>(
  width: number,
  run: () => Promise<T>
): Promise<T> {
  const innerWidthDesc = Object.getOwnPropertyDescriptor(window, "innerWidth")
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    writable: true,
    value: width,
  })
  try {
    return await run()
  } finally {
    if (innerWidthDesc) {
      Object.defineProperty(window, "innerWidth", innerWidthDesc)
    }
  }
}
