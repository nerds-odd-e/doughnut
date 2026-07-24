import DoughnutApp from "@/DoughnutApp.vue"
import {
  apiCallWithLoading,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import {
  CurrentUserInfoController,
  TestabilityRestController,
} from "@generated/doughnut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"
import { nextTick } from "vue"

describe("DoughnutApp thin loading bar", () => {
  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  it("shows the thin bar for an in-flight API call even before login", async () => {
    mockSdkService(TestabilityRestController, "getFeatureToggle", false)
    mockSdkService(CurrentUserInfoController, "currentUserInfo", {
      user: undefined,
      externalIdentifier: undefined,
    })

    helper.component(DoughnutApp).withRouter().render()
    await flushPromises()
    expect(
      document.querySelector("[data-app-busy]"),
      "busy marker should not show while no API call is in flight"
    ).toBeNull()

    let finishApiCall: () => void = () => undefined
    const inFlightCall = apiCallWithLoading(
      () =>
        new Promise<{ data: undefined; error: undefined }>((resolve) => {
          finishApiCall = () => resolve({ data: undefined, error: undefined })
        })
    )
    await nextTick()
    expect(
      document.querySelector("[data-app-busy]"),
      "busy marker should show pre-login while a wrapped API call is in flight"
    ).not.toBeNull()

    finishApiCall()
    await inFlightCall
    await nextTick()
    expect(
      document.querySelector("[data-app-busy]"),
      "busy marker should disappear once the API call finishes"
    ).toBeNull()
  })
})
