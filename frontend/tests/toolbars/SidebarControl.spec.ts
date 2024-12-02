import SidebarControl from "@/components/toolbars/SidebarControl.vue"
import type { User } from "@/generated/backend/models/User"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import timezoneParam from "@/managedApi/window/timezoneParam"

const useRouteValue = { name: "" }
vitest.mock("vue-router", () => ({
  useRoute: () => useRouteValue,
}))

describe("sidebar control", () => {
  let user: User

  beforeEach(() => {
    user = makeMe.aUser.please()
  })

  it("shows assimilate link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()

    const assimilateLink = screen.getByRole("button", { name: "Assimilate" })
    expect(assimilateLink).toBeInTheDocument()
  })

  it("highlights the note link when on notebooks page", () => {
    useRouteValue.name = "notebooks"

    helper.component(SidebarControl).withProps({ user }).render()

    const noteLink = screen.getByRole("button", { name: "Note" })
    expect(noteLink).toHaveClass("active")
  })

  it("shows note link in sidebar", async () => {
    helper.component(SidebarControl).withProps({ user }).render()

    const noteLink = screen.getByRole("button", { name: "Note" })
    expect(noteLink).toBeInTheDocument()
  })

  it("highlights the circles link when on circle show page", () => {
    useRouteValue.name = "circleShow"

    helper.component(SidebarControl).withProps({ user }).render()

    const circlesLink = screen.getByRole("button", { name: "Circles" })
    expect(circlesLink).toHaveClass("active")
  })

  it("shows assimilate link in both sidebar and dropdown menu", async () => {
    helper.component(SidebarControl).withProps({ user }).render()

    const sidebarAssimilateLink = screen.getByRole("button", {
      name: "Assimilate",
    })

    expect(sidebarAssimilateLink).toBeInTheDocument()
  })

  describe("assimilate due count", () => {
    it("shows due count when there are due items", async () => {
      helper.managedApi.assimilationController.getAssimilationCount = vitest
        .fn()
        .mockResolvedValue({ dueCount: 5 })

      helper.component(SidebarControl).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.getByText("5")
      expect(dueCount).toBeInTheDocument()
      expect(dueCount).toHaveClass("due-count")
    })

    it("does not show due count when there are no due items", async () => {
      helper.managedApi.assimilationController.getAssimilationCount = vitest
        .fn()
        .mockResolvedValue({ dueCount: 0 })

      helper.component(SidebarControl).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.queryByText("0")
      expect(dueCount).not.toBeInTheDocument()
    })

    it("fetches due count when user changes", async () => {
      const mockGetCount = vitest.fn().mockResolvedValue({ dueCount: 3 })
      helper.managedApi.assimilationController.getAssimilationCount =
        mockGetCount

      const { rerender } = helper
        .component(SidebarControl)
        .withProps({ user })
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(mockGetCount).toHaveBeenCalledTimes(2)
    })

    it("calls getAssimilationCount with the correct timezone", async () => {
      const mockGetCount = vitest.fn().mockResolvedValue({ dueCount: 3 })
      helper.managedApi.assimilationController.getAssimilationCount =
        mockGetCount

      helper.component(SidebarControl).withProps({ user }).render()
      await flushPromises()

      expect(mockGetCount).toHaveBeenCalledWith(timezoneParam())
    })
  })
})
