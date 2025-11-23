import MainMenu from "@/components/toolbars/MainMenu.vue"
import type { User } from "@generated/backend"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import timezoneParam from "@/managedApi/window/timezoneParam"
import * as sdk from "@generated/backend/sdk.gen"
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
    vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
      data: {
        dueCount: 0,
        assimilatedCountOfTheDay: 0,
        totalUnassimilatedCount: 0,
      },
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "overview").mockResolvedValue({
      data: {
        toRepeatCount: 0,
        recallWindowEndAt: "",
        totalAssimilatedCount: 0,
      },
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
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
      vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
        data: {
          dueCount: 5,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 0,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.getByText("5")
      expect(dueCount).toBeInTheDocument()
      expect(dueCount).toHaveClass("due-count")
    })

    it("does not show due count when there are no due items", async () => {
      vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
        data: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 0,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const dueCount = screen.queryByText("0")
      expect(dueCount).not.toBeInTheDocument()
    })

    it("fetches due count when user changes", async () => {
      const mockGetCount = vitest.fn().mockResolvedValue({
        data: {
          dueCount: 3,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "getAssimilationCount").mockImplementation(mockGetCount)

      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 0,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const { rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(mockGetCount).toHaveBeenCalledTimes(2)
    })

    it("calls getAssimilationCount with the correct timezone", async () => {
      const mockGetCount = vitest.fn().mockResolvedValue({
        data: {
          dueCount: 3,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "getAssimilationCount").mockImplementation(mockGetCount)

      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 0,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      expect(mockGetCount).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
      })
    })
  })

  describe("recall count", () => {
    it("shows recall count when there are items to repeat", async () => {
      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 789,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
        data: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const recallCount = screen.getByText("789")
      expect(recallCount).toBeInTheDocument()
      expect(recallCount).toHaveClass("recall-count")
    })

    it("does not show recall count when there are no items to repeat", async () => {
      vi.spyOn(sdk, "overview").mockResolvedValue({
        data: {
          toRepeatCount: 0,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
        data: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      helper.component(MainMenu).withProps({ user }).render()
      await flushPromises()

      const recallCount = screen.queryByText("0")
      expect(recallCount).not.toBeInTheDocument()
    })

    it("fetches recall count when user changes", async () => {
      const mockGetOverview = vitest.fn().mockResolvedValue({
        data: {
          toRepeatCount: 3,
          recallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })
      vi.spyOn(sdk, "overview").mockImplementation(mockGetOverview)

      vi.spyOn(sdk, "getAssimilationCount").mockResolvedValue({
        data: {
          dueCount: 0,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 0,
        },
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      vi.spyOn(sdk, "getUnreadConversations").mockResolvedValue({
        data: [],
        error: undefined,
        request: {} as Request,
        response: {} as Response,
      })

      const { rerender } = helper
        .component(MainMenu)
        .withProps({ user })
        .render()
      await flushPromises()

      const newUser = { ...user, id: 2 }
      await rerender({ user: newUser })
      await flushPromises()

      expect(mockGetOverview).toHaveBeenCalledTimes(2)
      expect(mockGetOverview).toHaveBeenCalledWith({
        query: { timezone: timezoneParam() },
      })
    })
  })
})
