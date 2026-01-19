import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect, beforeEach, vi } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { useRouter } from "vue-router"
import { page } from "vitest/browser"
import { flushPromises } from "@vue/test-utils"

const { mockedPush } = vi.hoisted(() => {
  return { mockedPush: vi.fn() }
})

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

describe("MessageCenterPage", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const getConversationsSpy = mockSdkService(
      "getConversationsOfCurrentUser",
      []
    )
    helper
      .component(MessageCenterPage)
      .withCleanStorage()
      .withProps({})
      .render()
    expect(getConversationsSpy).toBeCalledTimes(1)
  })

  it("should render no conversation selected by default", async () => {
    const conversation = makeMe.aConversation.please()
    mockSdkService("getConversationsOfCurrentUser", [conversation])
    helper
      .component(MessageCenterPage)
      .withCleanStorage()
      .withProps({})
      .render()
    await expect
      .element(page.getByText("No conversation selected"))
      .toBeInTheDocument()
  })

  describe("highlighting the selected conversation", () => {
    const conversations = [
      makeMe.aConversation.please(),
      makeMe.aConversation.please(),
    ]
    beforeEach(async () => {
      vi.clearAllMocks()
      mockSdkService("getConversationsOfCurrentUser", conversations)
      await page.viewport(1200, 800)
    })

    it("should highlight the selected conversation", async () => {
      helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({ conversationId: conversations[1]?.id })
        .render()
      const listItems = page.getByRole("listitem")
      await expect.element(listItems).toHaveLength(2)

      await expect.element(listItems.nth(1)).toHaveClass("daisy-active")
      await expect.element(listItems.nth(0)).not.toHaveClass("daisy-active")
    })

    it("should navigate when conversation clicked", async () => {
      helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({})
        .render()
      const listItems = page.getByRole("listitem")
      await expect.element(listItems).toHaveLength(2)

      // Initially, no conversation should be highlighted
      await expect.element(listItems.nth(0)).not.toHaveClass("active")
      await expect.element(listItems.nth(1)).not.toHaveClass("active")

      // Click on the first conversation
      await listItems.nth(0).click()
      await flushPromises()

      expect(useRouter().push).toHaveBeenCalledWith({
        name: "messageCenter",
        params: { conversationId: conversations[0]?.id },
      })
    })
  })
})
