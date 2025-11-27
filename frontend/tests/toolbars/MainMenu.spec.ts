import MainMenu from "@/components/toolbars/MainMenu.vue"
import type { User } from "@generated/backend"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { beforeEach, vi } from "vitest"

const useRouteValue = { name: "" }
vitest.mock("vue-router", () => ({
  useRoute: () => useRouteValue,
}))

describe("main menu", () => {
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    // Default mocks for all tests (can be overridden in individual tests)
    mockSdkService("getAssimilationCount", {
      dueCount: 0,
      assimilatedCountOfTheDay: 0,
      totalUnassimilatedCount: 0,
    })
    mockSdkService("overview", {
      toRepeatCount: 0,
      recallWindowEndAt: "",
      totalAssimilatedCount: 0,
    })
    mockSdkService("getUnreadConversations", [])
    user = makeMe.aUser.please()
  })

  it("shows assimilate link in main menu", async () => {
    helper.component(MainMenu).withProps({ user }).render()

    const assimilateLink = screen.getByRole("button", { name: "Assimilate" })
    expect(assimilateLink).toBeInTheDocument()
  })

  it("highlights the note link when on notebooks page", () => {
    useRouteValue.name = "notebooks"

    helper.component(MainMenu).withProps({ user }).render()

    const noteLink = screen.getByRole("button", { name: "Note" })
    expect(noteLink.querySelector(".daisy-text-primary")).toBeInTheDocument()
  })

  it("shows note link in main menu", async () => {
    helper.component(MainMenu).withProps({ user }).render()

    const noteLink = screen.getByRole("button", { name: "Note" })
    expect(noteLink).toBeInTheDocument()
  })

  it("highlights the circles link when on circle show page", () => {
    useRouteValue.name = "circleShow"

    helper.component(MainMenu).withProps({ user }).render()

    const circlesLink = screen.getByRole("button", { name: "Circles" })
    expect(circlesLink.querySelector(".daisy-text-primary")).toBeInTheDocument()
  })

  it("shows assimilate link in both main menu and dropdown menu", async () => {
    helper.component(MainMenu).withProps({ user }).render()

    const mainMenuAssimilateLink = screen.getByRole("button", {
      name: "Assimilate",
    })

    expect(mainMenuAssimilateLink).toBeInTheDocument()
  })

  describe("assimilate due count", () => {
    it("shows due count when there are due items", async () => {
      mockSdkService("getAssimilationCount", {
        dueCount: 5,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("overview", {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.getByText("5")
      expect(dueCount).toBeInTheDocument()
      expect(dueCount).toHaveClass("due-count")
    })

    it("does not show due count when there are no due items", async () => {
      mockSdkService("getAssimilationCount", {
        dueCount: 0,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("overview", {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.queryByText("0")
      expect(dueCount).not.toBeInTheDocument()
    })

    it("fetches due count when user changes", async () => {
      const getAssimilationCountSpy = mockSdkService("getAssimilationCount", {
        dueCount: 3,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("overview", {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      const { rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(getAssimilationCountSpy).toHaveBeenCalledTimes(2)
    })

    it("calls getAssimilationCount with the correct timezone", async () => {
      const getAssimilationCountSpy = mockSdkService("getAssimilationCount", {
        dueCount: 3,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("overview", {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      expect(getAssimilationCountSpy).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
        client: expect.anything(),
      })
    })
  })

  describe("recall count", () => {
    it("shows recall count when there are items to repeat", async () => {
      mockSdkService("overview", {
        toRepeatCount: 789,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getAssimilationCount", {
        dueCount: 0,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const recallCount = screen.getByText("789")
      expect(recallCount).toBeInTheDocument()
      expect(recallCount).toHaveClass("recall-count")
    })

    it("does not show recall count when there are no items to repeat", async () => {
      mockSdkService("overview", {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getAssimilationCount", {
        dueCount: 0,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const recallCount = screen.queryByText("0")
      expect(recallCount).not.toBeInTheDocument()
    })

    it("fetches recall count when user changes", async () => {
      const overviewSpy = mockSdkService("overview", {
        toRepeatCount: 3,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      })

      mockSdkService("getAssimilationCount", {
        dueCount: 0,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      })

      mockSdkService("getUnreadConversations", [])

      const { rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(overviewSpy).toHaveBeenCalledTimes(2)
      expect(overviewSpy).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
        client: expect.anything(),
      })
    })
  })
})
