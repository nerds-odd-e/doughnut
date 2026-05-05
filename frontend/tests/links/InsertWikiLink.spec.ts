import AddLinkDialog from "@/components/links/AddLinkDialog.vue"
import { useDetailsCursorInserter } from "@/composables/useDetailsCursorInserter"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("InsertWikiLink", () => {
  const insertedTexts: string[] = []

  beforeEach(() => {
    vi.clearAllMocks()
    insertedTexts.length = 0
    mockSdkService("getRecentNotes", [])
    mockSdkService("searchForRelationshipTarget", [])
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
    const { registerInserter } = useDetailsCursorInserter()
    registerInserter((text) => insertedTexts.push(text))
  })

  it("calls the registered inserter with a wiki link text when Insert as a wiki link is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Target CI").please()
    mockSdkService("searchForRelationshipTargetWithin", [
      { hitKind: "NOTE" as const, noteSearchResult: targetResult },
    ])

    helper
      .component(AddLinkDialog)
      .withCleanStorage()
      .withProps({ note })
      .render()

    const searchInput = await screen.findByPlaceholderText("Search")
    fireEvent.update(searchInput, "CI")
    await new Promise((resolve) => setTimeout(resolve, 1100))
    await flushPromises()

    fireEvent.click(await screen.findByRole("button", { name: "Add link" }))
    await flushPromises()

    fireEvent.click(
      await screen.findByRole("button", { name: "Insert as a wiki link" })
    )
    await flushPromises()

    expect(insertedTexts).toContain("[[Target CI]]")
  })

  it("does not call the inserter when Add a new relationship note is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Sedation").please()
    mockSdkService("searchForRelationshipTargetWithin", [
      { hitKind: "NOTE" as const, noteSearchResult: targetResult },
    ])

    helper
      .component(AddLinkDialog)
      .withCleanStorage()
      .withProps({ note })
      .render()

    const searchInput = await screen.findByPlaceholderText("Search")
    fireEvent.update(searchInput, "Sed")
    await new Promise((resolve) => setTimeout(resolve, 1100))
    await flushPromises()

    fireEvent.click(await screen.findByRole("button", { name: "Add link" }))
    await flushPromises()

    fireEvent.click(
      await screen.findByRole("button", {
        name: "Add a new relationship note",
      })
    )
    await flushPromises()

    expect(insertedTexts).toHaveLength(0)
    expect(await screen.findByText("Complete relationship")).toBeInTheDocument()
  })
})
