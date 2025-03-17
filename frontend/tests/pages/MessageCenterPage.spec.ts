import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect } from "vitest"
import helper from "@tests/helpers"
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
    helper.managedApi.restConversationMessageController.getConversationsOfCurrentUser =
      vi.fn().mockResolvedValue([])
    helper.component(MessageCenterPage).withStorageProps({}).render()
    expect(
      helper.managedApi.restConversationMessageController
        .getConversationsOfCurrentUser
    ).toBeCalledTimes(1)
  })

  it("should render no conversation selected by default", async () => {
    const conversation = makeMe.aConversation.please()
    helper.managedApi.restConversationMessageController.getConversationsOfCurrentUser =
      vi.fn().mockResolvedValue([conversation])
    const { findByText } = helper
      .component(MessageCenterPage)
      .withStorageProps({})
      .render()
    await findByText("No conversation selected")
  })

  describe("highlighting the selected conversation", () => {
    const conversations = [
      makeMe.aConversation.please(),
      makeMe.aConversation.please(),
    ]
    beforeEach(() => {
      helper.managedApi.restConversationMessageController.getConversationsOfCurrentUser =
        vi.fn().mockResolvedValue(conversations)
    })

    it("should highlight the selected conversation", async () => {
      const { findAllByRole } = helper
        .component(MessageCenterPage)
        .withStorageProps({ conversationId: conversations[1]?.id })
        .render()
      const listItems = await findAllByRole("listitem")
      expect(listItems).toHaveLength(2)

      expect(listItems[1]).toHaveClass("daisy:active")
      expect(listItems[0]).not.toHaveClass("daisy:active")
    })

    it("should highlight the selected conversation", async () => {
      const { findAllByRole } = helper
        .component(MessageCenterPage)
        .withStorageProps({})
        .render()
      const listItems = await findAllByRole("listitem")
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
