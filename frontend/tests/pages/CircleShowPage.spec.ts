import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it, beforeEach } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("circle show page", () => {
  const currentUser = makeMe.aUser.please()
  const notebook = makeMe.aNotebook
    .creator(currentUser.externalIdentifier)
    .please()
  const circleNote = makeMe.aCircleNote.notebooks(notebook).please()
  let showCircleSpy: ReturnType<typeof mockSdkService<"showCircle">>

  beforeEach(() => {
    showCircleSpy = mockSdkService("showCircle", circleNote)
  })

  it("fetch API to be called ONCE on mount", () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCleanStorage()
      .withProps({ circleId: circleNote.id })
      .render()

    expect(showCircleSpy).toBeCalledWith({
      path: { circle: circleNote.id },
    })
  })

  const moveButtonTitle = "Move to ..."
  it("shows the move notebook button", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCurrentUser(currentUser)
      .withCleanStorage()
      .withProps({ circleId: circleNote.id })
      .render()
    await flushPromises()
    screen.getByTitle(moveButtonTitle)
  })

  it("must not show the move button if not the creator", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .withCleanStorage()
      .withProps({
        circleId: circleNote.id,
      })
      .render()
    await flushPromises()
    expect(screen.queryByTitle(moveButtonTitle)).toBeNull()
  })
})
