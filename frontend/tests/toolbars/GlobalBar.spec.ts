import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import SoftKeyboardPrimer from "@/components/commons/SoftKeyboardPrimer.vue"
import type { User } from "@generated/doughnut-backend-api"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import {
  scheduleFocusTargetWithin,
  softKeyboardPrimerId,
} from "@/utils/focusTarget"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import { screen } from "@testing-library/vue"
import { mount } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi, describe, it, expect, afterEach } from "vitest"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { defineComponent, h, provide, ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { useGlobalNoteSearchKeyboardShortcut } from "@/composables/useGlobalNoteSearchKeyboardShortcut"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/" }),
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

function dispatchNoteSearchShortcut(modifiers: KeyboardEventInit) {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "f",
      code: "KeyF",
      bubbles: true,
      cancelable: true,
      ...modifiers,
    })
  )
}

function renderGlobalBarWithSearchShortcut(loggedInUser?: User) {
  const userRef = ref(loggedInUser)
  const Harness = defineComponent({
    setup() {
      provide("currentUser", userRef)
      useGlobalNoteSearchKeyboardShortcut(userRef)
      return () => h(GlobalBar)
    },
  })
  return helper.component(Harness).withCleanStorage().render()
}

describe("global bar", () => {
  let noteEditingHistory: NoteEditingHistory
  let user: User
  let matchMediaSpy: ReturnType<typeof vi.spyOn> | undefined

  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults (used by LinkNoteDialog in GlobalBar)
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
    user = makeMe.aUser.please()
    noteEditingHistory = new NoteEditingHistory()
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage(noteEditingHistory)
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
  })

  it("fetch API to be called ONCE", async () => {
    helper.component(GlobalBar).withCurrentUser(user).render()

    expect(screen.queryByTitle("undo")).toBeNull()
  })

  it("show undo when there is something to undo", async () => {
    const note = makeMe.aNote.please()
    noteEditingHistory.deleteNote(note.id)
    helper.component(GlobalBar).withCurrentUser(user).render()

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled()
  })

  it.each([
    { label: "Ctrl+F", modifiers: { ctrlKey: true } },
    { label: "Cmd+F", modifiers: { metaKey: true } },
  ])("opens note search on $label when logged in", async ({ modifiers }) => {
    renderGlobalBarWithSearchShortcut(user)
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    dispatchNoteSearchShortcut(modifiers)
    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("does not open note search on Ctrl+Shift+F when logged in", async () => {
    renderGlobalBarWithSearchShortcut(user)
    dispatchNoteSearchShortcut({ ctrlKey: true, shiftKey: true })
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })

  it("does not open note search on Ctrl+F when logged out", async () => {
    renderGlobalBarWithSearchShortcut()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    dispatchNoteSearchShortcut({ ctrlKey: true })
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })

  it("focuses primer synchronously when search button is tapped on touch device", async () => {
    matchMediaSpy = mockCoarsePointer(true)

    const router = createRouter({
      history: createWebHistory(),
      routes,
    })
    const GlobalBarWithPrimer = defineComponent({
      components: { SoftKeyboardPrimer, GlobalBar },
      template: "<SoftKeyboardPrimer /><GlobalBar />",
    })

    mount(GlobalBarWithPrimer, {
      global: {
        plugins: [router],
        provide: { currentUser: ref(user) },
        directives: {
          focus: {
            mounted(el: HTMLElement) {
              el.setAttribute("data-autofocus", "true")
              scheduleFocusTargetWithin(el)
            },
          },
        },
      },
      attachTo: document.body,
    })

    const searchButton = screen.getByTitle("Search note (Ctrl+F / Cmd+F)")
    searchButton.click()

    expect(document.activeElement).toBe(
      document.getElementById(softKeyboardPrimerId)
    )

    const searchInput = await screen.findByPlaceholderText("Search")
    await vi.waitUntil(() => document.activeElement === searchInput, {
      timeout: 2000,
    })

    expect(document.activeElement).toBe(searchInput)
  })
})
