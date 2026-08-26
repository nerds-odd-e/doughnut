import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import AccessTokensSettingsTab from "@/pages/settings/AccessTokensSettingsTab.vue"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import usePopups from "@/components/commons/Popups/usePopups"

describe("AccessTokensSettingsTab", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    vi.clearAllMocks()
    usePopups().popups.register({ popupInfo: [] })
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

  it("deletes a token only after confirm; keeps it when cancelled", async () => {
    mockSdkService(UserController, "getTokens", [
      { id: 7, label: "ci-runner" },
      { id: 8, label: "keep-me" },
    ])
    const deleteSpy = mockSdkService(UserController, "deleteToken", undefined)

    wrapper = helper
      .component(AccessTokensSettingsTab)
      .withRouter()
      .mount({ attachTo: document.body })
    await flushPromises()

    expect(wrapper.text()).toContain("ci-runner")
    expect(wrapper.text()).toContain("keep-me")

    const deleteButtons = () =>
      wrapper.findAll("button").filter((b) => b.text().includes("Delete"))

    await deleteButtons()[1]!.trigger("click")
    await flushPromises()

    const { popups } = usePopups()
    expect(popups.peek()).toHaveLength(1)
    expect(popups.peek()[0]?.type).toBe("confirm")
    expect(deleteSpy).not.toHaveBeenCalled()

    popups.done(false)
    await flushPromises()

    expect(deleteSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain("keep-me")

    await deleteButtons()[0]!.trigger("click")
    await flushPromises()
    expect(popups.peek()).toHaveLength(1)
    expect(deleteSpy).not.toHaveBeenCalled()

    popups.done(true)
    await flushPromises()

    expect(deleteSpy).toHaveBeenCalledWith({ path: { tokenId: 7 } })
    expect(wrapper.text()).not.toContain("ci-runner")
    expect(wrapper.text()).toContain("keep-me")
  })
})
