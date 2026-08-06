import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import type { RouteLocationRaw } from "vue-router"
import {
  createMatchMediaSpy,
  mountMainMenu,
  renderComponent,
  expectNavLinkPrimary,
  router,
  setupMainMenuTests,
} from "./mainMenuTestSupport"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useGoToNextAssimilation")
vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./mainMenuMocks")
  return aiReplyEventSourceMockExports()
})

setupMainMenuTests()

describe("MainMenu navigation", () => {
  it("calls goToNextAssimilation from the assimilate menu action link", async () => {
    const goToNextSpy = vi.fn()
    vi.mocked(useGoToNextAssimilation).mockReturnValue({
      goToNextAssimilation: goToNextSpy,
    })

    await renderComponent()
    const assimilateLink = screen.getByLabelText("Assimilate")
    expect(assimilateLink.tagName).toBe("A")
    expect(assimilateLink.getAttribute("href")).toBeNull()

    await fireEvent.click(assimilateLink)

    expect(goToNextSpy).toHaveBeenCalled()
  })

  it.each([
    {
      linkLabel: "Note",
      route: { name: "notebooks" } as RouteLocationRaw,
      context: "notebooks",
    },
    {
      linkLabel: "Note",
      route: {
        name: "notebookPage",
        params: { notebookId: "1" },
      } as RouteLocationRaw,
      context: "notebook page",
    },
    {
      linkLabel: "Note",
      route: {
        name: "folderPage",
        params: { notebookId: "1", folderId: "2" },
      } as RouteLocationRaw,
      context: "folder page",
    },
    {
      linkLabel: "Circles",
      route: {
        name: "circleShow",
        params: { circleId: "1" },
      } as RouteLocationRaw,
      context: "circle show page",
    },
  ])(
    "applies primary nav styling to $linkLabel on $context",
    async ({ linkLabel, route }) => {
      await router.push(route)
      await flushPromises()
      await renderComponent()
      expectNavLinkPrimary(linkLabel)
    }
  )

  it("collapses horizontal menu when clicking outside", async () => {
    createMatchMediaSpy(false)
    mountMainMenu()

    const expandButton = screen.getByLabelText("Toggle menu")
    await fireEvent.click(expandButton)

    expect(screen.getByLabelText("Assimilate")).toBeInTheDocument()

    await fireEvent.click(document.body)

    expect(screen.getByLabelText("Toggle menu")).toBeInTheDocument()
  })
})
