import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type { Note } from "@generated/backend"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationDialog (Edit Mode)", () => {
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

  const wikidataId = "Q123"

  async function putWikidataIdAndSave(note: Note, wikidataId: string) {
    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
        note,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const input = modal?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
    expect(input).toBeTruthy()
    input.value = wikidataId
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    const saveButton = Array.from(modal?.querySelectorAll("button") || []).find(
      (btn) => btn.textContent?.trim() === "Save"
    ) as HTMLButtonElement
    if (saveButton) {
      saveButton.click()
      await flushPromises()
    }
    return wrapper
  }

  async function selectFromSearchAndSave(note: Note, wikidataId: string) {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id(wikidataId)
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    mockedFetchWikidataEntity.mockResolvedValue(
      makeMe.aWikidataEntity.wikidataTitle("Dog").please() as never
    )
    mockedUpdateWikidataId.mockResolvedValue({} as never)

    const wrapper = helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
        note,
      })
      .mount({ attachTo: document.body })
    await flushPromises()

    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    if (select) {
      select.value = wikidataId
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()
    }
    return wrapper
  }

  it.each`
    noteTitle   | wikidataTitle | shouldSave | needsTitleAction
    ${"dog"}    | ${"dog"}      | ${true}    | ${false}
    ${"Dog"}    | ${"dog"}      | ${true}    | ${false}
    ${"Canine"} | ${"dog"}      | ${true}    | ${true}
    ${"Canine"} | ${""}         | ${true}    | ${false}
  `(
    "associate $noteTitle with $wikidataTitle via manual input",
    async ({ noteTitle, wikidataTitle, shouldSave, needsTitleAction }) => {
      vi.clearAllMocks()
      const note = makeMe.aNote.topicConstructor(noteTitle).please()
      const wikidata = makeMe.aWikidataEntity
        .wikidataTitle(wikidataTitle)
        .please()

      mockedFetchWikidataEntity.mockResolvedValue(wikidata as never)
      mockedUpdateWikidataId.mockResolvedValue({} as never)

      const wrapper = await putWikidataIdAndSave(note, wikidataId)
      await flushPromises()

      expect(
        helper.managedApi.services.fetchWikidataEntityDataById
      ).toBeCalledWith({ wikidataId })

      if (needsTitleAction) {
        // When titles differ, title options should be shown
        // User needs to select Replace or Append option, then click Save
        const modal = document.querySelector(".modal-container")
        const replaceButton = modal?.querySelector(
          'label[for*="Replace"]'
        ) as HTMLLabelElement
        expect(replaceButton).toBeTruthy()
        // Select Replace option
        replaceButton.click()
        await flushPromises()
        // Click Save button to confirm
        const saveButton = Array.from(
          modal?.querySelectorAll("button") || []
        ).find((btn) => btn.textContent?.trim() === "Save") as HTMLButtonElement
        expect(saveButton).toBeTruthy()
        saveButton.click()
        await flushPromises()
      }

      expect(mockedUpdateWikidataId).toBeCalledTimes(shouldSave ? 1 : 0)
      if (shouldSave) {
        expect(wrapper.emitted("closeDialog")).toBeTruthy()
      }
    }
  )

  it("searches using note title as searchKey", async () => {
    const note = makeMe.aNote.topicConstructor("dog").please()
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])

    helper
      .component(WikidataAssociationDialog)
      .withStorageProps({
        note,
      })
      .mount({ attachTo: document.body })
    await flushPromises()

    expect(mockedWikidataSearch).toHaveBeenCalledWith({ search: "dog" })
  })

  it("saves when selecting from search results with matching titles", async () => {
    const note = makeMe.aNote.topicConstructor("dog").please()
    const wrapper = await selectFromSearchAndSave(note, "Q11399")

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
    mockedUpdateWikidataId.mockResolvedValue({} as never)

    const wrapper = await putWikidataIdAndSave(note, wikidataId)
    await flushPromises()

    expect(mockedUpdateWikidataId).not.toHaveBeenCalled()
    expect(wrapper.emitted("closeDialog")).toBeFalsy()
    const modal = document.querySelector(".modal-container")
    const errorMessage = modal?.querySelector(".daisy-text-error")
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

    const wrapper = await putWikidataIdAndSave(note, wikidataId)
    await flushPromises()

    expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted("closeDialog")).toBeFalsy()
    const modal = document.querySelector(".modal-container")
    const errorMessage = modal?.querySelector(".daisy-text-error")
    expect(errorMessage?.textContent).toContain(
      "Duplicate Wikidata ID Detected."
    )
  })
})
