import { ConversationMessageController } from "@generated/doughnut-backend-api/sdk.gen"
import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect, beforeEach, vi } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { page } from "vitest/browser"
import { flushPromises } from "@vue/test-utils"

const { mockedPush } = vi.hoisted(() => ({ mockedPush: vi.fn() }))

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
      ConversationMessageController,
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
    const conversation = makeMe.aConversationListItem.please()
    mockSdkService(
      ConversationMessageController,
      "getConversationsOfCurrentUser",
      [conversation]
    )
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
      makeMe.aConversationListItem.please(),
      makeMe.aConversationListItem.please(),
    ]
    beforeEach(() => {
      vi.clearAllMocks()
      mockSdkService(
        ConversationMessageController,
        "getConversationsOfCurrentUser",
        conversations
      )
      mockSdkService(
        ConversationMessageController,
        "getConversation",
        makeMe.aConversation.withId(conversations[1]!.id).please()
      )
    })

    const conversationItems = () =>
      document.querySelectorAll(
        '[data-testid="message-center-conversation-item"]'
      )

    it("should highlight the selected conversation", async () => {
      helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({ conversationId: conversations[1]?.id })
        .render()
      await flushPromises()

      const items = conversationItems()
      expect(items).toHaveLength(2)
      expect(items[1]!.querySelector(".daisy-menu-active")).toBeTruthy()
      expect(items[0]!.querySelector(".daisy-menu-active")).toBeNull()
    })

    it("should navigate when conversation clicked", async () => {
      helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({})
        .render()
      await flushPromises()

      const items = conversationItems()
      expect(items).toHaveLength(2)
      await (items[0] as HTMLElement).click()
      await flushPromises()

      expect(mockedPush).toHaveBeenCalledWith({
        name: "messageCenter",
        params: { conversationId: conversations[0]?.id },
      })
    })
  })
})
