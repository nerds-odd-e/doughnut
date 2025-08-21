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
}))

const mockedSearch = vitest.fn()
const mockedSearchWithin = vitest.fn()
const mockedCreateNote = vitest.fn()

describe("adding new note", () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    helper.managedApi.restSearchController.searchForLinkTarget = mockedSearch
    helper.managedApi.restSearchController.searchForLinkTargetWithin =
      mockedSearchWithin
    helper.managedApi.restNoteCreationController.createNote =
      mockedCreateNote.mockResolvedValue({})
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  const note = makeMe.aNote.topicConstructor("mythical").please()

  it("search for duplicate", async () => {
    mockedSearchWithin.mockResolvedValue([note.noteTopology])
    const wrapper = helper
      .component(NoteNewDialog)
      .withStorageProps({ referenceNote: note, insertMode: "as-child" })
      .mount()
    await wrapper.find("input#note-title").setValue("myth")

    vi.runOnlyPendingTimers()
    await flushPromises()

    expect(wrapper.text()).toContain("mythical")
    expect(mockedSearchWithin).toHaveBeenCalledWith(
      note.id,
      expect.objectContaining({ searchKey: "myth" })
    )
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
      expect(mockedCreateNote).toHaveBeenCalledWith(note.id, expect.anything())
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
      helper.managedApi.restWikidataController.searchWikidata =
        mockedWikidataSearch
      wrapper = helper
        .component(NoteNewDialog)
        .withStorageProps({ referenceNote: note, insertMode: "as-child" })
        .mount({ attachTo: document.body })
    })

    const titleInput = () => {
      return wrapper.find("input#note-title")
    }

    const searchWikidata = async (key: string) => {
      await titleInput().setValue(key)
      await wrapper.find("button[title='Wikidata Id']").trigger("click")

      await flushPromises()

      return wrapper.find('select[name="wikidataSearchResult"]')
    }

    const searchAndSelectFirstResult = async (key: string) => {
      const select = await searchWikidata(key)
      select.findAll("option").at(1)?.setValue()
    }

    const replaceTitle = async () => {
      await wrapper.find("[id='topicRadio-Replace']").setValue()
    }
    const appendTitle = async () => {
      await wrapper.find("[id='topicRadio-Append']").setValue()
    }
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    const doNothing = () => {
      // noop
    }

    describe("the select for wikidata id", () => {
      let select
      beforeEach(async () => {
        const searchResult = makeMe.aWikidataSearchEntity.label("dog").please()
        mockedWikidataSearch.mockResolvedValue([searchResult])
        select = await searchWikidata("dog")
      })

      it("focus on the select", async () => {
        expect(select.element).toHaveFocus()
        expect(mockedWikidataSearch).toHaveBeenCalledWith("dog")
      })

      it("remove the select when lose focus", async () => {
        select.element.blur()
        await flushPromises()
        expect(wrapper.vm.$el).not.toContainElement(select.element)
      })
    })

    it.each`
      searchTitle | wikidataTitle | action          | expectedTitle
      ${"dog"}    | ${"dog"}      | ${doNothing}    | ${"dog"}
      ${"dog"}    | ${"Dog"}      | ${doNothing}    | ${"Dog"}
      ${"dog"}    | ${"Canine"}   | ${replaceTitle} | ${"Canine"}
      ${"dog"}    | ${"Canine"}   | ${appendTitle}  | ${"dog / Canine"}
    `(
      "search $searchTitle get $wikidataTitle and choose to $action",
      async ({ searchTitle, wikidataTitle, action, expectedTitle }) => {
        const searchResult = makeMe.aWikidataSearchEntity
          .label(wikidataTitle)
          .please()

        mockedWikidataSearch.mockResolvedValue([searchResult])
        await searchAndSelectFirstResult(searchTitle)

        action()
        await flushPromises()

        expect(mockedWikidataSearch).toHaveBeenCalledWith(searchTitle)
        expect((<HTMLInputElement>titleInput().element).value).toBe(
          expectedTitle
        )
      }
    )
  })
})
