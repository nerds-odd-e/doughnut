import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { type VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, testFolderStub } from "@tests/helpers"
import {
  mountNoteNewForm,
  noteNewFormNote,
  noteNewFormRealm,
  notebookRootProps,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  type NoteNewFormSdkSpies,
} from "@tests/notes/noteNewFormTestSupport"
import { RESERVED_README_TITLE_MESSAGE } from "@/utils/reservedReadmeTitles"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"

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

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({ currentRoute: { value: {} } }),
    useRoute: () => ({ path: "/", fullPath: "/" }),
  }
})

const notebook = noteNewFormRealm.notebookRealm.notebook.id

describe("NoteNewForm submit", () => {
  let sdkSpies: NoteNewFormSdkSpies
  let wrapper: VueWrapper<ComponentPublicInstance>

  const submit = () =>
    wrapper.find('[data-testid="note-new-form"]').trigger("submit")

  const expectCreatedWith = (body: Record<string, unknown>) =>
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook },
      body: expect.objectContaining(body),
    })

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("call the api without a folder", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await setNoteNewFormTitle(wrapper, "note title")
    vi.clearAllTimers()

    await submit()
    expectCreatedWith({ newTitle: "note title" })
    const createArgs = sdkSpies.mockedCreateNoteAtRoot.mock.calls[0]![0] as {
      body: Record<string, unknown>
    }
    expect(createArgs.body).not.toHaveProperty("folderId")
  })

  it("call the api once only", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await setNoteNewFormTitle(wrapper, "note title")
    vi.clearAllTimers()

    submit()
    submit()
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledTimes(1)
  })

  it("submits initialTitle as newTitle when unchanged", async () => {
    wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialTitle: "2026-05-09",
    })

    await submit()
    await flushPromises()
    expectCreatedWith({ newTitle: "2026-05-09 " })
  })

  it("displays reserved title error when api returns binding error for newTitle", async () => {
    wrapper = mountNoteNewForm(notebookRootProps)
    await setNoteNewFormTitle(wrapper, "readme")
    sdkSpies.mockedCreateNoteAtRoot.mockResolvedValueOnce({
      data: undefined,
      error: {
        message: "binding error",
        errorType: "BINDING_ERROR",
        errors: { newTitle: RESERVED_README_TITLE_MESSAGE },
      },
      request: {} as Request,
      response: { status: 400, url: "" } as Response,
      // biome-ignore lint/suspicious/noExplicitAny: SDK error result shape
    } as any)

    await submit()
    await flushPromises()

    expect(wrapper.text()).toContain("reserved")
  })

  it("sends folderId when a target folder is pre-selected", async () => {
    mockSdkService(NotebookFolderController, "listNotebookFolderIndex", [
      testFolderStub(42, "Alpha"),
    ])
    wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialFolder: testFolderStub(42, "Alpha"),
    })
    await setNoteNewFormTitle(wrapper, "in folder")

    await submit()
    expectCreatedWith({ newTitle: "in folder", folderId: 42 })
  })

  it("sends folderId after user picks a folder in FolderSelector", async () => {
    mockSdkService(NotebookFolderController, "listNotebookFolderListing", {
      folders: [testFolderStub(7, "One"), testFolderStub(8, "Two")],
    })
    mockSdkService(NotebookFolderController, "listNotebookFolderIndex", [
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

    await submit()
    expectCreatedWith({ newTitle: "moved", folderId: 8 })
  })

  it("shows folder dropdown label when initial folder is outside ancestorFolders", async () => {
    wrapper = mountNoteNewForm(
      {
        notebookId: notebook,
        titleSearchAnchorNote: noteNewFormNote,
        ancestorFolders: [],
        initialFolder: testFolderStub(99, "LeSS in Action"),
      },
      { attachTo: document.body }
    )
    await flushPromises()

    const select = wrapper.find('[data-testid="folder-move-parent-select"]')
      .element as HTMLSelectElement
    expect(select.selectedOptions[0]?.textContent?.trim()).toBe(
      "LeSS in Action"
    )
  })

  it("submits inherited parent frontmatter for the Same parent choice", async () => {
    const noteWithParent = makeMe.aNoteRealm
      .title("team")
      .content('---\nparent: "[[Course intro]]"\n---\n')
      .please().note
    wrapper = mountNoteNewForm(
      {
        notebookId: notebook,
        titleSearchAnchorNote: noteWithParent,
        ancestorFolders: [],
      },
      { attachTo: document.body }
    )
    await flushPromises()
    expect(
      wrapper.find('[data-testid="note-creation-parent-relationship"]').text()
    ).toContain("Same parent")
    await wrapper
      .find('label[for="note-creation-same_parent"]')
      .trigger("click")
    await setNoteNewFormTitle(wrapper, "Sibling")
    await submit()
    await flushPromises()
    expectCreatedWith({
      newTitle: "Sibling",
      content: expect.stringContaining('parent: "[[Course intro]]"'),
    })
  })

  it("hides relationship options without a context note", async () => {
    wrapper = mountNoteNewForm(
      { notebookId: notebook, ancestorFolders: [] },
      { attachTo: document.body }
    )
    await flushPromises()
    expect(
      wrapper.find('[data-testid="note-creation-parent-relationship"]').exists()
    ).toBe(false)
  })
})
