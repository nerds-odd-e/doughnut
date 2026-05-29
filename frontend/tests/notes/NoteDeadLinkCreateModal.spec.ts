import {
  NoteController,
  NotebookController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteDeadLinkCreateModal from "@/components/notes/NoteDeadLinkCreateModal.vue"
import { softKeyboardPrimerId } from "@/utils/focusTarget"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  focusDirective,
  modalBodyStub,
  mountSoftKeyboardPrimer,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import createNoteStorage from "@/store/createNoteStorage"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: vi.fn().mockResolvedValue(false),
      alert: vi.fn(),
      options: vi.fn(),
      done: vi.fn(),
      register: vi.fn(),
      peek: vi.fn(),
    },
  }),
}))

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const createNoteLabel = /Create a new note named/
const linkNoteLabel = "Link to an existing note"

describe("NoteDeadLinkCreateModal", () => {
  const noteRealm = makeMe.aNoteRealm.title("Ghost Page").please()
  const deadLinkPayload = {
    targetToken: "Ghost Page",
    displayText: "Ghost Page",
  }
  const commonProps = {
    notebookId: noteRealm.notebookRealm.notebook.id,
    noteRealm,
    modelValue: deadLinkPayload,
    sourceNoteId: noteRealm.note.id,
  }

  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage()
    storageAccessor.value.refreshNoteRealm(noteRealm)

    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    matchMediaSpy = undefined
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountModal = () => {
    mountSoftKeyboardPrimer()
    wrapper = mount(NoteDeadLinkCreateModal, {
      props: commonProps,
      attachTo: document.body,
      global: {
        plugins: [router],
        stubs: { Modal: modalBodyStub },
        directives: { focus: focusDirective },
      },
    })
    return wrapper
  }

  const waitForChooser = async () => {
    await vi.waitUntil(() => screen.queryByText(createNoteLabel) !== null, {
      timeout: 1000,
    })
  }

  const tapChooser = (label: RegExp | string) => {
    screen.getByText(label).click()
  }

  const tapChooserAndSettle = async (label: RegExp | string) => {
    tapChooser(label)
    await flushPromises()
    await wrapper?.vm.$nextTick()
  }

  const waitUntilFocused = async (selector: string) => {
    const element = await vi.waitUntil(
      () => {
        const el = document.querySelector(selector) as HTMLElement | null
        return el !== null && document.activeElement === el ? el : null
      },
      { timeout: 2000 }
    )
    expect(document.activeElement).toBe(element)
    return element
  }

  describe("soft keyboard primer", () => {
    it.each([
      { branch: "create", label: createNoteLabel },
      { branch: "link", label: linkNoteLabel },
    ])("focuses primer synchronously when $branch is tapped on touch device", async ({
      label,
    }) => {
      matchMediaSpy = mockCoarsePointer(true)
      mountModal()
      await waitForChooser()
      const primer = document.getElementById(softKeyboardPrimerId)
      expect(primer).toBeTruthy()

      tapChooser(label)

      expect(document.activeElement).toBe(primer)
    })

    it("transfers focus to note title after create form mounts", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      mountModal()
      await waitForChooser()

      await tapChooserAndSettle(createNoteLabel)

      await waitUntilFocused('[data-test="note-title"]')
    })

    it("transfers focus to search input after link form mounts", async () => {
      matchMediaSpy = mockCoarsePointer(true)
      mountModal()
      await waitForChooser()

      await tapChooserAndSettle(linkNoteLabel)

      await waitUntilFocused('input[placeholder="Search"]')
    })

    it("does not focus primer on create tap when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountModal()
      await waitForChooser()
      const primer = document.getElementById(softKeyboardPrimerId)

      await tapChooserAndSettle(createNoteLabel)

      expect(document.activeElement).not.toBe(primer)
      await waitUntilFocused('[data-test="note-title"]')
    })
  })
})
