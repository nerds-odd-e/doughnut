import NotebookMoveForm from "@/components/notebook/NotebookMoveForm.vue"
import { describe, it, beforeEach, expect } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"

describe("circle show page", () => {
  const notebook = makeMe.aNotebook.please()
  const circle1 = makeMe.aCircle.please()
  const circle2 = makeMe.aCircle.please()
  let indexSpy: ReturnType<typeof mockSdkService<"index">>

  beforeEach(() => {
    indexSpy = mockSdkService("index", [circle1, circle2])
  })

  it("fetch API to be called ONCE on mount", async () => {
    helper.component(NotebookMoveForm).withProps({ notebook }).render()
    expect(indexSpy).toBeCalled()
  })

  it("filters the current circle", async () => {
    helper.component(NotebookMoveForm).withProps({ notebook }).render()
    await flushPromises()
    await screen.findByText(circle2.name)
  })
})
