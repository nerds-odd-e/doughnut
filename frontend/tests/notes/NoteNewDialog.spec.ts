import NoteNewDialog from "@/components/notes/NoteNewDialog.vue"
import { VueWrapper, flushPromises } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

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

const mockedSearch = vitest.fn()
const mockedSearchWithin = vitest.fn()
let mockedCreateNote: ReturnType<typeof vi.fn>

describe("adding new note", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    vi.spyOn(
      helper.managedApi.services,
      "searchForLinkTarget"
    ).mockImplementation(mockedSearch)
    vi.spyOn(
      helper.managedApi.services,
      "searchForLinkTargetWithin"
    ).mockImplementation(mockedSearchWithin)
    mockedCreateNote = vi
      .spyOn(helper.managedApi.services, "createNoteUnderParent")
      .mockResolvedValue({} as never)
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  const note = makeMe.aNote.topicConstructor("mythical").please()

  it("does not search for initial default 'Untitled' title", async () => {
    mockedSearchWithin.mockResolvedValue([])
    helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()

    // Wait a bit to ensure any potential search would have been triggered
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should not be called for the initial "Untitled" title
    expect(mockedSearchWithin).not.toHaveBeenCalled()
  })

  it("searches when user edits title back to 'Untitled'", async () => {
    mockedSearchWithin.mockResolvedValue([
      { noteTopology: note.noteTopology, distance: 0.9 },
    ])
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()

    // First, change the title to something else (this marks it as edited)
    await wrapper.find("input#note-title").setValue("myth")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Clear previous calls
    mockedSearchWithin.mockClear()

    // Now change it back to "Untitled"
    await wrapper.find("input#note-title").setValue("Untitled")
    vi.runOnlyPendingTimers()
    await flushPromises()

    // Search should be called when user edits back to "Untitled"
    expect(mockedSearchWithin).toHaveBeenCalledWith({
      note: note.id,
      requestBody: expect.objectContaining({ searchKey: "Untitled" }),
    })
  })

  it("search for duplicate", async () => {
    mockedSearchWithin.mockResolvedValue([
      { noteTopology: note.noteTopology, distance: 0.9 },
    ])
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()
    await wrapper.find("input#note-title").setValue("myth")

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(mockedSearchWithin).toHaveBeenCalledWith({
      note: note.id,
      requestBody: expect.objectContaining({ searchKey: "myth" }),
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
        parentNote: note.id,
        requestBody: expect.anything(),
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
    const mockedWikidataSearch = vitest.fn()

    beforeEach(() => {
      mockedSearchWithin.mockResolvedValue([])
      vi.spyOn(helper.managedApi.services, "searchWikidata").mockImplementation(
        mockedWikidataSearch
      )
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

    const getDialogSelect = () => {
      return document.querySelector('select[name="wikidataSearchResult"]')
    }

    const searchAndSelectFirstResult = async (
      key: string,
      wikidataId: string
    ) => {
      await openWikidataDialog(key)
      await flushPromises()
      vi.runOnlyPendingTimers()
      await flushPromises()
      const select = getDialogSelect() as HTMLSelectElement
      if (select) {
        select.value = wikidataId
        select.dispatchEvent(new Event("change"))
        await flushPromises()
        vi.runOnlyPendingTimers()
        await flushPromises()
        // Wait for dialog to close after selection
        let attempts = 0
        while (
          document.querySelector(".modal-container") &&
          attempts < 10
        ) {
          await flushPromises()
          vi.runOnlyPendingTimers()
          attempts++
        }
      }
    }

    const selectTitleAction = async (
      action: "Replace" | "Append" | "Neither"
    ): Promise<void> => {
      await flushPromises()
      vi.runOnlyPendingTimers()
      const radio = document.querySelector(
        `input[type="radio"][value="${action}"]`
      ) as HTMLInputElement
      if (radio) {
        radio.checked = true
        radio.dispatchEvent(new Event("change", { bubbles: true }))
        await flushPromises()
        vi.runOnlyPendingTimers()
        await flushPromises()
      }
    }

    it("opens dialog when clicking search button", async () => {
      const searchResult = makeMe.aWikidataSearchEntity.label("dog").please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      await openWikidataDialog("dog")
      expect(mockedWikidataSearch).toHaveBeenCalledWith({ search: "dog" })
      const dialog = document.querySelector(".modal-container")
      expect(dialog).toBeTruthy()
    })

    it("closes dialog when cancel is clicked", async () => {
      const searchResult = makeMe.aWikidataSearchEntity.label("dog").please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      await openWikidataDialog("dog")
      await flushPromises()
      vi.runOnlyPendingTimers()
      await flushPromises()
      const cancelButtons = document.querySelectorAll(
        "button.daisy-btn-secondary"
      )
      const cancelButton = Array.from(cancelButtons).find(
        (btn) => (btn as HTMLElement).textContent?.trim() === "Cancel"
      ) as HTMLButtonElement
      if (cancelButton) {
        await cancelButton.click()
        await flushPromises()
        vi.runOnlyPendingTimers()
        await flushPromises()
      }
      const dialog = document.querySelector(".modal-container")
      expect(dialog).toBeFalsy()
    })

    it.each`
      searchTitle | wikidataTitle | wikidataId | titleAction  | expectedTitle
      ${"dog"}    | ${"dog"}      | ${"Q1"}    | ${undefined} | ${"dog"}
      ${"dog"}    | ${"Dog"}      | ${"Q1"}    | ${undefined} | ${"Dog"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"replace"} | ${"Canine"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"append"}  | ${"dog / Canine"}
      ${"dog"}    | ${"Canine"}   | ${"Q1"}    | ${"neither"} | ${"dog"}
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

        mockedWikidataSearch.mockResolvedValue([searchResult])
        await searchAndSelectFirstResult(searchTitle, wikidataId)

        if (titleAction) {
          await selectTitleAction(
            (titleAction.charAt(0).toUpperCase() + titleAction.slice(1)) as
              | "Replace"
              | "Append"
              | "Neither"
          )
        }

        // Wait for dialog to close
        let attempts = 0
        while (document.querySelector(".modal-container") && attempts < 10) {
          await flushPromises()
          vi.runOnlyPendingTimers()
          attempts++
        }

        await flushPromises()
        vi.runOnlyPendingTimers()
        await flushPromises()

        expect(mockedWikidataSearch).toHaveBeenCalledWith({
          search: searchTitle,
        })
        expect((<HTMLInputElement>titleInput().element).value).toBe(
          expectedTitle
        )
      }
    )
  })
})
