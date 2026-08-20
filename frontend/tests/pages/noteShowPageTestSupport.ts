import {
  AiController,
  AssimilationController,
  ConversationMessageController,
  NoteController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { Circle } from "@generated/doughnut-backend-api"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import NoteShowPage from "@/pages/NoteShowPage.vue"
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

function noteShowPageHelper(
  component: typeof NoteShowPage | typeof NoteShowPageWithNotebookSidebarLayout,
  router: Router,
  noteId: number
) {
  return helper
    .component(component)
    .withCurrentUser(makeMe.aUser.please())
    .withCleanStorage()
    .withProps({ noteId })
    .withRouter(router)
}

function noteShowPageWithSidebarLayoutMount(router: Router, noteId: number) {
  return noteShowPageHelper(
    NoteShowPageWithNotebookSidebarLayout,
    router,
    noteId
  )
}

function noteShowPageMount(router: Router, noteId: number) {
  return noteShowPageHelper(NoteShowPage, router, noteId)
}

export async function renderNoteShowPage(router: Router, noteId: number) {
  noteShowPageWithSidebarLayoutMount(router, noteId)
    .currentRoute(noteShowLocation(noteId))
    .render()
  await flushPromises()
}

export async function renderNoteShowPageWithConversation(
  router: Router,
  noteId: number
) {
  await router.push(noteShowConversationLocation(noteId))
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
    "button.maximize-button"
  ) as HTMLButtonElement | null
}

export function closeConversationButtonEl() {
  return document.querySelector(
    "button.minimize-button"
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
