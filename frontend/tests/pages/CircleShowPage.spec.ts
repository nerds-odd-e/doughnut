import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import * as sdk from "@generated/backend/sdk.gen"

describe("circle show page", () => {
  const currentUser = makeMe.aUser.please()
  const notebook = makeMe.aNotebook
    .creator(currentUser.externalIdentifier)
    .please()
  const circleNote = makeMe.aCircleNote.notebooks(notebook).please()

  beforeEach(() => {
    vi.spyOn(sdk, "showCircle").mockResolvedValue({
      data: circleNote,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withStorageProps({ circleId: circleNote.id })
      .render()
    expect(sdk.showCircle).toBeCalledWith({
      path: { circle: circleNote.id },
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
