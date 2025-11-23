import CirclesPage from "@/pages/CirclesPage.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import { index } from "@generated/backend/sdk.gen"
import { vi } from "vitest"

vi.mock("@generated/backend/sdk.gen", async () => {
  const actual = await vi.importActual("@generated/backend/sdk.gen")
  return {
    ...actual,
    index: vi.fn(),
  }
})

describe("global bar", () => {
  beforeEach(() => {
    vi.mocked(index).mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  it("opens the circles selection", async () => {
    const wrapper = helper.component(CirclesPage).withRouter().mount()
    wrapper.find("[role='button']").trigger("click")
    await flushPromises()
    expect(index).toBeCalled()
  })
})
