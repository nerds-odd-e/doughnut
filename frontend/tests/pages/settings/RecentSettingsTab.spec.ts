import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import { describe, it, expect, beforeEach, vi } from "vitest"
import RecentSettingsTab from "@/pages/settings/RecentSettingsTab.vue"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("RecentSettingsTab", () => {
  beforeEach(() => {
    mockSdkService(MemoryTrackerController, "getRecentMemoryTrackers", [])
    mockSdkService(MemoryTrackerController, "getRecentlyRecalled", [])
  })

  async function mountWithTabQuery(tab?: string) {
    const router = await productionRouterAt({
      name: "settingsRecent",
      query: tab === undefined ? {} : { tab },
    })
    const pushSpy = vi.spyOn(router, "push")
    const wrapper = helper
      .component(RecentSettingsTab)
      .withRouter(router)
      .mount()
    await flushPromises()
    return { wrapper, pushSpy }
  }

  it("shows Recently Assimilated tab by default when no query parameter", async () => {
    const { wrapper } = await mountWithTabQuery()
    expect(wrapper.find(".daisy-tab-active").text()).toBe(
      "Recently Assimilated"
    )
    expect(wrapper.find(".recently-assimilated-notes").exists()).toBe(true)
  })

  it("shows Recently Recalled tab when query parameter is recentlyRecalled", async () => {
    const { wrapper } = await mountWithTabQuery("recentlyRecalled")
    expect(wrapper.find(".daisy-tab-active").text()).toBe("Recently Recalled")
    expect(wrapper.find(".recently-recalled-notes").exists()).toBe(true)
  })

  it("defaults to Recently Assimilated tab when query parameter is invalid", async () => {
    const { wrapper } = await mountWithTabQuery("invalidTab")
    expect(wrapper.find(".daisy-tab-active").text()).toBe(
      "Recently Assimilated"
    )
  })

  it("updates route when Recently Recalled tab is clicked", async () => {
    const { wrapper, pushSpy } = await mountWithTabQuery()
    const tab = wrapper
      .findAll(".daisy-tab")
      .find((el) => el.text() === "Recently Recalled")
    await tab?.trigger("click")

    expect(pushSpy).toHaveBeenCalledWith({
      name: "settingsRecent",
      query: { tab: "recentlyRecalled" },
    })
  })
})
