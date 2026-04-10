import CircleShowPage from "@/pages/CircleShowPage.vue"
import { describe, it, beforeEach, expect } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
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

  it("shows catalog list and grid controls when the circle has notebooks", async () => {
    helper
      .component(CircleShowPage)
      .withRouter()
      .withCurrentUser(currentUser)
      .withCleanStorage()
      .withProps({ circleId: circleNote.id })
      .render()
    await flushPromises()
    expect(screen.getByRole("button", { name: "List view" })).toBeVisible()
    expect(screen.getByRole("button", { name: "Grid view" })).toBeVisible()
  })
})
