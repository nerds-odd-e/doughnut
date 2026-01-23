import AddRelationshipDialog from "@/components/links/AddRelationshipDialog.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"

describe("AddRelationshipDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults component
    mockSdkService("getRecentNotes", [])
    mockSdkService("searchForRelationshipTarget", [])
    mockSdkService("searchForRelationshipTargetWithin", [])
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
  })
  it("Search at the top level with no note", async () => {
    helper
      .component(AddRelationshipDialog)
      .withCleanStorage()
      .withProps({ note: null })
      .render()
    await screen.findByText("Searching")
    expect(
      await screen.findByLabelText("All My Notebooks And Subscriptions")
    ).toBeDisabled()
  })

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please()
    helper
      .component(AddRelationshipDialog)
      .withCleanStorage()
      .withProps({ note })
      .render()
    ;(await screen.findByLabelText("All My Circles")).click()
    expect(
      await screen.findByLabelText("All My Notebooks And Subscriptions")
    ).toBeChecked()
    flushPromises()
    ;(
      await screen.findByLabelText("All My Notebooks And Subscriptions")
    ).click()
    expect(await screen.findByLabelText("All My Circles")).not.toBeChecked()
  })
})
