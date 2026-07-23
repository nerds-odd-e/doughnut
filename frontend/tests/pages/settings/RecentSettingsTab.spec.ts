import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { describe, it, expect, beforeEach, vi } from "vitest"
import { reactive } from "vue"
import RecentSettingsTab from "@/pages/settings/RecentSettingsTab.vue"
import helper, { mockSdkService } from "@tests/helpers"

const mockPush = vi.fn()
const mockRoute = reactive({
  name: "settingsRecent",
  path: "/settings/recent",
  params: {},
  query: {},
})

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => mockRoute,
    useRouter: () => ({
      push: mockPush,
    }),
  }
})

describe("RecentSettingsTab.vue", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.query = {}
    mockSdkService(MemoryTrackerController, "getRecentMemoryTrackers", [])
    mockSdkService(MemoryTrackerController, "getRecentlyRecalled", [])
  })

  describe("Tab Navigation", () => {
    it("shows Recently Learned tab by default when no query parameter", () => {
      const wrapper = helper.component(RecentSettingsTab).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Learned")
      expect(
        wrapper.findComponent({ name: "RecentlyLearnedNotes" }).exists()
      ).toBe(true)
    })

    it("shows Recently Learned tab when query parameter is recentlyLearned", () => {
      mockRoute.query = { tab: "recentlyLearned" }
      const wrapper = helper.component(RecentSettingsTab).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Learned")
      expect(
        wrapper.findComponent({ name: "RecentlyLearnedNotes" }).exists()
      ).toBe(true)
    })

    it("shows Recently Recalled tab when query parameter is recentlyRecalled", () => {
      mockRoute.query = { tab: "recentlyRecalled" }
      const wrapper = helper.component(RecentSettingsTab).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Recalled")
      expect(
        wrapper.findComponent({ name: "RecentlyRecalledNotes" }).exists()
      ).toBe(true)
    })

    it("defaults to Recently Learned tab when query parameter is invalid", () => {
      mockRoute.query = { tab: "invalidTab" }
      const wrapper = helper.component(RecentSettingsTab).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Learned")
      expect(
        wrapper.findComponent({ name: "RecentlyLearnedNotes" }).exists()
      ).toBe(true)
    })

    it("updates route when Recently Learned tab is clicked", async () => {
      const wrapper = helper.component(RecentSettingsTab).mount()

      const tab = wrapper
        .findAll(".daisy-tab")
        .find((el) => el.text() === "Recently Learned")
      await tab?.trigger("click")

      expect(mockPush).toHaveBeenCalledWith({
        name: "settingsRecent",
        query: { tab: "recentlyLearned" },
      })
    })

    it("updates route when Recently Recalled tab is clicked", async () => {
      const wrapper = helper.component(RecentSettingsTab).mount()

      const tab = wrapper
        .findAll(".daisy-tab")
        .find((el) => el.text() === "Recently Recalled")
      await tab?.trigger("click")

      expect(mockPush).toHaveBeenCalledWith({
        name: "settingsRecent",
        query: { tab: "recentlyRecalled" },
      })
    })
  })
})
