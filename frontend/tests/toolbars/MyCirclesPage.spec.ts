import CirclesPage from "@/pages/CirclesPage.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"

describe("global bar", () => {
  let indexSpy: ReturnType<typeof mockSdkService<"index">>

  beforeEach(() => {
    indexSpy = mockSdkService("index", [])
  })

  it("opens the circles selection", async () => {
    const wrapper = helper.component(CirclesPage).withRouter().mount()
    wrapper.find("[role='button']").trigger("click")
    await flushPromises()
    expect(indexSpy).toBeCalled()
  })
})
