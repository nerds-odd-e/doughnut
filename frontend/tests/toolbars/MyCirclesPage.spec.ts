import CirclesPage from "@/pages/CirclesPage.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import { index } from "@generated/backend/sdk.gen"

describe("global bar", () => {
  beforeEach(() => {
    mockSdkService("index", [])
  })

  it("opens the circles selection", async () => {
    const wrapper = helper.component(CirclesPage).withRouter().mount()
    wrapper.find("[role='button']").trigger("click")
    await flushPromises()
    expect(index).toBeCalled()
  })
})
