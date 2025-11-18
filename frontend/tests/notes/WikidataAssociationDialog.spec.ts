import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationDialog", () => {
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

  const mountDialog = (
    searchKey?: string,
    options?: {
      modelValue?: string
      errorMessage?: string
      showSaveButton?: boolean
      note?: ReturnType<typeof makeMe.aNote.please>
    }
  ) => {
    const { note, ...props } = options || {}
    if (note) {
      return helper
        .component(WikidataAssociationDialog)
        .withStorageProps({ note })
        .mount({ attachTo: document.body })
    }
    return helper
      .component(WikidataAssociationDialog)
      .withProps({
        ...(searchKey !== undefined && { searchKey }),
        ...props,
      })
      .mount({ attachTo: document.body })
  }

  const getModal = () => document.querySelector(".modal-container")
  const getInput = () =>
    getModal()?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
  const getSelect = () =>
    getModal()?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement

  describe("basic functionality", () => {
    it("shows the current wikidata ID in the input field", async () => {
      mountDialog("Test Title", { modelValue: "Q123" })
      await flushPromises()
      expect(getInput()?.value).toBe("Q123")
    })

    it("displays error message in the input field", async () => {
      mountDialog("Test Title", { errorMessage: "Invalid Wikidata ID" })
      await flushPromises()
      const errorMessage = getModal()?.querySelector(".daisy-text-error")
      expect(errorMessage?.textContent).toContain("Invalid Wikidata ID")
    })

    it("shows header title", async () => {
      mountDialog("Test Title")
      await flushPromises()
      expect(getModal()?.textContent).toContain("Associate Wikidata")
    })

    it("emits close when close button is clicked", async () => {
      mockedWikidataSearch.mockResolvedValue([])
      const wrapper = mountDialog("test")
      await flushPromises()
      const closeButton = getModal()?.querySelector(
        "button.daisy-btn-secondary"
      ) as HTMLButtonElement
      closeButton?.click()
      await flushPromises()
      expect(wrapper.emitted("close")).toBeTruthy()
    })
  })

  describe("search functionality", () => {
    it("shows loading state when searching", async () => {
      mockedWikidataSearch.mockImplementation(
        () =>
          new Promise(() => {
            // Never resolves - intentionally empty to test loading state
          })
      )
      mountDialog("dog")
      await flushPromises()
      expect(getModal()?.textContent).toContain("Searching...")
    })

    it("shows not found message when results are empty and searchKey provided", async () => {
      mockedWikidataSearch.mockResolvedValue([])
      mountDialog("nonexistent")
      await flushPromises()
      expect(getModal()?.textContent).toContain(
        "No Wikidata entries found for 'nonexistent'"
      )
    })

    it("does not show not found message when searchKey is not provided", async () => {
      mockedWikidataSearch.mockResolvedValue([])
      mountDialog()
      await flushPromises()
      expect(getModal()?.textContent).not.toContain("No Wikidata entries found")
    })

    it("displays search results in select when searchKey is provided", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mountDialog("dog")
      await flushPromises()
      expect(getSelect()).toBeTruthy()
      expect(getSelect()?.textContent).toContain("Dog")
    })
  })

  describe("input handling", () => {
    it("emits update:modelValue when user types in the input", async () => {
      const wrapper = mountDialog("Test Title")
      await flushPromises()
      const input = getInput()
      expect(input).toBeTruthy()
      input.value = "Q456"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q456"])
    })

    it("works without searchKey (manual input only)", async () => {
      const wrapper = mountDialog()
      await flushPromises()
      const input = getInput()
      expect(input).toBeTruthy()
      expect(mockedWikidataSearch).not.toHaveBeenCalled()
      input.value = "Q999"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q999"])
    })
  })

  describe("title matching and actions", () => {
    it("emits selected with no titleAction when titles match", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      const wrapper = mountDialog("dog")
      await flushPromises()

      const select = getSelect()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBeUndefined()
      expect(getInput()?.value).toBe("Q11399")
    })

    it("emits selected with no titleAction when titles match case-insensitively", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Dog")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      const wrapper = mountDialog("DOG")
      await flushPromises()

      const select = getSelect()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBeUndefined()
    })

    it("shows title options when titles differ", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Canine")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      mountDialog("dog")
      await flushPromises()

      const select = getSelect()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      expect(getModal()?.textContent).toContain("Suggested Title: Canine")
      expect(getModal()?.querySelector('input[value="Replace"]')).toBeTruthy()
      expect(getModal()?.querySelector('input[value="Append"]')).toBeTruthy()
    })

    it("emits selected with replace action", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Canine")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      const wrapper = mountDialog("dog")
      await flushPromises()

      const select = getSelect()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      const replaceLabel = getModal()?.querySelector(
        'label[for="wikidataTitleAction-Replace"]'
      ) as HTMLLabelElement
      replaceLabel.click()
      await flushPromises()

      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBe("replace")
    })

    it("emits selected with append action", async () => {
      const searchResult = makeMe.aWikidataSearchEntity
        .label("Canine")
        .id("Q11399")
        .please()
      mockedWikidataSearch.mockResolvedValue([searchResult])
      const wrapper = mountDialog("dog")
      await flushPromises()

      const select = getSelect()
      select.value = "Q11399"
      select.dispatchEvent(new Event("change", { bubbles: true }))
      await flushPromises()

      const appendLabel = getModal()?.querySelector(
        'label[for="wikidataTitleAction-Append"]'
      ) as HTMLLabelElement
      appendLabel.click()
      await flushPromises()

      const emitted = wrapper.emitted("selected")?.[0]
      expect(emitted?.[0]).toEqual(searchResult)
      expect(emitted?.[1]).toBe("append")
    })
  })

  describe("open link button", () => {
    it("shows open link button when Wikidata ID is present and showSaveButton is true", async () => {
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()
      const openLinkButton = getModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.querySelector("svg")).toBeTruthy()
    })

    it("shows open link button when showSaveButton is false but Wikidata ID is present", async () => {
      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: false })
      await flushPromises()
      const openLinkButton = getModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.querySelector("svg")).toBeTruthy()
    })

    it("hides open link button when Wikidata ID is empty", async () => {
      mountDialog("Test Title", { modelValue: "", showSaveButton: true })
      await flushPromises()
      const openLinkButton = getModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.style.display).toBe("none")
    })

    it("opens Wikipedia URL when available", async () => {
      const wikipediaUrl = "https://en.wikipedia.org/wiki/Test"
      mockedFetchWikidataEntity.mockResolvedValue({
        WikipediaEnglishUrl: wikipediaUrl,
      })

      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => {
        return {
          location: { href: "" },
          focus: vi.fn(),
        } as unknown as Window
      })

      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()

      const openLinkButton = getModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      openLinkButton.click()
      await flushPromises()

      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(mockedFetchWikidataEntity).toHaveBeenCalledWith({
        wikidataId: "Q123",
      })

      windowOpenSpy.mockRestore()
    })

    it("opens Wikidata URL when Wikipedia URL is not available", async () => {
      mockedFetchWikidataEntity.mockResolvedValue({
        WikipediaEnglishUrl: "",
      })

      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => {
        return {
          location: { href: "" },
          focus: vi.fn(),
        } as unknown as Window
      })

      mountDialog("Test Title", { modelValue: "Q123", showSaveButton: true })
      await flushPromises()

      const openLinkButton = getModal()?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      openLinkButton.click()
      await flushPromises()

      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(mockedFetchWikidataEntity).toHaveBeenCalledWith({
        wikidataId: "Q123",
      })

      windowOpenSpy.mockRestore()
    })
  })
})
