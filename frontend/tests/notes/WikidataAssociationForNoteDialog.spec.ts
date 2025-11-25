import WikidataAssociationForNoteDialog from "@/components/notes/WikidataAssociationForNoteDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationForNoteDialog", () => {
  let searchWikidataSpy: ReturnType<typeof mockSdkService<"searchWikidata">>
  let fetchWikidataEntitySpy: ReturnType<
    typeof mockSdkService<"fetchWikidataEntityDataById">
  >
  let updateWikidataIdSpy: ReturnType<typeof mockSdkService<"updateWikidataId">>
  let updateNoteTitleSpy: ReturnType<typeof mockSdkService<"updateNoteTitle">>

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    searchWikidataSpy = mockSdkService("searchWikidata", [])
    fetchWikidataEntitySpy = mockSdkService(
      "fetchWikidataEntityDataById",
      makeMe.aWikidataEntity.wikidataTitle("").please()
    )
    updateWikidataIdSpy = mockSdkService(
      "updateWikidataId",
      makeMe.aNoteRealm.please()
    )
    updateNoteTitleSpy = mockSdkService(
      "updateNoteTitle",
      makeMe.aNoteRealm.please()
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
    getModal()?.querySelector(
      '[data-testid="wikidata-search-results"]'
    ) as HTMLElement
  const getSelectItem = (wikidataId: string) =>
    Array.from(
      getModal()?.querySelectorAll(
        '[data-testid="wikidata-search-result-item"]'
      ) || []
    ).find(
      (item) => item.getAttribute("data-wikidata-id") === wikidataId
    ) as HTMLElement
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

        // Set up mocks before calling inputWikidataIdAndSave
        // Must return non-empty search results so "No results" condition is false
        // This allows the title options to be shown when showTitleOptions is set
        searchWikidataSpy.mockResolvedValue(
          wrapSdkResponse([makeMe.aWikidataSearchEntity.please()])
        )
        fetchWikidataEntitySpy.mockResolvedValue(wrapSdkResponse(wikidata))
        updateWikidataIdSpy.mockResolvedValue(
          wrapSdkResponse(makeMe.aNoteRealm.please())
        )
        updateNoteTitleSpy.mockResolvedValue(
          wrapSdkResponse(makeMe.aNoteRealm.please())
        )

        const wrapper = await inputWikidataIdAndSave(note, wikidataId)
        await flushPromises()
        await flushPromises() // Wait for async operations

        expect(fetchWikidataEntitySpy).toHaveBeenCalledWith({
          path: { wikidataId },
        })

        if (needsTitleAction) {
          // Wait for title options dialog to appear after fetch completes
          await flushPromises()
          await flushPromises()
          const replaceLabel = getModal()?.querySelector(
            'label[for*="Replace"]'
          ) as HTMLLabelElement
          expect(replaceLabel).toBeTruthy()
          replaceLabel.click()
          await flushPromises()
          await flushPromises() // Wait for async operations in handleSelectedForEdit

          // When title action is selected, both updateNoteTitle and updateWikidataId are called
          expect(updateNoteTitleSpy).toHaveBeenCalledTimes(1)
          expect(updateWikidataIdSpy).toHaveBeenCalledTimes(1)
        } else {
          // When no title action is needed, only updateWikidataId is called
          expect(updateWikidataIdSpy).toHaveBeenCalledTimes(1)
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
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aWikidataEntity.wikidataTitle("Dog").please())
      )
      updateWikidataIdSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )

      const wrapper = mountDialog(note)
      await flushPromises()

      const select = getSelect()
      expect(select).toBeTruthy()
      const selectItem = getSelectItem("Q11399")
      expect(selectItem).toBeTruthy()
      selectItem.click()
      await flushPromises()

      // With showSaveButton=true, need to click Save button
      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      expect(fetchWikidataEntitySpy).toHaveBeenCalledWith({
        path: { wikidataId: "Q11399" },
      })
      expect(updateWikidataIdSpy).toHaveBeenCalledTimes(1)
      expect(wrapper.emitted("closeDialog")).toBeTruthy()
    })

    it("shows error when fetchWikidataEntityDataById fails", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const error = {
        message: "The wikidata service is not available",
      }
      fetchWikidataEntitySpy.mockResolvedValue({
        data: undefined,
        error: error.message,
        request: {} as Request,
        response: {} as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
      } as any)

      const wrapper = await inputWikidataIdAndSave(note, wikidataId)
      await flushPromises()

      expect(updateWikidataIdSpy).not.toHaveBeenCalled()
      expect(wrapper.emitted("closeDialog")).toBeFalsy()
      const errorMessage = getModal()?.querySelector(".daisy-text-error")
      expect(errorMessage?.textContent).toContain(
        "The wikidata service is not available"
      )
    })

    it("shows error when updateWikidataId fails", async () => {
      const note = makeMe.aNote.topicConstructor("dog").please()
      const wikidata = makeMe.aWikidataEntity.wikidataTitle("dog").please()
      fetchWikidataEntitySpy.mockResolvedValue(wrapSdkResponse(wikidata))
      const error = { wikidataId: "Duplicate Wikidata ID Detected." }
      updateWikidataIdSpy.mockResolvedValue({
        data: undefined,
        error: { errors: error },
        request: {} as Request,
        response: {} as Response,
        // biome-ignore lint/suspicious/noExplicitAny: SDK response types are complex unions that require any for proper mocking
      } as any)

      const wrapper = await inputWikidataIdAndSave(note, wikidataId)
      await flushPromises()

      expect(updateWikidataIdSpy).toHaveBeenCalledTimes(1)
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
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))

      mountDialog(note)
      await flushPromises()

      expect(searchWikidataSpy).toHaveBeenCalledWith({
        query: { search: "dog" },
      })
    })

    it("replaces title immediately when Replace title is selected", async () => {
      const note = makeMe.aNote.topicConstructor("Canine").please()
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aWikidataEntity.wikidataTitle("Dog").please())
      )
      updateWikidataIdSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )
      updateNoteTitleSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const selectItem = getSelectItem("Q11399")
      expect(selectItem).toBeTruthy()
      selectItem.click()
      await flushPromises()

      // Select Replace option - this should immediately save and close
      const replaceLabel = getModal()?.querySelector(
        'label[for*="Replace"]'
      ) as HTMLLabelElement
      expect(replaceLabel).toBeTruthy()
      replaceLabel.click()
      await flushPromises()

      // Verify title is replaced
      expect(updateNoteTitleSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(updateWikidataIdSpy).toHaveBeenCalledWith({
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
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aWikidataEntity.wikidataTitle("Dog").please())
      )
      updateWikidataIdSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )
      updateNoteTitleSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const selectItem = getSelectItem("Q11399")
      expect(selectItem).toBeTruthy()
      selectItem.click()
      await flushPromises()

      // Select Append option - this should immediately save and close
      const appendLabel = getModal()?.querySelector(
        'label[for*="Append"]'
      ) as HTMLLabelElement
      expect(appendLabel).toBeTruthy()
      appendLabel.click()
      await flushPromises()

      // Verify title is appended with / separator
      expect(updateNoteTitleSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Canine / Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(updateWikidataIdSpy).toHaveBeenCalledWith({
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
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aWikidataEntity.wikidataTitle("Dog").please())
      )
      updateWikidataIdSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )
      updateNoteTitleSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results
      const selectItem = getSelectItem("Q11399")
      expect(selectItem).toBeTruthy()
      selectItem.click()
      await flushPromises()

      // Select Append option - this should immediately save and close
      const appendLabel = getModal()?.querySelector(
        'label[for*="Append"]'
      ) as HTMLLabelElement
      expect(appendLabel).toBeTruthy()
      appendLabel.click()
      await flushPromises()

      // Verify title is set to the entity label when current title is empty
      expect(updateNoteTitleSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          newTitle: "Dog",
        },
      })
      // Verify wikidata ID is saved
      expect(updateWikidataIdSpy).toHaveBeenCalledWith({
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
      searchWikidataSpy.mockResolvedValue(wrapSdkResponse([searchResult]))
      fetchWikidataEntitySpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aWikidataEntity.wikidataTitle("Dog").please())
      )
      updateWikidataIdSpy.mockResolvedValue(
        wrapSdkResponse(makeMe.aNoteRealm.please())
      )

      const wrapper = mountDialog(note)
      await flushPromises()

      // Select from search results (no title conflict, so no title options shown)
      const selectItem = getSelectItem("Q11399")
      expect(selectItem).toBeTruthy()
      selectItem.click()
      await flushPromises()

      // Click Save button
      const saveButton = getSaveButton()
      expect(saveButton).toBeTruthy()
      saveButton.click()
      await flushPromises()

      // Verify title is NOT updated (no title action)
      expect(updateNoteTitleSpy).not.toHaveBeenCalled()
      // Verify wikidata ID is saved
      expect(updateWikidataIdSpy).toHaveBeenCalledWith({
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
