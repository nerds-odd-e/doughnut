import {
  NoteController,
  NotebookController,
  WikidataController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteNewForm from "@/components/notes/NoteNewForm.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  testFolderStub,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  mountNoteNewForm,
  mockWikidataSearchResult,
  noteNewFormNote,
  noteNewFormRealm,
  noteTitleText,
  notebookRootProps,
  openWikidataDialog,
  resolveWikidataSearch,
  selectWikidataSearchResult,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
  wikidataCancelButton,
  wikidataDialogIsOpen,
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
      currentRoute: {
        value: {},
      },
    }),
    useRoute: () => ({
      path: "/",
    }),
  }
})

describe("adding new note", () => {
  let sdkSpies: NoteNewFormSdkSpies

  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    sdkSpies = setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it("shows folder dropdown label when initial folder is outside ancestorFolders", async () => {
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps({
        notebookId: noteNewFormRealm.notebookRealm.notebook.id,
        titleSearchAnchorNote: noteNewFormNote,
        ancestorFolders: [],
        initialFolder: testFolderStub(99, "LeSS in Action"),
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const select = wrapper.find('[data-testid="folder-move-parent-select"]')
      .element as HTMLSelectElement
    const selectedText = select.selectedOptions[0]?.textContent?.trim() ?? ""
    expect(selectedText).toBe("LeSS in Action")

    wrapper.unmount()
  })

  it("does not search for initial default 'Untitled' title", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("submits initialTitle as newTitle when unchanged", async () => {
    const wrapper = mountNoteNewForm({
      ...notebookRootProps,
      initialTitle: "2026-05-09",
    })

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: noteNewFormRealm.notebookRealm.notebook.id },
      body: expect.objectContaining({ newTitle: "2026-05-09" }),
    })
    wrapper.unmount()
  })

  it("searches when user edits title back to 'Untitled'", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: noteNewFormNote.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })

    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()
    sdkSpies.searchForRelationshipTargetWithinSpy.mockClear()
    await setNoteNewFormTitle(wrapper, "Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "Untitled" }),
    })
    wrapper.unmount()
  })

  it("search for duplicate", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: noteNewFormNote.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = mountNoteNewForm(notebookRootProps, {
      attachTo: document.body,
    })
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(sdkSpies.searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    expect(sdkSpies.semanticSearchWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("runs semantic search when the semantic toggle is turned on", async () => {
    sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([])
    )
    sdkSpies.semanticSearchWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = mountNoteNewForm()
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()

    sdkSpies.semanticSearchWithinSpy.mockClear()
    sdkSpies.searchForRelationshipTargetWithinSpy.mockClear()

    await wrapper
      .find('[data-testid="note-new-form-semantic-search-toggle"]')
      .trigger("click")
    vi.runOnlyPendingTimers()

    expect(sdkSpies.semanticSearchWithinSpy).toHaveBeenCalledWith({
      path: { note: noteNewFormNote.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    wrapper.unmount()
  })

  describe("submit form", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>

    beforeEach(async () => {
      wrapper = mountNoteNewForm(notebookRootProps, {
        attachTo: document.body,
      })
      await setNoteNewFormTitle(wrapper, "note title")
      vi.clearAllTimers()
    })

    afterEach(() => {
      wrapper?.unmount()
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

    describe("folder target on submit", () => {
      beforeEach(() => {
        wrapper?.unmount()
      })

      it("sends folderId when a target folder is pre-selected", async () => {
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
    })

    it("call the api once only", async () => {
      wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      await flushPromises()
      expect(sdkSpies.mockedCreateNoteAtRoot).toHaveBeenCalledTimes(1)
    })

    it("displays reserved title error when api returns binding error for newTitle", async () => {
      await setNoteNewFormTitle(wrapper, "index")

      sdkSpies.mockedCreateNoteAtRoot.mockResolvedValueOnce({
        data: undefined,
        error: {
          message: "binding error",
          errorType: "BINDING_ERROR",
          errors: {
            newTitle:
              "'index' is reserved for notebook and folder index content.",
          },
        },
        request: {} as Request,
        response: { status: 400, url: "" } as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK error result shape
      } as any)

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      await flushPromises()

      expect(wrapper.text()).toContain("reserved")
    })

    it("asks confirmation on soft-deleted title conflict and calls undo delete when confirmed", async () => {
      popupsMock.confirm.mockResolvedValueOnce(true)
      const restoredRealm = makeMe.aNoteRealm.please()
      const undoSpy = mockSdkService(
        NoteController,
        "undoDeleteNote",
        restoredRealm
      )
      sdkSpies.mockedCreateNoteAtRoot.mockResolvedValueOnce({
        data: undefined,
        error: {
          message:
            "A note with this title already exists here but was deleted.",
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          errors: { deletedNoteId: "99" },
        },
        request: {} as Request,
        response: { status: 409, url: "" } as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK error result shape
      } as any)

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      await flushPromises()

      expect(popupsMock.confirm).toHaveBeenCalledWith(
        expect.stringContaining("deleted")
      )
      expect(undoSpy).toHaveBeenCalledWith({ path: { note: 99 } })
    })
  })

  describe("search wikidata entry", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>
    let searchWikidataSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      vi.useRealTimers()
      sdkSpies.searchForRelationshipTargetWithinSpy.mockResolvedValue(
        wrapSdkResponse([])
      )
      searchWikidataSpy = mockSdkService(
        WikidataController,
        "searchWikidata",
        []
      )
      wrapper = mountNoteNewForm(notebookRootProps, {
        attachTo: document.body,
      })
    })

    afterEach(() => {
      wrapper?.unmount()
      vi.useFakeTimers()
    })

    it("opens wikidata dialog on search and closes on cancel", async () => {
      resolveWikidataSearch(searchWikidataSpy, "dog", "Q1")
      await openWikidataDialog(wrapper, "dog")
      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
      expect(wikidataDialogIsOpen()).toBe(true)

      wikidataCancelButton().click()
      await flushPromises()

      expect(wikidataDialogIsOpen()).toBe(false)
    })

    it.each`
      searchTitle | wikidataTitle | wikidataId | titleAction  | expectedTitle
      ${"dog"}    | ${"dog"}      | ${"Q1"}    | ${undefined} | ${"dog"}
      ${"dog"}    | ${"Dog"}      | ${"Q1"}    | ${undefined} | ${"Dog"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"replace"} | ${"Canine"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"append"}  | ${"dog"}
    `(
      "search $searchTitle get $wikidataTitle with action $titleAction updates title as $expectedTitle",
      async ({
        searchTitle,
        wikidataTitle,
        wikidataId,
        titleAction,
        expectedTitle,
      }) => {
        searchWikidataSpy.mockResolvedValue(
          wrapSdkResponse([mockWikidataSearchResult(wikidataTitle, wikidataId)])
        )
        await openWikidataDialog(wrapper, searchTitle)
        await selectWikidataSearchResult(
          wikidataId,
          titleAction
            ? ((titleAction.charAt(0).toUpperCase() + titleAction.slice(1)) as
                | "Replace"
                | "Append")
            : undefined
        )

        expect(wikidataDialogIsOpen()).toBe(false)
        expect(noteTitleText(wrapper)).toBe(expectedTitle)
      }
    )
  })
})
