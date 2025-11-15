import CirclesPage from "@/pages/CirclesPage.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"

describe("global bar", () => {
  beforeEach(() => {
    helper.managedApi.restCircleController.index = vitest
      .fn()
      .mockResolvedValue([])
  })

  it("opens the circles selection", async () => {
    const wrapper = helper.component(CirclesPage).withRouter().mount()
    wrapper.find("[role='button']").trigger("click")
    await flushPromises()
    expect(helper.managedApi.restCircleController.index).toBeCalled()
  })
})
