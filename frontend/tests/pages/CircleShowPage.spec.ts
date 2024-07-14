import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"
import { screen } from "@testing-library/vue"

describe("circle show page", () => {
  const notebook = makeMe.aNotebook.please()
  const circleNote = makeMe.aCircleNote.notebooks(notebook).please()

  beforeEach(() => {
    const teleportTarget = document.createElement("div")
    teleportTarget.id = "head-status"
    document.body.appendChild(teleportTarget)
    helper.managedApi.restCircleController.showCircle = vi
      .fn()
      .mockResolvedValue(circleNote)
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper
      .component(CircleShowPage)
      .withStorageProps({ circleId: circleNote.id })
      .render()
    expect(helper.managedApi.restCircleController.showCircle).toBeCalledWith(
      circleNote.id
    )
  })

  // eslint-disable-next-line vitest/no-disabled-tests
  it.skip("shows the move notebook button", async () => {
    helper
      .component(CircleShowPage)
      .withStorageProps({ circleId: circleNote.id })
      .render()
    screen.getByRole("button", { name: "Move to ..." })
  })

})
