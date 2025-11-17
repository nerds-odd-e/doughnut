import WikidataSearchDialog from "@/components/notes/WikidataSearchDialog.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

vitest.mock("vue-router", () => ({
  useRoute: () => ({
    path: "/",
  }),
}))

describe("WikidataSearchDialog", () => {
  const mockedWikidataSearch = vitest.fn()

  beforeEach(() => {
    vi.resetAllMocks()
    document.body.innerHTML = ""
    vi.spyOn(helper.managedApi.services, "searchWikidata").mockImplementation(
      mockedWikidataSearch
    )
  })

  const mountDialog = (searchKey: string, currentTitle: string = "") => {
    return helper
      .component(WikidataSearchDialog)
      .withProps({
        searchKey,
        currentTitle,
      })
      .mount({ attachTo: document.body })
  }

  it("shows loading state initially", async () => {
    mockedWikidataSearch.mockImplementation(
      () =>
        new Promise(() => {
          // Never resolves - intentionally empty to test loading state
        })
    )
    mountDialog("dog")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain("Searching...")
  })

  it("shows not found message when results are empty", async () => {
    mockedWikidataSearch.mockResolvedValue([])
    mountDialog("nonexistent")
    await flushPromises()
    const modal = document.querySelector(".modal-container")
    expect(modal?.textContent).toContain(
      "No Wikidata entries found for 'nonexistent'"
    )
    const closeButton = modal?.querySelector("button.daisy-btn-secondary")
    expect(closeButton?.textContent).toBe("Close")
  })

  it("emits close when cancel button is clicked", async () => {
    mockedWikidataSearch.mockResolvedValue([])
    const wrapper = mountDialog("test")
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

  it("displays search results in select", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    mountDialog("dog")
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
  })

  it("emits selected with no titleAction when titles match case-insensitively", async () => {
    const searchResult = makeMe.aWikidataSearchEntity
      .label("Dog")
      .id("Q11399")
      .please()
    mockedWikidataSearch.mockResolvedValue([searchResult])
    const wrapper = mountDialog("dog", "DOG")
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
    expect(modal?.querySelector('input[value="Neither"]')).toBeTruthy()
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

  it("emits selected with neither action", async () => {
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
    const neitherLabel = modal?.querySelector(
      'label[for="wikidataTitleAction-Neither"]'
    ) as HTMLLabelElement
    expect(neitherLabel).toBeTruthy()
    neitherLabel.click()
    await flushPromises()
    expect(wrapper.emitted("selected")).toBeTruthy()
    const emitted = wrapper.emitted("selected")?.[0]
    expect(emitted?.[0]).toEqual(searchResult)
    expect(emitted?.[1]).toBe("neither")
  })

  it("can cancel from title options view", async () => {
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
    const cancelButton = modal?.querySelector(
      "button.daisy-btn-secondary"
    ) as HTMLButtonElement
    expect(cancelButton).toBeTruthy()
    cancelButton?.click()
    await flushPromises()
    expect(wrapper.emitted("close")).toBeTruthy()
  })
})
