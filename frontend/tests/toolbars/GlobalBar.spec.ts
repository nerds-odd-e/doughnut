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
import { defineComponent, ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

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

  it("opens note search on Ctrl+F when logged in", async () => {
    helper.component(GlobalBar).withCurrentUser(user).render()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("does not open note search on Ctrl+F when logged out", async () => {
    helper.component(GlobalBar).render()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
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
