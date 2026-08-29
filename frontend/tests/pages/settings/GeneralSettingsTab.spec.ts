import { UserController } from "@generated/donut-backend-api/sdk.gen"
import GeneralSettingsTab from "@/pages/settings/GeneralSettingsTab.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { fireEvent } from "@testing-library/vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { ref, type Ref } from "vue"
import type { User } from "@generated/donut-backend-api"

describe("GeneralSettingsTab", () => {
  let wrapper: VueWrapper
  let currentUser: Ref<User | undefined>

  beforeEach(() => {
    vi.restoreAllMocks()
    currentUser = ref<User | undefined>(undefined)
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("shows the next batch question generation time", async () => {
    const user = makeMe.aUser.please()
    const nextScheduledAt = "2024-06-15T09:00:00.000Z"
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {
      nextScheduledAt,
    })

    wrapper = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .mount()
    await flushPromises()

    expect(wrapper.text()).toContain("Next batch question generation:")
    expect(wrapper.text()).toContain(new Date(nextScheduledAt).toLocaleString())
  })

  it("shows the fallback when no batch question generation is scheduled", async () => {
    const user = makeMe.aUser.please()
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})

    wrapper = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .mount()
    await flushPromises()

    expect(wrapper.text()).toContain(
      "No batch question generation is scheduled yet"
    )
  })

  it("does not show a spaced-repetition day list", async () => {
    const user = makeMe.aUser.please()
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})

    wrapper = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .mount()
    await flushPromises()

    expect(wrapper.find("#user-spaceIntervals").exists()).toBe(false)
  })

  it("updates the injected currentUser ref after saving profile changes", async () => {
    const user = makeMe.aUser.please()
    const updatedUser = { ...user, name: "New name" }
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})
    mockSdkService(UserController, "updateUser", updatedUser)

    wrapper = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .mount()
    await flushPromises()

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(currentUser.value).toEqual(updatedUser)
  })

  const renderTab = async (user: User = makeMe.aUser.please()) => {
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})
    const view = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .render()
    await flushPromises()
    return view
  }

  it("Daily probe is off by default and explains what turning it off does", async () => {
    const { getByLabelText, getByText } = await renderTab()

    expect((getByLabelText("Daily probe") as HTMLInputElement).checked).toBe(
      false
    )
    getByText(
      "Turning this off stops new Daily probes and ends the probe's own trend readout."
    )
  })

  it("submits Daily probe as enabled with the profile", async () => {
    const user = makeMe.aUser.please()
    const updateUser = mockSdkService(UserController, "updateUser", {
      ...user,
      dailyProbeEnabled: true,
    })
    const { getByLabelText, getByDisplayValue } = await renderTab(user)

    await fireEvent.click(getByLabelText("Daily probe"))
    await fireEvent.click(getByDisplayValue("Submit"))
    await flushPromises()

    expect(updateUser).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ dailyProbeEnabled: true }),
      })
    )
  })
})
