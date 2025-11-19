import WikidataAssociationForNoteDialog from "@/components/notes/WikidataAssociationForNoteDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationForNoteDialog", () => {
  const mockedWikidataSearch = vitest.fn()
  const mockedFetchWikidataEntity = vitest.fn()
  const mockedUpdateWikidataId = vitest.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    vi.spyOn(helper.managedApi.services, "searchWikidata").mockImplementation(
      mockedWikidataSearch
    )
    vi.spyOn(
      helper.managedApi.services,
      "fetchWikidataEntityDataById"
    ).mockImplementation(mockedFetchWikidataEntity)
    vi.spyOn(helper.managedApi.services, "updateWikidataId").mockImplementation(
      mockedUpdateWikidataId
    )
  })

  const mountDialog = (note: ReturnType<typeof makeMe.aNote.please>) => {
    return helper
      .component(WikidataAssociationForNoteDialog)
      .withStorageProps({ note })
      .mount({ attachTo: document.body })
  }

  const getModal = () => document.querySelector(".modal-container")
  const getInput = () =>
    getModal()?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
  const getSelect = () =>
    getModal()?.querySelector("select") as HTMLSelectElement
  const getSaveButton = () =>
    getModal()?.querySelector('button[type="submit"]') as HTMLButtonElement

  describe("edit mode", () => {
    const wikidataId = "Q123"

    const inputWikidataIdAndSave = async (
      note: ReturnType<typeof makeMe.aNote.please>,
      id: string
    ) => {
      const wrapper = mountDialog(note)
      await flushPromises()

      const input = getInput()
      expect(input).toBeTruthy()
      input.value = id
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()

      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      return wrapper
    }

    it.each`
      noteTitle   | wikidataTitle | needsTitleAction
      ${"dog"}    | ${"dog"}      | ${false}
      ${"Dog"}    | ${"dog"}      | ${false}
      ${"Canine"} | ${"dog"}      | ${true}
      ${"Canine"} | ${""}         | ${false}
    `(
      "saves $noteTitle with $wikidataTitle via manual input",
      async ({ noteTitle, wikidataTitle, needsTitleAction }) => {
        const note = makeMe.aNote.topicConstructor(noteTitle).please()
        const wikidata = makeMe.aWikidataEntity
          .wikidataTitle(wikidataTitle)
          .please()

        mockedFetchWikidataEntity.mockResolvedValue(wikidata as never)
        mockedUpdateWikidataId.mockResolvedValue({} as never)

        const wrapper = await inputWikidataIdAndSave(note, wikidataId)
        await flushPromises()

        expect(mockedFetchWikidataEntity).toHaveBeenCalledWith({ wikidataId })

        if (needsTitleAction) {
          const replaceLabel = getModal()?.querySelector(
            'label[for*="Replace"]'
          ) as HTMLLabelElement
          expect(replaceLabel).toBeTruthy()
          replaceLabel.click()
          await flushPromises()

          const saveButton = getSaveButton()
          expect(saveButton).toBeTruthy()
          saveButton.click()
          await flushPromises()
        }

        expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
        expect(wrapper.emitted("closeDialog")).toBeTruthy()
      }
    )

    it("saves when selecting from search results with matching titles", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mockedFetchWikidataEntity.mockResolvedValue(
        makeMe.aWikidataEntity.wikidataTitle("Dog").please() as never
      )
      mockedUpdateWikidataId.mockResolvedValue({} as never)

      const wrapper = mountDialog(note)
      await flushPromises()

      const select = getSelect()
      expect(select).toBeTruthy()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      // With showSaveButton=true, need to click Save button
      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      expect(mockedFetchWikidataEntity).toHaveBeenCalledWith({
        wikidataId: "Q11399",
      })
      expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("shows error when fetchWikidataEntityDataById fails", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const error = new Error("Not found")
      // @ts-expect-error - mocking error structure
      error.body = { message: "The wikidata service is not available" }
      mockedFetchWikidataEntity.mockRejectedValue(error)

      const wrapper = await inputWikidataIdAndSave(note, wikidataId)
      await flushPromises()

      expect(mockedUpdateWikidataId).not.toHaveBeenCalled()
      expect(wrapper.emitted("closeDialog")).toBeFalsy()
      const errorMessage = getModal()?.querySelector(".daisy-text-error")
      expect(errorMessage?.textContent).toContain(
        "The wikidata service is not available"
      )
    })

    it("shows error when updateWikidataId fails", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const wikidata = makeMe.aWikidataEntity.wikidataTitle("dog").please()
      mockedFetchWikidataEntity.mockResolvedValue(wikidata as never)
      const error = { wikidataId: "Duplicate Wikidata ID Detected." }
      mockedUpdateWikidataId.mockRejectedValue(error)

      const wrapper = await inputWikidataIdAndSave(note, wikidataId)
      await flushPromises()

      expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
      expect(wrapper.emitted("closeDialog")).toBeFalsy()
      const errorMessage = getModal()?.querySelector(".daisy-text-error")
      expect(errorMessage?.textContent).toContain(
        "Duplicate Wikidata ID Detected."
      )
    })

    it("searches using note title as searchKey", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])

      mountDialog(note)
      await flushPromises()

      expect(mockedWikidataSearch).toHaveBeenCalledWith({ search: "dog" })
    })
  })
})
