import DonutApp from "@/DonutApp.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import {
  CurrentUserInfoController,
  TestabilityRestController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { cleanup } from "@testing-library/vue"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, describe, expect, it, vi } from "vitest"
import { navItemOf, pressLauncher } from "./toolbars/donutMenuTestSupport"
import {
  createMenuData,
  memoryTrackerLitesStub,
} from "./toolbars/menuDataMocks"

describe("DonutApp menu data", () => {
  afterEach(() => {
    cleanup()
    teardownGlobalClientForTesting()
    vi.restoreAllMocks()
    resetRecallData()
  })

  it("shows the signed-in user's recall count in the Donut menu", async () => {
    mockSdkService(TestabilityRestController, "getFeatureToggle", false)
    mockSdkService(CurrentUserInfoController, "currentUserInfo", {
      user: makeMe.aUser.please(),
    })
    mockSdkService(
      UserController,
      "getMenuData",
      createMenuData({
        recallStatus: {
          toRepeat: memoryTrackerLitesStub(12),
          currentRecallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
      })
    )
    helper
      .component(DonutApp)
      .withRealRouter(await productionRouterAt({ name: "root" }))
      .withRoutedPages()
      .render()
    await flushPromises()
    await pressLauncher()
    expect(
      navItemOf("Recall")?.querySelector(".recall-count")
    ).toHaveTextContent("12")
  })
})
