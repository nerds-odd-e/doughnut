import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"

describe("circle show page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div")
    teleportTarget.id = "head-status"
    document.body.appendChild(teleportTarget)
  })

  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please()
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please()
    helper.managedApi.restCircleController.showCircle = vi
      .fn()
      .mockResolvedValue(circleNote)
    helper
      .component(CircleShowPage)
      .withStorageProps({ circleId: circleNote.id })
      .render()
    expect(helper.managedApi.restCircleController.showCircle).toBeCalledWith(
      circleNote.id,
    )
  })
})
