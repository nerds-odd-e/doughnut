import CirclesPage from "@/pages/CirclesPage.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"

describe("global bar", () => {
  beforeEach(() => {
    vi.spyOn(helper.managedApi.services, "index").mockResolvedValue([])
  })

  it("opens the circles selection", async () => {
    const wrapper = helper.component(CirclesPage).withRouter().mount()
    wrapper.find("[role='button']").trigger("click")
    await flushPromises()
    expect(helper.managedApi.services.index).toBeCalled()
  })
})
