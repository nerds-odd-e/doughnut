import { describe, it, expect, beforeEach, vi } from "vitest"
import { reactive } from "vue"
import RecentPage from "@/pages/RecentPage.vue"
import helper, { mockSdkService } from "@tests/helpers"

const mockPush = vi.fn()
const mockRoute = reactive({
  name: "recent",
  path: "/d/recent",
  params: {},
  query: {},
})

vitest.mock("vue-router", () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: mockPush,
  }),
}))

describe("RecentPage.vue", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.query = {}
    mockSdkService("getRecentNotes", [])
    mockSdkService("getRecentMemoryTrackers", [])
    mockSdkService("getRecentlyReviewed", [])
  })

  describe("Tab Navigation", () => {
    it("shows Recently Added/Updated tab by default when no query parameter", () => {
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Added/Updated")
      expect(
        wrapper.findComponent({ name: "RecentlyAddedNotes" }).exists()
      ).toBe(true)
    })

    it("shows Recently Added/Updated tab when query parameter is recentlyAdded", () => {
      mockRoute.query = { tab: "recentlyAdded" }
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Added/Updated")
      expect(
        wrapper.findComponent({ name: "RecentlyAddedNotes" }).exists()
      ).toBe(true)
    })

    it("shows Recently Learned tab when query parameter is recentlyLearned", () => {
      mockRoute.query = { tab: "recentlyLearned" }
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Learned")
      expect(
        wrapper.findComponent({ name: "RecentlyLearnedNotes" }).exists()
      ).toBe(true)
    })

    it("shows Recently Reviewed tab when query parameter is recentlyReviewed", () => {
      mockRoute.query = { tab: "recentlyReviewed" }
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Reviewed")
      expect(
        wrapper.findComponent({ name: "RecentlyReviewedNotes" }).exists()
      ).toBe(true)
    })

    it("defaults to Recently Added/Updated tab when query parameter is invalid", () => {
      mockRoute.query = { tab: "invalidTab" }
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".daisy-tab-active")
      expect(activeTab.text()).toBe("Recently Added/Updated")
      expect(
        wrapper.findComponent({ name: "RecentlyAddedNotes" }).exists()
      ).toBe(true)
    })

    it("updates route when Recently Added/Updated tab is clicked", async () => {
      const wrapper = helper.component(RecentPage).mount()

      const tab = wrapper
        .findAll(".daisy-tab")
        .find((el) => el.text() === "Recently Added/Updated")
      await tab?.trigger("click")

      expect(mockPush).toHaveBeenCalledWith({
        name: "recent",
        query: { tab: "recentlyAdded" },
      })
    })

    it("updates route when Recently Learned tab is clicked", async () => {
      const wrapper = helper.component(RecentPage).mount()

      const tab = wrapper
        .findAll(".daisy-tab")
        .find((el) => el.text() === "Recently Learned")
      await tab?.trigger("click")

      expect(mockPush).toHaveBeenCalledWith({
        name: "recent",
        query: { tab: "recentlyLearned" },
      })
    })

    it("updates route when Recently Reviewed tab is clicked", async () => {
      const wrapper = helper.component(RecentPage).mount()

      const tab = wrapper
        .findAll(".daisy-tab")
        .find((el) => el.text() === "Recently Reviewed")
      await tab?.trigger("click")

      expect(mockPush).toHaveBeenCalledWith({
        name: "recent",
        query: { tab: "recentlyReviewed" },
      })
    })
  })

  describe("Component Loading", () => {
    it("passes correct props to ContainerPage", () => {
      const wrapper = helper.component(RecentPage).mount()

      const containerPage = wrapper.findComponent({ name: "ContainerPage" })
      expect(containerPage.props("contentLoaded")).toBe(true)
      expect(containerPage.props("title")).toBe("Recent")
    })
  })
})
