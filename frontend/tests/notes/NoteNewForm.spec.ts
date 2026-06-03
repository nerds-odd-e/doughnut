import {
  NoteController,
  NotebookController,
  SearchController,
  WikidataController,
} from "@generated/doughnut-backend-api/sdk.gen"
import NoteNewForm from "@/components/notes/NoteNewForm.vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import WikidataAssociationDialogBody from "@/components/notes/WikidataAssociationDialogBody.vue"
import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { nextTick } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  testFolderStub,
  wrapSdkResponse,
} from "@tests/helpers"
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

let searchForRelationshipTargetWithinSpy: ReturnType<typeof mockSdkService>
let semanticSearchWithinSpy: ReturnType<typeof mockSdkService>
let mockedCreateNoteAtRoot: ReturnType<typeof mockSdkService>

async function setNoteNewFormTitle(
  wrapper: VueWrapper<ComponentPublicInstance>,
  value: string
) {
  const el = wrapper.find('[data-test="note-title"]').element as HTMLElement
  el.innerText = value
  el.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
}

describe("adding new note", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    popupsMock.confirm.mockReset()
    popupsMock.confirm.mockResolvedValue(false)
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    searchForRelationshipTargetWithinSpy = mockSdkService(
      SearchController,
      "searchForRelationshipTargetWithin",
      []
    )
    mockSdkService(SearchController, "semanticSearch", [])
    semanticSearchWithinSpy = mockSdkService(
      SearchController,
      "semanticSearchWithin",
      []
    )
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
    const createNoteResult = makeMe.aNoteRealm.please()
    mockedCreateNoteAtRoot = mockSdkService(
      NotebookController,
      "createNoteAtNotebookRoot",
      createNoteResult
    )
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  const realm = makeMe.aNoteRealm.title("mythical").please()
  const note = realm.note

  const notebookRootProps = {
    notebookId: realm.notebookRealm.notebook.id,
    titleSearchAnchorNote: note,
    ancestorFolders: realm.ancestorFolders ?? [],
  }

  it("shows folder dropdown label when initial folder is outside ancestorFolders", async () => {
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps({
        notebookId: realm.notebookRealm.notebook.id,
        titleSearchAnchorNote: note,
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
    searchForRelationshipTargetWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })

    // Wait a bit to ensure any potential search would have been triggered
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should not be called for the initial "Untitled" title
    expect(searchForRelationshipTargetWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("submits initialTitle as newTitle when unchanged", async () => {
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps({
        ...notebookRootProps,
        initialTitle: "2026-05-09",
      })
      .mount({ attachTo: document.body })

    await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
    await flushPromises()
    expect(mockedCreateNoteAtRoot).toHaveBeenCalledWith({
      path: { notebook: realm.notebookRealm.notebook.id },
      body: expect.objectContaining({ newTitle: "2026-05-09" }),
    })
    wrapper.unmount()
  })

  it("searches when user edits title back to 'Untitled'", async () => {
    searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: note.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })

    // First, change the title to something else (this marks it as edited)
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Clear previous calls
    searchForRelationshipTargetWithinSpy.mockClear()

    // Now change it back to "Untitled"
    await setNoteNewFormTitle(wrapper, "Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should be called when user edits back to "Untitled"
    expect(searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "Untitled" }),
    })
    wrapper.unmount()
  })

  it("search for duplicate", async () => {
    searchForRelationshipTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([
        {
          hitKind: "NOTE",
          noteSearchResult: {
            noteTopology: note.noteTopology,
            notebookId: 1,
            distance: 0.9,
          },
        },
      ])
    )
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })
    await setNoteNewFormTitle(wrapper, "myth")

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    expect(semanticSearchWithinSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("runs semantic search when the semantic toggle is turned on", async () => {
    searchForRelationshipTargetWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    semanticSearchWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = helper
      .component(NoteNewForm)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })
    await setNoteNewFormTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    semanticSearchWithinSpy.mockClear()
    searchForRelationshipTargetWithinSpy.mockClear()

    await wrapper
      .find('[data-testid="note-new-form-semantic-search-toggle"]')
      .trigger("click")
    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(semanticSearchWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    wrapper.unmount()
  })

  describe("submit form", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>

    beforeEach(async () => {
      wrapper = helper
        .component(NoteNewForm)
        .withCleanStorage()
        .withProps(notebookRootProps)
        .mount({ attachTo: document.body })
      await setNoteNewFormTitle(wrapper, "note title")
      vi.clearAllTimers()
    })

    afterEach(() => {
      wrapper?.unmount()
    })

    it("call the api", async () => {
      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledWith({
        path: {
          notebook: realm.notebookRealm.notebook.id,
        },
        body: expect.objectContaining({
          newTitle: "note title",
        }),
      })
      const createArgs = mockedCreateNoteAtRoot.mock.calls[0]![0] as {
        body: Record<string, unknown>
      }
      expect(createArgs.body).not.toHaveProperty("folderId")
    })

    it("sends folderId when a target folder is pre-selected", async () => {
      wrapper.unmount()
      mockSdkService(NotebookController, "listNotebookFolderIndex", [
        testFolderStub(42, "Alpha"),
      ])
      wrapper = helper
        .component(NoteNewForm)
        .withCleanStorage()
        .withProps({
          ...notebookRootProps,
          initialFolder: testFolderStub(42, "Alpha"),
        })
        .mount({ attachTo: document.body })
      await setNoteNewFormTitle(wrapper, "in folder")
      await flushPromises()

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledWith({
        path: { notebook: realm.notebookRealm.notebook.id },
        body: expect.objectContaining({
          newTitle: "in folder",
          folderId: 42,
        }),
      })
    })

    it("sends folderId after user picks a folder in FolderSelector", async () => {
      wrapper.unmount()
      mockSdkService(NotebookController, "listNotebookFolderListing", {
        folders: [testFolderStub(7, "One"), testFolderStub(8, "Two")],
      })
      mockSdkService(NotebookController, "listNotebookFolderIndex", [
        testFolderStub(7, "One"),
        testFolderStub(8, "Two"),
      ])
      wrapper = helper
        .component(NoteNewForm)
        .withCleanStorage()
        .withProps({
          ...notebookRootProps,
          initialFolder: testFolderStub(7, "One"),
        })
        .mount({ attachTo: document.body })
      await setNoteNewFormTitle(wrapper, "moved")
      await flushPromises()

      const select = wrapper.find('[data-testid="folder-move-parent-select"]')
      await select.setValue("8")
      await flushPromises()

      await wrapper.find('[data-testid="note-new-form"]').trigger("submit")
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledWith({
        path: { notebook: realm.notebookRealm.notebook.id },
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
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledTimes(1)
    })

    it("displays reserved title error when api returns binding error for newTitle", async () => {
      await setNoteNewFormTitle(wrapper, "index")

      mockedCreateNoteAtRoot.mockResolvedValueOnce({
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
      mockedCreateNoteAtRoot.mockResolvedValueOnce({
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
    // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
    let wrapper: VueWrapper<any>
    let searchWikidataSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      vi.useRealTimers()
      searchForRelationshipTargetWithinSpy.mockResolvedValue(
        wrapSdkResponse([])
      )
      searchWikidataSpy = mockSdkService(
        WikidataController,
        "searchWikidata",
        []
      )
      wrapper = helper
        .component(NoteNewForm)
        .withCleanStorage()
        .withProps(notebookRootProps)
        .mount({ attachTo: document.body })
    })

    afterEach(() => {
      wrapper?.unmount()
      vi.useFakeTimers()
    })

    const openWikidataDialog = async (key: string) => {
      await setNoteNewFormTitle(wrapper, key)
      await wrapper.find("button[title='Wikidata Id']").trigger("click")
      await flushPromises()
    }

    const selectFromDropdown = async (wikidataId: string) => {
      const bodyComponent = wrapper.findComponent(WikidataAssociationDialogBody)
      expect(bodyComponent.exists()).toBe(true)

      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const vm = bodyComponent.vm as any
      expect(vm.searchResults.length).toBeGreaterThan(0)

      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const selected = vm.searchResults.find((r: any) => r.id === wikidataId)
      expect(selected).toBeDefined()

      vm.selectedOption = wikidataId
      vm.selectedItem = selected

      const currentLabel = (vm.searchKeyRef || vm.searchKey || "").toUpperCase()
      const newLabel = selected.label.toUpperCase()

      if (currentLabel === newLabel) {
        bodyComponent.vm.$emit("selected", selected)
      } else {
        vm.showTitleOptions = true
      }
      await flushPromises()
    }

    const selectTitleAction = async (
      action: "Replace" | "Append" | undefined
    ): Promise<void> => {
      if (!action) return

      const bodyComponent = wrapper.findComponent(WikidataAssociationDialogBody)
      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const vm = bodyComponent.vm as any
      expect(vm.selectedItem).toBeDefined()

      vm.titleAction = action
      const actionValueMap: Record<string, "replace" | "append"> = {
        Replace: "replace",
        Append: "append",
      }
      bodyComponent.vm.$emit(
        "selected",
        vm.selectedItem,
        actionValueMap[action]
      )
      await flushPromises()
    }

    const waitForDialogToClose = async () => {
      await flushPromises()
      expect(wrapper.findComponent(WikidataAssociationDialog).exists()).toBe(
        false
      )
    }

    it("opens dialog when clicking search button", async () => {
      const searchResult = makeMe.aWikidataSearchEntity.label("dog").please()
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      await openWikidataDialog("dog")
      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
      const dialog = document.querySelector(".modal-container")
      expect(dialog).toBeTruthy()
    })

    it("closes dialog when cancel is clicked", async () => {
      const searchResult = makeMe.aWikidataSearchEntity.label("dog").please()
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      await openWikidataDialog("dog")

      // Verify dialog is open
      let dialogComponent = wrapper.findComponent(WikidataAssociationDialog)
      expect(dialogComponent.exists()).toBe(true)

      // Find the WikidataSearchByLabel component and call closeDialog method
      const searchByLabelComponent = wrapper.findComponent(
        WikidataSearchByLabel
      )
      expect(searchByLabelComponent.exists()).toBe(true)
      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const vm = searchByLabelComponent.vm as any
      expect(vm.showDialog).toBe(true)
      vm.closeDialog()

      await nextTick()
      await flushPromises()
      await nextTick()

      // Check that showDialog is now false
      expect(vm.showDialog).toBe(false)

      // Check that the WikidataAssociationDialog component no longer exists
      dialogComponent = wrapper.findComponent(WikidataAssociationDialog)
      expect(dialogComponent.exists()).toBe(false)
    })

    it.each`
      searchTitle | wikidataTitle | wikidataId | titleAction  | expectedTitle
      ${"dog"}    | ${"dog"}      | ${"Q1"}    | ${undefined} | ${"dog"}
      ${"dog"}    | ${"Dog"}      | ${"Q1"}    | ${undefined} | ${"Dog"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"replace"} | ${"Canine"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"append"}  | ${"dog ／ Canine"}
    `(
      "search $searchTitle get $wikidataTitle with action $titleAction results in $expectedTitle",
      async ({
        searchTitle,
        wikidataTitle,
        wikidataId,
        titleAction,
        expectedTitle,
      }) => {
        const searchResult = makeMe.aWikidataSearchEntity
          .label(wikidataTitle)
          .id(wikidataId)
          .please()

        searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
        await openWikidataDialog(searchTitle)
        await selectFromDropdown(wikidataId)

        const titleActionCapitalized = titleAction
          ? ((titleAction.charAt(0).toUpperCase() + titleAction.slice(1)) as
              | "Replace"
              | "Append")
          : undefined

        await selectTitleAction(titleActionCapitalized)

        await waitForDialogToClose()

        expect(searchWikidataSpy).toHaveBeenCalledWith({
          query: { search: searchTitle },
        })
        expect(
          (wrapper.find('[data-test="note-title"]').element as HTMLElement)
            .innerText
        ).toBe(expectedTitle)
      }
    )
  })
})
