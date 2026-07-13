import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import {
  mountNoteNewForm,
  noteNewFormRealm,
  notebookRootProps,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

const popupsMock = {
  confirm: vi.fn().mockResolvedValue(false),
  alert: vi.fn(),
  options: vi.fn(),
  done: vi.fn(),
  register: vi.fn(),
  peek: vi.fn(),
}

vi.mock("@/components/commons/Popups/usePopups", () => ({
  default: () => ({ popups: popupsMock }),
}))

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      currentRoute: { value: {} },
    }),
    useRoute: () => ({ path: "/", fullPath: "/" }),
  }
})

describe("NoteNewForm submit", () => {
  let sdkSpies: NoteNewFormSdkSpies
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(async () => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
    wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })
    await setNoteNewFormTitle(wrapper, "note title")
    vi.clearAllTimers()
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("call the api", async () => {
    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: {
        notebook: noteNewFormRealm.notebookRealm.notebook.id,
      },
      body: expect.objectContaining({
        newTitle: "note title",
      }),
    })
    const createArgs = sdkSpies.mockedCreateNoteAtRoot.mock.calls[0]![0] as {
      body: Record<string, unknown>
    }
    expect(createArgs.body).not.toHaveProperty("folderId")
  })

  it("sends folderId when a target folder is pre-selected", async () => {
    wrapper?.unmount()
    mockSdkService(NotebookController, "listNotebookFolderIndex", [
      testFolderStub(42, "Alpha"),
    ])
    wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialFolder: testFolderStub(42, "Alpha"),
    })
    await setNoteNewFormTitle(wrapper, "in folder")

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: "in folder",
        folderId: 42,
      }),
    })
  })

  it("sends folderId after user picks a folder in FolderSelector", async () => {
    wrapper?.unmount()
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [testFolderStub(7, "One"), testFolderStub(8, "Two")],
    })
    mockSdkService(NotebookController, "listNotebookFolderIndex", [
      testFolderStub(7, "One"),
      testFolderStub(8, "Two"),
    ])
    wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialFolder: testFolderStub(7, "One"),
    })
    await setNoteNewFormTitle(wrapper, "moved")

    await wrapper
      .find('[data-testid="folder-move-parent-select"]')
      .setValue("8")

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({
        newTitle: "moved",
        folderId: 8,
      }),
    })
  })

  it("call the api once only", async () => {
    wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledTimes(1)
  })
})
