import LinkNoteDialog from "@/components/links/LinkNoteDialog.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import * as sdk from "@generated/backend/sdk.gen"
import { beforeEach, vi } from "vitest"

describe("LinkNoteDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults component
    vi.spyOn(sdk, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "searchForLinkTarget").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "searchForLinkTargetWithin").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "semanticSearch").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "semanticSearchWithin").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })
  it("Search at the top level with no note", async () => {
    helper.component(LinkNoteDialog).withStorageProps({ note: null }).render()
    await screen.findByText("Searching")
    expect(
      await screen.findByLabelText("All My Notebooks And Subscriptions")
    ).toBeDisabled()
  })

  it("toggle search settings", async () => {
    const note = MakeMe.aNote.please()
    helper.component(LinkNoteDialog).withStorageProps({ note }).render()
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
