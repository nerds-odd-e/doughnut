import { UserController } from "@generated/donut-backend-api/sdk.gen"
import GeneralSettingsTab from "@/pages/settings/GeneralSettingsTab.vue"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
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

  const mountTab = async (
    user = makeMe.aUser.please(),
    batchSchedule: { nextScheduledAt?: string } = {}
  ) => {
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(
      UserController,
      "getQuestionGenerationBatchSchedule",
      batchSchedule
    )
    wrapper = helper
      .component(GeneralSettingsTab)
      .withRouter()
      .withCurrentUserRef(currentUser)
      .mount()
    await flushPromises()
    return user
  }

  const submitButton = () =>
    wrapper.get('[data-testid="user-settings-submit"]')
      .element as HTMLButtonElement

  it("shows the next batch question generation time", async () => {
    const nextScheduledAt = "2024-06-15T09:00:00.000Z"
    await mountTab(makeMe.aUser.please(), { nextScheduledAt })

    expect(wrapper.text()).toContain("Next batch question generation:")
    expect(wrapper.text()).toContain(new Date(nextScheduledAt).toLocaleString())
  })

  it("shows the fallback when no batch question generation is scheduled", async () => {
    await mountTab()

    expect(wrapper.text()).toContain(
      "No batch question generation is scheduled yet"
    )
  })

  it("does not show a spaced-repetition day list", async () => {
    await mountTab()

    expect(wrapper.find("#user-spaceIntervals").exists()).toBe(false)
  })

  it("updates the injected currentUser ref after saving profile changes", async () => {
    const user = makeMe.aUser.please()
    const updatedUser = { ...user, name: "New name" }
    mockSdkService(UserController, "updateUser", updatedUser)
    await mountTab(user)

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(currentUser.value).toEqual(updatedUser)
  })

  it("Daily probe is off by default and explains what turning it off does", async () => {
    await mountTab()

    expect(
      (wrapper.get("#user-dailyProbeEnabled").element as HTMLInputElement)
        .checked
    ).toBe(false)
    expect(wrapper.text()).toContain(
      "Turning this off stops new Daily probes and ends the probe's own trend readout."
    )
  })

  it("submits Daily probe as enabled with the profile", async () => {
    const user = makeMe.aUser.please()
    const updateUser = mockSdkService(UserController, "updateUser", {
      ...user,
      dailyProbeEnabled: true,
    })
    await mountTab(user)

    await wrapper.get("#user-dailyProbeEnabled").setValue(true)
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(updateUser).toHaveBeenCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ dailyProbeEnabled: true }),
      })
    )
  })

  it("disables Submit when the profile has not changed", async () => {
    await mountTab()

    expect(submitButton().disabled).toBe(true)
  })

  it("enables Submit after the display name is edited", async () => {
    await mountTab()

    await wrapper.get("#user-name").setValue("New name")

    expect(submitButton().disabled).toBe(false)
  })

  it("does not enable Submit when the daily assimilation count is retyped as the same number", async () => {
    const user = await mountTab()

    await wrapper
      .get("#user-dailyAssimilationCount")
      .setValue(String(user.dailyAssimilationCount))

    expect(submitButton().disabled).toBe(true)
  })

  it("disables Submit and ignores a second submit while a save is in flight", async () => {
    const user = makeMe.aUser.please()
    const updateSpy = mockSdkServiceWithImplementation(
      UserController,
      "updateUser",
      () => new Promise<User>(() => {})
    )
    await mountTab(user)

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await wrapper.vm.$nextTick()

    expect(submitButton().disabled).toBe(true)

    await wrapper.get("form").trigger("submit")
    expect(updateSpy).toHaveBeenCalledOnce()
  })

  it("disables Submit after a successful save until the profile is edited again", async () => {
    const user = makeMe.aUser.please()
    mockSdkService(UserController, "updateUser", { ...user, name: "New name" })
    await mountTab(user)

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(submitButton().disabled).toBe(true)

    await wrapper.get("#user-name").setValue("Even newer")
    expect(submitButton().disabled).toBe(false)
  })

  it("keeps Submit enabled after a failed save so the same values can be retried", async () => {
    const user = makeMe.aUser.please()
    vi.spyOn(UserController, "updateUser").mockResolvedValue(
      wrapSdkError({ errors: { name: "too long" } })
    )
    await mountTab(user)

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(submitButton().disabled).toBe(false)
  })
})
