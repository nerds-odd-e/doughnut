import {
  AiController,
  AssimilationController,
  ConversationMessageController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { Circle } from "@generated/doughnut-backend-api"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import {
  createRouter,
  createWebHistory,
  type RouteLocationNamedRaw,
  type Router,
} from "vue-router"
import routes from "@/routes/routes"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
} from "@tests/helpers"
import { refinementLayoutItems } from "../components/recall/noteRefinementTestSupport"
import { flushPromises } from "@vue/test-utils"

export function createNoteShowPageRouter() {
  return createRouter({
    history: createWebHistory(),
    routes,
  })
}

export function noteShowConversationLocation(
  noteId: number
): RouteLocationNamedRaw {
  return {
    ...noteShowLocation(noteId),
    query: { conversation: "true" },
  }
}

export function setupNoteShowPageMocks(
  noteRealm = makeMe.aNoteRealm.please(),
  circle?: Circle
) {
  mockSdkService(NoteController, "showNote", noteRealm)
  mockNotebookGetForNoteRealm(noteRealm, circle)
  return noteRealm
}

export function setupNoteShowPageAssimilationPanelMocks() {
  useAssimilationView().dismiss()
  const noteRealm = setupNoteShowPageMocks(makeMe.aNoteRealm.please(), {
    id: 101,
    name: "a circle",
  })
  mockSdkService(NoteController, "getNoteInfo", {})
  mockSdkService(AiController, "generateRefinementSuggestions", {
    items: refinementLayoutItems([]),
  })
  mockSdkService(AssimilationController, "assimilate", [])
  return noteRealm
}

export function setupNoteShowPageConversationMocks() {
  const noteRealm = setupNoteShowPageMocks()
  mockSdkService(ConversationMessageController, "getConversationsAboutNote", [])
  return noteRealm
}

function noteShowPageMount(router: Router, noteId: number) {
  return helper
    .component(NoteShowPageWithNotebookSidebarLayout)
    .withCurrentUser(makeMe.aUser.please())
    .withCleanStorage()
    .withProps({ noteId })
    .withRouter(router)
}

export async function renderNoteShowPage(router: Router, noteId: number) {
  noteShowPageMount(router, noteId)
    .currentRoute(noteShowLocation(noteId))
    .render()
  await flushPromises()
}

export async function renderNoteShowPageWithConversation(
  router: Router,
  noteId: number
) {
  await router.push(noteShowConversationLocation(noteId))
  await flushPromises()
  noteShowPageMount(router, noteId).render()
  await flushPromises()
}

export function noteContentWrapperEl() {
  return document.querySelector(".note-content-wrapper")
}

export function conversationWrapperEl() {
  return document.querySelector(".conversation-wrapper")
}

export function conversationContainerEl() {
  return document.querySelector(".conversation-container")
}

export function toggleMaximizeButtonEl() {
  return document.querySelector(
    '[aria-label="Toggle maximize"]'
  ) as HTMLButtonElement | null
}

export function closeConversationButtonEl() {
  return document.querySelector(
    '[aria-label="Close dialog"]'
  ) as HTMLButtonElement | null
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
