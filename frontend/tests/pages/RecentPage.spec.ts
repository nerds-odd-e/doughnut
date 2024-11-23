import { describe, it, expect } from "vitest"
import RecentPage from "./RecentPage.vue"
import helper from "@tests/helpers"

describe("RecentPage.vue", () => {
  describe("Tab Navigation", () => {
    it("shows Recently Added/Updated tab by default", async () => {
      const wrapper = helper.component(RecentPage).mount()

      const activeTab = wrapper.find(".nav-link.active")
      expect(activeTab.text()).toBe("Recently Added/Updated")
      expect(
        wrapper.findComponent({ name: "RecentlyAddedNotes" }).exists()
      ).toBe(true)
    })

    it("switches to Recently Learned tab when clicked", async () => {
      const wrapper = helper.component(RecentPage).mount()

      await wrapper
        .find('a[href="#"]:contains("Recently Learned")')
        .trigger("click")

      const activeTab = wrapper.find(".nav-link.active")
      expect(activeTab.text()).toBe("Recently Learned")
      expect(
        wrapper.findComponent({ name: "RecentlyLearnedNotes" }).exists()
      ).toBe(true)
    })

    it("switches to Due for Review tab when clicked", async () => {
      const wrapper = helper.component(RecentPage).mount()

      await wrapper
        .find('a[href="#"]:contains("Due for Review")')
        .trigger("click")

      const activeTab = wrapper.find(".nav-link.active")
      expect(activeTab.text()).toBe("Due for Review")
      expect(
        wrapper.findComponent({ name: "DueForReviewNotes" }).exists()
      ).toBe(true)
    })
  })

  describe("Component Loading", () => {
    it("passes correct props to ContainerPage", () => {
      const wrapper = helper.component(RecentPage).mount()

      const containerPage = wrapper.findComponent({ name: "ContainerPage" })
      expect(containerPage.props("contentLoaded")).toBe(true)
      expect(containerPage.props("title")).toBe("Recent Notes")
    })
  })
})
