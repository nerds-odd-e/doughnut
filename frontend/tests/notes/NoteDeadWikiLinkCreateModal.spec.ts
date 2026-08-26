import {
  NoteController,
  NotebookController,
  SearchController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteDeadWikiLinkCreateModal from "@/components/notes/NoteDeadWikiLinkCreateModal.vue"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  focusDirective,
  modalBodyStub,
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
  waitUntilFocused,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import createNoteStorage from "@/store/createNoteStorage"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { screen } from "@testing-library/vue"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const { alertMock } = vi.hoisted(() => ({
  alertMock: vi.fn(),
}))

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({
    popups: {
      confirm: vi.fn().mockResolvedValue(false),
      alert: alertMock,
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
const pointAtExistingNoteLabel = "Point at an existing note"

describe("NoteDeadWikiLinkCreateModal", () => {
  const noteRealm = makeMe.aNoteRealm.title("Ghost Page").please()
  const deadWikiLinkPayload = {
    targetToken: "Ghost Page",
    displayText: "Ghost Page",
  }
  const commonProps = {
    notebookId: noteRealm.notebookRealm.notebook.id,
    noteRealm,
    modelValue: deadWikiLinkPayload,
    sourceNoteId: noteRealm.note.id,
  }

  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
    alertMock.mockReset()
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
    vi.useRealTimers()
  })

  const mountModal = (
    modelValue: typeof deadWikiLinkPayload = deadWikiLinkPayload
  ) => {
    mountSoftKeyboardPrimer()
    wrapper = mount(NoteDeadWikiLinkCreateModal, {
      props: { ...commonProps, modelValue },
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
    await flushPromises()
    expect(screen.getByText(createNoteLabel)).toBeTruthy()
  }

  const tapChooser = (label: RegExp | string) => {
    screen.getByText(label).click()
  }

  const tapChooserAndSettle = async (label: RegExp | string) => {
    tapChooser(label)
    await flushPromises()
    await wrapper?.vm.$nextTick()
  }

  it("shows create-or-retarget choice with dead wiki link copy", async () => {
    mountModal()
    await waitForChooser()
    expect(screen.getByText(/Dead wiki link:/)).toBeTruthy()
    expect(screen.getByText(pointAtExistingNoteLabel)).toBeTruthy()
  })

  it("uses the wiki target as the new note name when display text differs", async () => {
    mountModal({ targetToken: "Ghost Page", displayText: "shown text" })
    await waitForChooser()
    expect(screen.getByText("shown text")).toBeTruthy()

    await tapChooserAndSettle('Create a new note named "Ghost Page"')
    await waitUntilFocused('[data-test="note-title"]')
    const title = (
      document.querySelector('[data-test="note-title"]') as HTMLElement
    ).innerText.trim()
    expect(title).toBe("Ghost Page")
  })

  it("warns and does not show the create form when the target contains a slash", async () => {
    mountModal({
      targetToken: "/WikiPathMdDeadFolder/WikiPathMdDeadMissing.md",
      displayText: "label",
    })
    await waitForChooser()

    await tapChooserAndSettle(createNoteLabel)

    expect(alertMock).toHaveBeenCalledWith(
      "Cannot create a note from a path. You can point at an existing note instead."
    )
    expect(screen.queryByTestId("note-new-form")).toBeNull()
    expect(screen.getByText(pointAtExistingNoteLabel)).toBeTruthy()
  })

  it("shows create-or-retarget choice when reopened after modelValue cleared without close", async () => {
    mountModal()
    await waitForChooser()
    await tapChooserAndSettle(createNoteLabel)
    await waitUntilFocused('[data-test="note-title"]')
    expect(screen.queryByTestId("note-new-form")).not.toBeNull()

    await wrapper!.setProps({ modelValue: null })
    await flushPromises()
    await wrapper!.setProps({ modelValue: deadWikiLinkPayload })
    await flushPromises()

    await waitForChooser()
    expect(screen.queryByTestId("note-new-form")).toBeNull()
  })

  describe("soft keyboard primer", () => {
    it.each([
      {
        branch: "create",
        label: createNoteLabel,
        focusSelector: '[data-test="note-title"]',
      },
      {
        branch: "point-at-existing",
        label: pointAtExistingNoteLabel,
        focusSelector: 'input[placeholder="Search"]',
      },
    ])(
      "focuses primer then $branch target on touch device",
      async ({ label, focusSelector }) => {
        matchMediaSpy = mockCoarsePointer(true)
        mountModal()
        await waitForChooser()
        const primer = softKeyboardPrimerElement()
        expect(primer).toBeTruthy()

        tapChooser(label)
        expect(document.activeElement).toBe(primer)

        await flushPromises()
        await wrapper?.vm.$nextTick()
        await waitUntilFocused(focusSelector)
      }
    )

    it("does not focus primer on create tap when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountModal()
      await waitForChooser()
      const primer = softKeyboardPrimerElement()

      await tapChooserAndSettle(createNoteLabel)

      expect(document.activeElement).not.toBe(primer)
      await waitUntilFocused('[data-test="note-title"]')
    })
  })
})
