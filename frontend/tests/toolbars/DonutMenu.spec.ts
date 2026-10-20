import {
  AssimilationController,
  UserController,
} from "@generated/donut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import { logout } from "@/managedApi/window/logout"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, it, expect, vi } from "vitest"
import { userEvent } from "vitest/browser"
import type { RouteLocationRaw } from "vue-router"
import { createMenuData, memoryTrackerLitesStub } from "./menuDataMocks"
import {
  expectPanelClosed,
  launcher,
  navItemOf,
  openDonutMenu,
  panelItemLabels,
  pressLauncher,
  router,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

vi.mock("@/managedApi/window/logout", () => ({ logout: vi.fn() }))

setupDonutMenuTests()

describe("Donut menu panel", () => {
  it("lists the destinations in order with their counts, the account group, and the attribution", async () => {
    mockSdkService(
      UserController,
      "getMenuData",
      createMenuData({
        assimilationCount: {
          dueCount: 3,
          assimilatedCountOfTheDay: 0,
          totalUnassimilatedCount: 128,
        },
        recallStatus: {
          toRepeat: memoryTrackerLitesStub(12),
          currentRecallWindowEndAt: "",
          totalAssimilatedCount: 0,
        },
        unreadMessages: [
          { id: 1, message: "first" },
          { id: 2, message: "second" },
        ],
      })
    )

    await openDonutMenu()

    expect(panelItemLabels()).toEqual([
      "Note",
      "Assimilate",
      "Recall",
      "Circles",
      "Bazaar",
      "Messages",
      "Account",
    ])
    expect(
      navItemOf("Assimilate")?.querySelector(".due-count")
    ).toHaveAttribute("title", "3 due today, 128 total unassimilated")
    expect(
      navItemOf("Assimilate")?.querySelector(".due-count")
    ).toHaveTextContent("3")
    expect(
      navItemOf("Recall")?.querySelector(".recall-count")
    ).toHaveTextContent("12")
    expect(
      navItemOf("Messages")?.querySelector(".unread-count")
    ).toHaveTextContent("2")
    expect(screen.getByText("Donut by")).toBeInTheDocument()
  })

  it.each([
    { linkLabel: "Note", route: { name: "notebooks" } },
    {
      linkLabel: "Note",
      route: { name: "notebookPage", params: { notebookId: "1" } },
    },
    {
      linkLabel: "Note",
      route: { name: "folderPage", params: { notebookId: "1", folderId: "2" } },
    },
    {
      linkLabel: "Note",
      route: { name: "bookReading", params: { notebookId: "1" } },
    },
    { linkLabel: "Note", route: { name: "noteShow", params: { noteId: "1" } } },
    {
      linkLabel: "Circles",
      route: { name: "circleShow", params: { circleId: "1" } },
    },
    { linkLabel: "Messages", route: { name: "messageCenter" } },
  ])("marks $linkLabel active on $route.name", async ({ linkLabel, route }) => {
    await router.push(route as RouteLocationRaw)
    await flushPromises()
    await openDonutMenu()
    expect(navItemOf(linkLabel)).toHaveClass("text-primary")
  })

  it("goes to the next note to assimilate and closes", async () => {
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: 7 },
    })
    await openDonutMenu()

    await fireEvent.click(screen.getByLabelText("Assimilate"))
    await flushPromises()

    expect(router.currentRoute.value).toMatchObject(noteShowLocation(7))
    expectPanelClosed()
  })

  it("lists Resume first while recall is paused and resumes recall when chosen", async () => {
    const { setIsRecallPaused, setToRepeat } = useRecallData()
    setIsRecallPaused(true)
    setToRepeat(memoryTrackerLitesStub(5))
    await openDonutMenu()

    expect(panelItemLabels()[0]).toBe("Resume")
    expect(navItemOf("Resume")).toHaveClass("resume-recall-active")

    await fireEvent.click(screen.getByLabelText("Resume"))
    await flushPromises()

    expect(router.currentRoute.value.name).toBe("recall")
    expectPanelClosed()
  })

  describe("account group", () => {
    it("offers the account actions for the user", async () => {
      await openDonutMenu(makeMe.aUser.name("Ada").please())
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()

      for (const action of [
        "Recent...",
        "Settings for Ada",
        "Manage Access Tokens",
        "Logout",
      ]) {
        expect(screen.getByText(action)).toBeInTheDocument()
      }
      expect(screen.queryByText("Admin Dashboard")).toBeNull()
    })

    it("offers Admin Dashboard to an admin", async () => {
      await openDonutMenu(makeMe.aUser.admin(true).please())
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()

      expect(screen.getByText("Admin Dashboard")).toBeInTheDocument()
    })

    it("goes to an account destination and closes", async () => {
      await openDonutMenu()
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()
      await fireEvent.click(screen.getByText("Manage Access Tokens"))
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("settingsAccessTokens")
      expectPanelClosed()
    })

    it("closes when the account destination already shown is chosen", async () => {
      await router.push({ name: "settingsAccessTokens" })
      await flushPromises()
      await openDonutMenu()
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()
      await fireEvent.click(screen.getByText("Manage Access Tokens"))
      await flushPromises()

      expectPanelClosed()
    })

    it("logs out and closes when Logout is chosen", async () => {
      await openDonutMenu()
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()
      await fireEvent.click(screen.getByTitle("Logout"))
      await flushPromises()

      expect(logout).toHaveBeenCalledOnce()
      expectPanelClosed()
    })
  })

  describe("closing", () => {
    it("closes when a destination is chosen", async () => {
      await openDonutMenu()
      await fireEvent.click(screen.getByLabelText("Bazaar"))
      await flushPromises()

      expect(router.currentRoute.value.name).toBe("bazaar")
      expectPanelClosed()
    })

    it("closes when the launcher is pressed again", async () => {
      await openDonutMenu()
      await pressLauncher()
      expectPanelClosed()
    })

    it("closes on an outside click", async () => {
      await openDonutMenu()
      await fireEvent.click(document.body)
      await flushPromises()
      expectPanelClosed()
    })

    it("closes on Escape", async () => {
      await openDonutMenu()
      await userEvent.keyboard("{Escape}")
      await flushPromises()
      expectPanelClosed()
    })

    it("closes on a route change", async () => {
      await openDonutMenu()
      await router.push({ name: "recall" })
      await flushPromises()
      expectPanelClosed()
    })
  })

  it("is reachable by keyboard: the launcher is a focusable button and items follow in order", async () => {
    await openDonutMenu()
    launcher().focus()
    expect(document.activeElement).toBe(launcher())

    const reached: (string | null)[] = []
    for (const _ of panelItemLabels()) {
      await userEvent.tab()
      reached.push(document.activeElement!.getAttribute("aria-label"))
    }
    expect(reached).toEqual(panelItemLabels())
  })
})
