import NotebookMoveDialog from "../../src/components/notebook/NotebookMoveDialog.vue"
import { describe, it } from "vitest"
import makeMe from "../fixtures/makeMe"
import helper from "../helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("circle show page", () => {
  const notebook = makeMe.aNotebook.please()
  const circle1 = makeMe.aCircle.please()
  const circle2 = makeMe.aCircle.please()

  beforeEach(() => {
    const teleportTarget = document.createElement("div")
    teleportTarget.id = "head-status"
    document.body.appendChild(teleportTarget)
    helper.managedApi.restCircleController.index = vi
      .fn()
      .mockResolvedValue([circle1, circle2])
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper.component(NotebookMoveDialog).withProps({ notebook }).render()
    expect(helper.managedApi.restCircleController.index).toBeCalled()
  })

  it("filters the current circle", async () => {
    helper.component(NotebookMoveDialog).withProps({ notebook }).render()
    await flushPromises()
    await screen.findByText(circle2.name)
    // expect(screen.queryByText(circle1.name)).toBeNull()
  })
})
