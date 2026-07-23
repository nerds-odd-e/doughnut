import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import AccessTokensSettingsTab from "@/pages/settings/AccessTokensSettingsTab.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

describe("AccessTokensSettingsTab", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it('displays "No Label" when token label is empty', async () => {
    mockSdkService(UserController, "generateToken", {
      token: "mocked-token",
      label: "",
      id: 1,
    })
    mockSdkService(UserController, "getTokens", [])

    wrapper = helper
      .component(AccessTokensSettingsTab)
      .withRouter()
      .mount({ attachTo: document.body })
    await flushPromises()

    const generateBtn = wrapper
      .findAll("button")
      .find((b) => b.text().includes("Generate Token"))
    expect(generateBtn).toBeDefined()
    await generateBtn!.trigger("click")
    await flushPromises()

    const submit = document.body.querySelector(
      'input[type="submit"]'
    ) as HTMLInputElement
    expect(submit).toBeTruthy()
    submit.click()
    await flushPromises()

    expect(wrapper.text()).toContain("No Label")
  })
})
