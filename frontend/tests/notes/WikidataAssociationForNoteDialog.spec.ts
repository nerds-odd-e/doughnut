import WikidataAssociationForNoteDialog from "@/components/notes/WikidataAssociationForNoteDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationForNoteDialog", () => {
  const mockedWikidataSearch = vitest.fn()
  const mockedFetchWikidataEntity = vitest.fn()
  const mockedUpdateWikidataId = vitest.fn()
  const mockedUpdateNoteTitle = vitest.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    vi.spyOn(sdk, "searchWikidata").mockImplementation(async (...args) => {
      const result = await mockedWikidataSearch(...args)
      return {
        data: result,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }
    })
    vi
      .spyOn(sdk, "fetchWikidataEntityDataById")
      .mockImplementation(async (options) => {
        const result = await mockedFetchWikidataEntity(options)
        return {
          data: result,
          error: undefined,
          request: {} as Request,
          response: {} as Response,
        }
      }) as never
    vi.spyOn(sdk, "updateWikidataId").mockImplementation(async (options) => {
      const result = await mockedUpdateWikidataId(options)
      return {
        data: result,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }
    })
    vi.spyOn(sdk, "updateNoteTitle").mockImplementation(async (options) => {
      const result = await mockedUpdateNoteTitle(options)
      return {
        data: result,
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      }
    })
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
  const getListItem = (wikidataId: string) =>
    getModal()?.querySelector(
      `[data-wikidata-id="${wikidataId}"]`
    ) as HTMLDivElement
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
        mockedUpdateWikidataId.mockResolvedValue(
          makeMe.aNoteRealm.please() as never
        )
        mockedUpdateNoteTitle.mockResolvedValue(
          makeMe.aNoteRealm.please() as never
        )

        const wrapper = await inputWikidataIdAndSave(note, wikidataId)
        await flushPromises()

        expect(vi.mocked(sdk.fetchWikidataEntityDataById)).toHaveBeenCalledWith(
          {
            path: { wikidataId },
          }
        )

        if (needsTitleAction) {
          const replaceLabel = getModal()?.querySelector(
            'label[for*="Replace"]'
          ) as HTMLLabelElement
          expect(replaceLabel).toBeTruthy()
          replaceLabel.click()
          await flushPromises()
          await flushPromises() // Wait for async operations in handleSelectedForEdit

          // When title action is selected, both updateNoteTitle and updateWikidataId are called
          expect(mockedUpdateNoteTitle).toHaveBeenCalledTimes(1)
          expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
        } else {
          // When no title action is needed, only updateWikidataId is called
          expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
        }
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

      const listItem = getListItem("Q11399")
      expect(listItem).toBeTruthy()
      listItem.click()
      await flushPromises()

      // With showSaveButton=true, need to click Save button
      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      expect(vi.mocked(sdk.fetchWikidataEntityDataById)).toHaveBeenCalledWith({
        path: { wikidataId: "Q11399" },
      })
      expect(mockedUpdateWikidataId).toHaveBeenCalledTimes(1)
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("shows error when fetchWikidataEntityDataById fails", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const error = {
        message: "The wikidata service is not available",
      }
      vi.spyOn(sdk, "fetchWikidataEntityDataById").mockResolvedValue({
        data: undefined as never,
        error: error as never,
        request: {} as Request,
        response: {} as Response,
      })

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

      expect(mockedWikidataSearch).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
    })

    it("replaces title immediately when Replace title is selected", async () => {
      const note = makeMe.aNote.topicConstructor("Canine").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mockedFetchWikidataEntity.mockResolvedValue(
        makeMe.aWikidataEntity.wikidataTitle("Dog").please() as never
      )
      mockedUpdateWikidataId.mockResolvedValue({} as never)
      mockedUpdateNoteTitle.mockResolvedValue({} as never)

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const listItem = getListItem("Q11399")
      expect(listItem).toBeTruthy()
      listItem.click()
      await flushPromises()

      // Select Replace option - this should immediately save and close
      const replaceLabel = getModal()?.querySelector(
        'label[for*="Replace"]'
      ) as HTMLLabelElement
      expect(replaceLabel).toBeTruthy()
      replaceLabel.click()
      await flushPromises()

      // Verify title is replaced
      expect(mockedUpdateNoteTitle).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(mockedUpdateWikidataId).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          wikidataId: "Q11399",
        },
      })
      // Verify dialog is closed
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("appends title immediately when Append title is selected", async () => {
      const note = makeMe.aNote.topicConstructor("Canine").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mockedFetchWikidataEntity.mockResolvedValue(
        makeMe.aWikidataEntity.wikidataTitle("Dog").please() as never
      )
      mockedUpdateWikidataId.mockResolvedValue({} as never)
      mockedUpdateNoteTitle.mockResolvedValue({} as never)

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const listItem = getListItem("Q11399")
      expect(listItem).toBeTruthy()
      listItem.click()
      await flushPromises()

      // Select Append option - this should immediately save and close
      const appendLabel = getModal()?.querySelector(
        'label[for*="Append"]'
      ) as HTMLLabelElement
      expect(appendLabel).toBeTruthy()
      appendLabel.click()
      await flushPromises()

      // Verify title is appended with / separator
      expect(mockedUpdateNoteTitle).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Canine / Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(mockedUpdateWikidataId).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          wikidataId: "Q11399",
        },
      })
      // Verify dialog is closed
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("appends title correctly when current title is empty", async () => {
      const note = makeMe.aNote.topicConstructor("").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mockedFetchWikidataEntity.mockResolvedValue(
        makeMe.aWikidataEntity.wikidataTitle("Dog").please() as never
      )
      mockedUpdateWikidataId.mockResolvedValue({} as never)
      mockedUpdateNoteTitle.mockResolvedValue({} as never)

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const listItem = getListItem("Q11399")
      expect(listItem).toBeTruthy()
      listItem.click()
      await flushPromises()

      // Select Append option - this should immediately save and close
      const appendLabel = getModal()?.querySelector(
        'label[for*="Append"]'
      ) as HTMLLabelElement
      expect(appendLabel).toBeTruthy()
      appendLabel.click()
      await flushPromises()

      // Verify title is set to the entity label when current title is empty
      expect(mockedUpdateNoteTitle).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(mockedUpdateWikidataId).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          wikidataId: "Q11399",
        },
      })
      // Verify dialog is closed
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("only saves wikidata ID when Save is clicked without title action", async () => {
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

      // Select from search results (no title conflict, so no title options shown)
      const listItem = getListItem("Q11399")
      expect(listItem).toBeTruthy()
      listItem.click()
      await flushPromises()

      // Click Save button
      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      // Verify title is NOT updated (no title action)
      expect(mockedUpdateNoteTitle).not.toHaveBeenCalled()
      // Verify wikidata ID is saved
      expect(mockedUpdateWikidataId).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          wikidataId: "Q11399",
        },
      })
      // Verify dialog is closed
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })
  })
})
