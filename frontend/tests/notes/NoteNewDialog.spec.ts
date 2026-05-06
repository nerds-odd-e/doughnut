import NoteNewDialog from "@/components/notes/NoteNewDialog.vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { nextTick } from "vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
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

let searchForRelationshipTargetWithinSpy: ReturnType<
  typeof mockSdkService<"searchForRelationshipTargetWithin">
>
let mockedCreateNoteAtRoot: ReturnType<
  typeof mockSdkService<"createNoteAtNotebookRoot">
>

async function setNoteNewDialogTitle(
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
    mockSdkService("searchForRelationshipTarget", [])
    searchForRelationshipTargetWithinSpy = mockSdkService(
      "searchForRelationshipTargetWithin",
      []
    )
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
    mockSdkService("getRecentNotes", [])
    const createNoteResult = makeMe.aNoteRealm.please()
    mockedCreateNoteAtRoot = mockSdkService(
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
    notebookRootNotebookId: realm.notebookView.notebook.id,
    titleSearchAnchorNote: note,
  }

  it("does not search for initial default 'Untitled' title", async () => {
    searchForRelationshipTargetWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    const wrapper = helper
      .component(NoteNewDialog)
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
      .component(NoteNewDialog)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })

    // First, change the title to something else (this marks it as edited)
    await setNoteNewDialogTitle(wrapper, "myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Clear previous calls
    searchForRelationshipTargetWithinSpy.mockClear()

    // Now change it back to "Untitled"
    await setNoteNewDialogTitle(wrapper, "Untitled")
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
      .component(NoteNewDialog)
      .withCleanStorage()
      .withProps(notebookRootProps)
      .mount({ attachTo: document.body })
    await setNoteNewDialogTitle(wrapper, "myth")

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(searchForRelationshipTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "myth" }),
    })
    wrapper.unmount()
  })

  describe("submit form", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>

    beforeEach(async () => {
      wrapper = helper
        .component(NoteNewDialog)
        .withCleanStorage()
        .withProps(notebookRootProps)
        .mount({ attachTo: document.body })
      await setNoteNewDialogTitle(wrapper, "note title")
      vi.clearAllTimers()
    })

    afterEach(() => {
      wrapper?.unmount()
    })

    it("call the api", async () => {
      await wrapper
        .find('[data-testid="note-new-dialog-form"]')
        .trigger("submit")
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledWith({
        path: {
          notebook: realm.notebookView.notebook.id,
        },
        body: expect.objectContaining({ newTitle: "note title" }),
      })
    })

    it("call the api once only", async () => {
      wrapper.find('[data-testid="note-new-dialog-form"]').trigger("submit")
      wrapper.find('[data-testid="note-new-dialog-form"]').trigger("submit")
      await flushPromises()
      expect(mockedCreateNoteAtRoot).toHaveBeenCalledTimes(1)
    })

    it("asks confirmation on soft-deleted title conflict and calls undo delete when confirmed", async () => {
      popupsMock.confirm.mockResolvedValueOnce(true)
      const restoredRealm = makeMe.aNoteRealm.please()
      const undoSpy = mockSdkService("undoDeleteNote", restoredRealm)
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

      await wrapper
        .find('[data-testid="note-new-dialog-form"]')
        .trigger("submit")
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
    let searchWikidataSpy: ReturnType<typeof mockSdkService<"searchWikidata">>

    beforeEach(() => {
      searchForRelationshipTargetWithinSpy.mockResolvedValue(
        wrapSdkResponse([])
      )
      searchWikidataSpy = mockSdkService("searchWikidata", [])
      wrapper = helper
        .component(NoteNewDialog)
        .withCleanStorage()
        .withProps(notebookRootProps)
        .mount({ attachTo: document.body })
    })

    afterEach(() => {
      wrapper?.unmount()
    })

    const openWikidataDialog = async (key: string) => {
      await setNoteNewDialogTitle(wrapper, key)
      await wrapper.find("button[title='Wikidata Id']").trigger("click")
      await flushPromises()
    }

    const selectFromDropdown = async (wikidataId: string) => {
      await flushPromises()

      // Find the WikidataAssociationDialog component
      const dialogComponent = wrapper.findComponent(WikidataAssociationDialog)
      expect(dialogComponent.exists()).toBe(true)

      // Call the selection method directly on the component
      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const vm = dialogComponent.vm as any
      expect(vm.searchResults).toBeDefined()
      expect(vm.searchResults.length).toBeGreaterThan(0)

      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const selected = vm.searchResults.find((r: any) => r.id === wikidataId)
      expect(selected).toBeDefined()

      vm.selectedOption = wikidataId
      vm.selectedItem = selected

      const currentLabel = (vm.searchKeyRef || vm.searchKey || "").toUpperCase()
      const newLabel = selected.label.toUpperCase()

      if (currentLabel === newLabel) {
        dialogComponent.vm.$emit("selected", selected)
      } else {
        vm.showTitleOptions = true
      }
      await flushPromises()
    }

    const selectTitleAction = async (
      action: "Replace" | "Append" | undefined
    ): Promise<void> => {
      if (!action) return

      await flushPromises()

      // Find the WikidataAssociationDialog component
      const dialogComponent = wrapper.findComponent(WikidataAssociationDialog)
      // biome-ignore lint/suspicious/noExplicitAny: accessing Vue component internals in test
      const vm = dialogComponent.vm as any

      // Set the title action and trigger the handler
      vm.titleAction = action
      await flushPromises()

      // Manually call handleTitleAction to emit the event
      expect(vm.selectedItem).toBeDefined()

      const actionValueMap: Record<string, "replace" | "append"> = {
        Replace: "replace",
        Append: "append",
      }
      const actionValue = actionValueMap[action]

      dialogComponent.vm.$emit("selected", vm.selectedItem, actionValue)
      await flushPromises()
    }

    const waitForDialogToClose = async () => {
      await flushPromises()
      await nextTick()
      await flushPromises()
      await nextTick()
      await flushPromises()
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
