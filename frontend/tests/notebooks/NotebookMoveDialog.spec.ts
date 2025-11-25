import NotebookMoveDialog from "@/components/notebook/NotebookMoveDialog.vue"
import { describe, it } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import * as sdk from "@generated/backend/sdk.gen"

describe("circle show page", () => {
  const notebook = makeMe.aNotebook.please()
  const circle1 = makeMe.aCircle.please()
  const circle2 = makeMe.aCircle.please()

  beforeEach(() => {
    mockSdkService("index", [circle1, circle2])
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper.component(NotebookMoveDialog).withProps({ notebook }).render()
    expect(sdk.index).toBeCalled()
  })

  it("filters the current circle", async () => {
    helper.component(NotebookMoveDialog).withProps({ notebook }).render()
    await flushPromises()
    await screen.findByText(circle2.name)
  })
})
