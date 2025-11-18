import WikidataAssociationUnifiedDialog from "@/components/notes/WikidataAssociationUnifiedDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataAssociationUnifiedDialog", () => {
  const mockedWikidataSearch = vitest.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    vi.spyOn(helper.managedApi.services, "searchWikidata").mockImplementation(
      mockedWikidataSearch
    )
  })

  const mountDialog = (
    currentTitle: string,
    searchKey?: string,
    modelValue?: string,
    errorMessage?: string,
    headerTitle?: string,
    showSaveButton?: boolean
  ) => {
    return helper
      .component(WikidataAssociationUnifiedDialog)
      .withProps({
        searchKey,
        currentTitle,
        modelValue,
        errorMessage,
        headerTitle,
        showSaveButton,
      })
      .mount({ attachTo: document.body })
  }

  it("shows the current wikidata ID in the input field", async () => {
    mountDialog("Test Title", undefined, "Q123")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const input = modal?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
    expect(input?.value).toBe("Q123")
  })

  it("emits update:modelValue when user types in the input", async () => {
    const wrapper = mountDialog("Test Title")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const input = modal?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
    expect(input).toBeTruthy()
    input.value = "Q456"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted("update:modelValue")).toBeTruthy()
    expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q456"])
  })

  it("displays error message in the input field", async () => {
    mountDialog("Test Title", undefined, undefined, "Invalid Wikidata ID")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const errorMessage = modal?.querySelector(".daisy-text-error")
    expect(errorMessage?.textContent).toContain("Invalid Wikidata ID")
  })

  it("uses default header title when not provided", async () => {
    mountDialog("Test Title")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain("Search Wikidata")
  })

  it("uses custom header title when provided", async () => {
    mountDialog("Test Title", undefined, undefined, undefined, "Custom Title")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain("Custom Title")
  })

  it("shows loading state when searching", async () => {
    mockedWikidataSearch.mockImplementation(
      () =>
        new Promise(() => {
          // Never resolves - intentionally empty to test loading state
        })
    )
    mountDialog("Test Title", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain("Searching...")
  })

  it("shows not found message when results are empty and searchKey provided", async () => {
    mockedWikidataSearch.mockResolvedValue([])
    mountDialog("Test Title", "nonexistent")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain(
      "No Wikidata entries found for 'nonexistent'"
    )
  })

  it("does not show not found message when searchKey is not provided", async () => {
    mockedWikidataSearch.mockResolvedValue([])
    mountDialog("Test Title")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).not.toContain("No Wikidata entries found")
  })

  it("emits close when close button is clicked", async () => {
    mockedWikidataSearch.mockResolvedValue([])
    const wrapper = mountDialog("Test Title", "test")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const closeButton = modal?.querySelector(
      "button.daisy-btn-secondary"
    ) as HTMLButtonElement
    expect(closeButton).toBeTruthy()
    closeButton?.click()
    await flushPromises()
    expect(wrapper.emitted("close")).toBeTruthy()
  })

  it("displays search results in select when searchKey is provided", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    mountDialog("Test Title", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector('select[name="wikidataSearchResult"]')
    expect(select).toBeTruthy()
    expect(select?.textContent).toContain("Dog")
  })

  it("emits selected with no titleAction when titles match", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    const wrapper = mountDialog("dog", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    expect(select).toBeTruthy()
    select.value = "Q11399"
    select.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted("selected")).toBeTruthy()
    const emitted = wrapper.emitted("selected")?.[0]
    expect(emitted?.[0]).toEqual(searchResult)
    expect(emitted?.[1]).toBeUndefined()
    // Check that the input value is updated
    const input = modal?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
    expect(input?.value).toBe("Q11399")
    // Check that update:modelValue was emitted
    expect(wrapper.emitted("update:modelValue")).toBeTruthy()
    expect(
      wrapper.emitted("update:modelValue")?.some((args) => args[0] === "Q11399")
    ).toBe(true)
  })

  it("emits selected with no titleAction when titles match case-insensitively", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    const wrapper = mountDialog("DOG", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    expect(select).toBeTruthy()
    select.value = "Q11399"
    select.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted("selected")).toBeTruthy()
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
    mountDialog("dog", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal).toBeTruthy()
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    expect(select).toBeTruthy()
    select.value = "Q11399"
    select.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
    expect(modal?.textContent).toContain("Suggested Title: Canine")
    expect(modal?.querySelector('input[value="Replace"]')).toBeTruthy()
    expect(modal?.querySelector('input[value="Append"]')).toBeTruthy()
  })

  it("emits selected with replace action", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Canine")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    const wrapper = mountDialog("dog", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    expect(select).toBeTruthy()
    select.value = "Q11399"
    select.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
    const replaceLabel = modal?.querySelector(
      'label[for="wikidataTitleAction-Replace"]'
    ) as HTMLLabelElement
    expect(replaceLabel).toBeTruthy()
    replaceLabel.click()
    await flushPromises()
    expect(wrapper.emitted("selected")).toBeTruthy()
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
    const wrapper = mountDialog("dog", "dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const select = modal?.querySelector(
      'select[name="wikidataSearchResult"]'
    ) as HTMLSelectElement
    expect(select).toBeTruthy()
    select.value = "Q11399"
    select.dispatchEvent(new Event("change", { bubbles: true }))
    await flushPromises()
    const appendLabel = modal?.querySelector(
      'label[for="wikidataTitleAction-Append"]'
    ) as HTMLLabelElement
    expect(appendLabel).toBeTruthy()
    appendLabel.click()
    await flushPromises()
    expect(wrapper.emitted("selected")).toBeTruthy()
    const emitted = wrapper.emitted("selected")?.[0]
    expect(emitted?.[0]).toEqual(searchResult)
    expect(emitted?.[1]).toBe("append")
  })

  it("works without searchKey (manual input only)", async () => {
    const wrapper = mountDialog("Test Title")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    const input = modal?.querySelector(
      'input[id="wikidataID-wikidataID"]'
    ) as HTMLInputElement
    expect(input).toBeTruthy()
    expect(mockedWikidataSearch).not.toHaveBeenCalled()
    input.value = "Q999"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted("update:modelValue")).toBeTruthy()
    expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["Q999"])
  })

  describe("open link button", () => {
    const mockedFetchWikidataEntityDataById = vitest.fn()

    beforeEach(() => {
      vi.spyOn(
        helper.managedApi.services,
        "fetchWikidataEntityDataById"
      ).mockImplementation(mockedFetchWikidataEntityDataById)
    })

    it("shows open link button when Wikidata ID is present and showSaveButton is true", async () => {
      mountDialog("Test Title", undefined, "Q123", undefined, undefined, true)
      await flushPromises()
      const modal = document.querySelector(".modal-container")
      const openLinkButton = modal?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement
      expect(openLinkButton).toBeTruthy()
      expect(openLinkButton?.textContent).toContain("open link")
    })

    it("does not show open link button when showSaveButton is false", async () => {
      mountDialog("Test Title", undefined, "Q123", undefined, undefined, false)
      await flushPromises()
      const modal = document.querySelector(".modal-container")
      const openLinkButton = modal?.querySelector('button[title="open link"]')
      expect(openLinkButton).toBeFalsy()
    })

    it("does not show open link button when Wikidata ID is empty", async () => {
      mountDialog("Test Title", undefined, "", undefined, undefined, true)
      await flushPromises()
      const modal = document.querySelector(".modal-container")
      const openLinkButton = modal?.querySelector('button[title="open link"]')
      expect(openLinkButton).toBeFalsy()
    })

    it("opens Wikipedia URL when available", async () => {
      const wikipediaUrl = "https://en.wikipedia.org/wiki/Test"
      mockedFetchWikidataEntityDataById.mockResolvedValue({
        WikipediaEnglishUrl: wikipediaUrl,
      })

      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => {
        return {
          location: { href: "" },
          focus: vi.fn(),
        } as unknown as Window
      })

      mountDialog("Test Title", undefined, "Q123", undefined, undefined, true)
      await flushPromises()
      const modal = document.querySelector(".modal-container")
      const openLinkButton = modal?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement

      openLinkButton.click()
      await flushPromises()

      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(mockedFetchWikidataEntityDataById).toHaveBeenCalledWith({
        wikidataId: "Q123",
      })

      // Wait for the URL to be set
      await new Promise((resolve) => setTimeout(resolve, 100))
      windowOpenSpy.mockRestore()
    })

    it("opens Wikidata URL when Wikipedia URL is not available", async () => {
      mockedFetchWikidataEntityDataById.mockResolvedValue({
        WikipediaEnglishUrl: "",
      })

      const windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => {
        return {
          location: { href: "" },
          focus: vi.fn(),
        } as unknown as Window
      })

      mountDialog("Test Title", undefined, "Q123", undefined, undefined, true)
      await flushPromises()
      const modal = document.querySelector(".modal-container")
      const openLinkButton = modal?.querySelector(
        'button[title="open link"]'
      ) as HTMLButtonElement

      openLinkButton.click()
      await flushPromises()

      expect(windowOpenSpy).toHaveBeenCalledWith("")
      expect(mockedFetchWikidataEntityDataById).toHaveBeenCalledWith({
        wikidataId: "Q123",
      })

      // Wait for the URL to be set
      await new Promise((resolve) => setTimeout(resolve, 100))
      windowOpenSpy.mockRestore()
    })
  })
})
