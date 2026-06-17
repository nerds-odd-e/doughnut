import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import UserProfileForm from "@/components/toolbars/UserProfileForm.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("UserProfileForm", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    vi.restoreAllMocks()
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

    wrapper = helper.component(UserProfileForm).withRouter().mount()
    await flushPromises()

    expect(wrapper.text()).toContain("Next batch question generation:")
    expect(wrapper.text()).toContain(new Date(nextScheduledAt).toLocaleString())
  })

  it("shows the fallback when no batch question generation is scheduled", async () => {
    const user = makeMe.aUser.please()
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})

    wrapper = helper.component(UserProfileForm).withRouter().mount()
    await flushPromises()

    expect(wrapper.text()).toContain(
      "No batch question generation is scheduled yet"
    )
  })

  it("emits user-updated after saving profile changes", async () => {
    const user = makeMe.aUser.please()
    const updatedUser = { ...user, name: "New name" }
    mockSdkService(UserController, "getUserProfile", user)
    mockSdkService(UserController, "getQuestionGenerationBatchSchedule", {})
    mockSdkService(UserController, "updateUser", updatedUser)

    wrapper = helper.component(UserProfileForm).withRouter().mount()
    await flushPromises()

    await wrapper.get("#user-name").setValue("New name")
    await wrapper.get("form").trigger("submit")
    await flushPromises()

    expect(wrapper.emitted("user-updated")).toEqual([[updatedUser]])
  })
})
