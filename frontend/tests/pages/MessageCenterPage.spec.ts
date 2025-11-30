import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect, beforeEach } from "vitest"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { fireEvent } from "@testing-library/vue"
import { useRouter } from "vue-router"
import { flushPromises } from "@vue/test-utils"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRouter: () => ({
    push: mockedPush,
  }),
}))

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
    const { findByText } = helper
      .component(MessageCenterPage)
      .withCleanStorage()
      .withProps({})
      .render()
    await findByText("No conversation selected")
  })

  describe("highlighting the selected conversation", () => {
    const conversations = [
      makeMe.aConversation.please(),
      makeMe.aConversation.please(),
    ]
    beforeEach(() => {
      mockSdkService("getConversationsOfCurrentUser", conversations)
    })

    it("should highlight the selected conversation", async () => {
      const { container } = helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({ conversationId: conversations[1]?.id })
        .render()
      await flushPromises()
      const listItems = container.querySelectorAll("li.daisy-menu-item")
      expect(listItems).toHaveLength(2)

      expect(listItems[1]).toHaveClass("daisy-active")
      expect(listItems[0]).not.toHaveClass("daisy-active")
    })

    it("should highlight the selected conversation", async () => {
      const { container } = helper
        .component(MessageCenterPage)
        .withCleanStorage()
        .withProps({})
        .render()
      await flushPromises()
      const listItems = container.querySelectorAll("li.daisy-menu-item")
      expect(listItems).toHaveLength(2)

      // Initially, no conversation should be highlighted
      expect(listItems[0]).not.toHaveClass("active")
      expect(listItems[1]).not.toHaveClass("active")

      // Click on the first conversation
      await fireEvent.click(listItems[0]!)
      await flushPromises()

      expect(useRouter().push).toHaveBeenCalledWith({
        name: "messageCenter",
        params: { conversationId: conversations[0]?.id },
      })
    })
  })
})
