import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("circle show page", () => {
  const currentUser = makeMe.aUser.please()
  const notebook = makeMe.aNotebook
    .creator(currentUser.externalIdentifier)
    .please()
  const circleNote = makeMe.aCircleNote.notebooks(notebook).please()

  beforeEach(() => {
    vi.spyOn(
      helper.managedApi.services,
      "showCircle"
    ).mockResolvedValue(circleNote as never)
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withStorageProps({ circleId: circleNote.id })
      .render()
    expect(helper.managedApi.services.showCircle).toBeCalledWith({
      circle: circleNote.id,
    })
  })

  const moveButtonTitle = "Move to ..."
  it("shows the move notebook button", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCurrentUser(currentUser)
      .withStorageProps({ circleId: circleNote.id })
      .render()
    await flushPromises()
    screen.getByRole("button", { name: moveButtonTitle })
  })

  it("must not show the move button if not the creator", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .withStorageProps({
        circleId: circleNote.id,
      })
      .render()
    await flushPromises()
    expect(screen.queryByRole("button", { name: moveButtonTitle })).toBeNull()
  })
})
