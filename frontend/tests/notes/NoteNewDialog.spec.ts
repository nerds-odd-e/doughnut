import NoteNewDialog from "@/components/notes/NoteNewDialog.vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import WikidataSearchByLabel from "@/components/notes/WikidataSearchByLabel.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import { nextTick } from "vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRouter: () => ({
    currentRoute: {
      value: {},
    },
  }),
  useRoute: () => ({
    path: "/",
  }),
}))

let searchForLinkTargetWithinSpy: ReturnType<
  typeof mockSdkService<"searchForLinkTargetWithin">
>
let mockedCreateNote: ReturnType<typeof mockSdkService<"createNoteUnderParent">>

describe("adding new note", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    mockSdkService("searchForLinkTarget", [])
    searchForLinkTargetWithinSpy = mockSdkService(
      "searchForLinkTargetWithin",
      []
    )
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
    mockSdkService("getRecentNotes", [])
    const createNoteResult = {
      created: makeMe.aNoteRealm.please(),
      parent: makeMe.aNoteRealm.please(),
    }
    mockedCreateNote = mockSdkService("createNoteUnderParent", createNoteResult)
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  const note = makeMe.aNote.topicConstructor("mythical").please()

  it("does not search for initial default 'Untitled' title", async () => {
    searchForLinkTargetWithinSpy.mockResolvedValue(wrapSdkResponse([]))
    helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()

    // Wait a bit to ensure any potential search would have been triggered
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should not be called for the initial "Untitled" title
    expect(searchForLinkTargetWithinSpy).not.toHaveBeenCalled()
  })

  it("searches when user edits title back to 'Untitled'", async () => {
    searchForLinkTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([{ noteTopology: note.noteTopology, distance: 0.9 }])
    )
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()

    // First, change the title to something else (this marks it as edited)
    await wrapper.find("input#note-title").setValue("myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Clear previous calls
    searchForLinkTargetWithinSpy.mockClear()

    // Now change it back to "Untitled"
    await wrapper.find("input#note-title").setValue("Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should be called when user edits back to "Untitled"
    expect(searchForLinkTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "Untitled" }),
      client: expect.anything(),
    })
  })

  it("search for duplicate", async () => {
    searchForLinkTargetWithinSpy.mockResolvedValue(
      wrapSdkResponse([{ noteTopology: note.noteTopology, distance: 0.9 }])
    )
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()
    await wrapper.find("input#note-title").setValue("myth")

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(searchForLinkTargetWithinSpy).toHaveBeenCalledWith({
      path: { note: note.id },
      body: expect.objectContaining({ searchKey: "myth" }),
      client: expect.anything(),
    })
  })

  describe("submit form", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>

    beforeEach(async () => {
      wrapper = helper
        .component(NoteNewDialog)
        .withStorageProps({ referenceNote: note, insertMode: "as-child" })
        .mount({ attachTo: document.body })
      await wrapper.find("input#note-title").setValue("note title")
      vi.clearAllTimers()
    })

    it("call the api", async () => {
      await wrapper.find("form").trigger("submit")
      expect(mockedCreateNote).toHaveBeenCalledWith({
        path: { parentNote: note.id },
        body: expect.anything(),
      })
    })

    it("call the api once only", async () => {
      wrapper.find("form").trigger("submit")
      wrapper.find("form").trigger("submit")
      await flushPromises()
      expect(mockedCreateNote).toHaveBeenCalledTimes(1)
    })
  })

  describe("search wikidata entry", () => {
    let wrapper: VueWrapper<ComponentPublicInstance>
    let searchWikidataSpy: ReturnType<typeof mockSdkService<"searchWikidata">>

    beforeEach(() => {
      searchForLinkTargetWithinSpy.mockResolvedValue(wrapSdkResponse([]))
      searchWikidataSpy = mockSdkService("searchWikidata", [])
      wrapper = helper
        .component(NoteNewDialog)
        .withStorageProps({ referenceNote: note, insertMode: "as-child" })
        .mount({ attachTo: document.body })
    })

    const titleInput = () => {
      return wrapper.find("input#note-title")
    }

    const openWikidataDialog = async (key: string) => {
      await titleInput().setValue(key)
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
        client: expect.anything(),
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
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"append"}  | ${"dog / Canine"}
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
          client: expect.anything(),
        })
        expect((<HTMLInputElement>titleInput().element).value).toBe(
          expectedTitle
        )
      }
    )
  })
})
