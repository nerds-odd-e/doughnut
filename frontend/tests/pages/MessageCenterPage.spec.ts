import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it, expect } from "vitest"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { fireEvent } from "@testing-library/vue"

describe("MessageCenterPage", () => {
  it("fetch API to be called ONCE on mount", async () => {
    helper.managedApi.restConversationMessageController.getConversationsOfCurrentUser =
      vi.fn().mockResolvedValue([])
    helper.component(MessageCenterPage).withRouter().render()
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
      .withRouter()
      .render()
    await findByText("No conversation selected")
  })

  it("should highlight the selected conversation", async () => {
    const conversations = [
      makeMe.aConversation.please(),
      makeMe.aConversation.please(),
    ]
    helper.managedApi.restConversationMessageController.getConversationsOfCurrentUser =
      vi.fn().mockResolvedValue(conversations)

    const { findAllByRole } = helper
      .component(MessageCenterPage)
      .withRouter()
      .render()

    const listItems = await findAllByRole("listitem")
    expect(listItems).toHaveLength(2)

    // Initially, no conversation should be highlighted
    expect(listItems[0]).not.toHaveClass("active")
    expect(listItems[1]).not.toHaveClass("active")

    // Click on the first conversation
    await fireEvent.click(listItems[0]!)

    // The first conversation should now be highlighted
    expect(listItems[0]).toHaveClass("active")
    expect(listItems[1]).not.toHaveClass("active")

    // Click on the second conversation
    await fireEvent.click(listItems[1]!)

    // The second conversation should now be highlighted
    expect(listItems[0]).not.toHaveClass("active")
    expect(listItems[1]).toHaveClass("active")
  })
})
