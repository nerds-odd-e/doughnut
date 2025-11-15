import NotebookMoveDialog from "@/components/notebook/NotebookMoveDialog.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("circle show page", () => {
  const notebook = makeMe.aNotebook.please()
  const circle1 = makeMe.aCircle.please()
  const circle2 = makeMe.aCircle.please()

  beforeEach(() => {
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
  })
})
