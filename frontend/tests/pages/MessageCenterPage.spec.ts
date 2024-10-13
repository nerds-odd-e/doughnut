import MessageCenterPage from "@/pages/MessageCenterPage.vue"
import { describe, it } from "vitest"
import helper from "../helpers"
import makeMe from "../fixtures/makeMe"
describe("bazaar page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    helper.managedApi.restFeedbackController.getFeedbackThreadsForUser = vi
      .fn()
      .mockResolvedValue([])
    helper.component(MessageCenterPage).withRouter().render()
    expect(
      helper.managedApi.restFeedbackController.getFeedbackThreadsForUser
    ).toBeCalledTimes(1)
  })

  it("should render no conversation selected by default", async () => {
    const conversation = makeMe.aConversation.please()
    helper.managedApi.restFeedbackController.getFeedbackThreadsForUser = vi
      .fn()
      .mockResolvedValue([conversation])
    const { findByText } = helper
      .component(MessageCenterPage)
      .withRouter()
      .render()
    await findByText("No conversation selected")
  })
})
