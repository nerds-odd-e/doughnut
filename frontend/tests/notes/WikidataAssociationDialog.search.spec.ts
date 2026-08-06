import { flushPromises } from "@vue/test-utils"
import { wrapSdkResponse } from "@tests/helpers"
import {
  mockWikidataSearchResult,
  useWikidataAssociationDialogTestLifecycle,
  wikidataInput,
  wikidataModal,
  wikidataSearchResultItem,
  wikidataSearchResults,
} from "@tests/notes/wikidataAssociationDialogTestSupport"
import { describe, it, expect, vi } from "vitest"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({
      path: "/",
    }),
  }
})

const { mountDialog, getSdkSpies } = useWikidataAssociationDialogTestLifecycle()

describe("WikidataAssociationDialog search and input", () => {
  it("shows the current wikidata ID in the input field", async () => {
    mountDialog("Test Title", { modelValue: "Q123" })
    await flushPromises()
    expect(wikidataInput().value).toBe("Q123")
  })

  it("displays error message in the dialog", async () => {
    mountDialog("Test Title", { errorMessage: "Invalid Wikidata ID" })
    await flushPromises()
    expect(
      wikidataModal()?.querySelector(".text-error")?.textContent
    ).toContain("Invalid Wikidata ID")
  })

  it("shows header title", async () => {
    mountDialog("Test Title")
    await flushPromises()
    expect(wikidataModal()?.textContent).toContain("Associate Wikidata")
  })

  it("emits close when close button is clicked", async () => {
    getSdkSpies().searchWikidataSpy.mockResolvedValue(wrapSdkResponse([]))
    const dialog = mountDialog("test")
    await flushPromises()
    ;(
      wikidataModal()?.querySelector(
        "button.daisy-btn-secondary"
      ) as HTMLButtonElement
    )?.click()
    await flushPromises()
    expect(dialog.emitted("close")).toBeTruthy()
  })

  it("shows loading state when searching", async () => {
    getSdkSpies().searchWikidataSpy.mockImplementation(
      () =>
        new Promise(() => {
          // never resolves — loading state
        }) as never
    )
    mountDialog("dog")
    await flushPromises()
    expect(wikidataModal()?.textContent).toContain("Searching...")
  })

  it("shows not found message when results are empty", async () => {
    getSdkSpies().searchWikidataSpy.mockResolvedValue(wrapSdkResponse([]))
    mountDialog("nonexistent")
    await flushPromises()
    expect(wikidataModal()?.textContent).toContain(
      "No Wikidata entries found for 'nonexistent'"
    )
  })

  it("displays search results when found", async () => {
    mockWikidataSearchResult(getSdkSpies().searchWikidataSpy, "Dog", "Q11399")
    mountDialog("dog")
    await flushPromises()
    expect(wikidataSearchResults().textContent).toContain("Dog")
    expect(wikidataSearchResultItem("Q11399")).toBeTruthy()
  })

  it("emits update:modelValue when user types a Wikidata ID", async () => {
    const dialog = mountDialog("Test Title")
    await flushPromises()
    const input = wikidataInput()
    input.value = "Q456"
    input.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()
    expect(dialog.emitted("update:modelValue")?.[0]).toEqual(["Q456"])
  })
})
